package com.vibrent.aftership.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vibrent.aftership.domain.TrackingRequest;
import com.vibrent.aftership.util.JacksonUtil;
import com.vibrent.vxp.workflow.MessageHeaderDto;
import com.vibrent.vxp.workflow.StatusEnum;
import com.vibrent.vxp.workflow.TrackDeliveryRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TrackingRequestConverter {

    public TrackingRequest toTrackingRequest(TrackDeliveryRequestDto trackDeliveryRequestDto, MessageHeaderDto messageHeader) {
        if (trackDeliveryRequestDto == null || messageHeader == null) {
            log.warn("AfterShip: Null trackDeliveryRequestDto or messageHeader is provided to converter");
            return null;
        }
        TrackingRequest trackingRequest = new TrackingRequest();
        trackingRequest.setTrackingId(trackDeliveryRequestDto.getTrackingID());
        trackingRequest.setOperation(trackDeliveryRequestDto.getOperation());
        trackingRequest.setProvider(trackDeliveryRequestDto.getProvider());
        trackingRequest.setStatus(StatusEnum.PENDING_TRACKING.toValue());
        trackingRequest.setParticipant(getParticipant(trackDeliveryRequestDto));
        trackingRequest.setHeader(getMessageHeaders(messageHeader));
        return trackingRequest;
    }

    private String getParticipant(TrackDeliveryRequestDto trackDeliveryRequestDto) {
        try {
            return JacksonUtil.getMapper().writeValueAsString(trackDeliveryRequestDto.getParticipant());
        } catch (JsonProcessingException e) {
            log.warn("AfterShip: Error while writing participant DTO as string value", e);
        }
        return null;
    }

    private String getMessageHeaders(MessageHeaderDto messageHeader) {
        try {
            return JacksonUtil.getMapper().writeValueAsString(messageHeader);
        } catch (JsonProcessingException e) {
            log.warn("AfterShip: Error while writing messageHeader DTO as string value", e);
        }
        return null;
    }
}
