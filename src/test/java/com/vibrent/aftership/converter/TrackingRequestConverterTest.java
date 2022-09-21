package com.vibrent.aftership.converter;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.vibrent.aftership.domain.TrackingRequest;
import com.vibrent.aftership.vo.TrackDeliveryRequestVo;
import com.vibrent.vxp.workflow.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TrackingRequestConverterTest {

    private static final String VXP_HEADER_VERSION = "2.1.3";
    private static final String VXP_MESSAGE_SPEC_VERSION = "2.1.2";
    private static final String SOURCE_AFTER_SHIP = "AfterShip";

    private TrackingRequestConverter trackingRequestConverter;

    private TrackDeliveryRequestVo trackDeliveryRequestVo;
    private MessageHeaderDto messageHeaderDto;

    @BeforeEach
    void setUp() {
        trackingRequestConverter = new TrackingRequestConverter();
        initializeTrackDeliveryRequestVo();
        initializeMessageheaderDto();
    }

    @Test
    void convertToTrackingRequestWhenValid() {
        TrackingRequest trackingRequest = trackingRequestConverter.toTrackingRequest(trackDeliveryRequestVo, messageHeaderDto);
        assertEquals("tracking_number_1", trackingRequest.getTrackingId());
        assertEquals(ProviderEnum.USPS.toValue(), trackingRequest.getProvider());
        assertEquals(OperationEnum.TRACK_DELIVERY, trackingRequest.getOperation());
        assertEquals(StatusEnum.PENDING_TRACKING.toValue(), trackingRequest.getStatus());
        assertEquals("{\"externalId\":\"P1000\",\"vibrentId\":1000}", trackingRequest.getParticipant());
        assertEquals("{\"source\":\"AfterShip\",\"VXP-Header-Version\":\"2.1.3\",\"VXP-Message-Id\":\"MessageID_1\",\"VXP-Message-Spec\":\"TRACK_DELIVERY_RESPONSE\",\"VXP-Message-Spec-Version\":\"2.1.2\",\"VXP-Message-Timestamp\":1630645343162,\"VXP-Originator\":\"PTBE\",\"VXP-Pattern\":\"WORKFLOW\",\"VXP-Trigger\":\"EVENT\",\"VXP-User-ID\":1000,\"VXP-Workflow-Instance-ID\":\"WorkflowInstanceID_1\",\"VXP-Workflow-Name\":\"SALIVARY_KIT_ORDER\"}", trackingRequest.getHeader());
    }

    @Test
    void convertToTrackingRequestWhenInValidData() {
        Logger logger = (Logger) LoggerFactory.getLogger(TrackingRequestConverter.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        logger.addAppender(listAppender);
        listAppender.start();

        trackingRequestConverter.toTrackingRequest(null, messageHeaderDto);

        var logList = listAppender.list;
        assertEquals("WARN",logList.get(0).getLevel().toString());
        assertTrue(logList.get(0).getMessage().contains("Null trackDeliveryRequestDto or messageHeader is provided to converter"));
    }

    @Test
    void convertToTrackingRequestWhenInValidHeader() {
        Logger logger = (Logger) LoggerFactory.getLogger(TrackingRequestConverter.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        logger.addAppender(listAppender);
        listAppender.start();

        trackingRequestConverter.toTrackingRequest(trackDeliveryRequestVo, null);

        var logList = listAppender.list;
        assertEquals("WARN",logList.get(0).getLevel().toString());
        assertTrue(logList.get(0).getMessage().contains("Null trackDeliveryRequestDto or messageHeader is provided to converter"));
    }



    private void initializeTrackDeliveryRequestVo() {
        trackDeliveryRequestVo = new TrackDeliveryRequestVo();
        trackDeliveryRequestVo.setTrackingID("tracking_number_1");
        trackDeliveryRequestVo.setCarrierCode(ProviderEnum.USPS.name());

        ParticipantDetailsDto participantDto = new ParticipantDetailsDto();
        participantDto.setExternalID("P1000");
        participantDto.setVibrentID(1000L);
        trackDeliveryRequestVo.setParticipant(participantDto);
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
