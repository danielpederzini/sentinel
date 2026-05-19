package org.pdzsoftware.featuremanager.infrastructure.inbound.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.pdzsoftware.featuremanager.application.usecase.PersistTransactionUseCase;
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
public class TransactionController {
    private final PersistTransactionUseCase persistTransactionUseCase;

    @PostMapping
    public ResponseEntity<Void> persistTransaction(@RequestBody @Valid PersistTransactionRequest request) {
        persistTransactionUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
