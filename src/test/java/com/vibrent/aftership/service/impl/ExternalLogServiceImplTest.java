package com.vibrent.aftership.service.impl;

import com.aftership.sdk.model.tracking.NewTracking;
import com.aftership.sdk.model.tracking.SlugTrackingNumber;
import com.aftership.sdk.model.tracking.Tracking;
import com.vibrent.aftership.dto.ExternalLogDTO;
import com.vibrent.aftership.messaging.producer.impl.ExternalLogProducer;
import com.vibrent.aftership.service.ExternalLogService;
import com.vibrent.aftership.util.JacksonUtil;
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
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ExternalLogServiceImplTest {
    private static final String TOPIC_NAME = "event.vxp.workflow.outbound";
    private static final String VXP_HEADER_VERSION = "2.1.3";
    private static final String VXP_MESSAGE_SPEC_VERSION = "2.1.2";
    private static final String SOURCE_AFTER_SHIP = "AfterShip";

    private ExternalLogService externalLogService;

    @Mock
    private ExternalLogProducer externalLogProducer;

    private TrackDeliveryResponseDtoWrapper trackDeliveryResponseDtoWrapper;
    private TrackDeliveryResponseDto trackDeliveryResponseDto;
    private MessageHeaderDto messageHeaderDto;
    private TrackDeliveryRequestDto trackDeliveryRequestDto;
    private NewTracking newTracking;
    private Tracking tracking;
    private SlugTrackingNumber slugTrackingNumber;

    @Before
    public void setup() {
        externalLogService = new ExternalLogServiceImpl(externalLogProducer, TOPIC_NAME, "https://api.aftership.com/v4");
        initializeTrackDeliveryResponseDtoWrapper();
        initializeTrackDeliveryRequestDto();
        initializeNewTracking();
        initializeTracking();
        initializeSlugTrackingNumber();
    }

    @Test
    public void sendForTrackDeliveryResponse() {
        ExternalLogDTO externalLogDTO = externalLogService.send(trackDeliveryResponseDtoWrapper, messageHeaderDto.getVxpMessageTimestamp(),
                "AfterShip | Produce a Message to Kafka topic: " + TOPIC_NAME, HttpStatus.OK);

        verify(externalLogProducer).send(externalLogDTO);
    }

    @Test
    public void sendForTrackDeliveryRequest() throws Exception {
        ExternalLogDTO externalLogDTO = externalLogService.send(trackDeliveryRequestDto, 123412341234L, 200,
                newTracking, 112211221122L, JacksonUtil.getMapper().writeValueAsString(tracking), "AfterShip: Successfully created tracking");

        verify(externalLogProducer).send(externalLogDTO);
    }

    @Test
    public void sendForTrackDeliveryRequestWithInternalServerError() throws Exception {
        ExternalLogDTO externalLogDTO = externalLogService.send(trackDeliveryRequestDto, 123412341234L, 200,
                null, 112211221122L, JacksonUtil.getMapper().writeValueAsString(tracking), "AfterShip: Successfully created tracking");

        verify(externalLogProducer).send(externalLogDTO);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), externalLogDTO.getResponseCode());
    }

    @Test
    public void sendForTrackDeliveryResponseWithInternalServerError() {

        //Send TrackDeliveryResponseDtoWrapper as null
        ExternalLogDTO externalLogDTO = externalLogService.send(null, messageHeaderDto.getVxpMessageTimestamp(),
                "AfterShip | Produce a Message to Kafka topic: " + TOPIC_NAME, HttpStatus.OK);

        verify(externalLogProducer).send(externalLogDTO);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), externalLogDTO.getResponseCode());


        //Send TrackDeliveryResponseDtoWrapper's headers as null
        externalLogDTO = externalLogService.send(new TrackDeliveryResponseDtoWrapper(trackDeliveryResponseDto, null), messageHeaderDto.getVxpMessageTimestamp(),
                "AfterShip | Produce a Message to Kafka topic: " + TOPIC_NAME, HttpStatus.OK);

        verify(externalLogProducer).send(externalLogDTO);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), externalLogDTO.getResponseCode());

        //Send TrackDeliveryResponseDtoWrapper's payload as null
        externalLogDTO = externalLogService.send(new TrackDeliveryResponseDtoWrapper(new byte[10], messageHeaderDto), messageHeaderDto.getVxpMessageTimestamp(),
                "AfterShip | Produce a Message to Kafka topic: " + TOPIC_NAME, HttpStatus.OK);

        verify(externalLogProducer).send(externalLogDTO);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), externalLogDTO.getResponseCode());


        //Send trackDeliveryResponseDto's participant as null
        initializeTrackDeliveryResponseDto();
        trackDeliveryResponseDto.setParticipant(null);
        externalLogDTO = externalLogService.send(new TrackDeliveryResponseDtoWrapper(trackDeliveryResponseDto, messageHeaderDto), messageHeaderDto.getVxpMessageTimestamp(),
                "AfterShip | Produce a Message to Kafka topic: " + TOPIC_NAME, HttpStatus.OK);

        verify(externalLogProducer).send(externalLogDTO);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), externalLogDTO.getResponseCode());


        //Send msg with externalId as null
        initializeTrackDeliveryResponseDto();
        trackDeliveryResponseDto.getParticipant().setExternalID(null);
        externalLogDTO = externalLogService.send(new TrackDeliveryResponseDtoWrapper(trackDeliveryResponseDto, messageHeaderDto), messageHeaderDto.getVxpMessageTimestamp(),
                "AfterShip | Produce a Message to Kafka topic: " + TOPIC_NAME, HttpStatus.OK);

        verify(externalLogProducer).send(externalLogDTO);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), externalLogDTO.getResponseCode());
    }

    @Test
    public void sendForGetTrackingRequest() throws Exception {
        ExternalLogDTO externalLogDTO = externalLogService.send(slugTrackingNumber,tracking,123412341234L,
                 "Aftership | Successfully fetched latest tracking.",200);

        verify(externalLogProducer).send(externalLogDTO);
    }

    @Test
    public void sendForGetTrackingRequestOnException() throws Exception {
        ExternalLogDTO externalLogDTO = externalLogService.send("12365485", "USPS", 123412341234L,
                "Aftership | Exception whiling fetched latest tracking.", 500);

        verify(externalLogProducer).send(externalLogDTO);
    }


    private void initializeTrackDeliveryResponseDtoWrapper() {
        initializeTrackDeliveryResponseDto();
        initializeMessageHeaderDto();
        trackDeliveryResponseDtoWrapper = new TrackDeliveryResponseDtoWrapper(trackDeliveryResponseDto, messageHeaderDto);
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

    private ParticipantDto getParticipant() {
        ParticipantDto participantDto = new ParticipantDto();
        participantDto.setVibrentID(1000);
        participantDto.setExternalID("P1000");
        return participantDto;
    }

    private void initializeMessageHeaderDto() {
        messageHeaderDto = new MessageHeaderDto();
        messageHeaderDto.setVxpMessageID(UUID.randomUUID().toString());
        messageHeaderDto.setVxpHeaderVersion(VXP_HEADER_VERSION);
        messageHeaderDto.setVxpWorkflowName(WorkflowNameEnum.SALIVARY_KIT_ORDER);
        messageHeaderDto.setVxpMessageSpec(MessageSpecificationEnum.TRACK_DELIVERY_RESPONSE);
        messageHeaderDto.setVxpMessageTimestamp(System.currentTimeMillis());
        messageHeaderDto.setSource(SOURCE_AFTER_SHIP);
        messageHeaderDto.setVxpMessageSpecVersion(VXP_MESSAGE_SPEC_VERSION);
        messageHeaderDto.setVxpOriginator(RequestOriginatorEnum.VXPMS);
        messageHeaderDto.setVxpWorkflowInstanceID(UUID.randomUUID().toString());
        messageHeaderDto.setVxpPattern(IntegrationPatternEnum.WORKFLOW);
        messageHeaderDto.setVxpUserID(1000L);
        messageHeaderDto.setVxpTrigger(ContextTypeEnum.EVENT);
    }

    private void initializeTrackDeliveryRequestDto() {
        trackDeliveryRequestDto = new TrackDeliveryRequestDto();
        trackDeliveryRequestDto.setOperation(OperationEnum.TRACK_DELIVERY);
        trackDeliveryRequestDto.setTrackingID("tracking_number_1");
        trackDeliveryRequestDto.setProvider(ProviderEnum.USPS);

        ParticipantDto participantDto = new ParticipantDto();
        participantDto.setExternalID("P1000");
        participantDto.setVibrentID(1000L);
        participantDto.setEmailAddress("emailaddress@abc.com");
        participantDto.setPhoneNumber("1234567890");
        trackDeliveryRequestDto.setParticipant(participantDto);
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
}