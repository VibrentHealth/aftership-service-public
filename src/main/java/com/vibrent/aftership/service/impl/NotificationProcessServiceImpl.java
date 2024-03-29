package com.vibrent.aftership.service.impl;

import com.aftership.sdk.model.tracking.Tracking;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.vibrent.aftership.converter.FulfillmentTrackDeliveryResponseConverter;
import com.vibrent.aftership.converter.TrackDeliveryResponseConverter;
import com.vibrent.aftership.domain.TrackingRequest;
import com.vibrent.aftership.dto.NotificationDTO;
import com.vibrent.aftership.enums.CarrierResponseType;
import com.vibrent.aftership.messaging.producer.impl.FulfillmentTrackingResponseProducer;
import com.vibrent.aftership.messaging.producer.impl.TrackingResponseProducer;
import com.vibrent.aftership.repository.TrackingRequestRepository;
import com.vibrent.aftership.service.NotificationProcessService;
import com.vibrent.aftership.util.JacksonUtil;
import com.vibrent.vxp.workflow.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.vibrent.aftership.constants.AfterShipConstants.TAG_EXCEPTION;
import static com.vibrent.aftership.service.impl.TrackingRequestServiceImpl.CUSTOM_FIELD_PLATFORM_ID;

@Slf4j
@Service
public class NotificationProcessServiceImpl implements NotificationProcessService {

    private final TrackDeliveryResponseConverter trackDeliveryResponseConverter;
    private final FulfillmentTrackDeliveryResponseConverter fulfillmentTrackDeliveryResponseConverter;
    private final TrackingResponseProducer trackingResponseProducer;
    private final FulfillmentTrackingResponseProducer fulfillmentTrackingResponseProducer;
    private final TrackingRequestRepository trackingRequestRepository;
    private List<String> exceptionSubStatus;
    private final String platform;

    public NotificationProcessServiceImpl(TrackDeliveryResponseConverter trackDeliveryResponseConverter,
                                          FulfillmentTrackDeliveryResponseConverter fulfillmentTrackDeliveryResponseConverter,
                                          TrackingResponseProducer trackingResponseProducer,
                                          FulfillmentTrackingResponseProducer fulfillmentTrackingResponseProducer,
                                          TrackingRequestRepository trackingRequestRepository,
                                          @NotNull @Value("${afterShip.exceptionSubStatus}") List<String> exceptionSubStatus,
                                          @Value("${afterShip.platform}") String platform) {
        this.trackDeliveryResponseConverter = trackDeliveryResponseConverter;
        this.fulfillmentTrackDeliveryResponseConverter = fulfillmentTrackDeliveryResponseConverter;
        this.trackingResponseProducer = trackingResponseProducer;
        this.fulfillmentTrackingResponseProducer = fulfillmentTrackingResponseProducer;
        this.trackingRequestRepository = trackingRequestRepository;
        this.exceptionSubStatus = exceptionSubStatus;
        this.platform = platform;
    }

    @Override
    public void process(NotificationDTO notificationDTO) {
        String carrierResponse = null;
        if (notificationDTO == null || notificationDTO.getMsg() == null) {
            log.warn("AfterShip: Cannot process the notification as received notificationDTO is null or notificationDTO.getMsg() is null");
            return;
        }

        if (!validatePlatform(notificationDTO.getMsg())) {
            return;
        }
        try {

            Optional<TrackingRequest> optionalTrackingRequest = this.trackingRequestRepository.findByTrackingId(notificationDTO.getMsg().getTrackingNumber());
            TrackingRequest trackingRequest = optionalTrackingRequest.orElse(null);

            if (trackingRequest == null) {
                log.info("AfterShip: Could not find the saved tracking request in DB for tracking number: " + notificationDTO.getMsg().getTrackingNumber());
                return;
            }

            if(Objects.isNull(trackingRequest.getFulfillmentOrderID())){
                TrackDeliveryResponseDto trackDeliveryResponseDto = this.trackDeliveryResponseConverter.convert(notificationDTO.getMsg(), trackingRequest);
                sendTrackDeliveryResponse(notificationDTO.getMsg(), trackingRequest, trackDeliveryResponseDto);
            }else {
                FulfillmentTrackDeliveryResponseDto fulfillmentTrackDeliveryResponseDto = this.fulfillmentTrackDeliveryResponseConverter.convert(notificationDTO.getMsg(), trackingRequest);
                sendTrackDeliveryResponse(notificationDTO.getMsg(), trackingRequest, fulfillmentTrackDeliveryResponseDto);
            }

            //Update tracking request
            carrierResponse = getCarrierResponseAsString(notificationDTO);
            updateTrackingRequest(trackingRequest, notificationDTO.getMsg(), carrierResponse, CarrierResponseType.NOTIFICATION.toString());
        } catch (Exception e) {
            log.warn("AfterShip: Error while processing notificationDTO", e);
        }
    }

