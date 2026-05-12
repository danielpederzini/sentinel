package org.pdzsoftware.transactioningestor.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class KafkaPublishException extends ResponseStatusException {
    public static final HttpStatus HTTP_STATUS = HttpStatus.BAD_GATEWAY;

    public KafkaPublishException() {
        super(HTTP_STATUS);
    }

    public KafkaPublishException(String message) {
        super(HTTP_STATUS, message);
    }

    public KafkaPublishException(String message, Throwable cause) {
        super(HTTP_STATUS, message, cause);
    }
}
