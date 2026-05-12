package org.pdzsoftware.antifraudorchestrator.domain.exception;

public class TransactionOrchestrationException extends RuntimeException {
    public TransactionOrchestrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
