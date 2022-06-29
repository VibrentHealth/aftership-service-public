package com.vibrent.aftership.integration.scheduling;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.aftership.sdk.AfterShip;
import com.aftership.sdk.endpoint.TrackingEndpoint;
import com.aftership.sdk.exception.ApiException;
import com.aftership.sdk.exception.RequestException;
import com.aftership.sdk.exception.SdkException;
import com.aftership.sdk.model.tracking.SlugTrackingNumber;
import com.aftership.sdk.model.tracking.Tracking;
import com.vibrent.aftership.domain.TrackingRequest;
import com.vibrent.aftership.enums.CarrierResponseType;
import com.vibrent.aftership.integration.IntegrationTestBase;
import com.vibrent.aftership.repository.TrackingRequestRepository;
import com.vibrent.aftership.scheduling.GetTrackingJob;
import com.vibrent.aftership.service.ExternalLogService;
import com.vibrent.aftership.service.NotificationProcessService;
import com.vibrent.vxp.workflow.OperationEnum;
import com.vibrent.vxp.workflow.ProviderEnum;
import com.vibrent.vxp.workflow.StatusEnum;
import org.apache.kafka.test.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.vibrent.aftership.constants.AfterShipConstants.TAG_DELIVERED;
import static com.vibrent.aftership.constants.AfterShipConstants.TAG_EXCEPTION;
import static com.vibrent.aftership.service.impl.TrackingRequestServiceImpl.CUSTOM_FIELD_EXTERNAL_ID;
import static com.vibrent.aftership.service.impl.TrackingRequestServiceImpl.CUSTOM_FIELD_VIBRENT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Transactional
class GetTrackingJobTest extends IntegrationTestBase {
    public static final String trackingCarrierResponse = "{\"id\": \"sekjka2s71rs7kx5cb88101s\", \"ios\": [], \"tag\": \"Pending\", \"slug\": \"usps-api\", \"smses\": [], \"title\": \"420559059402011108131895519507\", \"active\": true, \"emails\": [], \"source\": \"api\", \"subtag\": \"Pending_001\", \"android\": [], \"createdAt\": 1639440109000, \"updatedAt\": 1649695189000, \"checkpoints\": [], \"uniqueToken\": \"deprecated\", \"customFields\": {\"vibrentid\": \"474052\", \"externalid\": \"P849246098\"}, \"deliveryTime\": 0, \"trackedCount\": 21, \"lastUpdatedAt\": 1641168135000, \"subtagMessage\": \"Pending\", \"returnToSender\": false, \"shipmentWeight\": 0, \"trackingNumber\": \"420559059402011108131895519507\", \"subscribedSmses\": [], \"subscribedEmails\": [], \"courierRedirectLink\": \"https://tools.usps.com/go/TrackConfirmAction?tRef=fullpage&tLc=2&text28777=&tLabels=420559059402011108131895519507%2C\", \"courierTrackingLink\": \"https://tools.usps.com/go/TrackConfirmAction?tLabels=420559059402011108131895519507\", \"shipmentPackageCount\": 0}";
    public static final String withoutSlug = "{\"id\": \"sekjka2s71rs7kx5cb88101s\", \"emails\": [], \"source\": \"api\", \"subtag\": \"Pending_001\", \"android\": [], \"createdAt\": 1639440109000, \"updatedAt\": 1649695189000, \"checkpoints\": [], \"uniqueToken\": \"deprecated\", \"customFields\": {\"vibrentid\": \"474052\", \"externalid\": \"P849246098\"}, \"deliveryTime\": 0, \"trackedCount\": 21, \"lastUpdatedAt\": 1641168135000, \"subtagMessage\": \"Pending\", \"returnToSender\": false, \"shipmentWeight\": 0, \"trackingNumber\": \"420559059402011108131895519507\", \"subscribedSmses\": [], \"subscribedEmails\": [], 059402011108131895519507%2C\", \"courierTrackingLink\": \"https://tools.usps.com/go/TrackConfirmAction?tLabels=420559059402011108131895519507\", \"shipmentPackageCount\": 0}";
    public static final String invalidCarrierResponse = "{\"id\":  \"android\": [], \"createdAt\": 1639440109000, \"updatedAt\": 1649695189000, \"checkpoints\": [], \"uniqueToken\": \"deprecated\", \"customFields\": {\"vibrentid\": \"474052\", \"externalid\": \"P849246098\"}, \"deliveryTime\": 0, \"trackedCount\": 21, \"lastUpdatedAt\": 1641168135000, \"subtagMessage\": \"Pending\", \"returnToSender\": false, \"shipmentWeight\": 0, \"trackingNumber\": \"420559059402011108131895519507\", \"subscribedSmses\": [], \"subscribedEmails\": [], \"courierRedirectLink\": \"https://tools.usps.com/go/TrackConfirmAction?tRef=fullpage&tLc=2&text28777=&tLabels=420559059402011108131895519507%2C\", \"courierTra}";
    public static final String notificationCarrierResponse ="{\"ts\": 1630997657, \"msg\": {\"id\": \"qpduxo4w1nmpmkt9pj6f40r2c\", \"ios\": [], \"tag\": \"Exception\", \"slug\": \"dhl\", \"smses\": [], \"title\": \"77771010\", \"active\": false, \"emails\": [], \"source\": \"api\", \"subtag\": \"Exception_007\", \"android\": [], \"createdAt\": 1630977250000, \"updatedAt\": 1630977857000, \"checkpoints\": [{\"tag\": \"InTransit\", \"city\": \"SOUTHWEST AREA\", \"slug\": \"dhl\", \"subtag\": \"InTransit_002\", \"message\": \"Shipment picked up\", \"location\": \"SOUTHWEST AREA - CHINA MAINLAND, China\", \"createdAt\": 1630977252000, \"coordinates\": [], \"countryIso3\": \"CHN\", \"countryName\": \"China\", \"subtagMessage\": \"Acceptance scan\", \"checkpointTime\": \"2021-08-19T20:30:00\"}, {\"tag\": \"InTransit\", \"city\": \"SYDNEY\", \"slug\": \"dhl\", \"subtag\": \"InTransit_002\", \"message\": \"Processed at  SYDNEY - AUSTRALIA\", \"location\": \"SYDNEY - AUSTRALIA\", \"createdAt\": 1630977252000, \"coordinates\": [], \"countryIso3\": \"AUS\", \"countryName\": \"Australia\", \"subtagMessage\": \"Acceptance scan\", \"checkpointTime\": \"2021-08-24T01:34:00\"}, {\"tag\": \"InTransit\", \"city\": \"SYDNEY\", \"slug\": \"dhl\", \"subtag\": \"InTransit_002\", \"message\": \"Processed at  SYDNEY - AUSTRALIA\", \"location\": \"SYDNEY - AUSTRALIA\", \"createdAt\": 1630977252000, \"coordinates\": [], \"countryIso3\": \"AUS\", \"countryName\": \"Australia\", \"subtagMessage\": \"Acceptance scan\", \"checkpointTime\": \"2021-08-24T19:10:00\"}, {\"tag\": \"InTransit\", \"city\": \"SYDNEY\", \"slug\": \"dhl\", \"subtag\": \"InTransit_007\", \"message\": \"Departed Facility in  SYDNEY - AUSTRALIA\", \"location\": \"SYDNEY - AUSTRALIA\", \"createdAt\": 1630977252000, \"coordinates\": [], \"countryIso3\": \"AUS\", \"countryName\": \"Australia\", \"subtagMessage\": \"Departure Scan\", \"checkpointTime\": \"2021-08-24T19:13:00\"}, {\"tag\": \"InTransit\", \"city\": \"BRISBANE\", \"slug\": \"dhl\", \"subtag\": \"InTransit_003\", \"message\": \"Arrived at Sort Facility  BRISBANE - AUSTRALIA\", \"location\": \"BRISBANE - AUSTRALIA\", \"createdAt\": 1630977252000, \"coordinates\": [], \"countryIso3\": \"AUS\", \"countryName\": \"Australia\", \"subtagMessage\": \"Arrival scan\", \"checkpointTime\": \"2021-08-25T05:40:00\"}, {\"tag\": \"InTransit\", \"city\": \"BRISBANE\", \"slug\": \"dhl\", \"subtag\": \"InTransit_002\", \"message\": \"Processed at  BRISBANE - AUSTRALIA\", \"location\": \"BRISBANE - AUSTRALIA\", \"createdAt\": 1630977252000, \"coordinates\": [], \"countryIso3\": \"AUS\", \"countryName\": \"Australia\", \"subtagMessage\": \"Acceptance scan\", \"checkpointTime\": \"2021-08-25T05:58:00\"}, {\"tag\": \"InTransit\", \"city\": \"BRISBANE\", \"slug\": \"dhl\", \"subtag\": \"InTransit_007\", \"message\": \"Departed Facility in  BRISBANE - AUSTRALIA\", \"location\": \"BRISBANE - AUSTRALIA\", \"createdAt\": 1630977252000, \"coordinates\": [], \"countryIso3\": \"AUS\", \"countryName\": \"Australia\", \"subtagMessage\": \"Departure Scan\", \"checkpointTime\": \"2021-08-25T06:40:00\"}, {\"tag\": \"InTransit\", \"city\": \"BRISBANE\", \"slug\": \"dhl\", \"subtag\": \"InTransit_003\", \"message\": \"Arrived at Delivery Facility in  BRISBANE - AUSTRALIA\", \"location\": \"BRISBANE - AUSTRALIA\", \"createdAt\": 1630977252000, \"coordinates\": [], \"countryIso3\": \"AUS\", \"countryName\": \"Australia\", \"subtagMessage\": \"Arrival scan\", \"checkpointTime\": \"2021-08-25T07:04:00\"}, {\"tag\": \"OutForDelivery\", \"city\": \"BRISBANE\", \"slug\": \"dhl\", \"subtag\": \"OutForDelivery_001\", \"message\": \"With delivery courier\", \"location\": \"BRISBANE - AUSTRALIA\", \"createdAt\": 1630977252000, \"coordinates\": [], \"countryIso3\": \"AUS\", \"countryName\": \"Australia\", \"subtagMessage\": \"Out for Delivery\", \"checkpointTime\": \"2021-08-25T09:39:00\"}, {\"tag\": \"Delivered\", \"city\": \"BRISBANE\", \"slug\": \"dhl\", \"subtag\": \"Delivered_001\", \"message\": \"Delivered\", \"location\": \"BRISBANE - AUSTRALIA\", \"createdAt\": 1630977252000, \"coordinates\": [], \"countryIso3\": \"AUS\", \"countryName\": \"Australia\", \"subtagMessage\": \"Delivered\", \"checkpointTime\": \"2021-08-25T14:16:00\"}], \"uniqueToken\": \"deprecated\", \"customFields\": {\"vibrentid\": \"85381410\", \"externalid\": \"P797801791\"}, \"deliveryTime\": 20, \"shipmentType\": \"DHL Express\", \"trackedCount\": 1, \"lastUpdatedAt\": 1630977857000, \"subtagMessage\": \"Returned to sender\", \"returnToSender\": true, \"shipmentWeight\": 0, \"trackingNumber\": \"77771010\", \"subscribedSmses\": [], \"firstAttemptedAt\": \"2021-08-25T14:16:00\", \"subscribedEmails\": [], \"originCountryIso3\": \"CHN\", \"shipmentPickupDate\": 1629385200000, \"courierRedirectLink\": \"https://delivery.dhl.com/waybill.xhtml?ctrycode=SG\", \"courierTrackingLink\": \"https://www.dhl.com/en/express/tracking.html?AWB=9108474550&brand=DHL\", \"shipmentDeliveryDate\": 1629881160000, \"shipmentPackageCount\": 1, \"destinationCountryIso3\": \"AUS\", \"lastMileTrackingSupported\": true, \"courierDestinationCountryIso3\": \"AUS\"}, \"event\": \"tracking_update\", \"event_id\": \"1defedb6-7970-41f2-9f57-6f8b562b7cdd\", \"is_tracking_first_tag\": true}";

