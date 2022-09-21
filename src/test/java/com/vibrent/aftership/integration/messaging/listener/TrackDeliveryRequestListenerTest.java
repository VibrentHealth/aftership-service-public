package com.vibrent.aftership.integration.messaging.listener;


import com.vibrent.aftership.integration.IntegrationTestBase;
import com.vibrent.aftership.messaging.listener.TrackDeliveryRequestListener;
import com.vibrent.aftership.service.TrackingRequestService;
import com.vibrent.aftership.vo.TrackDeliveryRequestVo;
import com.vibrent.vxp.workflow.MessageHeaderDto;
import com.vibrent.vxp.workflow.MessageSpecificationEnum;
import com.vibrent.vxp.workflow.TrackDeliveryRequestDto;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.KafkaContainer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.vibrent.aftership.constants.KafkaConstants.VXP_MESSAGE_SPEC;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

public class TrackDeliveryRequestListenerTest extends IntegrationTestBase {
    static KafkaTemplate<String, TrackDeliveryRequestDto> template = null;

    @MockBean
    TrackingRequestService trackingRequestService;

    @Autowired
    TrackDeliveryRequestListener trackDeliveryRequestListener;

    @BeforeAll
    public static void setUp() {
        KafkaContainer kafkaContainer = startKafkaContainer();
        template = getKafkaTemplate(kafkaContainer);
    }

    @DisplayName("when non TrackDeliveryRequest message is received " +
            "then verify message is not processed.")
    @Test
    void ignoreWhenNonTrackDeliveryRequestMsgIsReceived() {
        Message<TrackDeliveryRequestDto> message = buildMessage(new TrackDeliveryRequestDto(), MessageSpecificationEnum.WORKFLOW_REQUEST);
        template.send(message);
        waitForSeconds(3);
        verify(trackingRequestService, Mockito.times(0)).createTrackDeliveryRequest(any(TrackDeliveryRequestVo.class),
                any(MessageHeaderDto.class));
    }

    @DisplayName("when non TrackDeliveryRequest message is received " +
            "then verify message is not processed.")
    @Test
    void ignoreTrackDeliveryRequestWhenKafkaIsDisabled() {
        ReflectionTestUtils.setField(trackDeliveryRequestListener, "kafkaEnabled", false );
        trackDeliveryRequestListener.listener( new TrackDeliveryRequestDto(), null);
        verify(trackingRequestService, Mockito.times(0)).createTrackDeliveryRequest(any(TrackDeliveryRequestVo.class),
                any(MessageHeaderDto.class));
    }

    private Message<TrackDeliveryRequestDto> buildMessage(TrackDeliveryRequestDto payload, MessageSpecificationEnum messageSpecificationEnum) {

        MessageBuilder<TrackDeliveryRequestDto> messageBuilder = MessageBuilder.withPayload(payload);

        messageBuilder.setHeader(KafkaHeaders.TOPIC, "event.vxp.usps.tracking.inbound");
        messageBuilder.setHeader(VXP_MESSAGE_SPEC, messageSpecificationEnum.toValue());
        return messageBuilder.build();
    }

    @SneakyThrows
    static void waitForSeconds(int sec) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await(sec, TimeUnit.SECONDS);
    }
}
