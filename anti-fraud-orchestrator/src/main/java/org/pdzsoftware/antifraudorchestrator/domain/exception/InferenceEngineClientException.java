package org.pdzsoftware.antifraudorchestrator.domain.exception;

public class InferenceEngineClientException extends RuntimeException {
    public InferenceEngineClientException(String message) {
        super(message);
    }

    public InferenceEngineClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
