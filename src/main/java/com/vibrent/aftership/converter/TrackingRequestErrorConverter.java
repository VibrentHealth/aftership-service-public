package com.vibrent.aftership.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vibrent.aftership.domain.TrackingRequestError;
import com.vibrent.aftership.exception.AfterShipException;
import com.vibrent.aftership.repository.TrackingRequestErrorRepository;
import com.vibrent.aftership.util.JacksonUtil;
import com.vibrent.aftership.vo.TrackDeliveryRequestVo;
import com.vibrent.vxp.workflow.MessageHeaderDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class TrackingRequestErrorConverter {

    private final TrackingRequestErrorRepository trackingRequestErrorRepository;

    public TrackingRequestErrorConverter(TrackingRequestErrorRepository trackingRequestErrorRepository) {
        this.trackingRequestErrorRepository = trackingRequestErrorRepository;
    }

    public TrackingRequestError toTrackingRequestError(TrackDeliveryRequestVo trackDeliveryRequestVo,
                                                       MessageHeaderDto messageHeaderDto,
                                                       Throwable throwable) {
        if (trackDeliveryRequestVo == null || messageHeaderDto == null) {
            log.warn("AfterShip: Null trackDeliveryRequestDto or messageHeader is provided to TrackingRequestErrorConverter");
            return null;
        }
        Optional<TrackingRequestError> optionalTrackingRequestError = this.trackingRequestErrorRepository.findByTrackingId(trackDeliveryRequestVo.getTrackingID());
        TrackingRequestError trackingRequestError = optionalTrackingRequestError.orElse(new TrackingRequestError());
        trackingRequestError.setTrackingId(trackDeliveryRequestVo.getTrackingID());
        if(throwable instanceof AfterShipException) {
            trackingRequestError.setErrorCode(((AfterShipException) throwable).getErrorCode());
        }
        trackingRequestError.setTrackDeliveryRequest(getTrackDeliveryRequest(trackDeliveryRequestVo));
        trackingRequestError.setHeader(getMessageHeaders(messageHeaderDto));
        trackingRequestError.setRetryCount(trackingRequestError.getRetryCount() == null ? 0 : (trackingRequestError.getRetryCount() + 1));
        return trackingRequestError;
    }

    private String getTrackDeliveryRequest(TrackDeliveryRequestVo trackDeliveryRequestVo) {
        try {
            return JacksonUtil.getMapper().writeValueAsString(trackDeliveryRequestVo);
        } catch (JsonProcessingException e) {
            log.warn("AfterShip: Error while converting trackDeliveryRequestDto as String value", e);
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