    @Mock
    private TrackingEndpoint trackingEndpoint;

    private AfterShip afterShip;

    @Mock
    private TrackingRequestRepository trackingRequestRepository;

    @Mock
    private NotificationProcessService notificationProcessService;

    private Integer fetchTrackingBeforeDays = 3;

    GetTrackingJob getTrackingJob;

    private TrackingRequest trackingRequest;

    @Mock
    private ExternalLogService externalLogService;

    @Mock
    JobExecutionContext context;

    private Tracking tracking;

    private SlugTrackingNumber slugTrackingNumber;

    private List<String> excludeStatus;

    @Captor
    private ArgumentCaptor<SlugTrackingNumber> slugTrackingNumberArgumentCaptor;

    @BeforeEach
    void setUp() throws Exception {
        initializeExcludeStatusList();
        afterShip = new AfterShip("key");
        TestUtils.setFieldValue(afterShip, "trackingEndpoint", trackingEndpoint);
        getTrackingJob = new GetTrackingJob(afterShip, trackingRequestRepository, notificationProcessService, externalLogService, fetchTrackingBeforeDays, excludeStatus);
    }

    @DisplayName("When Get Tracking Job executed and carrier response type is tracking, " +
            "Then verify records before three days get process for latest tracking.")
    @Test
    void whenJobExecutesAndCarrierResponseTypeIsTrackingThenVerifyLatestTrackingIsFetched() throws JobExecutionException, RequestException, ApiException, SdkException {
        initializeTracking();
        TrackingRequest trackingRequest = initializeTrackingRequest(CarrierResponseType.TRACKING.toString(), trackingCarrierResponse);
        String slugFromCarrierResponse = GetTrackingJob.getSlugFromCarrierResponse(trackingRequest.getCarrierResponse(), CarrierResponseType.TRACKING.toString());
        slugTrackingNumber = new SlugTrackingNumber(slugFromCarrierResponse, trackingRequest.getTrackingId());
        when(trackingRequestRepository.findAllByStatusNotInAndUpdatedOnLessThan(anyList(), anyLong())).thenReturn(Stream.of(trackingRequest));
        when(trackingEndpoint.getTracking(slugTrackingNumber, null)).thenReturn(tracking);
        getTrackingJob.execute(context);
        verify(notificationProcessService, times(1)).process(any(Tracking.class), any(TrackingRequest.class));
        verify(externalLogService, times(1)).send(slugTrackingNumberArgumentCaptor.capture(), any(Tracking.class), anyLong(), anyString(), anyInt());
        SlugTrackingNumber value = slugTrackingNumberArgumentCaptor.getValue();
        assertEquals("usps-api", value.getSlug());
    }

