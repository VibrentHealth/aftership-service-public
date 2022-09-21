package com.vibrent.aftership.messaging.producer.impl;

import com.google.common.base.Preconditions;
import com.vibrent.aftership.messaging.producer.MessageProducer;
import com.vibrent.aftership.service.ExternalLogService;
import com.vibrent.vxp.workflow.*;
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
 * Kafka producer to send tracking status updates to Fulfillment
 */
@Service
@Slf4j
public class FulfillmentTrackingResponseProducer implements MessageProducer<FulfillmentTrackDeliveryResponseDtoWrapper> {

    private final KafkaTemplate<String, FulfillmentTrackDeliveryResponseDto> kafkaTemplate;

    private boolean kafkaEnabled;

    private final String topicName;

    private final ExternalLogService externalLogService;

    public FulfillmentTrackingResponseProducer(KafkaTemplate<String, FulfillmentTrackDeliveryResponseDto> kafkaTemplate, ExternalLogService externalLogService,
                                               @Value("${kafka.enabled}") boolean kafkaEnabled, @Value("${kafka.topics.track.response}") String topicName) {
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
    public void send(FulfillmentTrackDeliveryResponseDtoWrapper msg) {
        Preconditions.checkArgument(msg != null, "FulfillmentTrackDeliveryResponseDtoWrapper cannot be null");

        MessageHeaderDto headerDto = msg.getHeader();
        FulfillmentTrackDeliveryResponseDto fulfilmentTrackDeliveryResponseDto;
        try {
            fulfilmentTrackDeliveryResponseDto = msg.getPayload();
        } catch (IOException e) {
            log.warn("AfterShip: Error while getting payload from message: {}", msg, e);
            return;
        }

        Preconditions.checkArgument(fulfilmentTrackDeliveryResponseDto != null,
                "FulfillmentTrackDeliveryResponseDtoWrapper should non-null FulfilmentTrackDeliveryResponseDto");
        Preconditions.checkArgument(headerDto != null,
                "FulfillmentTrackDeliveryResponseDtoWrapper should non-null MessageHeaderDto");

        if (!kafkaEnabled) {
            log.error("Kafka is not enabled - Failed to send update for FulfilmentTrackDeliveryResponseDto with trackingID {}", fulfilmentTrackDeliveryResponseDto.getTrackingID());
            return;
        }

        try {
            Message<FulfillmentTrackDeliveryResponseDto> message = buildMessage(fulfilmentTrackDeliveryResponseDto, headerDto, topicName);
            kafkaTemplate.send(message).addCallback(newSendResultListenableFutureCallback());
            externalLogService.send(msg, headerDto.getVxpMessageTimestamp(),
                    "AfterShip | Fulfillment Track Delivery Response sent", HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to publish tracking update for fulfilmentTrackDeliveryResponseDto with trackingID {}, exception= {}",
                    fulfilmentTrackDeliveryResponseDto.getTrackingID(), e);
            externalLogService.send(msg, headerDto.getVxpMessageTimestamp(),
                    "AfterShip | FAILED TO PUBLISH OUTGOING MESSAGE TO " + topicName, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private static ListenableFutureCallback<SendResult<String, FulfillmentTrackDeliveryResponseDto>> newSendResultListenableFutureCallback() {
        return new ListenableFutureCallback<SendResult<String, FulfillmentTrackDeliveryResponseDto>>() {

            @Override
            public void onSuccess(SendResult<String, FulfillmentTrackDeliveryResponseDto> result) {
                log.debug("AfterShip: FulfillmentTrackDeliveryResponseDtoWrapper sent successfully on outbound topic");
            }

            @Override
            public void onFailure(Throwable ex) {
                log.error("PUBLISHING TO event.vxp.tracking.order.response FAILED", ex);
            }
        };
    }
}
