package com.vibrent.aftership.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vibrent.aftership.domain.TrackingRequest;
import com.vibrent.aftership.util.JacksonUtil;
import com.vibrent.aftership.vo.TrackDeliveryRequestVo;
import com.vibrent.vxp.workflow.MessageHeaderDto;
import com.vibrent.vxp.workflow.OperationEnum;
import com.vibrent.vxp.workflow.ProviderEnum;
import com.vibrent.vxp.workflow.StatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TrackingRequestConverter {

    public TrackingRequest toTrackingRequest(TrackDeliveryRequestVo trackDeliveryRequestVo, MessageHeaderDto messageHeader) {
        if (trackDeliveryRequestVo == null || messageHeader == null) {
            log.warn("AfterShip: Null trackDeliveryRequestDto or messageHeader is provided to converter");
            return null;
        }
        TrackingRequest trackingRequest = new TrackingRequest();
        trackingRequest.setTrackingId(trackDeliveryRequestVo.getTrackingID());
        trackingRequest.setFulfillmentOrderID(trackDeliveryRequestVo.getFulfillmentOrderID());
        trackingRequest.setOperation(OperationEnum.TRACK_DELIVERY);
        trackingRequest.setProvider(trackDeliveryRequestVo.getCarrierCode());
        trackingRequest.setStatus(StatusEnum.PENDING_TRACKING.toValue());
        trackingRequest.setParticipant(getParticipant(trackDeliveryRequestVo));
        trackingRequest.setHeader(getMessageHeaders(messageHeader));
        return trackingRequest;
    }

    private String getParticipant(TrackDeliveryRequestVo trackDeliveryRequestVo) {
        try {
            return JacksonUtil.getMapper().writeValueAsString(trackDeliveryRequestVo.getParticipant());
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
