package com.vibrent.aftership.service.impl;

import com.aftership.sdk.model.tracking.Tracking;
import com.vibrent.aftership.converter.FulfillmentTrackDeliveryResponseConverter;
import com.vibrent.aftership.converter.TrackDeliveryResponseConverter;
import com.vibrent.aftership.domain.TrackingRequest;
import com.vibrent.aftership.dto.NotificationDTO;
import com.vibrent.aftership.messaging.producer.impl.FulfillmentTrackingResponseProducer;
import com.vibrent.aftership.messaging.producer.impl.TrackingResponseProducer;
import com.vibrent.aftership.repository.TrackingRequestRepository;
import com.vibrent.aftership.service.NotificationProcessService;
import com.vibrent.vxp.workflow.OperationEnum;
import com.vibrent.vxp.workflow.ProviderEnum;
import com.vibrent.vxp.workflow.StatusEnum;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static com.vibrent.aftership.service.impl.TrackingRequestServiceImpl.CUSTOM_FIELD_EXTERNAL_ID;
import static com.vibrent.aftership.service.impl.TrackingRequestServiceImpl.CUSTOM_FIELD_VIBRENT_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class NotificationProcessServiceImplTest {

    private NotificationProcessService notificationProcessService;

    private TrackDeliveryResponseConverter trackDeliveryResponseConverter;

    private FulfillmentTrackDeliveryResponseConverter fulfillmentTrackDeliveryResponseConverter;

    @Mock
    private TrackingResponseProducer trackingResponseProducer;

    @Mock
    private FulfillmentTrackingResponseProducer fulfillmentTrackingResponseProducer;

    @Mock
    private TrackingRequestRepository trackingRequestRepository;

    private NotificationDTO notificationDTO;
    private TrackingRequest trackingRequest;
    List<String> exceptionSubStatus;

    private final Date lastUpdatedAt = new Date();

    @Before
    public void setup() {
        initializeExceptionSubStatusList();
        trackDeliveryResponseConverter = new TrackDeliveryResponseConverter();
        fulfillmentTrackDeliveryResponseConverter = new FulfillmentTrackDeliveryResponseConverter();
        notificationProcessService = new NotificationProcessServiceImpl(trackDeliveryResponseConverter, fulfillmentTrackDeliveryResponseConverter, trackingResponseProducer, fulfillmentTrackingResponseProducer, trackingRequestRepository, exceptionSubStatus);
        initializeNotificationDTO();
        initializeTrackingRequest();
    }

    @Test
    public void process() {
        when(this.trackingRequestRepository.findByTrackingId(notificationDTO.getMsg().getTrackingNumber())).thenReturn(Optional.of(trackingRequest));
        notificationProcessService.process(notificationDTO);
        verify(trackingResponseProducer).send(any());
        verify(trackingRequestRepository).save(any());
    }

    @Test
    public void processFulfillmentDto() {
        trackingRequest.setFulfillmentOrderID(1L);
        when(this.trackingRequestRepository.findByTrackingId(notificationDTO.getMsg().getTrackingNumber())).thenReturn(Optional.of(trackingRequest));
        notificationProcessService.process(notificationDTO);
        verify(fulfillmentTrackingResponseProducer, times(1)).send(any());
        verify(trackingRequestRepository, times(1)).save(any());
    }

    @Test
    public void processWhenUnknownStatus() {
        when(this.trackingRequestRepository.findByTrackingId(notificationDTO.getMsg().getTrackingNumber())).thenReturn(Optional.of(trackingRequest));
        notificationDTO.getMsg().setTag("Unknown");
        notificationProcessService.process(notificationDTO);
        verify(trackingResponseProducer, times(0)).send(any());
        verify(trackingRequestRepository, times(1)).save(any());
    }

    @DisplayName("When same status is received in Notification msg then verify no tracking response sent")
    @Test
    public void processWhenSameStatusReceived() {
        trackingRequest.setStatus("InTransit");
        when(this.trackingRequestRepository.findByTrackingId(notificationDTO.getMsg().getTrackingNumber())).thenReturn(Optional.of(trackingRequest));
        notificationDTO.getMsg().setTag("InTransit");

        notificationProcessService.process(notificationDTO);

        verify(trackingResponseProducer, times(0)).send(any());
        verify(trackingRequestRepository, times(1)).save(any());
    }

    @DisplayName("When exception sub tag verify no tracking response sent")
    @Test
    public void processWhenExceptionStatusReceived() {
        when(this.trackingRequestRepository.findByTrackingId(notificationDTO.getMsg().getTrackingNumber())).thenReturn(Optional.of(trackingRequest));
        notificationDTO.getMsg().setSubtag("Exception_011");

        notificationProcessService.process(notificationDTO);

        verify(trackingResponseProducer, times(0)).send(any());
        verify(trackingRequestRepository, times(1)).save(any());
    }

    @DisplayName("When different status is received in Notification msg and status saved in DB is null then verify tracking response sent")
    @Test
    public void processWhenDifferentStatusReceivedAndStatusInDbIsNull() {
        trackingRequest.setStatus(null);
        when(this.trackingRequestRepository.findByTrackingId(notificationDTO.getMsg().getTrackingNumber())).thenReturn(Optional.of(trackingRequest));
        notificationDTO.getMsg().setTag("InTransit");

        notificationProcessService.process(notificationDTO);

        verify(trackingResponseProducer, times(1)).send(any());
        verify(trackingRequestRepository, times(1)).save(any());
    }

    @DisplayName("When different status is received in Notification msg and status saved in DB is not null then verify tracking response sent")
    @Test
    public void processWhenDifferentStatusReceived() {
        trackingRequest.setStatus(StatusEnum.PENDING_TRACKING.toValue());
        when(this.trackingRequestRepository.findByTrackingId(notificationDTO.getMsg().getTrackingNumber())).thenReturn(Optional.of(trackingRequest));
        notificationDTO.getMsg().setTag("InTransit");

        notificationProcessService.process(notificationDTO);

        verify(trackingResponseProducer, times(1)).send(any());
        verify(trackingRequestRepository, times(1)).save(any());
    }

    @DisplayName("When Pending status is received in Tracking object then verify notification get process and tracking response sent")
    @Test
    public void processGetTracking() {
        notificationProcessService.process(notificationDTO.getMsg(),trackingRequest);
        verify(trackingResponseProducer, times(1)).send(any());
        verify(trackingRequestRepository, times(1)).save(any());
    }

    @DisplayName("When FulfillmentId is received in Tracking object then verify notification get process and tracking response sent")
    @Test
    public void processGetTrackingFulfillment() {
        trackingRequest.setFulfillmentOrderID(1L);
        notificationProcessService.process(notificationDTO.getMsg(),trackingRequest);
        verify(fulfillmentTrackingResponseProducer, times(1)).send(any());
        verify(trackingRequestRepository, times(1)).save(any());
    }

    @DisplayName("When FulfillmentId is received in Tracking object and status is received in Notification msg and status saved in DB is not null then verify tracking response not sent")
    @Test
    public void processGetTrackingFulfillmentUnknownTag() {
        trackingRequest.setFulfillmentOrderID(1L);
        notificationDTO.getMsg().setTag("Unknown");
        notificationProcessService.process(notificationDTO.getMsg(),trackingRequest);
        verify(trackingResponseProducer, times(0)).send(any());
        verify(trackingRequestRepository, times(1)).save(any());
    }

    @DisplayName("When FulfillmentId is received in Tracking object and Exception status is received in Notification msg and status saved in DB is not null then verify tracking response not sent")
    @Test
    public void processGetTrackingFulfillmentReturnNull() {
        trackingRequest.setFulfillmentOrderID(1L);
        notificationDTO.getMsg().setTag("Exception");
        notificationDTO.getMsg().setSubtag("Exception_007");
        notificationProcessService.process(notificationDTO.getMsg(),trackingRequest);
        verify(trackingResponseProducer, times(0)).send(any());
        verify(trackingRequestRepository, times(1)).save(any());
    }

    @DisplayName("When same status is received in Notification msg then verify no tracking response not sent")
    @Test
    public void processGetTrackingWhenUnknownStatus() {
        notificationDTO.getMsg().setTag("Unknown");
        notificationProcessService.process(notificationDTO.getMsg(),trackingRequest);
        verify(trackingResponseProducer, times(0)).send(any());
        verify(trackingRequestRepository, times(1)).save(any());
    }

    @DisplayName("When same status is received in Notification msg then verify no tracking response sent")
    @Test
    public void processGetTrackingWhenSameStatusReceived() {
        trackingRequest.setStatus("InTransit");
        notificationDTO.getMsg().setTag("InTransit");

        notificationProcessService.process(notificationDTO.getMsg(),trackingRequest);

        verify(trackingResponseProducer, times(0)).send(any());
        verify(trackingRequestRepository, times(0)).save(any());
    }

    @DisplayName("When different status is received in Notification msg and status saved in DB is null then verify tracking response sent")
    @Test
    public void processGetTrackingWhenDifferentStatusReceivedAndStatusInDbIsNull() {
        trackingRequest.setStatus(null);
        notificationDTO.getMsg().setTag("InTransit");

        notificationProcessService.process(notificationDTO.getMsg(),trackingRequest);

        verify(trackingResponseProducer, times(1)).send(any());
        verify(trackingRequestRepository, times(1)).save(any());
    }

    @DisplayName("When different status is received in Notification msg and status saved in DB is not null then verify tracking response sent")
    @Test
    public void processGetTrackingWhenDifferentStatusReceived() {
        trackingRequest.setStatus(StatusEnum.PENDING_TRACKING.toValue());
        notificationDTO.getMsg().setTag("InTransit");

        notificationProcessService.process(notificationDTO.getMsg(),trackingRequest);

        verify(trackingResponseProducer, times(1)).send(any());
        verify(trackingRequestRepository, times(1)).save(any());
    }

    @DisplayName("When Exception_011 sub tag received in Notification msg then verify no tracking response sent")
    @Test
    public void processGetTrackingWhenExceptionSubTag11StatusReceived() {
        notificationDTO.getMsg().setTag("Exception");
        notificationDTO.getMsg().setSubtag("Exception_011");
        notificationProcessService.process(notificationDTO.getMsg(),trackingRequest);
        verifyNoTrackingResponseSent();

    }

    @DisplayName("When Exception_012 sub tag received in Notification msg then verify no tracking response sent")
    @Test
    public void processGetTrackingWhenExceptionSubTag12StatusReceived() {
        notificationDTO.getMsg().setTag("Exception");
        notificationDTO.getMsg().setSubtag("Exception_012");
        notificationProcessService.process(notificationDTO.getMsg(),trackingRequest);
        verifyNoTrackingResponseSent();

    }

    @DisplayName("When Exception_013 sub tag received in Notification msg then verify no tracking response sent")
    @Test
    public void processGetTrackingWhenExceptionSubTag13StatusReceived() {

        notificationDTO.getMsg().setSubtag("Exception_013");
        notificationProcessService.process(notificationDTO.getMsg(),trackingRequest);
        verifyNoTrackingResponseSent();

    }

    @DisplayName("When Exception_002 sub tag received in Notification msg then verify no tracking response sent")
    @Test
    public void processGetTrackingWhenExceptionSubTag02StatusReceived() {
        notificationDTO.getMsg().setTag("Exception");
        notificationDTO.getMsg().setSubtag("Exception_002");
        notificationProcessService.process(notificationDTO.getMsg(),trackingRequest);
        verifyNoTrackingResponseSent();

    }

    @DisplayName("When Exception_003 sub tag received in Notification msg then verify no tracking response sent")
    @Test
    public void processGetTrackingWhenExceptionSubTag03StatusReceived() {
        notificationDTO.getMsg().setTag("Exception");
        notificationDTO.getMsg().setSubtag("Exception_003");
        notificationProcessService.process(notificationDTO.getMsg(),trackingRequest);
        verifyNoTrackingResponseSent();

    }
    @DisplayName("When Exception_007 sub tag received in Notification msg then verify no tracking response sent")
    @Test
    public void processGetTrackingWhenExceptionSubTag07StatusReceived() {
        notificationDTO.getMsg().setTag("Exception");
        notificationDTO.getMsg().setSubtag("Exception_007");
        notificationProcessService.process(notificationDTO.getMsg(),trackingRequest);
        verifyNoTrackingResponseSent();

    }

    private void verifyNoTrackingResponseSent(){
        verify(trackingResponseProducer, times(0)).send(any());
        verify(trackingRequestRepository, times(1)).save(any());
    }
    private void initializeNotificationDTO() {
        notificationDTO = new NotificationDTO();
        Map<String, String> customFields = new HashMap<>();
        customFields.put(CUSTOM_FIELD_VIBRENT_ID, "1000");
        customFields.put(CUSTOM_FIELD_EXTERNAL_ID, "P1000");

        Tracking tracking = new Tracking();
        tracking.setCustomFields(customFields);
        tracking.setTrackingNumber("tracking_number_1");
        tracking.setExpectedDelivery("2021-08-15");
        tracking.setTag("InTransit");
        tracking.setLastUpdatedAt(lastUpdatedAt);

        notificationDTO.setMsg(tracking);
    }


    private void initializeTrackingRequest() {
        trackingRequest = new TrackingRequest();
        trackingRequest.setProvider(ProviderEnum.USPS.toValue());
        trackingRequest.setOperation(OperationEnum.TRACK_DELIVERY);
        trackingRequest.setStatus(StatusEnum.PENDING_TRACKING.toValue());
    }

    private List<String> initializeExceptionSubStatusList() {

        exceptionSubStatus = new ArrayList<>();
        exceptionSubStatus.add("Exception_011");
        exceptionSubStatus.add("Exception_002");
        exceptionSubStatus.add("Exception_003");
        exceptionSubStatus.add("Exception_007");
        exceptionSubStatus.add("Exception_012");
        exceptionSubStatus.add("Exception_013");
        return exceptionSubStatus;
    }

}