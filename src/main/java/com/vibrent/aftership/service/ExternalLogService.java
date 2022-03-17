package com.vibrent.aftership.service;

import com.aftership.sdk.model.tracking.NewTracking;
import com.aftership.sdk.model.tracking.SlugTrackingNumber;
import com.aftership.sdk.model.tracking.Tracking;
import com.vibrent.aftership.dto.ExternalLogDTO;
import com.vibrent.vxp.workflow.TrackDeliveryRequestDto;
import com.vibrent.vxp.workflow.TrackDeliveryResponseDtoWrapper;
import org.springframework.http.HttpStatus;

public interface ExternalLogService {
    ExternalLogDTO send(TrackDeliveryResponseDtoWrapper trackingResponseWrapperBo, Long responseTimeStamp,
                        String description, HttpStatus httpStatus);

    ExternalLogDTO send(TrackDeliveryRequestDto trackDeliveryRequestDto, Long responseTimeStamp, Integer statusCode,
                        NewTracking newTracking, Long requestTimeStamp, String responseBody, String description);


    ExternalLogDTO send(SlugTrackingNumber slugTrackingNumber, Tracking tracking, Long responseTimeStamp, String description, Integer httpStatus);

    ExternalLogDTO send(String trackingNumber,String slug, Long responseTimeStamp, String description, Integer httpStatus);
}
