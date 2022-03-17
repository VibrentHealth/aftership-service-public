package com.vibrent.aftership.converter;

import com.vibrent.aftership.domain.TrackingRequest;
import com.vibrent.vxp.workflow.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class TrackingRequestConverterTest {

    private static final String VXP_HEADER_VERSION = "2.1.3";
    private static final String VXP_MESSAGE_SPEC_VERSION = "2.1.2";
    private static final String SOURCE_AFTER_SHIP = "AfterShip";

    private TrackingRequestConverter trackingRequestConverter;

    private TrackDeliveryRequestDto trackDeliveryRequestDto;
    private MessageHeaderDto messageHeaderDto;

    @BeforeEach
    void setUp() throws Exception {
        trackingRequestConverter = new TrackingRequestConverter();
        initializeTrackDeliveryRequestDto();
        initializeMessageheaderDto();
    }

    @Test
    void convertToTrackingRequestWhenValid() {
        TrackingRequest trackingRequest = trackingRequestConverter.toTrackingRequest(trackDeliveryRequestDto, messageHeaderDto);
        assertEquals("tracking_number_1", trackingRequest.getTrackingId());
        assertEquals(ProviderEnum.USPS, trackingRequest.getProvider());
        assertEquals(OperationEnum.TRACK_DELIVERY, trackingRequest.getOperation());
        assertEquals(StatusEnum.PENDING_TRACKING.toValue(), trackingRequest.getStatus());
        assertEquals("{\"emailAddress\":\"emailaddress@abc.com\",\"externalId\":\"P1000\",\"phoneNumber\":\"1234567890\",\"vibrentId\":1000}", trackingRequest.getParticipant());
        assertEquals("{\"source\":\"AfterShip\",\"VXP-Header-Version\":\"2.1.3\",\"VXP-Message-Id\":\"MessageID_1\",\"VXP-Message-Spec\":\"TRACK_DELIVERY_RESPONSE\",\"VXP-Message-Spec-Version\":\"2.1.2\",\"VXP-Message-Timestamp\":1630645343162,\"VXP-Originator\":\"PTBE\",\"VXP-Pattern\":\"WORKFLOW\",\"VXP-Trigger\":\"EVENT\",\"VXP-User-ID\":1000,\"VXP-Workflow-Instance-ID\":\"WorkflowInstanceID_1\",\"VXP-Workflow-Name\":\"SALIVARY_KIT_ORDER\"}", trackingRequest.getHeader());
    }

    private void initializeTrackDeliveryRequestDto() {
        trackDeliveryRequestDto = new TrackDeliveryRequestDto();
        trackDeliveryRequestDto.setOperation(OperationEnum.TRACK_DELIVERY);
        trackDeliveryRequestDto.setTrackingID("tracking_number_1");
        trackDeliveryRequestDto.setProvider(ProviderEnum.USPS);

        ParticipantDto participantDto = new ParticipantDto();
        participantDto.setExternalID("P1000");
        participantDto.setVibrentID(1000L);
        participantDto.setEmailAddress("emailaddress@abc.com");
        participantDto.setPhoneNumber("1234567890");
        trackDeliveryRequestDto.setParticipant(participantDto);
    }


    private void initializeMessageheaderDto() {
        messageHeaderDto = new MessageHeaderDto();
        messageHeaderDto.setVxpMessageID("MessageID_1");
        messageHeaderDto.setVxpHeaderVersion(VXP_HEADER_VERSION);
        messageHeaderDto.setVxpWorkflowName(WorkflowNameEnum.SALIVARY_KIT_ORDER);
        messageHeaderDto.setVxpMessageSpec(MessageSpecificationEnum.TRACK_DELIVERY_RESPONSE);
        messageHeaderDto.setVxpMessageTimestamp(1630645343162L);
        messageHeaderDto.setSource(SOURCE_AFTER_SHIP);
        messageHeaderDto.setVxpMessageSpecVersion(VXP_MESSAGE_SPEC_VERSION);
        messageHeaderDto.setVxpOriginator(RequestOriginatorEnum.PTBE);
        messageHeaderDto.setVxpWorkflowInstanceID("WorkflowInstanceID_1");
        messageHeaderDto.setVxpPattern(IntegrationPatternEnum.WORKFLOW);
        messageHeaderDto.setVxpUserID(1000L);
        messageHeaderDto.setVxpTrigger(ContextTypeEnum.EVENT);
    }
}