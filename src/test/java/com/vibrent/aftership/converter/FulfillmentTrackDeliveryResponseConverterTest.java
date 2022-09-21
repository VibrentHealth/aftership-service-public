package com.vibrent.aftership.converter;

import com.aftership.sdk.model.tracking.Tracking;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.vibrent.aftership.domain.TrackingRequest;
import com.vibrent.aftership.util.JacksonUtil;
import com.vibrent.vxp.workflow.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.vibrent.aftership.service.impl.TrackingRequestServiceImpl.CUSTOM_FIELD_EXTERNAL_ID;
import static com.vibrent.aftership.service.impl.TrackingRequestServiceImpl.CUSTOM_FIELD_VIBRENT_ID;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class FulfillmentTrackDeliveryResponseConverterTest {

    private FulfillmentTrackDeliveryResponseConverter fulfillmentTrackDeliveryResponseConverter;
    private Tracking tracking;
    private TrackingRequest trackingRequest;
    private final Date lastUpdatedAt = new Date();

    @BeforeEach
    void setUp() throws Exception {
        fulfillmentTrackDeliveryResponseConverter = new FulfillmentTrackDeliveryResponseConverter();
        initializeTracking();
        initializeTrackingRequest();
    }

    @Test
    @DisplayName("When valid notificationDTO is provided then verify trackDeliveryResponseDto")
    void convertWhenValidNotificationDtoIsProvided() {
        FulfillmentTrackDeliveryResponseDto trackDeliveryResponseDto = fulfillmentTrackDeliveryResponseConverter.convert(tracking, trackingRequest);
        assertEquals("tracking_number_1", trackDeliveryResponseDto.getTrackingID());
        assertEquals(lastUpdatedAt.getTime(), trackDeliveryResponseDto.getStatusTime());
        assertEquals(ProviderEnum.USPS.toValue(), trackDeliveryResponseDto.getCarrierCode());
        assertEquals(TrackingStatusEnum.IN_TRANSIT, trackDeliveryResponseDto.getStatus());

        ParticipantDetailsDto participantDetailsDto = trackDeliveryResponseDto.getParticipant();
        assertNotNull(participantDetailsDto);
        assertEquals(1000, participantDetailsDto.getVibrentID());
        assertEquals("P1000", participantDetailsDto.getExternalID());

        List<DatesDto> dates = trackDeliveryResponseDto.getDates();
        assertNotNull(dates);
        assertEquals(1, dates.size());
        DatesDto dateDto = dates.get(0);
        assertEquals(DateTypeEnum.EXPECTED_DELIVERY_DATE, dateDto.getType());
        assertEquals(1628985600000L, dateDto.getDateTime());
    }

    @Test
    @DisplayName("When notificationDTO tag is Delivered then verify trackDeliveryResponseDto status")
    void convertWhenNotificationDtoTagIsDeliveredThenVerifyStatus() {
        tracking.setTag("Delivered");
        tracking.setSubtag("Delivered_001");
        FulfillmentTrackDeliveryResponseDto trackDeliveryResponseDto = fulfillmentTrackDeliveryResponseConverter.convert(tracking, trackingRequest);
        assertEquals(TrackingStatusEnum.DELIVERED, trackDeliveryResponseDto.getStatus());
    }

    @Test
    void convertWhenNotificationDtoTagIsAvailableForPickupThenVerifyStatus() {
        tracking.setTag("AvailableForPickup");
        tracking.setSubtag("AvailableForPickup_001");
        FulfillmentTrackDeliveryResponseDto trackDeliveryResponseDto = fulfillmentTrackDeliveryResponseConverter.convert(tracking, trackingRequest);
        assertEquals(TrackingStatusEnum.AVAILABLE_TO_PICKUP, trackDeliveryResponseDto.getStatus());
    }

    @Test
    void convertWhenNotificationDtoTagIsAvailableForPickupElseThenVerifyStatus() {
        tracking.setTag("AvailableForPickup");
        tracking.setSubtag("AvailableForPickup_002");
        FulfillmentTrackDeliveryResponseDto trackDeliveryResponseDto = fulfillmentTrackDeliveryResponseConverter.convert(tracking, trackingRequest);
        assertEquals(TrackingStatusEnum.UNRECOGNIZE, trackDeliveryResponseDto.getStatus());
    }

    @Test
    void convertWhenNotificationDtoTagIsPENDINGThenVerifyStatus() {
        tracking.setTag("Pending");
        FulfillmentTrackDeliveryResponseDto trackDeliveryResponseDto = fulfillmentTrackDeliveryResponseConverter.convert(tracking, trackingRequest);
        assertEquals(TrackingStatusEnum.PENDING, trackDeliveryResponseDto.getStatus());
    }

    @Test
    void convertWhenNotificationDtoTagIsOutForDeliveryThenVerifyStatus() {
        tracking.setTag("OutForDelivery");
        tracking.setSubtag("OutForDelivery_004");
        FulfillmentTrackDeliveryResponseDto trackDeliveryResponseDto = fulfillmentTrackDeliveryResponseConverter.convert(tracking, trackingRequest);
        assertEquals(TrackingStatusEnum.DELIVERY_APPOINTMENT_SETUP, trackDeliveryResponseDto.getStatus());
    }

    @Test
    void convertWhenNotificationDtoTagIsOutForDeliveryElseThenVerifyStatus() {
        tracking.setTag("OutForDelivery");
        tracking.setSubtag("OutForDelivery_001");
        FulfillmentTrackDeliveryResponseDto trackDeliveryResponseDto = fulfillmentTrackDeliveryResponseConverter.convert(tracking, trackingRequest);
        assertEquals(TrackingStatusEnum.UNRECOGNIZE, trackDeliveryResponseDto.getStatus());
    }


    @Test
    @DisplayName("When notificationDTO tag is Exception then verify trackDeliveryResponseDto status")
    void convertWhenNotificationDtoTagIsErrorThenVerifyStatus() {
        tracking.setTag("Exception");
        tracking.setSubtag("Exception_011");
        FulfillmentTrackDeliveryResponseDto trackDeliveryResponseDto = fulfillmentTrackDeliveryResponseConverter.convert(tracking, trackingRequest);
        assertEquals(TrackingStatusEnum.RETURNED, trackDeliveryResponseDto.getStatus());
    }

    @Test
    @DisplayName("When notificationDTO tag is Exception then verify trackDeliveryResponseDto status")
    void convertWhenNotificationDtoTagIsError004ThenVerifyStatus() {
        tracking.setTag("Exception");
        tracking.setSubtag("Exception_004");
        FulfillmentTrackDeliveryResponseDto trackDeliveryResponseDto = fulfillmentTrackDeliveryResponseConverter.convert(tracking, trackingRequest);
        assertEquals(TrackingStatusEnum.PKG_DELAYED, trackDeliveryResponseDto.getStatus());
    }

    @Test
    @DisplayName("When notificationDTO tag is Exception then verify trackDeliveryResponseDto status")
    void convertWhenNotificationDtoTagIsError005ThenVerifyStatus() {
        tracking.setTag("Exception");
        tracking.setSubtag("Exception_005");
        FulfillmentTrackDeliveryResponseDto trackDeliveryResponseDto = fulfillmentTrackDeliveryResponseConverter.convert(tracking, trackingRequest);
        assertEquals(TrackingStatusEnum.PKG_DELAYED, trackDeliveryResponseDto.getStatus());
    }

    @Test
    @DisplayName("When notificationDTO tag is Exception then verify trackDeliveryResponseDto status")
    void convertWhenNotificationDtoTagIsError013ThenVerifyStatus() {
        tracking.setTag("Exception");
        tracking.setSubtag("Exception_013");
        FulfillmentTrackDeliveryResponseDto trackDeliveryResponseDto = fulfillmentTrackDeliveryResponseConverter.convert(tracking, trackingRequest);
        assertEquals(TrackingStatusEnum.PKG_LOST, trackDeliveryResponseDto.getStatus());
    }

    @Test
    @DisplayName("When notificationDTO tag is Exception then verify trackDeliveryResponseDto status")
    void convertWhenNotificationDtoTagIsError007ThenVerifyStatus() {
        tracking.setTag("Exception");
        tracking.setSubtag("Exception_007");
        FulfillmentTrackDeliveryResponseDto trackDeliveryResponseDto = fulfillmentTrackDeliveryResponseConverter.convert(tracking, trackingRequest);
        assertEquals(TrackingStatusEnum.INCORRECT_ADDRESS, trackDeliveryResponseDto.getStatus());
    }

    @Test
    @DisplayName("When notificationDTO tag is Exception then verify trackDeliveryResponseDto status")
    void convertWhenNotificationDtoTagIsError000ThenVerifyStatus() {
        tracking.setTag("Exception");
        tracking.setSubtag("Exception_000");
        FulfillmentTrackDeliveryResponseDto trackDeliveryResponseDto = fulfillmentTrackDeliveryResponseConverter.convert(tracking, trackingRequest);
        assertEquals(TrackingStatusEnum.DELIVERY_FAILED, trackDeliveryResponseDto.getStatus());
    }

    @Test
    @DisplayName("When notificationDTO tag is Unknown then verify trackDeliveryResponseDto status")
    void convertWhenNotificationDtoTagIsUnknownThenVerifyStatus() {
        tracking.setTag("Unknown");
        FulfillmentTrackDeliveryResponseDto trackDeliveryResponseDto = fulfillmentTrackDeliveryResponseConverter.convert(tracking, trackingRequest);
        assertEquals(TrackingStatusEnum.UNRECOGNIZE, trackDeliveryResponseDto.getStatus());
    }

    @Test
    @DisplayName("When notificationDTO tag is null then verify trackDeliveryResponseDto status")
    void convertWhenNotificationDtoTagIsNullThenVerifyStatus() {
        tracking.setTag(null);
        FulfillmentTrackDeliveryResponseDto trackDeliveryResponseDto = fulfillmentTrackDeliveryResponseConverter.convert(tracking, trackingRequest);
        assertNull(trackDeliveryResponseDto.getStatus());
    }

    @Test
    @DisplayName("When valid notificationDTO is provided then verify messageHeaderDto")
    void populateMessageHeaderDTO() {
        ParticipantDetailsDto participantDto = new ParticipantDetailsDto();
        participantDto.setVibrentID(1000L);
        MessageHeaderDto messageHeaderDto = fulfillmentTrackDeliveryResponseConverter.populateMessageHeaderDTO(participantDto, trackingRequest);
        assertNotNull(messageHeaderDto.getVxpMessageID());
        assertEquals("2.1.3", messageHeaderDto.getVxpHeaderVersion());
        assertEquals(WorkflowNameEnum.SALIVARY_KIT_ORDER, messageHeaderDto.getVxpWorkflowName());
        assertEquals(MessageSpecificationEnum.FULFILMENT_TRACK_DELIVERY_RESPONSE, messageHeaderDto.getVxpMessageSpec());
        assertTrue(messageHeaderDto.getVxpMessageTimestamp() > 0);
        assertEquals("AfterShip", messageHeaderDto.getSource());
        assertEquals("2.1.2", messageHeaderDto.getVxpMessageSpecVersion());
        assertEquals(RequestOriginatorEnum.PTBE, messageHeaderDto.getVxpOriginator());
        assertNotNull(messageHeaderDto.getVxpWorkflowInstanceID());
        assertEquals(IntegrationPatternEnum.WORKFLOW, messageHeaderDto.getVxpPattern());
        assertEquals(1000L, messageHeaderDto.getVxpUserID());
        assertEquals(ContextTypeEnum.EVENT, messageHeaderDto.getVxpTrigger());
    }

    @Test
    @DisplayName("When valid notificationDTO is provided then verify messageHeaderDto")
    void populateMessageHeaderDTONonNull() throws JsonProcessingException {
        ParticipantDetailsDto participantDto = new ParticipantDetailsDto();
        participantDto.setVibrentID(1000L);
        trackingRequest.setHeader(gatMessageHeaderDto());
        MessageHeaderDto messageHeaderDto = fulfillmentTrackDeliveryResponseConverter.populateMessageHeaderDTO(participantDto, trackingRequest);
        assertNotNull(messageHeaderDto.getVxpMessageID());
        assertEquals(null, messageHeaderDto.getVxpHeaderVersion());
        assertEquals(MessageSpecificationEnum.FULFILMENT_TRACK_DELIVERY_RESPONSE, messageHeaderDto.getVxpMessageSpec());
        assertTrue(messageHeaderDto.getVxpMessageTimestamp() > 0);
        assertEquals("AfterShip", messageHeaderDto.getSource());
        assertEquals(RequestOriginatorEnum.PTBE, messageHeaderDto.getVxpOriginator());
        assertEquals(1000L, messageHeaderDto.getVxpUserID());
    }

    private String gatMessageHeaderDto() throws JsonProcessingException {
        MessageHeaderDto messageHeaderDto = new MessageHeaderDto();
        return JacksonUtil.getMapper().writeValueAsString(messageHeaderDto);
    }

    private void initializeTracking() {
        Map<String, String> customFields = new HashMap<>();
        customFields.put(CUSTOM_FIELD_VIBRENT_ID, "1000");
        customFields.put(CUSTOM_FIELD_EXTERNAL_ID, "P1000");

        tracking = new Tracking();
        tracking.setCustomFields(customFields);
        tracking.setTrackingNumber("tracking_number_1");
        tracking.setExpectedDelivery("2021-08-15");
        tracking.setTag("InTransit");
        tracking.setLastUpdatedAt(lastUpdatedAt);

    }

    private void initializeTrackingRequest() {
        trackingRequest = new TrackingRequest();
        trackingRequest.setFulfillmentOrderID(0L);
        trackingRequest.setProvider("USPS");
        trackingRequest.setOperation(OperationEnum.TRACK_DELIVERY);
        trackingRequest.setStatus(StatusEnum.PENDING_TRACKING.toValue());
    }
}