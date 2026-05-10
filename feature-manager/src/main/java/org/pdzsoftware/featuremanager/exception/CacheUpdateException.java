package org.pdzsoftware.featuremanager.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class CacheUpdateException extends ResponseStatusException {
    public static final HttpStatus HTTP_STATUS = HttpStatus.INTERNAL_SERVER_ERROR;

    public CacheUpdateException() {
        super(HTTP_STATUS);
    }

    public CacheUpdateException(String message) {
        super(HTTP_STATUS, message);
    }

    public CacheUpdateException(String message, Throwable cause) {
        super(HTTP_STATUS, message, cause);
    }
}
