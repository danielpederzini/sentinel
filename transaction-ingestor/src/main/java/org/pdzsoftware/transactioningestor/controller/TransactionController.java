package org.pdzsoftware.transactioningestor.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.pdzsoftware.transactioningestor.dto.TransactionIngestionRequest;
import org.pdzsoftware.transactioningestor.dto.TransactionIngestionResponse;
import org.pdzsoftware.transactioningestor.service.TransactionIngestionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionIngestionService transactionIngestionService;

    @PostMapping
    public Mono<ResponseEntity<TransactionIngestionResponse>> ingestTransaction(@RequestBody @Valid TransactionIngestionRequest request) {
        return transactionIngestionService.ingest(request)
                .map(response -> ResponseEntity.status(HttpStatus.ACCEPTED).body(response));
    }
}
