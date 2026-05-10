package org.pdzsoftware.featuremanager.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class CacheReadException extends ResponseStatusException {
    public static final HttpStatus HTTP_STATUS = HttpStatus.INTERNAL_SERVER_ERROR;

    public CacheReadException() {
        super(HTTP_STATUS);
    }

    public CacheReadException(String message) {
        super(HTTP_STATUS, message);
    }

    public CacheReadException(String message, Throwable cause) {
        super(HTTP_STATUS, message, cause);
    }
}
