package org.pdzsoftware.riskactionhandler.domain.exception;

public class EmailClientException extends RuntimeException {
    public EmailClientException(String message) {
        super(message);
    }

    public EmailClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
