package org.pdzsoftware.antifraudorchestrator.infrastructure.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.pdzsoftware.antifraudorchestrator.infrastructure.config.properties.KafkaProducerProperties;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.producer.dto.TransactionScoredMessage;
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
@EnableConfigurationProperties(KafkaProducerProperties.class)
public class KafkaProducerConfig {
    @Bean
    public ProducerFactory<String, TransactionScoredMessage> transactionProducerFactory(KafkaProducerProperties kafkaProducerProperties) {
        Map<String, Object> producerConfigs = new HashMap<>();
        producerConfigs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProducerProperties.getBootstrapServers());
        producerConfigs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerConfigs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        producerConfigs.put(ProducerConfig.ACKS_CONFIG, kafkaProducerProperties.getAcks());
        producerConfigs.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, kafkaProducerProperties.isIdempotenceEnabled());
        return new DefaultKafkaProducerFactory<>(producerConfigs);
    }

    @Bean
    public KafkaTemplate<String, TransactionScoredMessage> transactionKafkaTemplate(ProducerFactory<String, TransactionScoredMessage> transactionProducerFactory) {
        return new KafkaTemplate<>(transactionProducerFactory);
    }
}
