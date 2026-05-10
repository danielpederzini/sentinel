package org.pdzsoftware.transactioningestor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.pdzsoftware.transactioningestor.dto.TransactionIngestionRequest;
import org.pdzsoftware.transactioningestor.dto.TransactionIngestionResponse;
import org.pdzsoftware.transactioningestor.exception.KafkaPublishException;
import org.pdzsoftware.transactioningestor.producer.TransactionProducer;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@Slf4j
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
                .doOnSuccess(sendResult -> logSuccess(sendResult, transactionId))
                .onErrorMap(exception -> new KafkaPublishException("Failed to publish transaction to Kafka", exception))
                .thenReturn(new TransactionIngestionResponse(transactionId));
    }

    private static void logSuccess(SendResult<String, TransactionIngestionRequest> sendResult, String transactionId) {
        if (Objects.isNull(sendResult)) {
            log.warn("Transaction {} published to Kafka but send result is null", transactionId);
            return;
        }

        RecordMetadata metadata = sendResult.getRecordMetadata();
        log.info("Transaction {} published to Kafka | topic: {} | partition: {} | offset: {}",
                transactionId, metadata.topic(), metadata.partition(), metadata.offset());
    }
}
