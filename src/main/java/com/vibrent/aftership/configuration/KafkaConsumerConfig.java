package com.vibrent.aftership.configuration;

import com.vibrent.aftership.constants.KafkaConstants;
import com.vibrent.aftership.dto.RetryRequestDTO;
import com.vibrent.vxp.workflow.FulfillmentTrackDeliveryRequestDto;
import com.vibrent.vxp.workflow.MessageSpecificationEnum;
import com.vibrent.vxp.workflow.TrackDeliveryRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


@Configuration
@EnableKafka
@Slf4j
public class KafkaConsumerConfig {

    private static final String TRACKING_REQUEST_GROUP_ID = "AFTER_SHIP_TRACKING_REQUEST_GROUP_ID";
    private static final String RETRY_TRACKING_REQUEST_GROUP_ID = "AFTER_SHIP_RETRY_TRACKING_REQUEST_GROUP_ID";
    private static final String FULFILLMENT_TRACKING_REQUEST_GROUP_ID = "AFTER_SHIP_FULFILLMENT_TRACKING_REQUEST_GROUP_ID";
    private final String bootstrapServers;
    private final int defaultConcurrency;

    public KafkaConsumerConfig(@Value("${kafka.bootstrap-servers}") String bootstrapServers,
                               @Value("${kafka.defaultConcurrency}") int defaultConcurrency) {
        this.bootstrapServers = bootstrapServers;
        this.defaultConcurrency = defaultConcurrency;
    }

    public static String extractHeader(Headers headers, String headerKey) {
        String headerValue = null;

        if (headers != null) {
            for (Header header : headers) {
                if (headerKey.equalsIgnoreCase(header.key())
                        && header.value() != null
                        && header.value().length > 0) {
                    headerValue = new String(header.value(), StandardCharsets.UTF_8).trim();
                    //Remove leading and tailing quotes
                    headerValue = headerValue.replaceAll("(^\"+)|(\"+$)", "");
                    break;
                }
            }
        }
        return headerValue;
    }

    private static <T> JsonDeserializer<T> getJsonDeserializer(Class<T> clazz) {
        JsonDeserializer<T> deserializer = new JsonDeserializer<>(clazz);
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages("com.vibrent.*");
        deserializer.setUseTypeMapperForKey(true);
        return deserializer;
    }

    private Map<String, Object> getConfigProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        return props;
    }

    @Bean
    public ConsumerFactory<String, byte[]> trackDeliveryRequestConsumerFactory() {
        Map<String, Object> consumerConfigProps = getConfigProps();
        consumerConfigProps.put(ConsumerConfig.GROUP_ID_CONFIG, TRACKING_REQUEST_GROUP_ID);
        consumerConfigProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerConfigProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        return new DefaultKafkaConsumerFactory<>(consumerConfigProps, new StringDeserializer(), new ByteArrayDeserializer());
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, byte[]>> kafkaListenerContainerFactoryTrackDeliveryRequest() {
        ConcurrentKafkaListenerContainerFactory<String, byte[]> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(trackDeliveryRequestConsumerFactory());
        factory.setConcurrency(defaultConcurrency);
        factory.getContainerProperties().setPollTimeout(KafkaConstants.POLL_TIMEOUT);
        factory.setRecordFilterStrategy(consumerRecord -> {
            String messageSpec = extractHeader(consumerRecord.headers(), KafkaConstants.VXP_MESSAGE_SPEC);

            //discard the Record if MessageSpecification is not equal to TRACK_DELIVERY_REQUEST
            final boolean canDiscardRecord = null == messageSpec
                    || !MessageSpecificationEnum.TRACK_DELIVERY_REQUEST.toString().equals(messageSpec);

            if (canDiscardRecord) {
                log.info("aftership-service: Discarding the Non TRACK_DELIVERY_REQUEST. Request Type: {}", messageSpec);
            }

            return canDiscardRecord;
        });

        return factory;
    }

    @Bean
    public ConsumerFactory<String, byte[]> retryTrackDeliveryRequestConsumerFactory() {
        Map<String, Object> consumerConfigProps = getConfigProps();
        consumerConfigProps.put(ConsumerConfig.GROUP_ID_CONFIG, RETRY_TRACKING_REQUEST_GROUP_ID);
        consumerConfigProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerConfigProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(consumerConfigProps, new StringDeserializer(), new ByteArrayDeserializer());
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, byte[]>> kafkaListenerContainerFactoryRetryTrackDeliveryRequest() {
        ConcurrentKafkaListenerContainerFactory<String, byte[]> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(retryTrackDeliveryRequestConsumerFactory());
        factory.setConcurrency(defaultConcurrency);
        factory.getContainerProperties().setPollTimeout(KafkaConstants.POLL_TIMEOUT);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, byte[]> fulfillmentTrackDeliveryRequestConsumerFactory() {
        Map<String, Object> consumerConfigProps = getConfigProps();
        consumerConfigProps.put(ConsumerConfig.GROUP_ID_CONFIG, FULFILLMENT_TRACKING_REQUEST_GROUP_ID);
        consumerConfigProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerConfigProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        return new DefaultKafkaConsumerFactory<>(consumerConfigProps, new StringDeserializer(), new ByteArrayDeserializer());
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, byte[]>> kafkaListenerContainerFactoryFulfillmentTrackDeliveryRequest() {
        ConcurrentKafkaListenerContainerFactory<String, byte[]> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(fulfillmentTrackDeliveryRequestConsumerFactory());
        factory.setConcurrency(defaultConcurrency);
        factory.getContainerProperties().setPollTimeout(KafkaConstants.POLL_TIMEOUT);
        factory.setRecordFilterStrategy(consumerRecord -> {
            String messageSpec = extractHeader(consumerRecord.headers(), KafkaConstants.VXP_MESSAGE_SPEC);

            //discard the Record if MessageSpecification is not equal to FULFILMENT_TRACK_DELIVERY_REQUEST
            final boolean canDiscardRecord = null == messageSpec
                    || !MessageSpecificationEnum.FULFILMENT_TRACK_DELIVERY_REQUEST.toString().equals(messageSpec);

            if (canDiscardRecord) {
                log.info("aftership-service: Discarding the Non FULFILMENT_TRACK_DELIVERY_REQUEST. Request Type: {}", messageSpec);
            }
            return canDiscardRecord;
        });
        return factory;
    }

}
