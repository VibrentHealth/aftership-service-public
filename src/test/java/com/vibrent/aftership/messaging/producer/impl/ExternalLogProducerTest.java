package com.vibrent.aftership.messaging.producer.impl;

import com.vibrent.aftership.dto.ExternalLogDTO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.RequestMethod;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ExternalLogProducerTest {

    private static final String TOPIC_NAME = "event.vrp.externalApiRequestLogs";
    private static final String RESPONSE_BODY = "RESPONSE_BODY";
    private static final String REQUEST_URL = "REQUEST_URL";
    private static final String REQUEST_BODY = "REQUEST_BODY";

    @Mock
    private KafkaTemplate<String, ExternalLogDTO> kafkaTemplate;

    private ExternalLogProducer messageProducer;

    private ExternalLogDTO externalLogDTO;

    @Mock
    private ListenableFuture future;

    @Before
    public void setup() {
        messageProducer = new ExternalLogProducer(true, kafkaTemplate, TOPIC_NAME);
        externalLogDTO = initializeExternalLogDTO();
    }

    @Test
    public void send() {
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);
        messageProducer.send(externalLogDTO);
        verify(kafkaTemplate).send(TOPIC_NAME, externalLogDTO.getExternalId(), externalLogDTO);
    }

    @Test
    public void send_kafkaIsDisabled() {
        messageProducer.setKafkaEnabled(false);
        messageProducer.send(externalLogDTO);
        verify(kafkaTemplate, Mockito.times(0)).send(TOPIC_NAME, externalLogDTO);
    }

    private ExternalLogDTO initializeExternalLogDTO() {
        ExternalLogDTO apiRequestLog = new ExternalLogDTO();
        apiRequestLog.setResponseBody(RESPONSE_BODY);
        apiRequestLog.setRequestBody(REQUEST_BODY);
        apiRequestLog.setResponseCode(200);
        apiRequestLog.setHttpMethod(RequestMethod.POST);
        apiRequestLog.setRequestUrl(REQUEST_URL);
        apiRequestLog.setExternalId("P237679");
        return apiRequestLog;
    }

}