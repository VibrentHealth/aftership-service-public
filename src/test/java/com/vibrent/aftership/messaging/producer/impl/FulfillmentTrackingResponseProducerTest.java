package com.vibrent.aftership.messaging.producer.impl;

import com.vibrent.aftership.service.ExternalLogService;
import com.vibrent.vxp.workflow.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FulfillmentTrackingResponseProducerTest {

    private static final String TOPIC_NAME = "event.vxp.workflow.outbound";
    private static final String VXP_HEADER_VERSION = "2.1.3";
    private static final String VXP_MESSAGE_SPEC_VERSION = "2.1.2";
    private static final String SOURCE_AFTER_SHIP = "AfterShip";

    @Mock
    private ExternalLogService externalLogService;

    @Mock
    private KafkaTemplate<String, FulfillmentTrackDeliveryResponseDto> kafkaTemplate;

    @Mock
    private ListenableFuture future;

    private FulfillmentTrackingResponseProducer trackingResponseProducer;

    private FulfillmentTrackDeliveryResponseDtoWrapper trackDeliveryResponseDtoWrapper;
    private MessageHeaderDto messageHeaderDto;
    private FulfillmentTrackDeliveryResponseDto trackDeliveryResponseDto;

    @Before
    public void setup() {
        trackingResponseProducer = new FulfillmentTrackingResponseProducer(kafkaTemplate, externalLogService, true, TOPIC_NAME);
        initializeTrackDeliveryResponseDtoWrapper();
    }

    @DisplayName("When valid message is sent to Producer then verify message is sent on Kafka and external log")
    @Test
    public void send() {
        when(kafkaTemplate.send((Message<TrackDeliveryResponseDto>) any())).thenReturn(future);
        trackingResponseProducer.send(trackDeliveryResponseDtoWrapper);
        verify(kafkaTemplate).send((Message<?>) any());
        verify(externalLogService).send(trackDeliveryResponseDtoWrapper, messageHeaderDto.getVxpMessageTimestamp(),
                "AfterShip | Fulfillment Track Delivery Response sent", HttpStatus.OK);
    }

    @DisplayName("When valid message is sent to Producer and exception while sending on kafka then verify externalLogService is invoked with error message")
    @Test
    public void sendWhenException() {
        when(kafkaTemplate.send((Message<TrackDeliveryResponseDto>) any())).thenThrow(new RuntimeException());
        trackingResponseProducer.send(trackDeliveryResponseDtoWrapper);
        verify(kafkaTemplate).send((Message<?>) any());
        verify(externalLogService).send(trackDeliveryResponseDtoWrapper, messageHeaderDto.getVxpMessageTimestamp(),
                "AfterShip | FAILED TO PUBLISH OUTGOING MESSAGE TO " + TOPIC_NAME, HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @Test
    public void sendKafkaDisabled() {
        trackingResponseProducer.setKafkaEnabled(false);
        trackingResponseProducer.send(trackDeliveryResponseDtoWrapper);
        verify(kafkaTemplate, times(0)).send((Message<?>) any());
        verify(externalLogService, times(0)).send(any(TrackDeliveryResponseDtoWrapper.class), anyLong(),
                anyString(), any(HttpStatus.class));
    }


    @Test
    public void sendInvalidTrackDeliveryResponseDto() {
        trackingResponseProducer.send(new FulfillmentTrackDeliveryResponseDtoWrapper(new byte[10], null));
        verify(kafkaTemplate, times(0)).send((Message<?>) any());
        verify(externalLogService, times(0)).send(any(TrackDeliveryResponseDtoWrapper.class), anyLong(),
                anyString(), any(HttpStatus.class));
    }


    private void initializeTrackDeliveryResponseDtoWrapper() {
        initializeTrackDeliveryResponseDto();
        initializeMessageHeaderDto();
        trackDeliveryResponseDtoWrapper = new FulfillmentTrackDeliveryResponseDtoWrapper(trackDeliveryResponseDto, messageHeaderDto);
    }

    private void initializeTrackDeliveryResponseDto() {
        trackDeliveryResponseDto = new FulfillmentTrackDeliveryResponseDto();
        trackDeliveryResponseDto.setTrackingID("tracking_number_1");
        trackDeliveryResponseDto.setStatusTime(111122223333L);
        trackDeliveryResponseDto.setStatus(TrackingStatusEnum.IN_TRANSIT);
        trackDeliveryResponseDto.setCarrierCode(ProviderEnum.USPS.toValue());
        trackDeliveryResponseDto.setParticipant(getParticipant());
    }

    private ParticipantDetailsDto getParticipant() {
        ParticipantDetailsDto participantDto = new ParticipantDetailsDto();
        participantDto.setVibrentID(1000);
        participantDto.setExternalID("P1000");
        return participantDto;
    }

    private void initializeMessageHeaderDto() {
        messageHeaderDto = new MessageHeaderDto();
        messageHeaderDto.setVxpMessageID(UUID.randomUUID().toString());
        messageHeaderDto.setVxpHeaderVersion(VXP_HEADER_VERSION);
        messageHeaderDto.setVxpWorkflowName(WorkflowNameEnum.SALIVARY_KIT_ORDER);
        messageHeaderDto.setVxpMessageSpec(MessageSpecificationEnum.FULFILMENT_TRACK_DELIVERY_RESPONSE);
        messageHeaderDto.setVxpMessageTimestamp(System.currentTimeMillis());
        messageHeaderDto.setSource(SOURCE_AFTER_SHIP);
        messageHeaderDto.setVxpMessageSpecVersion(VXP_MESSAGE_SPEC_VERSION);
        messageHeaderDto.setVxpOriginator(RequestOriginatorEnum.VXPMS);
        messageHeaderDto.setVxpWorkflowInstanceID(UUID.randomUUID().toString());
        messageHeaderDto.setVxpPattern(IntegrationPatternEnum.WORKFLOW);
        messageHeaderDto.setVxpUserID(1000L);
        messageHeaderDto.setVxpTrigger(ContextTypeEnum.EVENT);
    }

}