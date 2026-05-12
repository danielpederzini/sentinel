package org.pdzsoftware.featuremanager.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class MerchantNotFoundException extends ResponseStatusException {
    public static final HttpStatus HTTP_STATUS = HttpStatus.NOT_FOUND;

    public MerchantNotFoundException() {
        super(HTTP_STATUS);
    }

    public MerchantNotFoundException(String message) {
        super(HTTP_STATUS, message);
    }

    public MerchantNotFoundException(String message, Throwable cause) {
        super(HTTP_STATUS, message, cause);
    }
}
