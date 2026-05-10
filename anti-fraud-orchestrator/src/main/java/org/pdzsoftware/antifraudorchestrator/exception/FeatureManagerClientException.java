package org.pdzsoftware.antifraudorchestrator.exception;

public class FeatureManagerClientException extends RuntimeException {
    public FeatureManagerClientException(String message) {
        super(message);
    }

    public FeatureManagerClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
