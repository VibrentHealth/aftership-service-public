package com.vibrent.aftership.integration.messaging.listener;

import com.vibrent.aftership.dto.RetryRequestDTO;
import com.vibrent.aftership.messaging.listener.RetryTrackingDeliveryRequestListener;
import com.vibrent.aftership.repository.TrackingRequestErrorRepository;
import com.vibrent.aftership.service.TrackingRequestService;
import com.vibrent.vxp.workflow.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.MessageHeaders;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetryTrackingDeliveryRequestListenerTest {
    public static final String TRACKING_ID = "879658";
    private static final String VXP_HEADER_VERSION = "2.1.3";
    private static final String VXP_MESSAGE_SPEC_VERSION = "2.1.2";
    private static final String SOURCE_AFTER_SHIP = "AfterShip";

    private boolean kafkaEnabled = true;

    @Mock
    private TrackingRequestService trackingRequestService;

    @Mock
    private TrackingRequestErrorRepository trackingRequestErrorRepository;

    private RetryTrackingDeliveryRequestListener retryTrackingDeliveryRequestListener;

    @MockBean
    MessageHeaders messageHeaders;

    @BeforeEach
    void setUp() {
        retryTrackingDeliveryRequestListener = new RetryTrackingDeliveryRequestListener(kafkaEnabled, trackingRequestService, trackingRequestErrorRepository);
    }

    @Test
    void testWhenKafkaIsDisabled() {
        retryTrackingDeliveryRequestListener = new RetryTrackingDeliveryRequestListener(false, trackingRequestService, trackingRequestErrorRepository);
        retryTrackingDeliveryRequestListener.listener(createRetryRequestDTO());

        verify(trackingRequestService, Mockito.times(0)).createTrackDeliveryRequest(any(TrackDeliveryRequestDto.class),
                any(MessageHeaderDto.class));
    }


    @DisplayName("when Retry Track Delivery Request message is received " +
            "then verify message is processed.")
    @Test
    void processOnlyWhenRetryTrackDeliveryRequestMsgIsReceived() {
        retryTrackingDeliveryRequestListener = new RetryTrackingDeliveryRequestListener(kafkaEnabled, trackingRequestService, trackingRequestErrorRepository);
        retryTrackingDeliveryRequestListener.listener(createRetryRequestDTO());

        verify(trackingRequestService, Mockito.timeout(3000).times(1)).createTrackDeliveryRequest(any(TrackDeliveryRequestDto.class),
                any(MessageHeaderDto.class));
    }

    @DisplayName("when Retry Track Delivery Request message is received " +
            "And create tracking gets successes" +
            "then verify message is processed and tracking request error entry gets deleted")
    @Test
    void testWhenRetryTrackDeliveryRequestMsgIsReceivedAndTrackingGetSuccess() {
        retryTrackingDeliveryRequestListener = new RetryTrackingDeliveryRequestListener(kafkaEnabled, trackingRequestService, trackingRequestErrorRepository);
        when(trackingRequestService.createTrackDeliveryRequest(any(TrackDeliveryRequestDto.class),any(MessageHeaderDto.class))).thenReturn(true);
        retryTrackingDeliveryRequestListener.listener(createRetryRequestDTO());

        verify(trackingRequestService, Mockito.timeout(3000).times(1)).createTrackDeliveryRequest(any(TrackDeliveryRequestDto.class),
                any(MessageHeaderDto.class));
        verify(trackingRequestErrorRepository, times(1)).deleteByTrackingId(anyString());
    }

    @DisplayName("when Retry Track Delivery Request message is received " +
            "And create tracking gets failed" +
            "then verify message is processed and tracking request error entry not deleted")
    @Test
    void testWhenRetryTrackDeliveryRequestMsgIsReceivedAndTrackingGetFailed() {
        retryTrackingDeliveryRequestListener = new RetryTrackingDeliveryRequestListener(kafkaEnabled, trackingRequestService, trackingRequestErrorRepository);
        when(trackingRequestService.createTrackDeliveryRequest(any(TrackDeliveryRequestDto.class),any(MessageHeaderDto.class))).thenReturn(false);
        retryTrackingDeliveryRequestListener.listener(createRetryRequestDTO());

        verify(trackingRequestService, Mockito.timeout(3000).times(1)).createTrackDeliveryRequest(any(TrackDeliveryRequestDto.class),
                any(MessageHeaderDto.class));
        verify(trackingRequestErrorRepository, times(0)).deleteByTrackingId(anyString());
    }


    RetryRequestDTO createRetryRequestDTO() {
        RetryRequestDTO retryRequestDTO = new RetryRequestDTO(getTrackDeliveryRequestDto(), getMessageHeaderDto());
        return retryRequestDTO;
    }

    TrackDeliveryRequestDto getTrackDeliveryRequestDto() {

        ParticipantDto participantDto = new ParticipantDto();
        participantDto.setExternalID("P1000");
        participantDto.setVibrentID(1000L);
        participantDto.setEmailAddress("emailaddress@abc.com");
        participantDto.setPhoneNumber("1234567890");

        TrackDeliveryRequestDto trackDeliveryRequestDto = new TrackDeliveryRequestDto();
        trackDeliveryRequestDto.setTrackingID(TRACKING_ID);
        trackDeliveryRequestDto.setOperation(OperationEnum.TRACK_DELIVERY);
        trackDeliveryRequestDto.setProvider(ProviderEnum.USPS);
        trackDeliveryRequestDto.setParticipant(participantDto);

        return trackDeliveryRequestDto;
    }

    MessageHeaderDto getMessageHeaderDto() {
        MessageHeaderDto messageHeaderDto = new MessageHeaderDto();
        messageHeaderDto.setVxpMessageID("MessageID_1");
        messageHeaderDto.setVxpHeaderVersion(VXP_HEADER_VERSION);
        messageHeaderDto.setVxpWorkflowName(WorkflowNameEnum.SALIVARY_KIT_ORDER);
        messageHeaderDto.setVxpMessageSpec(MessageSpecificationEnum.TRACK_DELIVERY_RESPONSE);
        messageHeaderDto.setVxpMessageTimestamp(1630645343162L);
        messageHeaderDto.setSource(SOURCE_AFTER_SHIP);
        messageHeaderDto.setVxpMessageSpecVersion(VXP_MESSAGE_SPEC_VERSION);
        messageHeaderDto.setVxpOriginator(RequestOriginatorEnum.PTBE);
        messageHeaderDto.setVxpWorkflowInstanceID("WorkflowInstanceID_1");
        messageHeaderDto.setVxpPattern(IntegrationPatternEnum.WORKFLOW);
        messageHeaderDto.setVxpUserID(1000L);
        messageHeaderDto.setVxpTrigger(ContextTypeEnum.EVENT);
        return messageHeaderDto;
    }

    MessageHeaders getMessageHeaders(){
        Map<String, Object> headers = new HashMap<>();
        headers.put("ID","WorkflowInstanceID_");
        MessageHeaders messageHeaders = new MessageHeaders(headers);

        return messageHeaders;
    }

}
