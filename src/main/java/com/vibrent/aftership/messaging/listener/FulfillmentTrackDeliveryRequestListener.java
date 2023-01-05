package com.vibrent.aftership.messaging.listener;


import com.vibrent.aftership.converter.TrackDeliveryRequestConverter;
import com.vibrent.aftership.messaging.KafkaMessageBuilder;
import com.vibrent.aftership.service.ExternalLogService;
import com.vibrent.aftership.service.TrackingRequestService;
import com.vibrent.aftership.util.JacksonUtil;
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
import java.util.Arrays;

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

   public void listener(@Payload byte[] payloadByteArray,
                         @Headers MessageHeaders messageHeaders) {

       FulfillmentTrackDeliveryRequestDto fulfillmentTrackDeliveryRequestDto = convertToFulfillmentTrackDeliveryRequestDto(payloadByteArray, messageHeaders );

        if (!kafkaEnabled) {
            log.warn("kafka is not enabled");
            return;
        }
        log.info("aftership-Service: Received Fulfillment Track Delivery Request, payload: {} messageHeaders{} ", payloadByteArray,messageHeaders);

        TrackDeliveryRequestVo trackDeliveryRequestVo = trackDeliveryRequestConverter.toTrackDeliveryRequestVo(fulfillmentTrackDeliveryRequestDto);
        trackingRequestService.createTrackDeliveryRequest(trackDeliveryRequestVo, KafkaMessageBuilder.toMessageHeaderDto(messageHeaders));
    }

    FulfillmentTrackDeliveryRequestDto convertToFulfillmentTrackDeliveryRequestDto(byte[] payloadByteArray, MessageHeaders messageHeaders) {
        FulfillmentTrackDeliveryRequestDto fulfillmentTrackDeliveryRequestDto = null;
        try {
            fulfillmentTrackDeliveryRequestDto = JacksonUtil.getMapper().readValue(payloadByteArray, FulfillmentTrackDeliveryRequestDto.class);
        } catch (Exception e) {
            log.warn("aftership-service: Cannot convert Payload to fulfillmentTrackDeliveryRequestDto  headers {} payload: {}",  messageHeaders.toString(), Arrays.toString(payloadByteArray), e);
        }
        return fulfillmentTrackDeliveryRequestDto;
    }

}
