package com.vibrent.aftership.service.impl;

import com.aftership.sdk.model.tracking.NewTracking;
import com.aftership.sdk.model.tracking.SlugTrackingNumber;
import com.aftership.sdk.model.tracking.Tracking;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Preconditions;
import com.vibrent.aftership.dto.ExternalLogDTO;
import com.vibrent.aftership.enums.ExternalEventEnum;
import com.vibrent.aftership.enums.ExternalEventSourceEnum;
import com.vibrent.aftership.enums.ExternalServiceTypeEnum;
import com.vibrent.aftership.messaging.producer.impl.ExternalLogProducer;
import com.vibrent.aftership.service.ExternalLogService;
import com.vibrent.aftership.util.JacksonUtil;
import com.vibrent.vxp.workflow.TrackDeliveryRequestDto;
import com.vibrent.vxp.workflow.TrackDeliveryResponseDto;
import com.vibrent.vxp.workflow.TrackDeliveryResponseDtoWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;
import java.util.Map;
import java.util.StringJoiner;

import static com.vibrent.aftership.service.impl.TrackingRequestServiceImpl.CUSTOM_FIELD_EXTERNAL_ID;
import static com.vibrent.aftership.service.impl.TrackingRequestServiceImpl.CUSTOM_FIELD_VIBRENT_ID;

@Service
@Slf4j
public class ExternalLogServiceImpl implements ExternalLogService {

    public static final String RESPONSE_NO_HEADER = "RESPONSE WITH NO HEADER";
    public static final String RESPONSE_NO_BODY = "RESPONSE WITH NO BODY";
    public static final String REQUEST_NO_BODY = "REQUEST WITH NO BODY";
    public static final String RESPONSE_NO_TRACKING_INFO = "RESPONSE WITH NO TRACKING INFO";
    public static final String RESPONSE_NO_PARTICIPANT_INFO = "RESPONSE WITH NO PARTICIPANT INFO";
    public static final String RESPONSE_NO_EXTERNAL_ID = "RESPONSE WITH NO PARTICIPANT EXTERNAL ID";
    public static final String VIBRENT_ID = "vibrentid";
    public static final String EXTERNAL_ID = "externalid";

    private final ExternalLogProducer externalLogProducer;

    private final String responseUrl;
    private final String afterShipBaseUrl;

    public ExternalLogServiceImpl(ExternalLogProducer externalLogProducer,
                                  @Value("${kafka.topics.tracking.response}") String responseUrl,
                                  @Value("${afterShip.baseUrl}") String afterShipBaseUrl) {
        this.externalLogProducer = externalLogProducer;
        this.responseUrl = responseUrl;
        this.afterShipBaseUrl = afterShipBaseUrl;
    }

    @Override
    public ExternalLogDTO send(TrackDeliveryResponseDtoWrapper trackDeliveryResponseDtoWrapper, Long responseTimeStamp, String description, HttpStatus httpStatus) {
        Preconditions.checkArgument(responseTimeStamp != null, "Response Time Stamp cannot be null");

        ExternalLogDTO externalLogDTO = this.trackingResponseDTOToExternalLogDTO(trackDeliveryResponseDtoWrapper, responseTimeStamp, description, httpStatus);
        this.externalLogProducer.send(externalLogDTO);
        return externalLogDTO;
    }

    @Override
    public ExternalLogDTO send(TrackDeliveryRequestDto trackDeliveryRequestDto, Long responseTimeStamp, Integer statusCode,
                               NewTracking newTracking, Long requestTimeStamp, String responseBody, String description) {
        Preconditions.checkArgument(responseTimeStamp != null, "Response Time Stamp cannot be null");
        ExternalLogDTO externalLogDTO = this.trackingRequestDTOToExternalLogDTO(trackDeliveryRequestDto, responseTimeStamp, statusCode,
                newTracking, requestTimeStamp, responseBody, description);
        this.externalLogProducer.send(externalLogDTO);
        return externalLogDTO;
    }

    @Override
    public ExternalLogDTO send(SlugTrackingNumber slugTrackingNumber, Tracking tracking, Long responseTimeStamp, String description, Integer httpStatus) {
        ExternalLogDTO externalLogDTO = this.slugTrackingNumberToExternalLogDTO(slugTrackingNumber, tracking, responseTimeStamp, description, httpStatus);
        this.externalLogProducer.send(externalLogDTO);
        return externalLogDTO;
    }

