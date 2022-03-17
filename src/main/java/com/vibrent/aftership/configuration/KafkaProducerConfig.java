package com.vibrent.aftership.configuration;

import com.vibrent.aftership.dto.ExternalLogDTO;
import com.vibrent.aftership.dto.RetryRequestDTO;
import com.vibrent.vxp.workflow.TrackDeliveryResponseDtoWrapper;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {
    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;

    private Map<String, Object> producerConfigs(Map<String, Object> configs) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        props.putAll(configs);
        return props;
    }

    @Bean
    public ProducerFactory<String, TrackDeliveryResponseDtoWrapper> trackingResponseProducerFactory(Map<String, Object> configs) {
        return new DefaultKafkaProducerFactory<>(producerConfigs(configs));
    }

    @Bean
    public KafkaTemplate<String, TrackDeliveryResponseDtoWrapper> trackingResponseKafkaTemplate() {
        Map<String, Object> configs = new HashMap<>();
        return new KafkaTemplate<>(trackingResponseProducerFactory(configs));
    }

    @Bean
    public ProducerFactory<String, ExternalLogDTO> producerFactoryExternalLog() {
        Map<String, Object> configs = new HashMap<>();
        return new DefaultKafkaProducerFactory<>(this.producerConfigs(configs));
    }

    @Bean
    public KafkaTemplate<String, ExternalLogDTO> kafkaTemplateExternalLogDTO() {
        return new KafkaTemplate<>(producerFactoryExternalLog());
    }

    @Bean
    public ProducerFactory<String, RetryRequestDTO> producerFactoryRetryRequestDTO(Map<String, Object> configs) {
        return new DefaultKafkaProducerFactory<>(producerConfigs(configs));
    }

    @Bean
    public KafkaTemplate<String, RetryRequestDTO> kafkaTemplateRetryRequestDTO() {
        Map<String, Object> configs = new HashMap<>();
        return new KafkaTemplate<>(producerFactoryRetryRequestDTO(configs));
    }
}
