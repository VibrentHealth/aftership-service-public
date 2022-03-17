package com.vibrent.aftership.converter;

import com.aftership.sdk.model.tracking.Tracking;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.vibrent.aftership.domain.TrackingRequest;
import com.vibrent.aftership.util.DateTimeUtil;
import com.vibrent.aftership.util.JacksonUtil;
import com.vibrent.vxp.workflow.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.vibrent.aftership.constants.AfterShipConstants.*;
import static com.vibrent.aftership.service.impl.TrackingRequestServiceImpl.CUSTOM_FIELD_EXTERNAL_ID;
import static com.vibrent.aftership.service.impl.TrackingRequestServiceImpl.CUSTOM_FIELD_VIBRENT_ID;

@Slf4j
@Component
public class TrackDeliveryResponseConverter {

    private static final String VXP_HEADER_VERSION = "2.1.3";
    private static final String VXP_MESSAGE_SPEC_VERSION = "2.1.2";
    private static final String SOURCE_AFTER_SHIP = "AfterShip";


    public TrackDeliveryResponseDto convert(Tracking tracking, TrackingRequest trackingRequest) {
        TrackDeliveryResponseDto trackDeliveryResponseDto = new TrackDeliveryResponseDto();
        trackDeliveryResponseDto.setOperation(OperationEnum.TRACK_DELIVERY);
        trackDeliveryResponseDto.setProvider(trackingRequest.getProvider());
        trackDeliveryResponseDto.setTrackingID(tracking.getTrackingNumber());
        trackDeliveryResponseDto.setParticipant(populateParticipant(tracking, trackingRequest));
        trackDeliveryResponseDto.setDates(populateDates(tracking));
        trackDeliveryResponseDto.setStatus(getStatusEnum(tracking));
        trackDeliveryResponseDto.setDateTime(tracking.getLastUpdatedAt() != null ?
                tracking.getLastUpdatedAt().getTime() : -1);
        return trackDeliveryResponseDto;
    }

    public MessageHeaderDto populateMessageHeaderDTO(Tracking tracking, TrackingRequest trackingRequest) {
        Map<String, String> customFields = tracking.getCustomFields();
        if (customFields == null) {
            customFields = Collections.emptyMap();
        }

        MessageHeaderDto savedMessageHeaderDto = getMessageHeaderDto(trackingRequest);

        String vibrentID = customFields.get(CUSTOM_FIELD_VIBRENT_ID);
        MessageHeaderDto messageHeaderDto;
        if (savedMessageHeaderDto != null) {
            messageHeaderDto = populateMessageHeaderDtoFromDto(savedMessageHeaderDto, vibrentID);
        } else {
            messageHeaderDto = populateDefaultMessageHeaderDto(vibrentID);
        }
        return messageHeaderDto;
    }

    private static ParticipantDto populateParticipant(Tracking tracking, TrackingRequest trackingRequest) {
        try {
            if (!StringUtils.isEmpty(trackingRequest.getParticipant())) {
                return JacksonUtil.getMapper().readValue(trackingRequest.getParticipant(), ParticipantDto.class);
            }
        } catch (JsonProcessingException e) {
            log.warn("AfterShip: Error while parsing Participant value: {} as ParticipantDto", trackingRequest.getParticipant(), e);
        }

        Map<String, String> customFields = tracking.getCustomFields();
        if (CollectionUtils.isEmpty(customFields)) {
            log.warn("AfterShip: Received empty customFields from AfterShip cloud service.");
            return null;
        }

        String vibrentID = customFields.get(CUSTOM_FIELD_VIBRENT_ID);
        String externalID = customFields.get(CUSTOM_FIELD_EXTERNAL_ID);
        ParticipantDto participantDto = new ParticipantDto();
        participantDto.setVibrentID(StringUtils.isEmpty(vibrentID) ? -1 : Long.parseLong(vibrentID));
        participantDto.setExternalID(externalID);
        return participantDto;
    }

    private static List<DateDto> populateDates(Tracking tracking) {
        List<DateDto> dateDtos = new ArrayList<>();
        if (!StringUtils.isEmpty(tracking.getExpectedDelivery())) {
            DateDto dateDto = new DateDto();
            dateDto.setDate(DateTimeUtil.getTimestampFromStringDate(tracking.getExpectedDelivery(),
                    DateTimeFormatter.ISO_DATE, DateTimeUtil.getTimeZoneFromString("UTC")));
            dateDto.setType(DateTypeEnum.EXPECTED_DELIVERY_DATE);
            dateDtos.add(dateDto);
        }
        return dateDtos;
    }

