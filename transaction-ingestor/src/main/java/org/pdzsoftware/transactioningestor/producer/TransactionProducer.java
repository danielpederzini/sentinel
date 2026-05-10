package org.pdzsoftware.transactioningestor.producer;

import lombok.RequiredArgsConstructor;
import org.pdzsoftware.transactioningestor.config.KafkaProperties;
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
        return Mono.fromFuture(
                kafkaTemplate.send(kafkaProperties.getTransactionsCreatedTopic(), request.transactionId(), request)
        );
    }
}
