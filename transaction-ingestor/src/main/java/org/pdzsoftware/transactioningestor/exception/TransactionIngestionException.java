package org.pdzsoftware.transactioningestor.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class TransactionIngestionException extends ResponseStatusException {
    public static final HttpStatus HTTP_STATUS = HttpStatus.INTERNAL_SERVER_ERROR;

    public TransactionIngestionException() {
        super(HTTP_STATUS);
    }

    public TransactionIngestionException(String message) {
        super(HTTP_STATUS, message);
    }

    public TransactionIngestionException(String message, Throwable cause) {
        super(HTTP_STATUS, message, cause);
    }
}
