package com.vibrent.aftership.service.impl;

import com.aftership.sdk.model.tracking.NewTracking;
import com.aftership.sdk.model.tracking.Tracking;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.vibrent.aftership.converter.TrackingRequestConverter;
import com.vibrent.aftership.converter.TrackingRequestErrorConverter;
import com.vibrent.aftership.domain.TrackingRequest;
import com.vibrent.aftership.domain.TrackingRequestError;
import com.vibrent.aftership.exception.AfterShipException;
import com.vibrent.aftership.repository.TrackingRequestErrorRepository;
import com.vibrent.aftership.repository.TrackingRequestRepository;
import com.vibrent.aftership.service.AfterShipTrackingService;
import com.vibrent.aftership.service.ExternalLogService;
import com.vibrent.aftership.service.TrackingRequestService;
import com.vibrent.aftership.util.JacksonUtil;
import com.vibrent.aftership.vo.TrackDeliveryRequestVo;
import com.vibrent.vxp.workflow.MessageHeaderDto;
import com.vibrent.vxp.workflow.ParticipantDetailsDto;
import com.vibrenthealth.resiliency.core.Output;
import com.vibrenthealth.resiliency.core.RetryModesEnum;
import com.vibrenthealth.resiliency.core.RockSteadySystem;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Optional;
import java.util.function.Supplier;

@Service
@Slf4j
public class TrackingRequestServiceImpl implements TrackingRequestService {

    public static final String CUSTOM_FIELD_EXTERNAL_ID = "externalId";
    public static final String CUSTOM_FIELD_VIBRENT_ID = "vibrentId";

    private final AfterShipTrackingService afterShipTrackingService;
    private final TrackingRequestConverter trackingRequestConverter;
    private final TrackingRequestRepository trackingRequestRepository;
    private final RockSteadySystem rockSteadySystem;
    private final TrackingRequestErrorRepository trackingRequestErrorRepository;
    private final ExternalLogService externalLogService;
    private final TrackingRequestErrorConverter trackingRequestErrorConverter;

    public TrackingRequestServiceImpl(AfterShipTrackingService afterShipTrackingService, TrackingRequestConverter trackingRequestConverter,
                                      TrackingRequestRepository trackingRequestRepository, RockSteadySystem rockSteadySystem,
                                      TrackingRequestErrorRepository trackingRequestErrorRepository, ExternalLogService externalLogService,
                                      TrackingRequestErrorConverter trackingRequestErrorConverter) {
        this.afterShipTrackingService = afterShipTrackingService;
        this.trackingRequestConverter = trackingRequestConverter;
        this.trackingRequestRepository = trackingRequestRepository;
        this.rockSteadySystem = rockSteadySystem;
        this.trackingRequestErrorRepository = trackingRequestErrorRepository;
        this.externalLogService = externalLogService;
        this.trackingRequestErrorConverter = trackingRequestErrorConverter;
    }

    @Override
    @Transactional
    public boolean createTrackDeliveryRequest(TrackDeliveryRequestVo trackDeliveryRequestVo, MessageHeaderDto messageHeader) {
        log.debug("TrackDeliveryRequest = {}, headers = {}", trackDeliveryRequestVo, messageHeader);

        if (trackDeliveryRequestVo == null || messageHeader == null) {
            log.warn("afterShip: Null TrackDeliveryRequestDto or messageHeader object received.");
            return false;
        }

        Optional<TrackingRequest> optionalTrackingRequest = this.trackingRequestRepository.findByTrackingId(trackDeliveryRequestVo.getTrackingID());
        if (optionalTrackingRequest.isPresent()) {
            log.warn("AfterShip: Received duplicate tracking request for tracking ID: {}, Ignoring the track delivery message: {}",
                    trackDeliveryRequestVo.getTrackingID(), trackDeliveryRequestVo);
            return false;
        }

        NewTracking newTracking = new NewTracking();
        newTracking.setTrackingNumber(trackDeliveryRequestVo.getTrackingID());
        newTracking.setCustomFields(getCustomFields(trackDeliveryRequestVo));
        Tracking tracking = createTracking(() -> afterShipTrackingService.createTracking(newTracking), newTracking, trackDeliveryRequestVo, messageHeader);
        boolean isSuccess = tracking != null;
        if (isSuccess) {
            saveTrackingRequest(trackDeliveryRequestVo, messageHeader);
        }
        return isSuccess;

    }

