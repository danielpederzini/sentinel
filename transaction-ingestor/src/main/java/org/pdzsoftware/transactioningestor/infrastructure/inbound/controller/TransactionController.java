package org.pdzsoftware.transactioningestor.infrastructure.inbound.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pdzsoftware.transactioningestor.domain.dto.ErrorResponse;
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
@Tag(name = "Transactions", description = "Submit transactions for fraud analysis")
public class TransactionController {
    private final TransactionIngestionService transactionIngestionService;

    @Operation(
            summary = "Ingest a transaction",
            description = "Validates and accepts a transaction, then publishes it to Kafka for asynchronous fraud scoring.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Transaction accepted for processing"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public Mono<ResponseEntity<TransactionIngestionResponse>> ingestTransaction(@RequestBody @Valid TransactionIngestionRequest request) {
        log.info("Received transaction ingestion request | transactionId: {} | userId: {} | amount: {}",
                request.transactionId(), request.userId(), request.amount());
        return transactionIngestionService.ingest(request)
                .map(response -> ResponseEntity.status(HttpStatus.ACCEPTED).body(response));
    }
}
