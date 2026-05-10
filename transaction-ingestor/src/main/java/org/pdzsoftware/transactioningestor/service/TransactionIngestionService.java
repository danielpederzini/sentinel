package org.pdzsoftware.transactioningestor.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.pdzsoftware.transactioningestor.dto.TransactionIngestionRequest;
import org.pdzsoftware.transactioningestor.dto.TransactionIngestionResponse;
import org.pdzsoftware.transactioningestor.exception.KafkaPublishException;
import org.pdzsoftware.transactioningestor.producer.TransactionProducer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionIngestionService {
    private final TransactionProducer transactionProducer;

    public Mono<TransactionIngestionResponse> ingest(TransactionIngestionRequest request) {
        boolean isTransactionIdMissing = StringUtils.isBlank(request.transactionId());

        String transactionId = isTransactionIdMissing ? UUID.randomUUID().toString() : request.transactionId();

        TransactionIngestionRequest transactionToPublish = new TransactionIngestionRequest(
                transactionId,
                request.userId(),
                request.cardId(),
                request.merchantId(),
                request.deviceId(),
                request.amount(),
                request.countryCode(),
                request.ipAddress(),
                request.creationDateTime()
        );

        return transactionProducer.publish(transactionToPublish)
            .onErrorMap(exception -> new KafkaPublishException("Failed to publish transaction to Kafka", exception))
                .thenReturn(new TransactionIngestionResponse(transactionId));
    }
}
