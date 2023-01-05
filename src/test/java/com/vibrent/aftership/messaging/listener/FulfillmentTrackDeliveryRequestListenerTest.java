package com.vibrent.aftership.messaging.listener;


import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.vibrent.aftership.converter.TrackDeliveryRequestConverter;
import com.vibrent.aftership.service.ExternalLogService;
import com.vibrent.aftership.service.TrackingRequestService;
import com.vibrent.aftership.util.JacksonUtil;
import com.vibrent.aftership.vo.TrackDeliveryRequestVo;
import com.vibrent.vxp.workflow.FulfillmentTrackDeliveryRequestDto;
import com.vibrent.vxp.workflow.MessageHeaderDto;
import com.vibrent.vxp.workflow.MessageSpecificationEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.List;

import static com.vibrent.aftership.constants.KafkaConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class FulfillmentTrackDeliveryRequestListenerTest {
    @Mock
    TrackingRequestService trackingRequestService;

    private FulfillmentTrackDeliveryRequestListener fulfillmentTrackDeliveryRequestListener;
    @Mock
    private TrackDeliveryRequestConverter trackDeliveryRequestConverter;
    @Mock
    private ExternalLogService externalLogService;

    @BeforeEach
    void setUp() {
        fulfillmentTrackDeliveryRequestListener = new FulfillmentTrackDeliveryRequestListener(true, trackingRequestService, trackDeliveryRequestConverter, externalLogService);
    }

    @DisplayName("when non FulfillmentTrackDeliveryRequest message is received " +
            "then verify message is not processed.")
    @Test
    void shouldNotProcessWhenKafkaIsNotEnabled() throws JsonProcessingException {
        Message<FulfillmentTrackDeliveryRequestDto> message = buildMessage(new FulfillmentTrackDeliveryRequestDto(), MessageSpecificationEnum.WORKFLOW_REQUEST);

        Logger logger = (Logger) LoggerFactory.getLogger(FulfillmentTrackDeliveryRequestListener.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        logger.addAppender(listAppender);
        listAppender.start();

        fulfillmentTrackDeliveryRequestListener.listener(buildPayload(message.getPayload()), message.getHeaders());
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("INFO", logsList.get(0).getLevel().toString());
        verify(trackingRequestService, Mockito.times(0)).createTrackDeliveryRequest(any(TrackDeliveryRequestVo.class), any(MessageHeaderDto.class));
    }

    @DisplayName("when non FulfillmentTrackDeliveryRequest message is received " +
            "then verify message is not processed.")
    @Test
    void ignoreTrackDeliveryRequestWhenKafkaIsDisabled() throws JsonProcessingException {

        fulfillmentTrackDeliveryRequestListener = new FulfillmentTrackDeliveryRequestListener(false, trackingRequestService, trackDeliveryRequestConverter, externalLogService);
        Message<FulfillmentTrackDeliveryRequestDto> message = buildMessage(new FulfillmentTrackDeliveryRequestDto(), MessageSpecificationEnum.FULFILMENT_TRACK_DELIVERY_REQUEST);
        Logger logger = (Logger) LoggerFactory.getLogger(FulfillmentTrackDeliveryRequestListener.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        logger.addAppender(listAppender);
        listAppender.start();

        fulfillmentTrackDeliveryRequestListener.listener(buildPayload(message.getPayload()), message.getHeaders());
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("WARN", logsList.get(0).getLevel().toString());
    }

    private Message<FulfillmentTrackDeliveryRequestDto> buildMessage(FulfillmentTrackDeliveryRequestDto payload, MessageSpecificationEnum messageSpecificationEnum) {

        MessageBuilder<FulfillmentTrackDeliveryRequestDto> messageBuilder = MessageBuilder.withPayload(payload);
        messageBuilder.setHeader(KafkaHeaders.TOPIC, "event.vxp.tracking.order.request");
        messageBuilder.setHeader(VXP_MESSAGE_SPEC, messageSpecificationEnum.toValue());
        messageBuilder.setHeader(VXP_TRIGGER, "EVENT");
        messageBuilder.setHeader(VXP_PATTERN, "WORKFLOW");
        messageBuilder.setHeader(VXP_ORIGINATOR, "PTBE");
        return messageBuilder.build();
    }
    private byte[] buildPayload(FulfillmentTrackDeliveryRequestDto fulfillmentTrackDeliveryRequestDto) throws JsonProcessingException {
        return JacksonUtil.getMapper().writeValueAsBytes(fulfillmentTrackDeliveryRequestDto);
    }
}
