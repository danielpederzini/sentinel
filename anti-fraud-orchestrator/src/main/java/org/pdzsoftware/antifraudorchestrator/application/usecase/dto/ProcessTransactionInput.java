package org.pdzsoftware.antifraudorchestrator.application.usecase.dto;

import org.pdzsoftware.antifraudorchestrator.infrastructure.inbound.consumer.dto.TransactionCreatedMessage;

public record ProcessTransactionInput(TransactionCreatedMessage payload, String messageKey) {
}
