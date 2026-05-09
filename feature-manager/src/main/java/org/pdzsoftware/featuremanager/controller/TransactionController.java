package org.pdzsoftware.featuremanager.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.pdzsoftware.featuremanager.dto.TransactionPersistenceRequest;
import org.pdzsoftware.featuremanager.dto.TransactionPersistenceResult;
import org.pdzsoftware.featuremanager.usecase.PersistTransactionUseCase;
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
    public ResponseEntity<TransactionPersistenceResult> persistTransaction(@RequestBody @Valid TransactionPersistenceRequest request) {
        TransactionPersistenceResult result = persistTransactionUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
