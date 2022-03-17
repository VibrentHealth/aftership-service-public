package com.vibrent.aftership.converter;

import com.aftership.sdk.model.tracking.Tracking;
import com.vibrent.aftership.domain.TrackingRequest;
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
public class TrackDeliveryResponseConverterTest {

    private TrackDeliveryResponseConverter trackDeliveryResponseConverter;
    private Tracking tracking;
    private TrackingRequest trackingRequest;
    private final Date lastUpdatedAt = new Date();

    @BeforeEach
    void setUp() throws Exception {
        trackDeliveryResponseConverter = new TrackDeliveryResponseConverter();
        initializeTracking();
        initializeTrackingRequest();
    }

    @Test
    @DisplayName("When valid notificationDTO is provided then verify trackDeliveryResponseDto")
    void convertWhenValidNotificationDtoIsProvided() {
        TrackDeliveryResponseDto trackDeliveryResponseDto = trackDeliveryResponseConverter.convert(tracking, trackingRequest);
        assertEquals("tracking_number_1", trackDeliveryResponseDto.getTrackingID());
        assertEquals(OperationEnum.TRACK_DELIVERY, trackDeliveryResponseDto.getOperation());
        assertEquals(lastUpdatedAt.getTime(), trackDeliveryResponseDto.getDateTime());
        assertEquals(ProviderEnum.USPS, trackDeliveryResponseDto.getProvider());
        assertEquals(StatusEnum.IN_TRANSIT, trackDeliveryResponseDto.getStatus());

        ParticipantDto participantDto = trackDeliveryResponseDto.getParticipant();
        assertNotNull(participantDto);
        assertEquals(1000, participantDto.getVibrentID());
        assertEquals("P1000", participantDto.getExternalID());

        List<DateDto> dates = trackDeliveryResponseDto.getDates();
        assertNotNull(dates);
        assertEquals(1, dates.size());
        DateDto dateDto = dates.get(0);
        assertEquals(DateTypeEnum.EXPECTED_DELIVERY_DATE, dateDto.getType());
        assertEquals(1628985600000L, dateDto.getDate());
    }

    @Test
    @DisplayName("When notificationDTO tag is Delivered then verify trackDeliveryResponseDto status")
    void convertWhenNotificationDtoTagIsDeliveredThenVerifyStatus() {
        tracking.setTag("Delivered");
        TrackDeliveryResponseDto trackDeliveryResponseDto = trackDeliveryResponseConverter.convert(tracking, trackingRequest);
        assertEquals(StatusEnum.DELIVERED, trackDeliveryResponseDto.getStatus());
    }

    @Test
    @DisplayName("When notificationDTO tag is Exception then verify trackDeliveryResponseDto status")
    void convertWhenNotificationDtoTagIsErrorThenVerifyStatus() {
        tracking.setTag("Exception");
        TrackDeliveryResponseDto trackDeliveryResponseDto = trackDeliveryResponseConverter.convert(tracking, trackingRequest);
        assertEquals(StatusEnum.ERROR, trackDeliveryResponseDto.getStatus());
    }

    @Test
    @DisplayName("When notificationDTO tag is Unknown then verify trackDeliveryResponseDto status")
    void convertWhenNotificationDtoTagIsUnknownThenVerifyStatus() {
        tracking.setTag("Unknown");
        TrackDeliveryResponseDto trackDeliveryResponseDto = trackDeliveryResponseConverter.convert(tracking, trackingRequest);
        assertEquals(StatusEnum.UNRECOGNIZED, trackDeliveryResponseDto.getStatus());
    }

    @Test
    @DisplayName("When notificationDTO tag is null then verify trackDeliveryResponseDto status")
    void convertWhenNotificationDtoTagIsNullThenVerifyStatus() {
        tracking.setTag(null);
        TrackDeliveryResponseDto trackDeliveryResponseDto = trackDeliveryResponseConverter.convert(tracking, trackingRequest);
        assertNull(trackDeliveryResponseDto.getStatus());
    }

    @Test
    @DisplayName("When valid notificationDTO is provided then verify messageHeaderDto")
    void populateMessageHeaderDTO() {
        MessageHeaderDto messageHeaderDto = trackDeliveryResponseConverter.populateMessageHeaderDTO(tracking, trackingRequest);
        assertNotNull(messageHeaderDto.getVxpMessageID());
        assertEquals("2.1.3", messageHeaderDto.getVxpHeaderVersion());
        assertEquals(WorkflowNameEnum.SALIVARY_KIT_ORDER, messageHeaderDto.getVxpWorkflowName());
        assertEquals(MessageSpecificationEnum.TRACK_DELIVERY_RESPONSE, messageHeaderDto.getVxpMessageSpec());
        assertTrue(messageHeaderDto.getVxpMessageTimestamp() > 0);
        assertEquals("AfterShip", messageHeaderDto.getSource());
        assertEquals("2.1.2", messageHeaderDto.getVxpMessageSpecVersion());
        assertEquals(RequestOriginatorEnum.PTBE, messageHeaderDto.getVxpOriginator());
        assertNotNull(messageHeaderDto.getVxpWorkflowInstanceID());
        assertEquals(IntegrationPatternEnum.WORKFLOW, messageHeaderDto.getVxpPattern());
        assertEquals(1000L, messageHeaderDto.getVxpUserID());
        assertEquals(ContextTypeEnum.EVENT, messageHeaderDto.getVxpTrigger());
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
        trackingRequest.setProvider(ProviderEnum.USPS);
        trackingRequest.setOperation(OperationEnum.TRACK_DELIVERY);
        trackingRequest.setStatus(StatusEnum.PENDING_TRACKING.toValue());
        trackingRequest.setProvider(ProviderEnum.USPS);
    }
}