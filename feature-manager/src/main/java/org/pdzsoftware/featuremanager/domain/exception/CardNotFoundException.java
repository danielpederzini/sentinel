package org.pdzsoftware.featuremanager.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class CardNotFoundException extends ResponseStatusException {
    public static final HttpStatus HTTP_STATUS = HttpStatus.NOT_FOUND;

    public CardNotFoundException() {
        super(HTTP_STATUS);
    }

    public CardNotFoundException(String message) {
        super(HTTP_STATUS, message);
    }

    public CardNotFoundException(String message, Throwable cause) {
        super(HTTP_STATUS, message, cause);
    }
}
