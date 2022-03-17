package com.vibrent.aftership.integration.scheduling;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vibrent.aftership.domain.TrackingRequestError;
import com.vibrent.aftership.dto.RetryRequestDTO;
import com.vibrent.aftership.integration.IntegrationTestBase;
import com.vibrent.aftership.messaging.producer.impl.RetryTrackingDeliveryRequestProducer;
import com.vibrent.aftership.repository.TrackingRequestErrorRepository;
import com.vibrent.aftership.scheduling.RetryTrackingDeliveryRequestJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Transactional
class RetryTrackingDeliveryRequestJobTest extends IntegrationTestBase {
    public static final String DELIVERY_REQUEST = "{\"operation\":\"TRACK_DELIVERY\",\"participant\":{\"addresses\":[{\"city\":\"Arizona\"}],\"emailAddress\":\"abc@abc.com\",\"externalId\":\"P1234\",\"firstName\":\"Ram\",\"vibrentId\":1234},\"provider\":\"USPS\",\"trackingId\":\"93674815\"}";
    public static final String MESSAGE_HEADERS = "{\"source\":\"AfterShip\",\"VXP-Header-Version\":\"2.1.3\",\"VXP-Message-Id\":\"MessageID_1\",\"VXP-Message-Spec\":\"TRACK_DELIVERY_RESPONSE\",\"VXP-Message-Spec-Version\":\"2.1.2\",\"VXP-Message-Timestamp\":1630645343162,\"VXP-Originator\":\"PTBE\",\"VXP-Pattern\":\"WORKFLOW\",\"VXP-Trigger\":\"EVENT\",\"VXP-User-ID\":71019410,\"VXP-Workflow-Instance-ID\":\"WorkflowInstanceID_1\",\"VXP-Workflow-Name\":\"SALIVARY_KIT_ORDER\"}";
    public static final String TRACKING_ID = "879658";
    public final Integer maxRetryCount = 3;


    @Autowired
    private TrackingRequestErrorRepository trackingRequestErrorRepository;

    @Mock
    RetryTrackingDeliveryRequestProducer retryTrackingDeliveryRequestProducer;

    @Mock
    JobExecutionContext context;

    RetryTrackingDeliveryRequestJob retryTrackingDeliveryRequestJob;

    @BeforeEach
    void setUp() throws Exception {
        retryTrackingDeliveryRequestJob = new RetryTrackingDeliveryRequestJob(trackingRequestErrorRepository, retryTrackingDeliveryRequestProducer, maxRetryCount);
        saveTrackingRequestError();
    }

    @DisplayName("When Retry Tracking Delivery Request Job executed, " +
            "Then verify kafka message sent for each error tracking Delivery.")
    @Test
    void whenJobExecutesThenVerifyErrorTrackingDeliverySentOnKafka() throws JobExecutionException, JsonProcessingException {
        retryTrackingDeliveryRequestJob.execute(context);
        verify(retryTrackingDeliveryRequestProducer, times(1)).send(any(RetryRequestDTO.class));
    }


    TrackingRequestError saveTrackingRequestError() {
        TrackingRequestError trackingRequestError = new TrackingRequestError();
        trackingRequestError.setRetryCount(0);
        trackingRequestError.setTrackingId(TRACKING_ID);
        trackingRequestError.setErrorCode(4012);
        trackingRequestError.setTrackDeliveryRequest(DELIVERY_REQUEST);
        trackingRequestError.setHeader(MESSAGE_HEADERS);
        return trackingRequestErrorRepository.save(trackingRequestError);
    }

}
