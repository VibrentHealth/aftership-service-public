package com.vibrent.aftership.service;

import com.aftership.sdk.model.tracking.NewTracking;
import com.aftership.sdk.model.tracking.SlugTrackingNumber;
import com.aftership.sdk.model.tracking.Tracking;
import com.vibrent.aftership.dto.ExternalLogDTO;
import com.vibrent.aftership.vo.TrackDeliveryRequestVo;
import com.vibrent.vxp.workflow.FulfillmentTrackDeliveryRequestDtoWrapper;
import com.vibrent.vxp.workflow.FulfillmentTrackDeliveryResponseDtoWrapper;

import com.vibrent.vxp.workflow.TrackDeliveryResponseDtoWrapper;
import org.springframework.http.HttpStatus;

public interface ExternalLogService {
    ExternalLogDTO send(TrackDeliveryResponseDtoWrapper trackingResponseWrapperBo, Long responseTimeStamp,
                        String description, HttpStatus httpStatus);

    ExternalLogDTO send(TrackDeliveryRequestVo trackDeliveryRequestVo, Long responseTimeStamp, Integer statusCode,
                        NewTracking newTracking, Long requestTimeStamp, String responseBody, String description);


    ExternalLogDTO send(SlugTrackingNumber slugTrackingNumber, Tracking tracking, Long responseTimeStamp, String description, Integer httpStatus);

    ExternalLogDTO send(String trackingNumber,String slug, Long responseTimeStamp, String description, Integer httpStatus);

    ExternalLogDTO send(FulfillmentTrackDeliveryResponseDtoWrapper trackingResponseWrapper, Long responseTimeStamp,
                        String description, HttpStatus httpStatus);

    ExternalLogDTO send(FulfillmentTrackDeliveryRequestDtoWrapper fulfillmentTrackDeliveryRequestDtoWrapper);
}
