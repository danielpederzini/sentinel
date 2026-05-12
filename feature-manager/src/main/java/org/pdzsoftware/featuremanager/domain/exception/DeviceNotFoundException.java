package org.pdzsoftware.featuremanager.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class DeviceNotFoundException extends ResponseStatusException {
    public static final HttpStatus HTTP_STATUS = HttpStatus.NOT_FOUND;

    public DeviceNotFoundException() {
        super(HTTP_STATUS);
    }

    public DeviceNotFoundException(String message) {
        super(HTTP_STATUS, message);
    }

    public DeviceNotFoundException(String message, Throwable cause) {
        super(HTTP_STATUS, message, cause);
    }
}
