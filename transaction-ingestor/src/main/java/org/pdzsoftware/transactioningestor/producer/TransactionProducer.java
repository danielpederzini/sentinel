package org.pdzsoftware.transactioningestor.producer;

import lombok.RequiredArgsConstructor;
import org.pdzsoftware.transactioningestor.config.properties.KafkaProperties;
import org.pdzsoftware.transactioningestor.dto.TransactionIngestionRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class TransactionProducer {
    private final KafkaTemplate<String, TransactionIngestionRequest> kafkaTemplate;
    private final KafkaProperties kafkaProperties;

    public Mono<SendResult<String, TransactionIngestionRequest>> publish(TransactionIngestionRequest request) {
        String topic = kafkaProperties.getTransactionsCreatedTopic();
        String key = request.transactionId();
        return Mono.fromFuture(kafkaTemplate.send(topic, key, request));
    }
}
