package com.vibrent.aftership.messaging.producer.impl;

import com.vibrent.aftership.dto.RetryRequestDTO;
import com.vibrent.aftership.vo.TrackDeliveryRequestVo;
import com.vibrent.vxp.workflow.MessageHeaderDto;
import com.vibrent.vxp.workflow.ParticipantDetailsDto;
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
        retryRequestDTO.setTrackDeliveryRequestVo(buildTrackDeliveryRequestVo());
        retryRequestDTO.setMessageHeaderDto(new MessageHeaderDto());
        return retryRequestDTO;
    }


    private static TrackDeliveryRequestVo buildTrackDeliveryRequestVo() {
        ParticipantDetailsDto participant = new ParticipantDetailsDto();
        participant.setVibrentID(123L);
        participant.setExternalID("p123L");

        TrackDeliveryRequestVo requestDto = new TrackDeliveryRequestVo();
        requestDto.setParticipant(participant);
        requestDto.setTrackingID("trackingID");
        return requestDto;
    }
}