    @DisplayName("When Get Tracking Job executed and and carrier response type is notification" +
            "Then verify records before three days get process for latest tracking.")
    @Test
    void whenJobExecutesAndCarrierResponseTypeIsNotificationThenVerifyLatestTrackingIsFetched() throws JobExecutionException, RequestException, ApiException, SdkException {
        initializeTracking();
        TrackingRequest trackingRequest = initializeTrackingRequest(CarrierResponseType.NOTIFICATION.toString(), notificationCarrierResponse);
        String slugFromCarrierResponse = GetTrackingJob.getSlugFromCarrierResponse(trackingRequest.getCarrierResponse(), CarrierResponseType.NOTIFICATION.toString());
        slugTrackingNumber = new SlugTrackingNumber(slugFromCarrierResponse, trackingRequest.getTrackingId());
        when(trackingRequestRepository.findAllByStatusNotInAndUpdatedOnLessThan(anyList(), anyLong())).thenReturn(Stream.of(trackingRequest));
        when(trackingEndpoint.getTracking(slugTrackingNumber, null)).thenReturn(tracking);
        getTrackingJob.execute(context);
        verify(notificationProcessService, times(1)).process(any(Tracking.class), any(TrackingRequest.class));
        verify(externalLogService, times(1)).send(slugTrackingNumberArgumentCaptor.capture(), any(Tracking.class), anyLong(), anyString(), anyInt());
        SlugTrackingNumber value = slugTrackingNumberArgumentCaptor.getValue();
        assertEquals("dhl", value.getSlug());
    }

