package com.vibrent.aftership.integration.scheduling;

import com.aftership.sdk.AfterShip;
import com.aftership.sdk.endpoint.TrackingEndpoint;
import com.aftership.sdk.exception.ApiException;
import com.aftership.sdk.exception.RequestException;
import com.aftership.sdk.exception.SdkException;
import com.aftership.sdk.model.tracking.SlugTrackingNumber;
import com.aftership.sdk.model.tracking.Tracking;
import com.vibrent.aftership.domain.TrackingRequest;
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
import org.mockito.Mock;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Transactional
class GetTrackingJobTest extends IntegrationTestBase {

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

    @BeforeEach
    void setUp() throws Exception {
        initializeExcludeStatusList();
        afterShip = new AfterShip("key");
        TestUtils.setFieldValue(afterShip, "trackingEndpoint", trackingEndpoint);
        getTrackingJob = new GetTrackingJob(afterShip, trackingRequestRepository, notificationProcessService, externalLogService, fetchTrackingBeforeDays, excludeStatus);
    }

    @DisplayName("When Get Tracking Job executed, " +
            "Then verify records before three days get process for latest tracking.")
    @Test
    void whenJobExecutesThenVerifyLatestTrackingIsFetched() throws JobExecutionException, RequestException, ApiException, SdkException {
        initializeTracking();
        slugTrackingNumber = new SlugTrackingNumber("USPS", "123456789");
        when(trackingRequestRepository.findAllByStatusNotInAndUpdatedOnLessThan(anyList(), anyLong())).thenReturn(Stream.of(initializeTrackingRequest()));
        when(trackingEndpoint.getTracking(slugTrackingNumber, null)).thenReturn(tracking);
        getTrackingJob.execute(context);
        verify(notificationProcessService, times(1)).process(any(Tracking.class), any(TrackingRequest.class));
        verify(externalLogService, times(1)).send(any(SlugTrackingNumber.class), any(Tracking.class), anyLong(), anyString(), anyInt());
    }


    @DisplayName("When Get Tracking Job executed" +
            "And any Job exception encounters, " +
            "Then verify exception gets logs and event sent to external log.")
    @Test
    void whenJobExecutesAndExceptionEncountersThenVerifyEventLogToExternalLog() throws JobExecutionException, RequestException, ApiException, SdkException {
        initializeTracking();
        slugTrackingNumber = new SlugTrackingNumber("USPS", "123456789");
        when(trackingRequestRepository.findAllByStatusNotInAndUpdatedOnLessThan(anyList(), anyLong())).thenReturn(Stream.of(initializeTrackingRequest()));
        when(trackingEndpoint.getTracking(slugTrackingNumber, null)).thenThrow(new RuntimeException());
        getTrackingJob.execute(context);
        verify(notificationProcessService, times(0)).process(any(Tracking.class), any(TrackingRequest.class));
        verify(externalLogService, times(1)).send(anyString(),anyString(), anyLong(), anyString(), anyInt());
    }

    private TrackingRequest initializeTrackingRequest() {
        trackingRequest = new TrackingRequest();
        trackingRequest.setTrackingId("123456789");
        trackingRequest.setProvider(ProviderEnum.USPS);
        trackingRequest.setOperation(OperationEnum.TRACK_DELIVERY);
        trackingRequest.setStatus(StatusEnum.PENDING_TRACKING.toValue());
        trackingRequest.setProvider(ProviderEnum.USPS);
        trackingRequest.setParticipant("{\"vibrentId\": 71019410, \"externalId\": \"P512268268\", \"phoneNumber\": \"1234567890\", \"emailAddress\": \"emailaddress@abc.com\"}");
        trackingRequest.setHeader("{\"source\":\"AfterShip\",\"VXP-Header-Version\":\"2.1.3\",\"VXP-Message-Id\":\"MessageID_1\",\"VXP-Message-Spec\":\"TRACK_DELIVERY_RESPONSE\",\"VXP-Message-Spec-Version\":\"2.1.2\",\"VXP-Message-Timestamp\":1630645343162,\"VXP-Originator\":\"PTBE\",\"VXP-Pattern\":\"WORKFLOW\",\"VXP-Trigger\":\"EVENT\",\"VXP-User-ID\":71019410,\"VXP-Workflow-Instance-ID\":\"WorkflowInstanceID_1\",\"VXP-Workflow-Name\":\"SALIVARY_KIT_ORDER\"}");
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
