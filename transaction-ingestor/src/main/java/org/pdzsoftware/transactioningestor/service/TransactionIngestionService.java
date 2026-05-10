package org.pdzsoftware.transactioningestor.service;

import lombok.RequiredArgsConstructor;
import org.pdzsoftware.transactioningestor.dto.TransactionIngestionRequest;
import org.pdzsoftware.transactioningestor.dto.TransactionIngestionResponse;
import org.pdzsoftware.transactioningestor.producer.TransactionProducer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionIngestionService {
    private final TransactionProducer transactionProducer;

    public Mono<TransactionIngestionResponse> ingest(TransactionIngestionRequest request) {
        String transactionId = StringUtils.hasText(request.transactionId())
                ? request.transactionId()
                : UUID.randomUUID().toString();

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
                .thenReturn(new TransactionIngestionResponse(transactionId));
    }
}
