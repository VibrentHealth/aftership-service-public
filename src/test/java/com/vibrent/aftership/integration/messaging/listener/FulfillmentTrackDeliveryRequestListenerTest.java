package com.vibrent.aftership.integration.messaging.listener;


import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.vibrent.aftership.integration.IntegrationTestBase;
import com.vibrent.aftership.messaging.listener.FulfillmentTrackDeliveryRequestListener;
import com.vibrent.aftership.service.TrackingRequestService;
import com.vibrent.aftership.vo.TrackDeliveryRequestVo;
import com.vibrent.vxp.workflow.FulfillmentTrackDeliveryRequestDto;
import com.vibrent.vxp.workflow.MessageHeaderDto;
import com.vibrent.vxp.workflow.MessageSpecificationEnum;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.KafkaContainer;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.vibrent.aftership.constants.KafkaConstants.VXP_MESSAGE_SPEC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

public class FulfillmentTrackDeliveryRequestListenerTest extends IntegrationTestBase {
    static KafkaTemplate<String, FulfillmentTrackDeliveryRequestDto> template = null;

    @MockBean
    TrackingRequestService trackingRequestService;

    @Autowired
    FulfillmentTrackDeliveryRequestListener fulfillmentTrackDeliveryRequestListener;

    @BeforeAll
    public static void setUpKafka() {
        KafkaContainer kafkaContainer = startKafkaContainer();
        template = getKafkaTemplate(kafkaContainer);
    }

    @DisplayName("when non FulfillmentTrackDeliveryRequest message is received " +
            "then verify message is not processed.")
    @Test
    void ignoreWhenNonFulfillmentTrackDeliveryRequestMsgIsReceived() {
        Message<FulfillmentTrackDeliveryRequestDto> message = buildMessage(new FulfillmentTrackDeliveryRequestDto(), MessageSpecificationEnum.WORKFLOW_REQUEST);
        Logger logger = (Logger) LoggerFactory.getLogger(FulfillmentTrackDeliveryRequestListener.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        logger.addAppender(listAppender);
        listAppender.start();

        template.send(message);
        waitForSeconds(3);
        List<ILoggingEvent> logsList = listAppender.list;
        assertTrue(logsList.isEmpty());
        verify(trackingRequestService, Mockito.times(0)).createTrackDeliveryRequest(any(TrackDeliveryRequestVo.class),any(MessageHeaderDto.class));
    }

    @DisplayName("when message is received but kafka is not enabled " +
            "then verify message is not processed.")
    @Test
    void ignoreFulfillmentTrackDeliveryRequestWhenKafkaIsDisabled() {
        ReflectionTestUtils.setField(fulfillmentTrackDeliveryRequestListener, "kafkaEnabled", false);

        Logger logger = (Logger) LoggerFactory.getLogger(FulfillmentTrackDeliveryRequestListener.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        logger.addAppender(listAppender);
        listAppender.start();

        fulfillmentTrackDeliveryRequestListener.listener(new FulfillmentTrackDeliveryRequestDto(), null);
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("WARN", logsList.get(0).getLevel().toString());
        verify(trackingRequestService, Mockito.times(0)).createTrackDeliveryRequest(any(TrackDeliveryRequestVo.class),any(MessageHeaderDto.class));
    }

    private Message<FulfillmentTrackDeliveryRequestDto> buildMessage(FulfillmentTrackDeliveryRequestDto payload, MessageSpecificationEnum messageSpecificationEnum) {

        MessageBuilder<FulfillmentTrackDeliveryRequestDto> messageBuilder = MessageBuilder.withPayload(payload);
        messageBuilder.setHeader(KafkaHeaders.TOPIC, "event.vxp.tracking.order.request");
        messageBuilder.setHeader(VXP_MESSAGE_SPEC, messageSpecificationEnum.toValue());
        return messageBuilder.build();
    }

    @SneakyThrows
    static void waitForSeconds(int sec) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await(sec, TimeUnit.SECONDS);
    }
}
