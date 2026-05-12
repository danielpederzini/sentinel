package org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.producer;

import lombok.RequiredArgsConstructor;
import org.pdzsoftware.antifraudorchestrator.infrastructure.config.properties.KafkaProducerProperties;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.producer.dto.TransactionScoredMessage;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class TransactionProducer {
    private final KafkaTemplate<String, TransactionScoredMessage> kafkaTemplate;
    private final KafkaProducerProperties kafkaProducerProperties;

    public CompletableFuture<SendResult<String, TransactionScoredMessage>> publish(TransactionScoredMessage request) {
        String topic = kafkaProducerProperties.getTransactionsScoredTopic();
        String key = request.transactionId();
        return kafkaTemplate.send(topic, key, request);
    }
}