    private static StatusEnum getStatusEnum(Tracking tracking) {
        if (StringUtils.isEmpty(tracking.getTag())) {
            log.warn("AfterShip: Received status as empty or null from AfterShip cloud service.");
            return null;
        }

        String tag = tracking.getTag();
        StatusEnum statusEnum;
        switch (tag) {
            case TAG_IN_TRANSIT:
                statusEnum = StatusEnum.IN_TRANSIT;
                break;
            case TAG_DELIVERED:
                statusEnum = StatusEnum.DELIVERED;
                break;
            case TAG_EXCEPTION:
                statusEnum = StatusEnum.ERROR;
                break;
            case TAG_PENDING:
                statusEnum = StatusEnum.PENDING_TRACKING;
                break;
            default:
                statusEnum = StatusEnum.UNRECOGNIZED;
                break;
        }
        return statusEnum;
    }

    /**
     *
     * @param trackingRequest
     * @return
     */
    private static MessageHeaderDto getMessageHeaderDto(TrackingRequest trackingRequest) {
        try {
            if (!StringUtils.isEmpty(trackingRequest.getHeader())) {
                return JacksonUtil.getMapper().readValue(trackingRequest.getHeader(), MessageHeaderDto.class);
            }
        } catch (JsonProcessingException e) {
            log.warn("AfterShip: Error while parsing Header value String as MessageHeaderDto", e);
        }
        return null;
    }

    /**
     *
     * @param savedMessageHeaderDto
     * @param vibrentID
     * @return
     */
    private static MessageHeaderDto populateMessageHeaderDtoFromDto(MessageHeaderDto savedMessageHeaderDto, String vibrentID) {
        MessageHeaderDto messageHeaderDto = new MessageHeaderDto();
        messageHeaderDto.setVxpMessageID(UUID.randomUUID().toString());
        messageHeaderDto.setVxpHeaderVersion(savedMessageHeaderDto.getVxpHeaderVersion());
        messageHeaderDto.setVxpWorkflowName(savedMessageHeaderDto.getVxpWorkflowName());
        messageHeaderDto.setVxpMessageSpec(MessageSpecificationEnum.TRACK_DELIVERY_RESPONSE);
        messageHeaderDto.setVxpMessageTimestamp(System.currentTimeMillis());
        messageHeaderDto.setSource(SOURCE_AFTER_SHIP);
        messageHeaderDto.setVxpMessageSpecVersion(savedMessageHeaderDto.getVxpMessageSpecVersion());
        messageHeaderDto.setVxpOriginator(RequestOriginatorEnum.PTBE);
        messageHeaderDto.setVxpWorkflowInstanceID(savedMessageHeaderDto.getVxpWorkflowInstanceID());
        messageHeaderDto.setVxpPattern(savedMessageHeaderDto.getVxpPattern());
        messageHeaderDto.setVxpUserID(StringUtils.isEmpty(vibrentID) ? -1 : Long.parseLong(vibrentID));
        messageHeaderDto.setVxpTrigger(savedMessageHeaderDto.getVxpTrigger());
        messageHeaderDto.setVxpTenantID(savedMessageHeaderDto.getVxpTenantID());
        messageHeaderDto.setVxpProgramID(savedMessageHeaderDto.getVxpProgramID());
        return messageHeaderDto;
    }

    /**
     *
     * @param vibrentID
     * @return
     */
    private static MessageHeaderDto populateDefaultMessageHeaderDto(String vibrentID) {
        MessageHeaderDto messageHeaderDto = new MessageHeaderDto();
        messageHeaderDto.setVxpMessageID(UUID.randomUUID().toString());
        messageHeaderDto.setVxpHeaderVersion(VXP_HEADER_VERSION);
        messageHeaderDto.setVxpWorkflowName(WorkflowNameEnum.SALIVARY_KIT_ORDER);
        messageHeaderDto.setVxpMessageSpec(MessageSpecificationEnum.TRACK_DELIVERY_RESPONSE);
        messageHeaderDto.setVxpMessageTimestamp(System.currentTimeMillis());
        messageHeaderDto.setSource(SOURCE_AFTER_SHIP);
        messageHeaderDto.setVxpMessageSpecVersion(VXP_MESSAGE_SPEC_VERSION);
        messageHeaderDto.setVxpOriginator(RequestOriginatorEnum.PTBE);
        messageHeaderDto.setVxpWorkflowInstanceID(UUID.randomUUID().toString());
        messageHeaderDto.setVxpPattern(IntegrationPatternEnum.WORKFLOW);
        messageHeaderDto.setVxpUserID(StringUtils.isEmpty(vibrentID) ? -1 : Long.parseLong(vibrentID));
        messageHeaderDto.setVxpTrigger(ContextTypeEnum.EVENT);
        return messageHeaderDto;
    }


}
