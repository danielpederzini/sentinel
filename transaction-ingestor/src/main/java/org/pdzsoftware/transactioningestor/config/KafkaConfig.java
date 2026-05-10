package org.pdzsoftware.transactioningestor.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.pdzsoftware.transactioningestor.dto.TransactionIngestionRequest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaConfig {
    private static final String ACKS_MODE = "all";

    @Bean
    public ProducerFactory<String, TransactionIngestionRequest> transactionProducerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> producerConfigs = new HashMap<>();
        producerConfigs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        producerConfigs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerConfigs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        producerConfigs.put(ProducerConfig.ACKS_CONFIG, ACKS_MODE);
        producerConfigs.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(producerConfigs);
    }

    @Bean
    public KafkaTemplate<String, TransactionIngestionRequest> transactionKafkaTemplate(ProducerFactory<String, TransactionIngestionRequest> transactionProducerFactory) {
        return new KafkaTemplate<>(transactionProducerFactory);
    }
}
