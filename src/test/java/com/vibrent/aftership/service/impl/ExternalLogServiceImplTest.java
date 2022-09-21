package com.vibrent.aftership.service.impl;

import com.aftership.sdk.model.tracking.NewTracking;
import com.aftership.sdk.model.tracking.SlugTrackingNumber;
import com.aftership.sdk.model.tracking.Tracking;
import com.vibrent.aftership.dto.ExternalLogDTO;
import com.vibrent.aftership.messaging.producer.impl.ExternalLogProducer;
import com.vibrent.aftership.service.ExternalLogService;
import com.vibrent.aftership.util.JacksonUtil;
import com.vibrent.aftership.vo.TrackDeliveryRequestVo;
import com.vibrent.vxp.workflow.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.vibrent.aftership.service.impl.TrackingRequestServiceImpl.CUSTOM_FIELD_EXTERNAL_ID;
import static com.vibrent.aftership.service.impl.TrackingRequestServiceImpl.CUSTOM_FIELD_VIBRENT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ExternalLogServiceImplTest {
    private static final String TOPIC_NAME = "event.vxp.workflow.outbound";
    private static final String FULFILLMENT_TOPIC_NAME = "event.vxp.tracking.order.response";
    private static final String FULFILLMENT_REQUEST_TOPIC_NAME = "event.vxp.tracking.order.request";
    private static final String VXP_HEADER_VERSION = "2.1.3";
    private static final String VXP_MESSAGE_SPEC_VERSION = "2.1.2";
    private static final String SOURCE_AFTER_SHIP = "AfterShip";

    private ExternalLogService externalLogService;

    @Mock
    private ExternalLogProducer externalLogProducer;

    private TrackDeliveryResponseDtoWrapper trackDeliveryResponseDtoWrapper;
    private FulfillmentTrackDeliveryResponseDtoWrapper fulfillmentTrackDeliveryResponseDtoWrapper;
    private TrackDeliveryResponseDto trackDeliveryResponseDto;
    private FulfillmentTrackDeliveryResponseDto fulfillmentTrackDeliveryResponseDto;
    private MessageHeaderDto messageHeaderDto;
    private TrackDeliveryRequestVo trackDeliveryRequestVo;
    private NewTracking newTracking;
    private Tracking tracking;
    private SlugTrackingNumber slugTrackingNumber;

    @Before
    public void setup() {
        externalLogService = new ExternalLogServiceImpl(externalLogProducer, TOPIC_NAME, FULFILLMENT_TOPIC_NAME, "https://api.aftership.com/v4", FULFILLMENT_REQUEST_TOPIC_NAME);
        initializeTrackDeliveryResponseDtoWrapper();
        initializeFulfillmentTrackDeliveryResponseDtoWrapper();
        initializeTrackDeliveryRequestVo();
        initializeNewTracking();
        initializeTracking();
        initializeSlugTrackingNumber();
    }

    @Test
    public void sendForTrackDeliveryResponse() {
        ExternalLogDTO externalLogDTO = externalLogService.send(trackDeliveryResponseDtoWrapper, messageHeaderDto.getVxpMessageTimestamp(),
                "AfterShip | Track Delivery Response sent", HttpStatus.OK);

        verify(externalLogProducer).send(externalLogDTO);
    }

    @Test
    public void sendForTrackDeliveryRequest() throws Exception {
        ExternalLogDTO externalLogDTO = externalLogService.send(trackDeliveryRequestVo, 123412341234L, 200,
                newTracking, 112211221122L, JacksonUtil.getMapper().writeValueAsString(tracking), "AfterShip: Successfully created tracking");

        verify(externalLogProducer).send(externalLogDTO);
    }

    @Test
    public void sendForTrackDeliveryRequestWithInternalServerError() throws Exception {
        ExternalLogDTO externalLogDTO = externalLogService.send(trackDeliveryRequestVo, 123412341234L, 200,
                null, 112211221122L, JacksonUtil.getMapper().writeValueAsString(tracking), "AfterShip: Successfully created tracking");

        verify(externalLogProducer).send(externalLogDTO);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), externalLogDTO.getResponseCode());
    }

    @Test
    public void sendForTrackDeliveryResponseWithInternalServerError() {

        //Send TrackDeliveryResponseDtoWrapper as null
        ExternalLogDTO externalLogDTO = externalLogService.send((TrackDeliveryResponseDtoWrapper) null, messageHeaderDto.getVxpMessageTimestamp(),
                "AfterShip | Track Delivery Response sent", HttpStatus.OK);

        verify(externalLogProducer).send(externalLogDTO);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), externalLogDTO.getResponseCode());

        //Send TrackDeliveryResponseDtoWrapper's payload as null
        externalLogDTO = externalLogService.send(new TrackDeliveryResponseDtoWrapper(new byte[10], messageHeaderDto), messageHeaderDto.getVxpMessageTimestamp(),
                "AfterShip | Track Delivery Response sent", HttpStatus.OK);

        verify(externalLogProducer).send(externalLogDTO);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), externalLogDTO.getResponseCode());

        verify(externalLogProducer).send(externalLogDTO);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), externalLogDTO.getResponseCode());
    }

    @Test
    public void sendForGetTrackingRequest() {
        ExternalLogDTO externalLogDTO = externalLogService.send(slugTrackingNumber,tracking,123412341234L,
                 "Aftership | Successfully fetched latest tracking.",200);

        verify(externalLogProducer).send(externalLogDTO);
    }

    @Test
    public void sendForGetTrackingRequestOnException() {
        ExternalLogDTO externalLogDTO = externalLogService.send("12365485", "USPS", 123412341234L,
                "Aftership | Exception whiling fetched latest tracking.", 500);

        verify(externalLogProducer).send(externalLogDTO);
    }

    private void initializeTrackDeliveryResponseDtoWrapper() {
        initializeTrackDeliveryResponseDto();
        initializeTrackDeliveryResponseMessageHeaderDto();
        trackDeliveryResponseDtoWrapper = new TrackDeliveryResponseDtoWrapper(trackDeliveryResponseDto, messageHeaderDto);
    }

    private void initializeFulfillmentTrackDeliveryResponseDtoWrapper() {
        initializeFulfillmentTrackDeliveryResponseDto();
        initializeTrackDeliveryResponseMessageHeaderDto();
        fulfillmentTrackDeliveryResponseDtoWrapper = new FulfillmentTrackDeliveryResponseDtoWrapper(fulfillmentTrackDeliveryResponseDto, messageHeaderDto);
    }

    private void initializeTrackDeliveryResponseDto() {
        trackDeliveryResponseDto = new TrackDeliveryResponseDto();
        trackDeliveryResponseDto.setTrackingID("tracking_number_1");
        trackDeliveryResponseDto.setDateTime(111122223333L);
        trackDeliveryResponseDto.setStatus(StatusEnum.IN_TRANSIT);
        trackDeliveryResponseDto.setProvider(ProviderEnum.USPS);
        trackDeliveryResponseDto.setOperation(OperationEnum.TRACK_DELIVERY);
        trackDeliveryResponseDto.setParticipant(getParticipant());
    }

    private void initializeFulfillmentTrackDeliveryResponseDto() {
        fulfillmentTrackDeliveryResponseDto = new FulfillmentTrackDeliveryResponseDto();
        fulfillmentTrackDeliveryResponseDto.setTrackingID("tracking_number_1");
        fulfillmentTrackDeliveryResponseDto.setStatusTime(111122223333L);
        fulfillmentTrackDeliveryResponseDto.setStatus(TrackingStatusEnum.IN_TRANSIT);
        fulfillmentTrackDeliveryResponseDto.setCarrierCode(ProviderEnum.USPS.toValue());
        fulfillmentTrackDeliveryResponseDto.setParticipant(getParticipantDetailsDto());
        fulfillmentTrackDeliveryResponseDto.setFulfillmentOrderID(1L);
    }

    private ParticipantDto getParticipant() {
        ParticipantDto participantDto = new ParticipantDto();
        participantDto.setVibrentID(1000);
        participantDto.setExternalID("P1000");
        return participantDto;
    }

    private ParticipantDetailsDto getParticipantDetailsDto() {
        ParticipantDetailsDto participantDetailsDto = new ParticipantDetailsDto();
        participantDetailsDto.setVibrentID(1000);
        participantDetailsDto.setExternalID("P1000");
        return participantDetailsDto;
    }

    private void initializeTrackDeliveryResponseMessageHeaderDto() {
        messageHeaderDto = buildMessageHeaders(MessageSpecificationEnum.TRACK_DELIVERY_RESPONSE, WorkflowNameEnum.SALIVARY_KIT_ORDER);
    }

    private MessageHeaderDto buildMessageHeaders(MessageSpecificationEnum messageSpecificationEnum, WorkflowNameEnum salivaryKitOrder) {
        var messageHeaderDto = new MessageHeaderDto();
        messageHeaderDto.setVxpMessageID(UUID.randomUUID().toString());
        messageHeaderDto.setVxpHeaderVersion(VXP_HEADER_VERSION);
        messageHeaderDto.setVxpWorkflowName(salivaryKitOrder);
        messageHeaderDto.setVxpMessageSpec(messageSpecificationEnum);
        messageHeaderDto.setVxpMessageTimestamp(System.currentTimeMillis());
        messageHeaderDto.setSource(SOURCE_AFTER_SHIP);
        messageHeaderDto.setVxpMessageSpecVersion(VXP_MESSAGE_SPEC_VERSION);
        messageHeaderDto.setVxpOriginator(RequestOriginatorEnum.VXPMS);
        messageHeaderDto.setVxpWorkflowInstanceID(UUID.randomUUID().toString());
        messageHeaderDto.setVxpPattern(IntegrationPatternEnum.WORKFLOW);
        messageHeaderDto.setVxpUserID(1000L);
        messageHeaderDto.setVxpTrigger(ContextTypeEnum.EVENT);

        return messageHeaderDto;
    }

    private void initializeTrackDeliveryRequestVo() {
        trackDeliveryRequestVo = new TrackDeliveryRequestVo();
        trackDeliveryRequestVo.setTrackingID("tracking_number_1");
        trackDeliveryRequestVo.setCarrierCode(ProviderEnum.USPS.name());

        ParticipantDetailsDto participantDto = new ParticipantDetailsDto();
        participantDto.setExternalID("P1000");
        participantDto.setVibrentID(1000L);
        trackDeliveryRequestVo.setParticipant(participantDto);
    }

    private void initializeNewTracking() {
        newTracking = new NewTracking();
        newTracking.setTrackingNumber("tracking_number_1");
    }

    private void initializeTracking() {
        Map<String, String> customFields = new HashMap<>();
        customFields.put(CUSTOM_FIELD_VIBRENT_ID, "1000");
        customFields.put(CUSTOM_FIELD_EXTERNAL_ID, "P1000");

        tracking = new Tracking();
        tracking.setTrackingNumber("tracking_number_1");
        tracking.setActive(true);
        tracking.setTag("InfoReceived");
        tracking.setCustomFields(customFields);
    }
    private void initializeSlugTrackingNumber(){
        slugTrackingNumber = new SlugTrackingNumber("USPS","tracking_number_1");
    }

    @Test
    public void sendForFulfillmentTrackDeliveryResponse() {
        ExternalLogDTO externalLogDTO = externalLogService.send(fulfillmentTrackDeliveryResponseDtoWrapper, messageHeaderDto.getVxpMessageTimestamp(),
                "AfterShip | Produce a Message to Kafka topic: " + FULFILLMENT_TOPIC_NAME, HttpStatus.OK);

        verify(externalLogProducer).send(externalLogDTO);
    }

    @Test
    public void sendForFulfillmentTrackDeliveryResponseWithInternalServerError() {

        //Send FulfillmentTrackDeliveryResponseDtoWrapper as null
        ExternalLogDTO externalLogDTO = externalLogService.send((FulfillmentTrackDeliveryResponseDtoWrapper) null, messageHeaderDto.getVxpMessageTimestamp(),
                "AfterShip | Produce a Message to Kafka topic: " + FULFILLMENT_TOPIC_NAME, HttpStatus.OK);

        verify(externalLogProducer).send(externalLogDTO);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), externalLogDTO.getResponseCode());

        //Send FulfillmentTrackDeliveryResponseDtoWrapper's payload as null
        externalLogDTO = externalLogService.send(new FulfillmentTrackDeliveryResponseDtoWrapper(new byte[10], messageHeaderDto), messageHeaderDto.getVxpMessageTimestamp(),
                "AfterShip | Produce a Message to Kafka topic: " + FULFILLMENT_TOPIC_NAME, HttpStatus.OK);

        verify(externalLogProducer).send(externalLogDTO);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), externalLogDTO.getResponseCode());

        verify(externalLogProducer).send(externalLogDTO);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), externalLogDTO.getResponseCode());
    }

    @Test
    public void testWhenNullFulfillmentTrackDeliveryRequestDtoWrapperReceived() {
        ExternalLogDTO externalLogDTO =  externalLogService.send(null);
        verify(externalLogProducer, times(0)).send(any(ExternalLogDTO.class));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), externalLogDTO.getResponseCode());
    }

    @Test
    public void testWhenValidFulfillmentTrackDeliveryRequestDtoWrapperReceived() {
        FulfillmentTrackDeliveryRequestDto requestDto = buildFulfillmentTrackDeliveryRequestDto();
        MessageHeaderDto headerDto = buildMessageHeaders(MessageSpecificationEnum.FULFILMENT_TRACK_DELIVERY_REQUEST, WorkflowNameEnum.FULFILLMENT_KIT_ORDER);
        ExternalLogDTO externalLogDTO =  externalLogService.send(new FulfillmentTrackDeliveryRequestDtoWrapper(requestDto, headerDto));

        verify(externalLogProducer, times(1)).send(any(ExternalLogDTO.class));
        assertEquals(HttpStatus.OK.value(), externalLogDTO.getResponseCode());
        assertEquals("event.vxp.tracking.order.request", externalLogDTO.getRequestUrl());
        assertEquals(123L, externalLogDTO.getInternalId());
        assertNotNull(externalLogDTO.getRequestHeaders());
    }

    private FulfillmentTrackDeliveryRequestDto buildFulfillmentTrackDeliveryRequestDto() {
        FulfillmentTrackDeliveryRequestDto requestDto = new FulfillmentTrackDeliveryRequestDto();
        requestDto.setCarrierCode("USPS");
        requestDto.setFulfillmentOrderID(1L);
        requestDto.setTrackingID("trackingID");
        ParticipantDetailsDto participant = new ParticipantDetailsDto();
        participant.setVibrentID(123L);
        requestDto.setParticipant(participant);

        return requestDto;
    }
}
