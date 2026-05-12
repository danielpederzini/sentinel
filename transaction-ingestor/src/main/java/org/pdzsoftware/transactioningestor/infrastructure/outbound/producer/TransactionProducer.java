package org.pdzsoftware.transactioningestor.infrastructure.outbound.producer;

import lombok.RequiredArgsConstructor;
import org.pdzsoftware.transactioningestor.infrastructure.config.properties.KafkaProducerProperties;
import org.pdzsoftware.transactioningestor.infrastructure.inbound.controller.dto.TransactionIngestionRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class TransactionProducer {
    private final KafkaTemplate<String, TransactionIngestionRequest> kafkaTemplate;
    private final KafkaProducerProperties kafkaProducerProperties;

    public Mono<SendResult<String, TransactionIngestionRequest>> publish(TransactionIngestionRequest request) {
        String topic = kafkaProducerProperties.getTransactionsCreatedTopic();
        String key = request.transactionId();
        return Mono.fromFuture(kafkaTemplate.send(topic, key, request));
    }
}
