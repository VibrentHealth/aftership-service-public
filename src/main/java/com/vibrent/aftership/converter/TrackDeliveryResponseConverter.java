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

import java.util.*;

import static com.vibrent.aftership.constants.AfterShipConstants.*;
import static com.vibrent.aftership.service.impl.TrackingRequestServiceImpl.CUSTOM_FIELD_EXTERNAL_ID;
import static com.vibrent.aftership.service.impl.TrackingRequestServiceImpl.CUSTOM_FIELD_VIBRENT_ID;

@Slf4j
@Component
public class TrackDeliveryResponseConverter {

    public TrackDeliveryResponseDto convert(Tracking tracking, TrackingRequest trackingRequest) {
        TrackDeliveryResponseDto trackDeliveryResponseDto = new TrackDeliveryResponseDto();
        trackDeliveryResponseDto.setOperation(OperationEnum.TRACK_DELIVERY);
        trackDeliveryResponseDto.setProvider(ProviderEnum.valueOf(trackingRequest.getProvider()));
        trackDeliveryResponseDto.setTrackingID(tracking.getTrackingNumber());
        trackDeliveryResponseDto.setParticipant(populateParticipant(tracking, trackingRequest));
        trackDeliveryResponseDto.setDates(populateDates(tracking));
        trackDeliveryResponseDto.setStatus(getStatusEnum(tracking));
        trackDeliveryResponseDto.setDateTime(tracking.getLastUpdatedAt() != null ?
                tracking.getLastUpdatedAt().getTime() : -1);
        return trackDeliveryResponseDto;
    }

    public MessageHeaderDto populateMessageHeaderDTO(ParticipantDto participantDto, TrackingRequest trackingRequest) {

        MessageHeaderDto savedMessageHeaderDto = KafkaMessageHeaderDtoBuilder.getMessageHeaderDto(trackingRequest);

        long vibrentID = participantDto == null ? -1 : participantDto.getVibrentID();
        MessageHeaderDto messageHeaderDto;
        if (savedMessageHeaderDto != null) {
            messageHeaderDto = KafkaMessageHeaderDtoBuilder.populateMessageHeaderDtoFromDto(savedMessageHeaderDto, vibrentID, MessageSpecificationEnum.TRACK_DELIVERY_RESPONSE);
        } else {
            messageHeaderDto = KafkaMessageHeaderDtoBuilder.populateDefaultMessageHeaderDto(vibrentID, MessageSpecificationEnum.TRACK_DELIVERY_RESPONSE);
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
            dateDto.setDate(DateTimeUtil.getTimestampFromStringISODate(tracking.getExpectedDelivery(),
                    DateTimeUtil.getTimeZoneFromString("UTC")));
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
}
