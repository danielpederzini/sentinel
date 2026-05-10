package org.pdzsoftware.antifraudorchestrator.exception;

public class TransactionOrchestrationException extends RuntimeException {
    public TransactionOrchestrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