    /**
     * This method check for the custom field
     * If it exists and does not match with current platform, then ignore the webhook request.
     * Otherwise process as it is
     * @param tracking
     */
    private boolean validatePlatform(Tracking tracking) {
        Map<String, String> customFields = tracking.getCustomFields();
        if (!CollectionUtils.isEmpty(customFields)) {
            String receivedPlatform = customFields.get(CUSTOM_FIELD_PLATFORM_ID);
            if (StringUtils.hasText(receivedPlatform) && !platform.equalsIgnoreCase(receivedPlatform)) {
                log.warn("AfterShip: Ignoring web-hook request due to platform mismatch. Received platform:{}", receivedPlatform);
                return false;
            }
        }
        return true;
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

            if(Objects.isNull(trackingRequest.getFulfillmentOrderID())){
                TrackDeliveryResponseDto trackDeliveryResponseDto = this.trackDeliveryResponseConverter.convert(tracking, trackingRequest);
                sendTrackDeliveryResponse(tracking, trackingRequest, trackDeliveryResponseDto);
            }else {
                FulfillmentTrackDeliveryResponseDto fulfillmentTrackDeliveryResponseDto = this.fulfillmentTrackDeliveryResponseConverter.convert(tracking, trackingRequest);
                sendTrackDeliveryResponse(tracking, trackingRequest, fulfillmentTrackDeliveryResponseDto);
            }

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

            MessageHeaderDto messageHeaderDto = this.trackDeliveryResponseConverter.populateMessageHeaderDTO(trackDeliveryResponseDto.getParticipant(), trackingRequest);
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

    private void sendTrackDeliveryResponse(Tracking tracking, TrackingRequest trackingRequest, FulfillmentTrackDeliveryResponseDto fulfillmentTrackDeliveryResponseDto) {

        if (fulfillmentTrackDeliveryResponseDto.getStatus() != TrackingStatusEnum.UNRECOGNIZE) {
            String newStatus = tracking.getTag();
            String newSubStatus = tracking.getSubtag();
            if (StringUtils.hasText(trackingRequest.getStatus()) && StringUtils.hasText(trackingRequest.getSubStatusCode())
                    && StringUtils.hasText(newStatus) && trackingRequest.getStatus().equals(newStatus)
                    && StringUtils.hasText(newSubStatus) && trackingRequest.getSubStatusCode().equals(newSubStatus)) {
                log.info("AfterShip: Received same status in the notification. No need to send tracking response.");
                return;
            }
            if (TAG_EXCEPTION.equalsIgnoreCase(newStatus)) {
                log.info("AfterShip: Received tracking request with status Exception subTag {}.  participantId: {} ", newSubStatus, Optional.ofNullable(fulfillmentTrackDeliveryResponseDto.getParticipant()).map(ParticipantDetailsDto::getVibrentID).orElse(null));
            }

            MessageHeaderDto messageHeaderDto = this.fulfillmentTrackDeliveryResponseConverter.populateMessageHeaderDTO(fulfillmentTrackDeliveryResponseDto.getParticipant(), trackingRequest);
            this.fulfillmentTrackingResponseProducer.send(new FulfillmentTrackDeliveryResponseDtoWrapper(fulfillmentTrackDeliveryResponseDto, messageHeaderDto));
        } else {
            log.info("AfterShip: Received status other than TrackingStatusEnum from AfterShip. Received tag: {}", tracking.getTag());
        }
    }

}
