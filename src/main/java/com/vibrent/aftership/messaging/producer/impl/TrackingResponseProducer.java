package com.vibrent.aftership.messaging.producer.impl;

import com.google.common.base.Preconditions;
import com.vibrent.aftership.messaging.producer.MessageProducer;
import com.vibrent.aftership.service.ExternalLogService;
import com.vibrent.vxp.workflow.MessageHeaderDto;
import com.vibrent.vxp.workflow.TrackDeliveryResponseDto;
import com.vibrent.vxp.workflow.TrackDeliveryResponseDtoWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.io.IOException;

/**
 * Kafka producer to send tracking status updates to VRP
 */
@Service
@Slf4j
public class TrackingResponseProducer implements MessageProducer<TrackDeliveryResponseDtoWrapper> {

    private final KafkaTemplate<String, TrackDeliveryResponseDtoWrapper> kafkaTemplate;

    private boolean kafkaEnabled;

    private final String topicName;

    private final ExternalLogService externalLogService;

    public TrackingResponseProducer(KafkaTemplate<String, TrackDeliveryResponseDtoWrapper> kafkaTemplate, ExternalLogService externalLogService,
                                    @Value("${kafka.enabled}") boolean kafkaEnabled, @Value("${kafka.topics.tracking.response}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.externalLogService = externalLogService;
        this.kafkaEnabled = kafkaEnabled;
        this.topicName = topicName;
    }

    @Override
    public void setKafkaEnabled(boolean newState) {
        this.kafkaEnabled = newState;
    }

    @Override
    public void send(TrackDeliveryResponseDtoWrapper msg) {
        Preconditions.checkArgument(msg != null, "TrackDeliveryResponseDtoWrapper cannot be null");

        MessageHeaderDto headerDto = msg.getHeader();
        TrackDeliveryResponseDto trackDeliveryResponseDto;
        try {
            trackDeliveryResponseDto = msg.getPayload();
        } catch (IOException e) {
            log.warn("AfterShip: Error while getting payload from message: {}", msg, e);
            return;
        }

        Preconditions.checkArgument(trackDeliveryResponseDto != null,
                "TrackDeliveryResponseDtoWrapper should non-null TrackDeliveryResponseDto");
        Preconditions.checkArgument(headerDto != null,
                "TrackDeliveryResponseDtoWrapper should non-null MessageHeaderDto");

        if (!kafkaEnabled) {
            log.error("Kafka is not enabled - Failed to send update for trackDeliveryResponseDto with trackingID {}", trackDeliveryResponseDto.getTrackingID());
            return;
        }

        try {
            Message<TrackDeliveryResponseDto> message = buildMessage(trackDeliveryResponseDto, headerDto, topicName);
            kafkaTemplate.send(message).addCallback(newSendResultListenableFutureCallback());
            externalLogService.send(msg, headerDto.getVxpMessageTimestamp(),
                    "AfterShip | Track Delivery Response sent", HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to publish tracking update for trackDeliveryResponseDto with trackingID {}, exception= {}",
                    trackDeliveryResponseDto.getTrackingID(), e);
            externalLogService.send(msg, headerDto.getVxpMessageTimestamp(),
                    "AfterShip | Failed to send Track Delivery Response", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private static ListenableFutureCallback<SendResult<String, TrackDeliveryResponseDtoWrapper>> newSendResultListenableFutureCallback() {
        return new ListenableFutureCallback<SendResult<String, TrackDeliveryResponseDtoWrapper>>() {

            @Override
            public void onSuccess(SendResult<String, TrackDeliveryResponseDtoWrapper> result) {
                log.debug("AfterShip: TrackDeliveryResponseDtoWrapper sent successfully on outbound topic");
            }

            @Override
            public void onFailure(Throwable ex) {
                log.error("PUBLISHING TO event.vxp.workflow.outbound FAILED", ex);
            }
        };
    }
}