    @DisplayName("When Get Tracking Job executed, " +
            "And carrier response does not contains slug" +
            "Then verify tracking request not sent.")
    @Test
    void whenJobExecutesAndSlugValueNotPresentThenVerifyLatestTrackingIsNotFetched() throws JobExecutionException {
        initializeTracking();
        when(trackingRequestRepository.findAllByStatusNotInAndUpdatedOnLessThan(anyList(), anyLong())).thenReturn(Stream.of(initializeTrackingRequest("someInvalidType", withoutSlug)));
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        Logger logger = (Logger) LoggerFactory.getLogger(GetTrackingJob.class);
        logger.addAppender(listAppender);
        listAppender.start();
        getTrackingJob.execute(context);
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(Level.WARN, logsList.get(1).getLevel());
        assertEquals("AfterShip| Carrier response don't have slug value for tracking id : {}", logsList.get(1).getMessage());
        verify(notificationProcessService, times(0)).process(any(Tracking.class), any(TrackingRequest.class));
        verify(externalLogService, times(0)).send(any(SlugTrackingNumber.class), any(Tracking.class), anyLong(), anyString(), anyInt());
    }

    @DisplayName("When Get Tracking Job executed, " +
            "And carrier response does not contains slug" +
            "Then verify tracking request not sent.")
    @Test
    void whenJobExecutesAndCarrierResponseIsNullNotPresentThenVerifyLatestTrackingIsNotFetched() throws JobExecutionException {
        initializeTracking();
        when(trackingRequestRepository.findAllByStatusNotInAndUpdatedOnLessThan(anyList(), anyLong())).thenReturn(Stream.of(initializeTrackingRequest("someInvalidType", null)));
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        Logger logger = (Logger) LoggerFactory.getLogger(GetTrackingJob.class);
        logger.addAppender(listAppender);
        listAppender.start();
        getTrackingJob.execute(context);
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(Level.WARN, logsList.get(1).getLevel());
        assertEquals("AfterShip| Carrier response don't have slug value for tracking id : {}", logsList.get(1).getMessage());
        verify(notificationProcessService, times(0)).process(any(Tracking.class), any(TrackingRequest.class));
        verify(externalLogService, times(0)).send(any(SlugTrackingNumber.class), any(Tracking.class), anyLong(), anyString(), anyInt());
    }

