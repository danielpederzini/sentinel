package org.pdzsoftware.featuremanager.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class UserNotFoundException extends ResponseStatusException {
    public static final HttpStatus HTTP_STATUS = HttpStatus.NOT_FOUND;

    public UserNotFoundException() {
        super(HTTP_STATUS);
    }

    public UserNotFoundException(String message) {
        super(HTTP_STATUS, message);
    }

    public UserNotFoundException(String message, Throwable cause) {
        super(HTTP_STATUS, message, cause);
    }
}
