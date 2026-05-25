package org.pdzsoftware.transactioningestor.infrastructure.inbound.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pdzsoftware.transactioningestor.infrastructure.inbound.controller.dto.TransactionIngestionRequest;
import org.pdzsoftware.transactioningestor.infrastructure.inbound.controller.dto.TransactionIngestionResponse;
import org.pdzsoftware.transactioningestor.application.service.TransactionIngestionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionIngestionService transactionIngestionService;

    @PostMapping
    public Mono<ResponseEntity<TransactionIngestionResponse>> ingestTransaction(@RequestBody @Valid TransactionIngestionRequest request) {
        log.info("Received transaction ingestion request | transactionId: {} | userId: {} | amount: {}",
                request.transactionId(), request.userId(), request.amount());
        return transactionIngestionService.ingest(request)
                .map(response -> ResponseEntity.status(HttpStatus.ACCEPTED).body(response));
    }
}
