package com.vibrent.aftership.messaging.producer.impl;

import com.vibrent.aftership.dto.RetryRequestDTO;
import com.vibrent.aftership.messaging.producer.MessageProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Slf4j
@Service
public class RetryTrackingDeliveryRequestProducer implements MessageProducer<RetryRequestDTO> {
    private final KafkaTemplate<String, RetryRequestDTO> kafkaTemplate;
    private boolean kafkaEnabled;
    private final String topicName;

    public RetryTrackingDeliveryRequestProducer(KafkaTemplate<String, RetryRequestDTO> kafkaTemplate,
                                                @Value("${kafka.enabled}") boolean kafkaEnabled,
                                                @Value("${kafka.topics.tracking.retryRequest}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaEnabled = kafkaEnabled;
        this.topicName = topicName;
    }


    @Override
    public void setKafkaEnabled(boolean newState) {
        this.kafkaEnabled = newState;
    }

    @Override
    public void send(RetryRequestDTO msg) {
        if (msg == null || msg.getTrackDeliveryRequestDto() == null) {
            log.warn("Aftership | Cannot publish retry tracking delivery request as Retry Request dto or track delivery request dto is null");
            return;
        }
        if (!kafkaEnabled) {
            log.error("Kafka is not enabled - Failed to send update for retryRequest with trackingID {}", msg.getTrackDeliveryRequestDto().getTrackingID());
            return;
        }
        Message<RetryRequestDTO> retryRequestDTOMessage = buildMessage(msg, topicName);
        kafkaTemplate.send(retryRequestDTOMessage)
                .addCallback(new ListenableFutureCallback<SendResult<String, RetryRequestDTO>>() {
                                 @Override
                                 public void onFailure(Throwable throwable) {
                                     log.warn("Aftership | Fail to sent retry request for tracking. TrackingId : {}", msg.getTrackDeliveryRequestDto().getTrackingID());
                                 }

                                 @Override
                                 public void onSuccess(SendResult<String, RetryRequestDTO> retryRequestDTOSendResult) {
                                     log.debug("Aftership | Successfully sent retry request for tracking. TrackingId : {}", msg.getTrackDeliveryRequestDto().getTrackingID());
                                 }
                             }

                );


    }

}