    @DisplayName("When Get Tracking Job executed, " +
            "And get exception while fetching carrier response" +
            "Then verify tracking request not sent.")
    @Test
    void whenJobExecutesAndCarrierResponseInInvalidThenVerifyLatestTrackingIsNotFetched() throws JobExecutionException {
        initializeTracking();
        when(trackingRequestRepository.findAllByStatusNotInAndUpdatedOnLessThan(anyList(), anyLong())).thenReturn(Stream.of(initializeTrackingRequest(CarrierResponseType.TRACKING.toString(), invalidCarrierResponse)));
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        Logger logger = (Logger) LoggerFactory.getLogger(GetTrackingJob.class);
        logger.addAppender(listAppender);
        listAppender.start();
        getTrackingJob.execute(context);
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(Level.WARN, logsList.get(1).getLevel());
        assertEquals("AfterShip| Error while extracting slug from carrier response ", logsList.get(1).getMessage());

        verify(notificationProcessService, times(0)).process(any(Tracking.class), any(TrackingRequest.class));
        verify(externalLogService, times(0)).send(any(SlugTrackingNumber.class), any(Tracking.class), anyLong(), anyString(), anyInt());
    }


    @DisplayName("When Get Tracking Job executed" +
            "And any Job exception encounters, " +
            "Then verify exception gets logs and event sent to external log.")
    @Test
    void whenJobExecutesAndExceptionEncountersThenVerifyEventLogToExternalLog() throws JobExecutionException, RequestException, ApiException, SdkException {
        initializeTracking();
        slugTrackingNumber = new SlugTrackingNumber("USPS", "123456789");
        when(trackingRequestRepository.findAllByStatusNotInAndUpdatedOnLessThan(anyList(), anyLong())).thenReturn(Stream.of(initializeTrackingRequest(CarrierResponseType.TRACKING.toString(), trackingCarrierResponse)));
        when(trackingEndpoint.getTracking(slugTrackingNumber, null)).thenThrow(new RuntimeException());
        getTrackingJob.execute(context);
        verify(notificationProcessService, times(0)).process(any(Tracking.class), any(TrackingRequest.class));
        verify(externalLogService, times(1)).send(anyString(),anyString(), anyLong(), anyString(), anyInt());
    }
    
