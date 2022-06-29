package com.vibrent.aftership.service.impl;

import com.aftership.sdk.model.tracking.Tracking;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.vibrent.aftership.converter.TrackDeliveryResponseConverter;
import com.vibrent.aftership.domain.TrackingRequest;
import com.vibrent.aftership.dto.NotificationDTO;
import com.vibrent.aftership.enums.CarrierResponseType;
import com.vibrent.aftership.messaging.producer.impl.TrackingResponseProducer;
import com.vibrent.aftership.repository.TrackingRequestRepository;
import com.vibrent.aftership.service.NotificationProcessService;
import com.vibrent.aftership.util.JacksonUtil;
import com.vibrent.vxp.workflow.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

import static com.vibrent.aftership.constants.AfterShipConstants.TAG_EXCEPTION;

@Slf4j
@Service
public class NotificationProcessServiceImpl implements NotificationProcessService {

    private final TrackDeliveryResponseConverter trackDeliveryResponseConverter;
    private final TrackingResponseProducer trackingResponseProducer;
    private final TrackingRequestRepository trackingRequestRepository;
    private List<String> exceptionSubStatus;

    public NotificationProcessServiceImpl(TrackDeliveryResponseConverter trackDeliveryResponseConverter,
                                          TrackingResponseProducer trackingResponseProducer,
                                          TrackingRequestRepository trackingRequestRepository,
                                          @NotNull @Value("${afterShip.exceptionSubStatus}") List<String> exceptionSubStatus) {
        this.trackDeliveryResponseConverter = trackDeliveryResponseConverter;
        this.trackingResponseProducer = trackingResponseProducer;
        this.trackingRequestRepository = trackingRequestRepository;
        this.exceptionSubStatus = exceptionSubStatus;
    }

    @Override
    public void process(NotificationDTO notificationDTO) {
        String carrierResponse = null;
        if (notificationDTO == null || notificationDTO.getMsg() == null) {
            log.warn("AfterShip: Cannot process the notification as received notificationDTO is null or notificationDTO.getMsg() is null");
            return;
        }

        try {
            Optional<TrackingRequest> optionalTrackingRequest = this.trackingRequestRepository.findByTrackingId(notificationDTO.getMsg().getTrackingNumber());
            TrackingRequest trackingRequest = optionalTrackingRequest.orElse(null);

            if (trackingRequest == null) {
                log.warn("AfterShip: Could not find the saved tracking request in DB for tracking number: " + notificationDTO.getMsg().getTrackingNumber());
                return;
            }

            TrackDeliveryResponseDto trackDeliveryResponseDto = this.trackDeliveryResponseConverter.convert(notificationDTO.getMsg(), trackingRequest);

            sendTrackDeliveryResponse(notificationDTO.getMsg(), trackingRequest, trackDeliveryResponseDto);

            //Update tracking request
            carrierResponse = getCarrierResponseAsString(notificationDTO);
            updateTrackingRequest(trackingRequest, notificationDTO.getMsg(), carrierResponse, CarrierResponseType.NOTIFICATION.toString());
        } catch (Exception e) {
            log.warn("AfterShip: Error while processing notificationDTO", e);
        }
    }

    @Override
    public void process(@NotNull Tracking tracking, @NotNull TrackingRequest trackingRequest) {
        String carrierResponse = null;
        if (tracking == null || trackingRequest == null) {
            log.warn("AfterShip: Cannot process the tracking as trackingRequest or tracking received as null ");
            return;
        }
        try {
            String newStatus = tracking.getTag();
            if (StringUtils.hasText(trackingRequest.getStatus()) && StringUtils.hasText(newStatus) && trackingRequest.getStatus().equals(newStatus)) {
                log.info("AfterShip: Received same status in the get call.");
                return;
            }

            TrackDeliveryResponseDto trackDeliveryResponseDto = this.trackDeliveryResponseConverter.convert(tracking, trackingRequest);
            sendTrackDeliveryResponse(tracking, trackingRequest, trackDeliveryResponseDto);
            //Update tracking request
            carrierResponse = getCarrierResponseAsString(tracking);
            updateTrackingRequest(trackingRequest, tracking, carrierResponse, CarrierResponseType.TRACKING.toString());

        } catch (Exception e) {
            log.warn("AfterShip: Error while processing tracking", e);
        }
    }

    private String getCarrierResponseAsString(Tracking tracking) {
        String carrierResponse = null;
        try {
            carrierResponse = JacksonUtil.getMapper().writeValueAsString(tracking);
        } catch (JsonProcessingException e) {
            log.warn("AfterShip: Error while writing notificationDTO object as string value", e);
        }
        return carrierResponse;
    }

    private String getCarrierResponseAsString(NotificationDTO notificationDTO) {
        String carrierResponse = null;
        try {
            carrierResponse = JacksonUtil.getMapper().writeValueAsString(notificationDTO);
        } catch (JsonProcessingException e) {
            log.warn("AfterShip: Error while writing notificationDTO object as string value", e);
        }
        return carrierResponse;
    }

    private void sendTrackDeliveryResponse(Tracking tracking, TrackingRequest trackingRequest, TrackDeliveryResponseDto trackDeliveryResponseDto) {
        //Sending event only if the status is IN_TRANSIT, DELIVERED, ERROR
        if (trackDeliveryResponseDto.getStatus() == StatusEnum.IN_TRANSIT ||
                trackDeliveryResponseDto.getStatus() == StatusEnum.DELIVERED ||
                trackDeliveryResponseDto.getStatus() == StatusEnum.ERROR) {
            String newStatus = tracking.getTag();
            if (StringUtils.hasText(trackingRequest.getStatus()) && StringUtils.hasText(newStatus) && trackingRequest.getStatus().equals(newStatus)) {
                log.info("AfterShip: Received same status in the notification. No need to send tracking response.");
                return;
            }
            String subtagMessage = tracking.getSubtag();
            if (TAG_EXCEPTION.equalsIgnoreCase(newStatus)) {
                log.info("AfterShip: Received tracking request with status Exception subTag {}.  participantId: {} ", subtagMessage, Optional.ofNullable(trackDeliveryResponseDto.getParticipant()).map(ParticipantDto::getVibrentID).orElse(null));
            }

            if (StringUtils.hasText(subtagMessage) && exceptionSubStatus.contains(subtagMessage)) {
                log.info("AfterShip: Track Delivery Response not sent as received exception subTag {}.  participantId: {}", subtagMessage, Optional.ofNullable(trackDeliveryResponseDto.getParticipant()).map(ParticipantDto::getVibrentID).orElse(null));
                return;
            }

            MessageHeaderDto messageHeaderDto = this.trackDeliveryResponseConverter.populateMessageHeaderDTO(tracking, trackingRequest);
            this.trackingResponseProducer.send(new TrackDeliveryResponseDtoWrapper(trackDeliveryResponseDto, messageHeaderDto));
        } else {
            log.info("AfterShip: Received status other than InTransit, Delivered or Exception from AfterShip. Received tag: {}", tracking.getTag());
        }
    }

    private void updateTrackingRequest(TrackingRequest trackingRequest, Tracking tracking, String carrierResponse, String carrierResponseType) {
        trackingRequest.setCarrierResponse(carrierResponse);
        trackingRequest.setStatus(tracking.getTag());
        trackingRequest.setSubStatusCode(tracking.getSubtag());
        trackingRequest.setSubStatusDescription(tracking.getSubtagMessage());
        trackingRequest.setCarrierResponseType(carrierResponseType);
        this.trackingRequestRepository.save(trackingRequest);
    }

}
