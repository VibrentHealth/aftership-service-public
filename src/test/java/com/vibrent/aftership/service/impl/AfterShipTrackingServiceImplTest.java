package com.vibrent.aftership.service.impl;

import com.aftership.sdk.AfterShip;
import com.aftership.sdk.endpoint.TrackingEndpoint;
import com.aftership.sdk.exception.ApiException;
import com.aftership.sdk.model.Meta;
import com.aftership.sdk.model.RateLimit;
import com.aftership.sdk.model.tracking.NewTracking;
import com.aftership.sdk.model.tracking.Tracking;
import com.vibrent.aftership.exception.AfterShipNonRetriableException;
import com.vibrent.aftership.exception.AfterShipRetriableException;
import com.vibrent.aftership.service.AfterShipTrackingService;
import lombok.SneakyThrows;
import org.apache.kafka.test.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AfterShipTrackingServiceImplTest {

    @Mock
    private TrackingEndpoint trackingEndpoint;

    private AfterShipTrackingService afterShipTrackingService;

    private NewTracking newTracking;
    private Tracking tracking;

    @BeforeEach
    void setUp() throws Exception {
        AfterShip afterShip = new AfterShip("key");
        TestUtils.setFieldValue(afterShip, "trackingEndpoint", trackingEndpoint);
        afterShipTrackingService = new AfterShipTrackingServiceImpl(afterShip, "408,429,503,504");
        initializeNewTracking();
        initializeTracking();
    }

    @DisplayName("Given createTracking is invoked with valid request and " +
            " aftership's createTracking request is successful " +
            "then verify method return Tracking response")
    @SneakyThrows
    @Test
    void whenCreateTrackingSuccessfulVerifySuccessResponse() {
        when(trackingEndpoint.createTracking(newTracking)).thenReturn(tracking);
        Tracking tracking = afterShipTrackingService.createTracking(newTracking);
        assertNotNull(tracking);
    }

    @DisplayName("Given createTracking is invoked with valid request and " +
            "aftership's createTracking request is failed with exception " +
            "then verify createTracking throws AfterShipNonRetriableException")
    @SneakyThrows
    @Test
    void whenCreateTrackingRequestFailedWith401VerifyFailureResponse() {
        Meta meta = new Meta();
        meta.setCode(401);
        doThrow(new ApiException(new RateLimit(), meta)).when(trackingEndpoint).createTracking(newTracking);
        assertThrows(AfterShipNonRetriableException.class, () -> afterShipTrackingService.createTracking(newTracking));
    }

    @DisplayName("Given createTracking is invoked with valid request and " +
            "aftership's createTracking request is failed with exception " +
            "then verify createTracking throws AfterShipNonRetriableException")
    @SneakyThrows
    @Test
    void whenCreateTrackingRequestFailedWith429VerifyFailureResponse() {
        Meta meta = new Meta();
        meta.setCode(429);
        doThrow(new ApiException(new RateLimit(), meta)).when(trackingEndpoint).createTracking(newTracking);
        assertThrows(AfterShipRetriableException.class, () -> afterShipTrackingService.createTracking(newTracking));
    }

    private void initializeNewTracking() {
        newTracking = new NewTracking();
        newTracking.setTrackingNumber("tracking_number_1");
    }

    private void initializeTracking() {
        tracking = new Tracking();
        tracking.setTrackingNumber("tracking_number_1");
        tracking.setActive(true);
        tracking.setTag("InfoReceived");
    }
}