    @Test
    void testGetSlugFromCarrierResponse() {
        assertEquals("dhl", GetTrackingJob.getSlugFromCarrierResponse(notificationCarrierResponse, CarrierResponseType.NOTIFICATION.toString()));
        assertEquals("usps-api", GetTrackingJob.getSlugFromCarrierResponse(trackingCarrierResponse, CarrierResponseType.TRACKING.toString()));
        assertNull(GetTrackingJob.getSlugFromCarrierResponse(withoutSlug, CarrierResponseType.TRACKING.toString()));
        assertNull(GetTrackingJob.getSlugFromCarrierResponse(null, CarrierResponseType.TRACKING.toString()));
        assertNull(GetTrackingJob.getSlugFromCarrierResponse(notificationCarrierResponse, null));

    }

    private TrackingRequest initializeTrackingRequest(String carrierResponseType, String carrierResponse) {
        trackingRequest = new TrackingRequest();
        trackingRequest.setTrackingId("123456789");
        trackingRequest.setProvider(ProviderEnum.USPS);
        trackingRequest.setOperation(OperationEnum.TRACK_DELIVERY);
        trackingRequest.setStatus(StatusEnum.PENDING_TRACKING.toValue());
        trackingRequest.setProvider(ProviderEnum.USPS);
        trackingRequest.setParticipant("{\"vibrentId\": 71019410, \"externalId\": \"P512268268\", \"phoneNumber\": \"1234567890\", \"emailAddress\": \"emailaddress@abc.com\"}");
        trackingRequest.setHeader("{\"source\":\"AfterShip\",\"VXP-Header-Version\":\"2.1.3\",\"VXP-Message-Id\":\"MessageID_1\",\"VXP-Message-Spec\":\"TRACK_DELIVERY_RESPONSE\",\"VXP-Message-Spec-Version\":\"2.1.2\",\"VXP-Message-Timestamp\":1630645343162,\"VXP-Originator\":\"PTBE\",\"VXP-Pattern\":\"WORKFLOW\",\"VXP-Trigger\":\"EVENT\",\"VXP-User-ID\":71019410,\"VXP-Workflow-Instance-ID\":\"WorkflowInstanceID_1\",\"VXP-Workflow-Name\":\"SALIVARY_KIT_ORDER\"}");
        trackingRequest.setCarrierResponseType(carrierResponseType);
        trackingRequest.setCarrierResponse(carrierResponse);
        return trackingRequest;
    }

    private void initializeTracking() {
        Map<String, String> customFields = new HashMap<>();
        customFields.put(CUSTOM_FIELD_VIBRENT_ID, "71019410");
        customFields.put(CUSTOM_FIELD_EXTERNAL_ID, "P512268268");

        tracking = new Tracking();
        tracking.setTrackingNumber("123456789");
        tracking.setActive(true);
        tracking.setTag("Delivered");
        tracking.setCustomFields(customFields);

    }

    private List<String> initializeExcludeStatusList() {
        excludeStatus = new ArrayList<>();
        excludeStatus.add(TAG_DELIVERED);
        excludeStatus.add(TAG_EXCEPTION);
        return excludeStatus;
    }
}
