package org.pdzsoftware.antifraudorchestrator.usecase;

import org.pdzsoftware.antifraudorchestrator.dto.TransactionCreatedMessage;

public record ProcessTransactionInput(
        TransactionCreatedMessage payload,
        String messageKey
) {
}
