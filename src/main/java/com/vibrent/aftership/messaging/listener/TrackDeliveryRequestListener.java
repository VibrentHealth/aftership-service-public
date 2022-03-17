package com.vibrent.aftership.messaging.listener;


import com.vibrent.aftership.messaging.KafkaMessageBuilder;
import com.vibrent.aftership.service.TrackingRequestService;
import com.vibrent.vxp.workflow.TrackDeliveryRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * Kafka Listener for Tracking Requests
 */
@Service
@Slf4j
public class TrackDeliveryRequestListener {


    private final boolean kafkaEnabled;
    private final TrackingRequestService trackingRequestService;

    public TrackDeliveryRequestListener(@Value("${kafka.enabled}") boolean kafkaEnabled,
                                        TrackingRequestService trackingRequestService) {
        this.kafkaEnabled = kafkaEnabled;
        this.trackingRequestService = trackingRequestService;
    }

    @KafkaListener(topics = "${kafka.topics.tracking.request}", groupId = "aftershipTrackingRequestListener",
            containerFactory = "kafkaListenerContainerFactoryTrackDeliveryRequest")
    public void listener(@Payload TrackDeliveryRequestDto trackDeliveryRequestDto,
                         @Headers MessageHeaders requestHeaders) {
        if (!kafkaEnabled) {
            log.warn("kafka is not enabled");
            return;
        }

        trackingRequestService.createTrackDeliveryRequest(trackDeliveryRequestDto, KafkaMessageBuilder.toMessageHeaderDto(requestHeaders));
    }
}
