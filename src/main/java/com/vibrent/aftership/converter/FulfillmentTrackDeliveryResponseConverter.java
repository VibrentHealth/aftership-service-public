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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.vibrent.aftership.constants.AfterShipConstants.*;
import static com.vibrent.aftership.service.impl.TrackingRequestServiceImpl.CUSTOM_FIELD_EXTERNAL_ID;
import static com.vibrent.aftership.service.impl.TrackingRequestServiceImpl.CUSTOM_FIELD_VIBRENT_ID;

@Slf4j
@Component
public class FulfillmentTrackDeliveryResponseConverter {

    public FulfillmentTrackDeliveryResponseDto convert(Tracking tracking, TrackingRequest trackingRequest) {
        FulfillmentTrackDeliveryResponseDto trackDeliveryResponseDto = new FulfillmentTrackDeliveryResponseDto();
        trackDeliveryResponseDto.setFulfillmentOrderID(trackingRequest.getFulfillmentOrderID());
        trackDeliveryResponseDto.setCarrierCode(trackingRequest.getProvider());
        trackDeliveryResponseDto.setTrackingID(tracking.getTrackingNumber());
        trackDeliveryResponseDto.setParticipant(populateParticipant(tracking, trackingRequest));
        trackDeliveryResponseDto.setDates(populateDates(tracking));
        trackDeliveryResponseDto.setStatus(getStatusEnum(tracking));
        trackDeliveryResponseDto.setStatusTime(tracking.getLastUpdatedAt() != null ?
                tracking.getLastUpdatedAt().getTime() : -1);
        return trackDeliveryResponseDto;
    }

    public MessageHeaderDto populateMessageHeaderDTO(ParticipantDetailsDto participantDetailsDto, TrackingRequest trackingRequest) {

        MessageHeaderDto savedMessageHeaderDto = KafkaMessageHeaderDtoBuilder.getMessageHeaderDto(trackingRequest);

        long vibrentID = participantDetailsDto == null ? -1 : participantDetailsDto.getVibrentID();
        MessageHeaderDto messageHeaderDto;
        if (savedMessageHeaderDto != null) {
            messageHeaderDto = KafkaMessageHeaderDtoBuilder.populateMessageHeaderDtoFromDto(savedMessageHeaderDto, vibrentID, MessageSpecificationEnum.FULFILMENT_TRACK_DELIVERY_RESPONSE);
        } else {
            messageHeaderDto = KafkaMessageHeaderDtoBuilder.populateDefaultMessageHeaderDto(vibrentID, MessageSpecificationEnum.FULFILMENT_TRACK_DELIVERY_RESPONSE);
        }
        return messageHeaderDto;
    }

    private static ParticipantDetailsDto populateParticipant(Tracking tracking, TrackingRequest trackingRequest) {
        try {
            if (!StringUtils.isEmpty(trackingRequest.getParticipant())) {
                return JacksonUtil.getMapper().readValue(trackingRequest.getParticipant(), ParticipantDetailsDto.class);
            }
        } catch (JsonProcessingException e) {
            log.warn("AfterShip: Error while parsing Participant value: {} as ParticipantDetailsDto", trackingRequest.getParticipant(), e);
        }

        Map<String, String> customFields = tracking.getCustomFields();
        if (CollectionUtils.isEmpty(customFields)) {
            log.warn("AfterShip: Received empty customFields from AfterShip cloud service.");
            return null;
        }

        String vibrentID = customFields.get(CUSTOM_FIELD_VIBRENT_ID);
        String externalID = customFields.get(CUSTOM_FIELD_EXTERNAL_ID);
        ParticipantDetailsDto participantDetailsDto = new ParticipantDetailsDto();
        participantDetailsDto.setVibrentID(StringUtils.isEmpty(vibrentID) ? -1 : Long.parseLong(vibrentID));
        participantDetailsDto.setExternalID(externalID);
        return participantDetailsDto;
    }

    private static List<DatesDto> populateDates(Tracking tracking) {
        List<DatesDto> datesDtos = new ArrayList<>();
        if (!StringUtils.isEmpty(tracking.getExpectedDelivery())) {
            DatesDto datesDto = new DatesDto();
            datesDto.setDateTime(DateTimeUtil.getTimestampFromStringISODate(tracking.getExpectedDelivery(),
                    DateTimeUtil.getTimeZoneFromString("UTC")));
            datesDto.setType(DateTypeEnum.EXPECTED_DELIVERY_DATE);
            datesDtos.add(datesDto);
        }
        return datesDtos;
    }

    private static TrackingStatusEnum getStatusEnum(Tracking tracking) {
        if (StringUtils.isEmpty(tracking.getTag())) {
            log.warn("AfterShip: Received status as empty or null from AfterShip cloud service.");
            return null;
        }


        String tag = tracking.getTag();
        String subTag = tracking.getSubtag();
        TrackingStatusEnum statusEnum;
        switch (tag) {
            case TAG_IN_TRANSIT:
                statusEnum = TrackingStatusEnum.IN_TRANSIT;
                break;
            case TAG_DELIVERED:
                statusEnum = TrackingStatusEnum.DELIVERED;
                break;
            case TAG_AVAILABLE_FOR_PICKUP:
                if(subTag.equals(SUBTAG_AVAILABLE_FOR_PICKUP_001)){
                    statusEnum = TrackingStatusEnum.AVAILABLE_TO_PICKUP;
                } else{
                    statusEnum = TrackingStatusEnum.UNRECOGNIZE;
                }
                break;
            case TAG_PENDING:
                statusEnum = TrackingStatusEnum.PENDING;
                break;
            case TAG_OUT_FOR_DELIVERY:
                if(subTag.equals(SUBTAG_OUT_FOR_DELIVERY_004)){
                    statusEnum = TrackingStatusEnum.DELIVERY_APPOINTMENT_SETUP;
                } else{
                    statusEnum = TrackingStatusEnum.UNRECOGNIZE;
                }
                break;
            case TAG_EXCEPTION:
                switch (subTag) {
                    case TAG_EXCEPTION_011:
                        statusEnum = TrackingStatusEnum.RETURNED;
                        break;
                    case TAG_EXCEPTION_004:
                    case TAG_EXCEPTION_005:
                        statusEnum = TrackingStatusEnum.PKG_DELAYED;
                        break;
                    case TAG_EXCEPTION_013:
                        statusEnum = TrackingStatusEnum.PKG_LOST;
                        break;
                    case TAG_EXCEPTION_007:
                        statusEnum = TrackingStatusEnum.INCORRECT_ADDRESS;
                        break;
                    default:
                        statusEnum = TrackingStatusEnum.DELIVERY_FAILED;
                        break;
                }
                break;
            default:
                statusEnum = TrackingStatusEnum.UNRECOGNIZE;
                break;
        }
        return statusEnum;
    }
}
