package com.vibrent.aftership.messaging.listener;


import com.vibrent.aftership.converter.TrackDeliveryRequestConverter;
import com.vibrent.aftership.messaging.KafkaMessageBuilder;
import com.vibrent.aftership.service.TrackingRequestService;
import com.vibrent.aftership.vo.TrackDeliveryRequestVo;
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
    private final TrackDeliveryRequestConverter trackDeliveryRequestConverter;

    public TrackDeliveryRequestListener(@Value("${kafka.enabled}") boolean kafkaEnabled,
                                        TrackingRequestService trackingRequestService, TrackDeliveryRequestConverter trackDeliveryRequestConverter) {
        this.kafkaEnabled = kafkaEnabled;
        this.trackingRequestService = trackingRequestService;
        this.trackDeliveryRequestConverter = trackDeliveryRequestConverter;
    }

    @KafkaListener(topics = "${kafka.topics.tracking.request}", groupId = "aftershipTrackingRequestListener",
            containerFactory = "kafkaListenerContainerFactoryTrackDeliveryRequest")
    public void listener(@Payload TrackDeliveryRequestDto trackDeliveryRequestDto,
                         @Headers MessageHeaders requestHeaders) {
        if (!kafkaEnabled) {
            log.warn("kafka is not enabled");
            return;
        }

        try {
            TrackDeliveryRequestVo trackDeliveryRequestVo = trackDeliveryRequestConverter.toTrackDeliveryRequestVo(trackDeliveryRequestDto);
            trackingRequestService.createTrackDeliveryRequest(trackDeliveryRequestVo, KafkaMessageBuilder.toMessageHeaderDto(requestHeaders));
        } catch (Exception e) {
            log.error("Error while processing the create tracking request - {}", trackDeliveryRequestDto, e);
        }
    }
}
