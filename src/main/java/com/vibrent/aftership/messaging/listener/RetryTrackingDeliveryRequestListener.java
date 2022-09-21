package com.vibrent.aftership.messaging.listener;

import com.vibrent.aftership.dto.RetryRequestDTO;
import com.vibrent.aftership.repository.TrackingRequestErrorRepository;
import com.vibrent.aftership.service.TrackingRequestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Kafka Listener for Retrying Tracking Requests
 */
@Service
@Slf4j
public class RetryTrackingDeliveryRequestListener {
    private final boolean kafkaEnabled;
    private final TrackingRequestService trackingRequestService;
    private final TrackingRequestErrorRepository trackingRequestErrorRepository;

    public RetryTrackingDeliveryRequestListener(@Value("${kafka.enabled}") boolean kafkaEnabled,
                                                TrackingRequestService trackingRequestService, TrackingRequestErrorRepository trackingRequestErrorRepository) {
        this.kafkaEnabled = kafkaEnabled;
        this.trackingRequestService = trackingRequestService;
        this.trackingRequestErrorRepository = trackingRequestErrorRepository;
    }

    @Transactional
    @KafkaListener(topics = "${kafka.topics.tracking.retryRequest}", groupId = "retryTrackingRequestListener",
            containerFactory = "kafkaListenerContainerFactoryRetryTrackDeliveryRequest")
    public void listener(@Payload RetryRequestDTO retryRequestDTO) {
        if (!kafkaEnabled) {
            log.warn("kafka is not enabled");
            return;
        }

        if (retryRequestDTO == null || retryRequestDTO.getTrackDeliveryRequestVo() == null || retryRequestDTO.getMessageHeaderDto() == null) {
            log.warn("Aftership | Cannot process retry tracking delivery request as Retry Request dto is null. RetryRequestDTO: {}", retryRequestDTO);
            return;
        }

        try {

            boolean trackDeliveryRequest = trackingRequestService.createTrackDeliveryRequest(retryRequestDTO.getTrackDeliveryRequestVo(), retryRequestDTO.getMessageHeaderDto());
            if (trackDeliveryRequest && StringUtils.hasText(retryRequestDTO.getTrackDeliveryRequestVo().getTrackingID())) {
                trackingRequestErrorRepository.deleteByTrackingId(retryRequestDTO.getTrackDeliveryRequestVo().getTrackingID());
                log.info("AfterShip: Successfully retried tracking request for trackingID: {}", (retryRequestDTO.getTrackDeliveryRequestVo().getTrackingID()));
            }
        } catch (Exception e) {
            log.error("Error while retrying tracking request. {}", retryRequestDTO, e);
        }
    }
}
