package com.vibrent.aftership.service;

import com.aftership.sdk.AfterShip;
import com.aftership.sdk.endpoint.TrackingEndpoint;
import com.aftership.sdk.model.tracking.NewTracking;
import com.aftership.sdk.model.tracking.Tracking;
import com.vibrent.aftership.converter.TrackingRequestConverter;
import com.vibrent.aftership.converter.TrackingRequestErrorConverter;
import com.vibrent.aftership.domain.TrackingRequest;
import com.vibrent.aftership.exception.AfterShipNonRetriableException;
import com.vibrent.aftership.repository.TrackingRequestErrorRepository;
import com.vibrent.aftership.repository.TrackingRequestRepository;
import com.vibrent.aftership.service.impl.AfterShipTrackingServiceImpl;
import com.vibrent.aftership.service.impl.TrackingRequestServiceImpl;
import com.vibrent.aftership.vo.TrackDeliveryRequestVo;
import com.vibrent.vxp.workflow.MessageHeaderDto;
import com.vibrent.vxp.workflow.ParticipantDetailsDto;
import com.vibrent.vxp.workflow.ProviderEnum;
import com.vibrenthealth.resiliency.core.Output;
import com.vibrenthealth.resiliency.core.RockSteadySystem;
import lombok.SneakyThrows;
import org.apache.kafka.test.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrackingRequestServiceTest {

    private TrackingRequestService trackingRequestService;

    private Tracking tracking;

    @Mock
    private TrackingRequestConverter trackingRequestConverter;

    @Mock
    private TrackingRequestRepository trackingRequestRepository;

    @Mock
    private RockSteadySystem rockSteadySystem;

    @Mock
    private TrackingRequestErrorRepository trackingRequestErrorRepository;

    @Mock
    private TrackingEndpoint trackingEndpoint;

    @Mock
    private ExternalLogService externalLogService;

    @Mock
    private TrackingRequestErrorConverter trackingRequestErrorConverter;

    private static TrackDeliveryRequestVo buildTrackDeliveryRequestVo() {
        ParticipantDetailsDto participant = new ParticipantDetailsDto();
        participant.setVibrentID(123L);
        participant.setExternalID("p123L");

        TrackDeliveryRequestVo requestDto = new TrackDeliveryRequestVo();
        requestDto.setParticipant(participant);
        requestDto.setTrackingID("trackingID");
        return requestDto;
    }

    @BeforeEach
    void setUp() throws Exception {
        AfterShip afterShip = new AfterShip("key");
        AfterShipTrackingService afterShipTrackingService = new AfterShipTrackingServiceImpl(afterShip, "408,429,503,504");
        TestUtils.setFieldValue(afterShip, "trackingEndpoint", trackingEndpoint);

        trackingRequestService = new TrackingRequestServiceImpl(afterShipTrackingService, trackingRequestConverter, trackingRequestRepository,
                rockSteadySystem, trackingRequestErrorRepository, externalLogService, trackingRequestErrorConverter);

        initializeTracking();
    }

    @DisplayName("when createTrackDeliveryRequest is invoked with null request " +
            "then verify createTrackDeliveryRequest return false value.")
    @SneakyThrows
    @Test
    void whenCreateTrackingWithNullRequestVerifyFailureResponse() {
        var response = trackingRequestService.createTrackDeliveryRequest(null, null);
        assertFalse(response);
    }

    @DisplayName("when try to creating the tracking request for already existing trackingId " +
            "then verify createTrackDeliveryRequest return false value.")
    @SneakyThrows
    @Test
    void whenDuplicateTrackingIDReceivedThenVerifyCreateTrackDeliveryRequestReturnsFalseResponse() {

        TrackDeliveryRequestVo requestDto = buildTrackDeliveryRequestVo();
        TrackingRequest trackingRequest = new TrackingRequest();
        trackingRequest.setTrackingId(requestDto.getTrackingID());
        trackingRequest.setProvider(ProviderEnum.USPS.toValue());


        when(trackingRequestRepository.findByTrackingId(trackingRequest.getTrackingId())).thenReturn(Optional.of(trackingRequest));
        var response = trackingRequestService.createTrackDeliveryRequest(requestDto, new MessageHeaderDto());
        assertFalse(response);
    }


    @DisplayName("Given createTrackDeliveryRequest is invoked with valid request and " +
            " aftership's createTracking request is successful " +
            "then verify createTrackDeliveryRequest return true value" +
            " and verify trackingRequest is saved to DB " +
            " and and trackingRequestError is not saved to DB " +
            " and verify external log event is send")
    @SneakyThrows
    @Test
    void whenCreateTrackingSuccessfulVerifySuccessResponse() {
        var request = buildTrackDeliveryRequestVo();
        var messageHeaderDto = new MessageHeaderDto();
        when(rockSteadySystem.executeWithRetries(any(), any(), any())).thenReturn(new Output<>(tracking));
        var response = trackingRequestService.createTrackDeliveryRequest(request, messageHeaderDto);
        assertTrue(response);
        verify(trackingRequestConverter).toTrackingRequest(request, messageHeaderDto);
        verify(trackingRequestRepository).save(any());
        verify(trackingRequestErrorRepository, times(0)).save(any());
        verify(externalLogService, times(1)).send(any(TrackDeliveryRequestVo.class), any(Long.class), any(Integer.class),
                any(NewTracking.class), any(Long.class), any(String.class), any(String.class));
    }

    @DisplayName("Given createTrackDeliveryRequest is invoked with valid request and " +
            " aftership's createTracking request is failed with exception " +
            "then verify createTrackDeliveryRequest return false value " +
            " and verify trackingRequest is not saved to DB " +
            "and trackingRequestError are saved to DB" +
            " and verify external log event is send")
    @SneakyThrows
    @Test
    void whenCreateTrackingRequestFailedVerifyFailureResponse() {
        var request = buildTrackDeliveryRequestVo();
        when(rockSteadySystem.executeWithRetries(any(), any(), any())).thenReturn(new Output<>(new AfterShipNonRetriableException("Invalid Key", 401)));
        var response = trackingRequestService.createTrackDeliveryRequest(request, new MessageHeaderDto());
        assertFalse(response);
        verify(trackingRequestRepository, times(0)).save(any());
        verify(trackingRequestErrorRepository, times(1)).save(any());
        verify(externalLogService, times(1)).send(any(TrackDeliveryRequestVo.class), any(Long.class), any(Integer.class),
                any(NewTracking.class), any(Long.class), any(String.class), any(String.class));
    }


    @DisplayName("When create tracking return with null error code " +
            "then verify createTrackDeliveryRequest return false value " +
            " and verify trackingRequest is not saved to DB " +
            "and trackingRequestError are saved to DB" +
            " and verify external log event is send")
    @SneakyThrows
    @Test
    void whenCreateTrackingRequestFailedWithNullErrorVerifyFailureResponse() {
        var request = buildTrackDeliveryRequestVo();
        when(rockSteadySystem.executeWithRetries(any(), any(), any())).thenReturn(new Output<>(new AfterShipNonRetriableException("Error", null)));
        var response = trackingRequestService.createTrackDeliveryRequest(request, new MessageHeaderDto());
        assertFalse(response);
        verify(trackingRequestRepository, times(0)).save(any());
        verify(trackingRequestErrorRepository, times(1)).save(any());
        verify(externalLogService, times(1)).send(any(TrackDeliveryRequestVo.class), any(Long.class), nullable(Integer.class),
                any(NewTracking.class), any(Long.class), any(String.class), any(String.class));
    }


    private void initializeTracking() {
        tracking = new Tracking();
        tracking.setTrackingNumber("tracking_number_1");
        tracking.setActive(true);
        tracking.setTag("InfoReceived");
    }
}
