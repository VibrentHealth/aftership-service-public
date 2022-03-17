package com.vibrent.aftership.messaging.producer.impl;

import com.vibrent.aftership.dto.ExternalLogDTO;
import com.vibrent.aftership.messaging.producer.MessageProducer;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * Produce external log to kafka topic: event.vrp.externalApiRequestLogs
 */
@Service
@Log4j2
public class ExternalLogProducer implements MessageProducer<ExternalLogDTO> {

    private boolean kafkaEnabled;

    private final KafkaTemplate<String, ExternalLogDTO> kafkaTemplate;

    private final String topicName;

    /**
     * All arguments constructor
     * @param kafkaEnabled boolean represents whether kafka is enabled
     * @param kafkaTemplate kafkaTemplate to send ExternalLogDTO
     * @param topicName kafka topic collecting external log
     */
    public ExternalLogProducer(@Value("${kafka.enabled}") boolean kafkaEnabled, KafkaTemplate<String,
            ExternalLogDTO> kafkaTemplate, @Value("${kafka.topics.externalApiRequestLogs}") String topicName) {
        this.kafkaEnabled = kafkaEnabled;
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    /**
     * Setter for kafkaEnabled
     * @param state boolean represents whether kafka in enabled
     */
    @Override
    public void setKafkaEnabled(boolean state) { this.kafkaEnabled = state; }

    /**
     * Send ExternalLogDTO to kafka topic
     * @param msg ExternalLogDTO to be sent
     */
    @Override
    public void send(ExternalLogDTO msg){

        if (!kafkaEnabled) {
            return ;
        }

        kafkaTemplate.send(topicName, msg.getExternalId(), msg)
                .addCallback(new ListenableFutureCallback<SendResult<String, ExternalLogDTO>>() {
                    @Override
                    public void onFailure(Throwable ex) {
                        log.error("PUBLISHING TO EXTERNAL_LOG_TOPIC FAILED", ex);
                    }

                    @Override
                    public void onSuccess(SendResult<String, ExternalLogDTO> result) {
                        log.debug("AfterShip: Message sent successfully on external log topic.");
                    }
                });
    }

}
