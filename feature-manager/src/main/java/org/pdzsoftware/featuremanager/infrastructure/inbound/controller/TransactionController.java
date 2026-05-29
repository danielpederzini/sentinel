package org.pdzsoftware.featuremanager.infrastructure.inbound.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.pdzsoftware.featuremanager.application.usecase.PersistTransactionUseCase;
import org.pdzsoftware.featuremanager.infrastructure.inbound.controller.dto.ErrorResponse;
import org.pdzsoftware.featuremanager.infrastructure.inbound.controller.dto.PersistTransactionRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Persistence of scored transactions and their features")
public class TransactionController {
    private final PersistTransactionUseCase persistTransactionUseCase;

    @Operation(
            summary = "Persist a scored transaction",
            description = "Stores a transaction together with its computed features and fraud prediction.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transaction persisted"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT bearer token", content = @Content)
    })
    @PostMapping
    public ResponseEntity<Void> persistTransaction(@RequestBody @Valid PersistTransactionRequest request) {
        persistTransactionUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
