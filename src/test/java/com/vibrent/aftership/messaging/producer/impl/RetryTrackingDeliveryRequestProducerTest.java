package com.vibrent.aftership.messaging.producer.impl;

import com.vibrent.aftership.dto.ExternalLogDTO;
import com.vibrent.aftership.dto.RetryRequestDTO;
import com.vibrent.vxp.workflow.MessageHeaderDto;
import com.vibrent.vxp.workflow.ParticipantDto;
import com.vibrent.vxp.workflow.TrackDeliveryRequestDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.util.concurrent.ListenableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RetryTrackingDeliveryRequestProducerTest {

    private static final String TOPIC_NAME = "event.vxp.aftership.tracking.retry";

    @Mock
    private KafkaTemplate<String, RetryRequestDTO> kafkaTemplate;

    private RetryTrackingDeliveryRequestProducer messageProducer;

    private ExternalLogDTO externalLogDTO;

    @Mock
    private ListenableFuture<SendResult<String, RetryRequestDTO>> future;

    @Before
    public void setup() {
        messageProducer = new RetryTrackingDeliveryRequestProducer(kafkaTemplate, true, TOPIC_NAME);
    }

    @Test
    public void send() {
        when(kafkaTemplate.send(ArgumentMatchers.<Message<RetryRequestDTO>>any())).thenReturn(future);
        messageProducer.send(buildRetryRequestDTO());
        verify(kafkaTemplate).send(any(Message.class));
    }

    @Test
    public void sendKafkaIsDisabled() {
        messageProducer.setKafkaEnabled(false);
        messageProducer.send(buildRetryRequestDTO());
        verify(kafkaTemplate, Mockito.times(0)).send(any(Message.class));
    }

    @Test
    public void sendNullValues() {
        messageProducer.send(null);
        verify(kafkaTemplate, Mockito.times(0)).send(any(Message.class));
    }

    private RetryRequestDTO buildRetryRequestDTO() {
        RetryRequestDTO retryRequestDTO = new RetryRequestDTO();
        retryRequestDTO.setTrackDeliveryRequestDto(buildTrackDeliveryRequestDto());
        retryRequestDTO.setMessageHeaderDto(new MessageHeaderDto());
        return retryRequestDTO;
    }


    private static TrackDeliveryRequestDto buildTrackDeliveryRequestDto() {
        ParticipantDto participant = new ParticipantDto();
        participant.setVibrentID(123L);
        participant.setExternalID("p123L");

        TrackDeliveryRequestDto requestDto = new TrackDeliveryRequestDto();
        requestDto.setParticipant(participant);
        requestDto.setTrackingID("trackingID");
        return requestDto;
    }
}
