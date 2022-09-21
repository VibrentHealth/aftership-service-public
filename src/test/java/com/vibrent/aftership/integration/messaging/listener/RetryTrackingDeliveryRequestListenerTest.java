package com.vibrent.aftership.integration.messaging.listener;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.vibrent.aftership.dto.RetryRequestDTO;
import com.vibrent.aftership.messaging.listener.RetryTrackingDeliveryRequestListener;
import com.vibrent.aftership.repository.TrackingRequestErrorRepository;
import com.vibrent.aftership.service.TrackingRequestService;
import com.vibrent.aftership.vo.TrackDeliveryRequestVo;
import com.vibrent.vxp.workflow.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.MessageHeaders;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetryTrackingDeliveryRequestListenerTest {
    public static final String TRACKING_ID = "879658";
    private static final String VXP_HEADER_VERSION = "2.1.3";
    private static final String VXP_MESSAGE_SPEC_VERSION = "2.1.2";
    private static final String SOURCE_AFTER_SHIP = "AfterShip";

    private boolean kafkaEnabled = true;

    @Mock
    private TrackingRequestService trackingRequestService;

    @Mock
    private TrackingRequestErrorRepository trackingRequestErrorRepository;

    private RetryTrackingDeliveryRequestListener retryTrackingDeliveryRequestListener;

    @MockBean
    MessageHeaders messageHeaders;

    @BeforeEach
    void setUp() {
        retryTrackingDeliveryRequestListener = new RetryTrackingDeliveryRequestListener(kafkaEnabled, trackingRequestService, trackingRequestErrorRepository);
    }

    @Test
    void testWhenKafkaIsDisabled() {
        retryTrackingDeliveryRequestListener = new RetryTrackingDeliveryRequestListener(false, trackingRequestService, trackingRequestErrorRepository);
        retryTrackingDeliveryRequestListener.listener(createRetryRequestDTO());

        verify(trackingRequestService, Mockito.times(0)).createTrackDeliveryRequest(any(TrackDeliveryRequestVo.class),
                any(MessageHeaderDto.class));
    }


    @DisplayName("when Retry Track Delivery Request message is received " +
            "then verify message is processed.")
    @Test
    void processOnlyWhenRetryTrackDeliveryRequestMsgIsReceived() {
        retryTrackingDeliveryRequestListener = new RetryTrackingDeliveryRequestListener(kafkaEnabled, trackingRequestService, trackingRequestErrorRepository);
        retryTrackingDeliveryRequestListener.listener(createRetryRequestDTO());

        verify(trackingRequestService, Mockito.timeout(3000).times(1)).createTrackDeliveryRequest(any(TrackDeliveryRequestVo.class),
                any(MessageHeaderDto.class));
    }

    @DisplayName("when Retry Track Delivery Request message is received " +
            "And create tracking gets successes" +
            "then verify message is processed and tracking request error entry gets deleted")
    @Test
    void testWhenRetryTrackDeliveryRequestMsgIsReceivedAndTrackingGetSuccess() {
        retryTrackingDeliveryRequestListener = new RetryTrackingDeliveryRequestListener(kafkaEnabled, trackingRequestService, trackingRequestErrorRepository);
        when(trackingRequestService.createTrackDeliveryRequest(any(TrackDeliveryRequestVo.class),any(MessageHeaderDto.class))).thenReturn(true);
        retryTrackingDeliveryRequestListener.listener(createRetryRequestDTO());

        verify(trackingRequestService, Mockito.timeout(3000).times(1)).createTrackDeliveryRequest(any(TrackDeliveryRequestVo.class),
                any(MessageHeaderDto.class));
        verify(trackingRequestErrorRepository, times(1)).deleteByTrackingId(anyString());
    }

    @DisplayName("when Retry Track Delivery Request message is received " +
            "And create tracking gets failed" +
            "then verify message is processed and tracking request error entry not deleted")
    @Test
    void testWhenRetryTrackDeliveryRequestMsgIsReceivedAndTrackingGetFailed() {
        retryTrackingDeliveryRequestListener = new RetryTrackingDeliveryRequestListener(kafkaEnabled, trackingRequestService, trackingRequestErrorRepository);
        when(trackingRequestService.createTrackDeliveryRequest(any(TrackDeliveryRequestVo.class),any(MessageHeaderDto.class))).thenReturn(false);
        retryTrackingDeliveryRequestListener.listener(createRetryRequestDTO());

        verify(trackingRequestService, Mockito.timeout(3000).times(1)).createTrackDeliveryRequest(any(TrackDeliveryRequestVo.class),
                any(MessageHeaderDto.class));
        verify(trackingRequestErrorRepository, times(0)).deleteByTrackingId(anyString());
    }


    @DisplayName("when Retry Track Delivery Request message is received with null tracking " +
            "then verify message is not processed.")
    @Test
    void warnWhenRetryTrackDeliveryRequestMsgHavingNullTrackingRequest() {
        retryTrackingDeliveryRequestListener = new RetryTrackingDeliveryRequestListener(kafkaEnabled, trackingRequestService, trackingRequestErrorRepository);
        var requestDto=createRetryRequestDTO();
        requestDto.setTrackDeliveryRequestVo(null);
        Logger logger = (Logger) LoggerFactory.getLogger(RetryTrackingDeliveryRequestListener.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        logger.addAppender(listAppender);
        listAppender.start();

        retryTrackingDeliveryRequestListener.listener(requestDto);

        var logList = listAppender.list;
        assertEquals("WARN",logList.get(0).getLevel().toString());
        assertTrue(logList.get(0).getMessage().contains("Cannot process retry tracking delivery request"));
        verify(trackingRequestService, Mockito.timeout(3000).times(0)).createTrackDeliveryRequest(any(TrackDeliveryRequestVo.class),
                any(MessageHeaderDto.class));
    }

    @DisplayName("when Retry Track Delivery Request message is received with null message header " +
            "then verify message is not processed.")
    @Test
    void warnWhenRetryTrackDeliveryRequestMsgHavingNullMessageHeader() {
        retryTrackingDeliveryRequestListener = new RetryTrackingDeliveryRequestListener(kafkaEnabled, trackingRequestService, trackingRequestErrorRepository);
        var requestDto=createRetryRequestDTO();
        requestDto.setMessageHeaderDto(null);
        Logger logger = (Logger) LoggerFactory.getLogger(RetryTrackingDeliveryRequestListener.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        logger.addAppender(listAppender);
        listAppender.start();

        retryTrackingDeliveryRequestListener.listener(requestDto);

        var logList = listAppender.list;
        assertEquals("WARN",logList.get(0).getLevel().toString());
        assertTrue(logList.get(0).getMessage().contains("Cannot process retry tracking delivery request"));
        verify(trackingRequestService, Mockito.timeout(3000).times(0)).createTrackDeliveryRequest(any(TrackDeliveryRequestVo.class),
                any(MessageHeaderDto.class));
    }

    @DisplayName("when null Retry Track Delivery Request message is received " +
            "then message is not processed.")
    @Test
    void warnWhenNullRetryTrackDeliveryRequestMsg() {
        retryTrackingDeliveryRequestListener = new RetryTrackingDeliveryRequestListener(kafkaEnabled, trackingRequestService, trackingRequestErrorRepository);
        Logger logger = (Logger) LoggerFactory.getLogger(RetryTrackingDeliveryRequestListener.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        logger.addAppender(listAppender);
        listAppender.start();

        retryTrackingDeliveryRequestListener.listener(null);

        var logList = listAppender.list;
        assertEquals("WARN",logList.get(0).getLevel().toString());
        assertTrue(logList.get(0).getMessage().contains("Cannot process retry tracking delivery request"));
        verify(trackingRequestService, Mockito.timeout(3000).times(0)).createTrackDeliveryRequest(any(TrackDeliveryRequestVo.class),
                any(MessageHeaderDto.class));
    }

    RetryRequestDTO createRetryRequestDTO() {
        RetryRequestDTO retryRequestDTO = new RetryRequestDTO(getTrackDeliveryRequestVo(), getMessageHeaderDto());
        return retryRequestDTO;
    }

    TrackDeliveryRequestVo getTrackDeliveryRequestVo() {

        ParticipantDetailsDto participantDto = new ParticipantDetailsDto();
        participantDto.setExternalID("P1000");
        participantDto.setVibrentID(1000L);
        TrackDeliveryRequestVo trackDeliveryRequestVo = new TrackDeliveryRequestVo();
        trackDeliveryRequestVo.setTrackingID(TRACKING_ID);
        trackDeliveryRequestVo.setCarrierCode(ProviderEnum.USPS.name());
        trackDeliveryRequestVo.setParticipant(participantDto);
        return trackDeliveryRequestVo;
    }

    MessageHeaderDto getMessageHeaderDto() {
        MessageHeaderDto messageHeaderDto = new MessageHeaderDto();
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
        return messageHeaderDto;
    }

    MessageHeaders getMessageHeaders(){
        Map<String, Object> headers = new HashMap<>();
        headers.put("ID","WorkflowInstanceID_");
        MessageHeaders messageHeaders = new MessageHeaders(headers);

        return messageHeaders;
    }

}