    private static HashMap<String, String> getCustomFields(TrackDeliveryRequestVo trackDeliveryRequestVo) {
        HashMap<String, String> customFields = new HashMap<>();
        ParticipantDetailsDto participant = trackDeliveryRequestVo.getParticipant();

        if (participant != null) {
            customFields.put(CUSTOM_FIELD_VIBRENT_ID, String.valueOf(participant.getVibrentID()));
            customFields.put(CUSTOM_FIELD_EXTERNAL_ID, participant.getExternalID());
        }

        return customFields;
    }

    private void saveTrackingRequest(TrackDeliveryRequestVo trackDeliveryRequestVo, MessageHeaderDto messageHeader) {
        TrackingRequest trackingRequest = this.trackingRequestConverter.toTrackingRequest(trackDeliveryRequestVo, messageHeader);
        this.trackingRequestRepository.save(trackingRequest);
    }

    private Tracking createTracking(@NonNull Supplier<Tracking> createTracking, NewTracking newTracking,
                                    TrackDeliveryRequestVo trackDeliveryRequestVo, MessageHeaderDto messageHeaderDto) {
        log.info("AfterShip: Received request to create new tracking {}", newTracking);
        Long requestTimeStamp = System.currentTimeMillis();
        Output<Tracking> outputFromAfterShip = rockSteadySystem.executeWithRetries(createTracking, MDC.getCopyOfContextMap(), RetryModesEnum.IMMEDIATE);
        Long responseTimeStamp = System.currentTimeMillis();

        if (outputFromAfterShip.result != null) {
            log.info("AfterShip: Successfully created trackingRequest {}", newTracking);
            this.externalLogService.send(trackDeliveryRequestVo, System.currentTimeMillis(),
                    HttpStatus.OK.value(), newTracking, requestTimeStamp, getResponseBody(outputFromAfterShip.result),
                    "AfterShip | Successfully sent the Create Tracking request to AfterShip");
            return outputFromAfterShip.result;
        } else {
            String message = outputFromAfterShip.error == null ? "NA" : outputFromAfterShip.error.getMessage();
            log.warn("AfterShip: Failed to create new trackingRequest, cause - {}, trackingRequest: {}", message, newTracking);
            saveTrackingRequestError(trackDeliveryRequestVo, messageHeaderDto, outputFromAfterShip.error);
            //Sending event in case of AfterShipException. For other CircuitBreaker exception like CallNotPermitted not sending the event
            //as we are not communicating with AfterShip cloud service
            if (outputFromAfterShip.error instanceof AfterShipException) {
                this.externalLogService.send(trackDeliveryRequestVo, responseTimeStamp,
                        getErrorCode(outputFromAfterShip), newTracking, requestTimeStamp, outputFromAfterShip.error.getMessage(),
                        "AfterShip | Failed to sent the Create Tracking request to AfterShip");
            }
            return null;
        }
    }

    private void saveTrackingRequestError(TrackDeliveryRequestVo trackDeliveryRequestVo, MessageHeaderDto messageHeaderDto,
                                          Throwable throwable) {
        TrackingRequestError trackingRequestError = this.trackingRequestErrorConverter.toTrackingRequestError(trackDeliveryRequestVo, messageHeaderDto, throwable);
        this.trackingRequestErrorRepository.save(trackingRequestError);
    }

    private static Integer getErrorCode(Output<Tracking> outputFromAfterShip) {
        Integer errorCode = null;

        if (outputFromAfterShip != null && outputFromAfterShip.error instanceof AfterShipException) {
            errorCode = ((AfterShipException) outputFromAfterShip.error).getErrorCode();
        }

        return errorCode;
    }

    private static String getResponseBody(Tracking tracking) {
        try {
            return JacksonUtil.getMapper().writeValueAsString(tracking);
        } catch (JsonProcessingException e) {
            log.warn("AfterShip: Error while writing tracking to response body of externalLogDTO", e);
        }
        return null;
    }
}
