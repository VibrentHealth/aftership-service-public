package com.vibrent.aftership.messaging.listener;


import com.vibrent.aftership.converter.TrackDeliveryRequestConverter;
import com.vibrent.aftership.messaging.KafkaMessageBuilder;
import com.vibrent.aftership.service.ExternalLogService;
import com.vibrent.aftership.service.TrackingRequestService;
import com.vibrent.aftership.vo.TrackDeliveryRequestVo;
import com.vibrent.vxp.workflow.FulfillmentTrackDeliveryRequestDto;
import com.vibrent.vxp.workflow.FulfillmentTrackDeliveryRequestDtoWrapper;
import com.vibrent.vxp.workflow.MessageHeaderDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * Kafka Listener for Fulfillment Tracking Requests
 */
@Service
@Slf4j
public class FulfillmentTrackDeliveryRequestListener {


    private final boolean kafkaEnabled;
    private final TrackingRequestService trackingRequestService;
    private final TrackDeliveryRequestConverter trackDeliveryRequestConverter;
    private final ExternalLogService externalLogService;

    public FulfillmentTrackDeliveryRequestListener(@Value("${kafka.enabled}") boolean kafkaEnabled,
                                                   TrackingRequestService trackingRequestService, TrackDeliveryRequestConverter trackDeliveryRequestConverter, ExternalLogService externalLogService) {
        this.kafkaEnabled = kafkaEnabled;
        this.trackingRequestService = trackingRequestService;
        this.trackDeliveryRequestConverter = trackDeliveryRequestConverter;
        this.externalLogService = externalLogService;
    }

    @KafkaListener(topics = "${kafka.topics.track.request}", groupId = "aftershipFulfillmentTrackingRequestListener",
            containerFactory = "kafkaListenerContainerFactoryFulfillmentTrackDeliveryRequest")
    public void listener(@Payload FulfillmentTrackDeliveryRequestDto fulfillmentTrackDeliveryRequestDto,
                         @Headers MessageHeaders requestHeaders) {
        if (!kafkaEnabled) {
            log.warn("kafka is not enabled");
            return;
        }
        log.info("aftership-Service: Received Fulfillment Track Delivery Request, FulfillmentTrackDeliveryRequestDto: {} ", fulfillmentTrackDeliveryRequestDto);

        try {
            MessageHeaderDto messageHeader = KafkaMessageBuilder.toMessageHeaderDto(requestHeaders);
            externalLogService.send(new FulfillmentTrackDeliveryRequestDtoWrapper(fulfillmentTrackDeliveryRequestDto, messageHeader));

            TrackDeliveryRequestVo trackDeliveryRequestVo = trackDeliveryRequestConverter.toTrackDeliveryRequestVo(fulfillmentTrackDeliveryRequestDto);
            trackingRequestService.createTrackDeliveryRequest(trackDeliveryRequestVo, messageHeader);
        } catch (Exception e) {
            log.error("Error while processing the create tracking request - {}", fulfillmentTrackDeliveryRequestDto, e);
        }
    }
}
