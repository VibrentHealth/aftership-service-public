package com.vibrent.aftership.integration.web;

import com.vibrent.aftership.constants.AfterShipConstants;
import com.vibrent.aftership.domain.TrackingRequest;
import com.vibrent.aftership.integration.IntegrationTestBase;
import com.vibrent.aftership.messaging.producer.impl.TrackingResponseProducer;
import com.vibrent.aftership.repository.TrackingRequestRepository;
import com.vibrent.aftership.web.AfterShipCallbackResource;
import com.vibrent.vxp.workflow.*;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AfterShipCallbackResourceTest extends IntegrationTestBase {

    public static final MediaType APPLICATION_JSON_UTF8 = new MediaType(MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(), StandardCharsets.UTF_8);

    @Value("${afterShip.webhookSecret}")
    String webHookSecret;

    String VALID_HMAC = "FceTeaOp8Nvp9CVTnfoda0RQiRjq8VZQErBg+Gn88/s=";
    String INVALID_VALID_HMAC = "RaaHIWnGfgK3KFh3kkiUxZ1DQNKqx6E8vMgrM793dIs=";
    String DATA = "{\"event_id\":\"c6e7be2c-e670-42fd-b59f-faf4e559e7d0\",\"event\":\"tracking_update\",\"is_tracking_first_tag\":true,\"msg\":{\"id\":\"jz7ho3394wh9skspqn97m00g\",\"tracking_number\":\"123456789\",\"title\":\"123456789\",\"note\":null,\"origin_country_iso3\":null,\"destination_country_iso3\":null,\"courier_destination_country_iso3\":null,\"shipment_package_count\":null,\"active\":false,\"order_id\":null,\"order_id_path\":null,\"order_date\":null,\"customer_name\":null,\"source\":\"api\",\"emails\":[],\"smses\":[],\"subscribed_smses\":[],\"subscribed_emails\":[],\"android\":[],\"ios\":[],\"return_to_sender\":false,\"custom_fields\":{\"emailaddress\":\"1@gmail.com\",\"vibrentId\":\"71019410\",\"externalId\":\"P512268268\"},\"tag\":\"Delivered\",\"subtag\":\"Delivered_001\",\"subtag_message\":\"Delivered\",\"tracked_count\":1,\"expected_delivery\":null,\"signed_by\":null,\"shipment_type\":null,\"created_at\":\"2021-08-24T07:19:56+00:00\",\"updated_at\":\"2021-08-24T10:40:08+00:00\",\"slug\":\"tnt\",\"unique_token\":\"deprecated\",\"path\":\"deprecated\",\"shipment_weight\":null,\"shipment_weight_unit\":null,\"delivery_time\":0,\"last_mile_tracking_supported\":null,\"language\":null,\"shipment_pickup_date\":null,\"shipment_delivery_date\":null,\"last_updated_at\":\"2021-08-24T10:40:08+00:00\",\"checkpoints\":[],\"order_promised_delivery_date\":null,\"delivery_type\":null,\"pickup_location\":null,\"pickup_note\":null,\"tracking_account_number\":null,\"tracking_origin_country\":null,\"tracking_destination_country\":null,\"tracking_key\":null,\"tracking_postal_code\":null,\"tracking_ship_date\":null,\"tracking_state\":null,\"courier_tracking_link\":\"https://www.tnt.com/express/en_in/site/shipping-tools/tracking.html?searchType=CON&cons=123456789\",\"first_attempted_at\":null,\"courier_redirect_link\":null},\"ts\":1629801608}";

    MockMvc mockMvc;

    @Autowired
    AfterShipCallbackResource afterShipCallbackResource;

    @MockBean
    private TrackingResponseProducer trackingResponseProducer;

    @Autowired
    private TrackingRequestRepository trackingRequestRepository;

    @Captor
    private ArgumentCaptor<TrackDeliveryResponseDtoWrapper> responseDtoWrapperArgumentCaptor;

    private TrackingRequest trackingRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(afterShipCallbackResource)
                .build();
    }

    @SneakyThrows
    @DisplayName("When the webhook event received from Aftership with valid signature " +
            "Then verify endpoint return with success response and track delivery response event is sent")
    @Test
    void testWebhookReceivedWithValidSignature() {
        initializeTrackingRequest();
        mockMvc.perform(
                post("/api/aftership/notification")
                .contentType(APPLICATION_JSON_UTF8)
                .header(AfterShipConstants.HEADER_HMAC_SHA256_V4_4_3, VALID_HMAC)
                .content(DATA))
                .andExpect(status().isOk());

        Mockito.verify(trackingResponseProducer).send(responseDtoWrapperArgumentCaptor.capture());

        verifyTrackDeliveryResponseDto();
        verifyTrackingRequest();
    }

    @SneakyThrows
    @DisplayName("When the webhook event received from Aftership with invalid valid signature" +
            "Then verify endpoint return with 401 response")
    @Test
    void testWebhookReceivedWithInvalidValidSignature() {
        mockMvc.perform(
                post("/api/aftership/notification")
                        .contentType(APPLICATION_JSON_UTF8)
                        .header(AfterShipConstants.HEADER_HMAC_SHA256_V4_4_3, INVALID_VALID_HMAC)
                        .content(DATA))
                .andExpect(status().isNotFound());


        mockMvc.perform(
                post("/api/aftership/notification")
                        .contentType(APPLICATION_JSON_UTF8)
                        .header(AfterShipConstants.HEADER_HMAC_SHA256_V4_4_4, INVALID_VALID_HMAC)
                        .content(DATA))
                .andExpect(status().isNotFound());

        mockMvc.perform(
                        post("/api/aftership/notification")
                                .contentType(APPLICATION_JSON_UTF8)
                                .content(DATA))
                .andExpect(status().isNotFound());

        mockMvc.perform(
                post("/api/aftership/notification")
                        .contentType(APPLICATION_JSON_UTF8)
                        .header(AfterShipConstants.HEADER_HMAC_SHA256_V4_4_3, VALID_HMAC))
                .andExpect(status().isNotFound());
    }

    private void initializeTrackingRequest() {
        trackingRequest = new TrackingRequest();
        trackingRequest.setTrackingId("123456789");
        trackingRequest.setProvider(ProviderEnum.USPS);
        trackingRequest.setOperation(OperationEnum.TRACK_DELIVERY);
        trackingRequest.setStatus(StatusEnum.PENDING_TRACKING.toValue());
        trackingRequest.setProvider(ProviderEnum.USPS);
        trackingRequest.setParticipant("{\"vibrentId\": 71019410, \"externalId\": \"P512268268\", \"phoneNumber\": \"1234567890\", \"emailAddress\": \"emailaddress@abc.com\"}");
        trackingRequest.setHeader("{\"source\":\"AfterShip\",\"VXP-Header-Version\":\"2.1.3\",\"VXP-Message-Id\":\"MessageID_1\",\"VXP-Message-Spec\":\"TRACK_DELIVERY_RESPONSE\",\"VXP-Message-Spec-Version\":\"2.1.2\",\"VXP-Message-Timestamp\":1630645343162,\"VXP-Originator\":\"PTBE\",\"VXP-Pattern\":\"WORKFLOW\",\"VXP-Trigger\":\"EVENT\",\"VXP-User-ID\":71019410,\"VXP-Workflow-Instance-ID\":\"WorkflowInstanceID_1\",\"VXP-Workflow-Name\":\"SALIVARY_KIT_ORDER\"}");
        this.trackingRequestRepository.save(trackingRequest);
    }

    private void verifyTrackDeliveryResponseDto() throws IOException {
        TrackDeliveryResponseDtoWrapper trackDeliveryResponseDtoWrapper = responseDtoWrapperArgumentCaptor.getValue();
        assertNotNull(trackDeliveryResponseDtoWrapper);

        TrackDeliveryResponseDto trackDeliveryResponseDto = trackDeliveryResponseDtoWrapper.getPayload();
        assertNotNull(trackDeliveryResponseDto);

        assertEquals("123456789", trackDeliveryResponseDto.getTrackingID());
        assertEquals(OperationEnum.TRACK_DELIVERY, trackDeliveryResponseDto.getOperation());
        assertTrue(trackDeliveryResponseDto.getDateTime() > 0);
        assertEquals(ProviderEnum.USPS, trackDeliveryResponseDto.getProvider());
        assertEquals(StatusEnum.DELIVERED, trackDeliveryResponseDto.getStatus());

        ParticipantDto participantDto = trackDeliveryResponseDto.getParticipant();
        assertNotNull(participantDto);
        assertEquals(71019410, participantDto.getVibrentID());
        assertEquals("P512268268", participantDto.getExternalID());
        assertNotNull(trackDeliveryResponseDto.getDates());
        assertEquals(0, trackDeliveryResponseDto.getDates().size());
    }

    private void verifyTrackingRequest() {
        TrackingRequest trackingRequest = this.trackingRequestRepository.findByTrackingId("123456789").orElse(null);
        assertNotNull(trackingRequest);
        assertEquals("123456789", trackingRequest.getTrackingId());
        assertEquals(OperationEnum.TRACK_DELIVERY, trackingRequest.getOperation());
        assertEquals(ProviderEnum.USPS, trackingRequest.getProvider());
        assertEquals("Delivered", trackingRequest.getStatus());
        assertEquals("Delivered_001", trackingRequest.getSubStatusCode());
        assertEquals("Delivered", trackingRequest.getSubStatusDescription());
        assertEquals("{\"vibrentId\": 71019410, \"externalId\": \"P512268268\", \"phoneNumber\": \"1234567890\", \"emailAddress\": \"emailaddress@abc.com\"}", trackingRequest.getParticipant());
        assertEquals("{\"source\": \"AfterShip\", \"VXP-Pattern\": \"WORKFLOW\", \"VXP-Trigger\": \"EVENT\", \"VXP-User-ID\": 71019410, \"VXP-Message-Id\": \"MessageID_1\", \"VXP-Originator\": \"PTBE\", \"VXP-Message-Spec\": \"TRACK_DELIVERY_RESPONSE\", \"VXP-Workflow-Name\": \"SALIVARY_KIT_ORDER\", \"VXP-Header-Version\": \"2.1.3\", \"VXP-Message-Timestamp\": 1630645343162, \"VXP-Message-Spec-Version\": \"2.1.2\", \"VXP-Workflow-Instance-ID\": \"WorkflowInstanceID_1\"}", trackingRequest.getHeader());
        assertNotNull(trackingRequest.getCarrierResponse());
    }
}