    @Override
    public ExternalLogDTO send(String trackingNumber, String slug, Long responseTimeStamp, String description, Integer httpStatus) {
        ExternalLogDTO externalLogDTO = this.slugTrackingNumberToExternalLogDTO(trackingNumber, slug, responseTimeStamp, description, httpStatus);
        this.externalLogProducer.send(externalLogDTO);
        return externalLogDTO;
    }

    /* -------------------------------------------------------------------- */
    /*     private functions                                                */
    /* -------------------------------------------------------------------- */
    private ExternalLogDTO trackingResponseDTOToExternalLogDTO(TrackDeliveryResponseDtoWrapper trackDeliveryResponseDtoWrapper,
                                                               Long responseTimeStamp, String description, HttpStatus httpStatus) {
        StringJoiner errorMsg = new StringJoiner(",").add(description);
        ExternalLogDTO externalLogDTO = new ExternalLogDTO();
        externalLogDTO.setService(ExternalServiceTypeEnum.AFTER_SHIP);
        externalLogDTO.setEventSource(ExternalEventSourceEnum.AFTER_SHIP_SERVICE);
        externalLogDTO.setHttpMethod(RequestMethod.POST);
        externalLogDTO.setRequestUrl(responseUrl);

        externalLogDTO.setResponseTimestamp(responseTimeStamp);
        externalLogDTO.setRequestTimestamp(responseTimeStamp);

        externalLogDTO.setEventType(ExternalEventEnum.AFTER_SHIP_TRACKING_RESPONSE_SENT);

        if (trackDeliveryResponseDtoWrapper == null) {
            errorMsg.add(RESPONSE_NO_BODY);
            externalLogDTO.setDescription(errorMsg.toString());
            externalLogDTO.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            return externalLogDTO;
        }

        try {
            externalLogDTO.setResponseBody(JacksonUtil.getMapper().writeValueAsString(trackDeliveryResponseDtoWrapper));
        } catch (JsonProcessingException e) {
            log.warn("AfterShip: Error while writing trackDeliveryResponseDtoWrapper to response body of externalLogDTO", e);
        }

        if (trackDeliveryResponseDtoWrapper.getHeader() != null) {
            externalLogDTO.setRequestHeaders(trackDeliveryResponseDtoWrapper.getHeader().toString());
        } else {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            errorMsg.add(RESPONSE_NO_HEADER);
        }

        TrackDeliveryResponseDto trackDeliveryResponseDto = null;

        try {
            trackDeliveryResponseDto = trackDeliveryResponseDtoWrapper.getPayload();
        } catch (IOException e) {
            log.warn("AfterShip: Error while getting payload from message: {}", trackDeliveryResponseDtoWrapper, e);
        }

        if (trackDeliveryResponseDto == null) {
            errorMsg.add(RESPONSE_NO_TRACKING_INFO);
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        } else if (trackDeliveryResponseDto.getParticipant() != null) {
            if (trackDeliveryResponseDto.getParticipant().getExternalID() != null) {
                externalLogDTO.setExternalId(trackDeliveryResponseDto.getParticipant().getExternalID());
            } else {
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
                errorMsg.add(RESPONSE_NO_EXTERNAL_ID);
            }
            externalLogDTO.setInternalId(trackDeliveryResponseDto.getParticipant().getVibrentID());
        } else {
            errorMsg.add(RESPONSE_NO_PARTICIPANT_INFO);
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        externalLogDTO.setDescription(errorMsg.toString());
        externalLogDTO.setResponseCode(httpStatus.value());
        return externalLogDTO;
    }

    private ExternalLogDTO trackingRequestDTOToExternalLogDTO(TrackDeliveryRequestDto trackDeliveryRequestDto,
                                                              Long responseTimeStamp, Integer statusCode, NewTracking newTracking,
                                                              Long requestTimeStamp, String responseBody, String description) {
        StringJoiner errorMsg = new StringJoiner(",").add(description);
        ExternalLogDTO externalLogDTO = new ExternalLogDTO();
        externalLogDTO.setService(ExternalServiceTypeEnum.AFTER_SHIP);
        externalLogDTO.setEventSource(ExternalEventSourceEnum.AFTER_SHIP_SERVICE);
        externalLogDTO.setHttpMethod(RequestMethod.POST);
        externalLogDTO.setRequestUrl(afterShipBaseUrl + "/trackings");

        externalLogDTO.setResponseTimestamp(responseTimeStamp);
        externalLogDTO.setRequestTimestamp(requestTimeStamp);

        externalLogDTO.setEventType(ExternalEventEnum.AFTER_SHIP_TRACKING_REQUEST);

        if (newTracking == null) {
            errorMsg.add(REQUEST_NO_BODY);
            externalLogDTO.setDescription(errorMsg.toString());
            externalLogDTO.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            return externalLogDTO;
        }

        try {
            externalLogDTO.setRequestBody(JacksonUtil.getMapper().writeValueAsString(newTracking));
        } catch (JsonProcessingException e) {
            log.warn("AfterShip: Error while writing tracking to request or response body of externalLogDTO", e);
        }

        externalLogDTO.setResponseBody(responseBody);

        if (trackDeliveryRequestDto.getParticipant() != null) {
            externalLogDTO.setExternalId(trackDeliveryRequestDto.getParticipant().getExternalID());
            externalLogDTO.setInternalId(trackDeliveryRequestDto.getParticipant().getVibrentID());
        }
        externalLogDTO.setDescription(errorMsg.toString());
        externalLogDTO.setResponseCode(statusCode);
        return externalLogDTO;
    }

    private ExternalLogDTO slugTrackingNumberToExternalLogDTO(SlugTrackingNumber slugTrackingNumber, Tracking tracking, Long responseTimeStamp, String description, Integer httpStatus) {
        ExternalLogDTO externalLogDTO = new ExternalLogDTO();
        externalLogDTO.setService(ExternalServiceTypeEnum.AFTER_SHIP);
        externalLogDTO.setEventSource(ExternalEventSourceEnum.AFTER_SHIP_SERVICE);
        externalLogDTO.setHttpMethod(RequestMethod.GET);
        externalLogDTO.setRequestUrl(afterShipBaseUrl + "/trackings/" + slugTrackingNumber.getSlug() + "/" + slugTrackingNumber.getTrackingNumber());
        externalLogDTO.setResponseTimestamp(responseTimeStamp);
        externalLogDTO.setRequestTimestamp(responseTimeStamp);
        externalLogDTO.setDescription(description);
        externalLogDTO.setResponseCode(httpStatus);
        externalLogDTO.setEventType(ExternalEventEnum.AFTER_SHIP_GET_TRACKING);
        try {
            externalLogDTO.setResponseBody(JacksonUtil.getMapper().writeValueAsString(tracking));
        } catch (JsonProcessingException e) {
            log.warn("AfterShip: Error while parsing tracking response received from AfterShip", e);
        }
        Map<String, String> customFields = tracking.getCustomFields();
        if (!CollectionUtils.isEmpty(customFields)) {
            String vibrentID = !StringUtils.isEmpty(customFields.get(VIBRENT_ID)) ? customFields.get(VIBRENT_ID) : customFields.get(CUSTOM_FIELD_VIBRENT_ID);
            if (!StringUtils.isEmpty(vibrentID)) {
                externalLogDTO.setInternalId(Long.parseLong(vibrentID));
            }
            String externalID = !StringUtils.isEmpty(customFields.get(EXTERNAL_ID)) ? customFields.get(EXTERNAL_ID) : customFields.get(CUSTOM_FIELD_EXTERNAL_ID);
            if (!StringUtils.isEmpty(externalID)) {
                externalLogDTO.setExternalId(externalID);
            }

        }
        return externalLogDTO;
    }

    private ExternalLogDTO slugTrackingNumberToExternalLogDTO(String trackingNumber, String slug, Long responseTimeStamp, String description, Integer httpStatus) {
        ExternalLogDTO externalLogDTO = new ExternalLogDTO();
        externalLogDTO.setService(ExternalServiceTypeEnum.AFTER_SHIP);
        externalLogDTO.setEventSource(ExternalEventSourceEnum.AFTER_SHIP_SERVICE);
        externalLogDTO.setHttpMethod(RequestMethod.GET);
        externalLogDTO.setRequestUrl(afterShipBaseUrl + "/trackings/" + slug + "/" + trackingNumber);
        externalLogDTO.setResponseTimestamp(responseTimeStamp);
        externalLogDTO.setRequestTimestamp(responseTimeStamp);
        externalLogDTO.setDescription(description);
        externalLogDTO.setResponseCode(httpStatus);
        externalLogDTO.setEventType(ExternalEventEnum.AFTER_SHIP_GET_TRACKING);

        return externalLogDTO;
    }


}
