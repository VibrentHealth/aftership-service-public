package com.vibrent.aftership.converter;

import com.vibrent.aftership.domain.TrackingRequestError;
import com.vibrent.aftership.exception.AfterShipRetriableException;
import com.vibrent.aftership.repository.TrackingRequestErrorRepository;
import com.vibrent.aftership.util.JacksonUtil;
import com.vibrent.vxp.workflow.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TrackingRequestErrorConverterTest {

    private static final String VXP_HEADER_VERSION = "2.1.3";
    private static final String VXP_MESSAGE_SPEC_VERSION = "2.1.2";
    private static final String SOURCE_AFTER_SHIP = "AfterShip";

    private TrackingRequestErrorConverter trackingRequestErrorConverter;

    private TrackDeliveryRequestDto trackDeliveryRequestDto;
    private MessageHeaderDto messageHeaderDto;

    @Mock
    private TrackingRequestErrorRepository trackingRequestErrorRepository;

    @BeforeEach
    void setUp() {
        trackingRequestErrorConverter = new TrackingRequestErrorConverter(trackingRequestErrorRepository);
        initializeTrackDeliveryRequestDto();
        initializeMessageHeaderDto();
    }

    @Test
    void toTrackingRequestError() throws Exception {
        TrackingRequestError trackingRequestError = trackingRequestErrorConverter.toTrackingRequestError(trackDeliveryRequestDto,
                messageHeaderDto, new AfterShipRetriableException("AfterShip: Invalid key", 401));

        assertNotNull(trackingRequestError);
        assertEquals("tracking_number_1", trackingRequestError.getTrackingId());
        assertEquals(401, trackingRequestError.getErrorCode());
        assertEquals(0, trackingRequestError.getRetryCount());
        assertEquals(JacksonUtil.getMapper().writeValueAsString(messageHeaderDto), trackingRequestError.getHeader());
        assertEquals(JacksonUtil.getMapper().writeValueAsString(trackDeliveryRequestDto), trackingRequestError.getTrackDeliveryRequest());
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


    private void initializeMessageHeaderDto() {
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