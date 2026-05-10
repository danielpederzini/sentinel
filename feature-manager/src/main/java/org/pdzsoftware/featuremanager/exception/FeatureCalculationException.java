package org.pdzsoftware.featuremanager.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class FeatureCalculationException extends ResponseStatusException {
    public static final HttpStatus HTTP_STATUS = HttpStatus.INTERNAL_SERVER_ERROR;

    public FeatureCalculationException() {
        super(HTTP_STATUS);
    }

    public FeatureCalculationException(String message) {
        super(HTTP_STATUS, message);
    }

    public FeatureCalculationException(String message, Throwable cause) {
        super(HTTP_STATUS, message, cause);
    }
}
