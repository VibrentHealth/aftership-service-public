package com.vibrent.aftership.converter;

import com.vibrent.aftership.vo.TrackDeliveryRequestVo;
import com.vibrent.vxp.workflow.FulfillmentTrackDeliveryRequestDto;
import com.vibrent.vxp.workflow.ParticipantDetailsDto;
import com.vibrent.vxp.workflow.ParticipantDto;
import com.vibrent.vxp.workflow.TrackDeliveryRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class TrackDeliveryRequestConverter {

    public TrackDeliveryRequestVo toTrackDeliveryRequestVo(FulfillmentTrackDeliveryRequestDto fulfillmentTrackDeliveryRequestDto) {
        if (fulfillmentTrackDeliveryRequestDto == null) {
            log.warn("AfterShip: Null fulfillmentTrackDeliveryRequestDto is provided to converter");
            return null;
        }
        TrackDeliveryRequestVo trackDeliveryRequestVo = new TrackDeliveryRequestVo();
        trackDeliveryRequestVo.setTrackingID(fulfillmentTrackDeliveryRequestDto.getTrackingID());
        trackDeliveryRequestVo.setParticipant(fulfillmentTrackDeliveryRequestDto.getParticipant());
        trackDeliveryRequestVo.setCarrierCode(fulfillmentTrackDeliveryRequestDto.getCarrierCode());
        trackDeliveryRequestVo.setFulfillmentOrderID(fulfillmentTrackDeliveryRequestDto.getFulfillmentOrderID());
        return trackDeliveryRequestVo;
    }

    public TrackDeliveryRequestVo toTrackDeliveryRequestVo(TrackDeliveryRequestDto trackDeliveryRequestDto) {
        if (trackDeliveryRequestDto == null) {
            log.warn("AfterShip: Null trackDeliveryRequestDto is provided to converter");
            return null;
        }
        TrackDeliveryRequestVo trackDeliveryRequestVo = new TrackDeliveryRequestVo();
        trackDeliveryRequestVo.setTrackingID(trackDeliveryRequestDto.getTrackingID());
        trackDeliveryRequestVo.setParticipant(getParticipant(trackDeliveryRequestDto.getParticipant()));
        trackDeliveryRequestVo.setCarrierCode(trackDeliveryRequestDto.getProvider().name());
        return trackDeliveryRequestVo;
    }

    private ParticipantDetailsDto getParticipant(ParticipantDto participantDto) {
        ParticipantDetailsDto participantDetailsDto = new ParticipantDetailsDto();
        participantDetailsDto.setVibrentID(participantDto.getVibrentID());
        participantDetailsDto.setExternalID(participantDto.getExternalID());
        return participantDetailsDto;
    }

}
