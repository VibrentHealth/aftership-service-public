package com.vibrent.aftership.messaging.producer;

import com.vibrent.aftership.constants.KafkaConstants;
import com.vibrent.aftership.dto.RetryRequestDTO;
import com.vibrent.vxp.workflow.MessageHeaderDto;
import com.vibrent.vxp.workflow.TrackDeliveryResponseDto;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Message Producer for sending messages to Kafka
 *
 * @param <T>
 */
public interface MessageProducer<T> {
    void setKafkaEnabled(boolean newState);

    void send(T msg);

    default Message<TrackDeliveryResponseDto> buildMessage(TrackDeliveryResponseDto payload, MessageHeaderDto headers, String topicName) {
        MessageBuilder<TrackDeliveryResponseDto> messageBuilder = MessageBuilder.withPayload(payload);
        messageBuilder.setHeader(KafkaHeaders.TOPIC, topicName);
        messageBuilder.setHeader(KafkaConstants.VXP_HEADER_VERSION, headers.getVxpHeaderVersion());
        messageBuilder.setHeader(KafkaConstants.VXP_ORIGINATOR, headers.getVxpOriginator().toValue());
        messageBuilder.setHeader(KafkaConstants.VXP_PATTERN, headers.getVxpPattern().toValue());
        messageBuilder.setHeader(KafkaConstants.VXP_MESSAGE_SPEC, headers.getVxpMessageSpec().toValue());
        messageBuilder.setHeader(KafkaConstants.VXP_MESSAGE_SPEC_VERSION, headers.getVxpMessageSpecVersion());
        messageBuilder.setHeader(KafkaConstants.VXP_TENANT_ID, headers.getVxpTenantID());
        messageBuilder.setHeader(KafkaConstants.VXP_PROGRAM_ID, headers.getVxpProgramID());
        messageBuilder.setHeader(KafkaConstants.VXP_TRIGGER, headers.getVxpTrigger().toValue());
        messageBuilder.setHeader(KafkaConstants.VXP_WORKFLOW_NAME, headers.getVxpWorkflowName().toValue());
        messageBuilder.setHeader(KafkaConstants.VXP_WORKFLOW_INSTANCE_ID, headers.getVxpWorkflowInstanceID());
        messageBuilder.setHeader(KafkaConstants.VXP_MESSAGE_ID, headers.getVxpMessageID());
        messageBuilder.setHeader(KafkaConstants.VXP_IN_REPLY_TO_ID, "");
        messageBuilder.setHeader(KafkaConstants.VXP_MESSAGE_TIMESTAMP, headers.getVxpMessageTimestamp());
        return messageBuilder.build();
    }

    default Message<RetryRequestDTO> buildMessage(RetryRequestDTO payload,  String topicName) {
        MessageBuilder<RetryRequestDTO> messageBuilder = MessageBuilder.withPayload(payload);
        messageBuilder.setHeader(KafkaHeaders.TOPIC, topicName);
        return messageBuilder.build();
    }
}
