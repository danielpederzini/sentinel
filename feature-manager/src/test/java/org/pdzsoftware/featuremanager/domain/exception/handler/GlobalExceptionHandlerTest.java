package org.pdzsoftware.featuremanager.domain.exception.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pdzsoftware.featuremanager.infrastructure.inbound.controller.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.pdzsoftware.featuremanager.support.TestConstants.HTTP_STATUS_BAD_REQUEST;
import static org.pdzsoftware.featuremanager.support.TestConstants.HTTP_STATUS_FORBIDDEN;
import static org.pdzsoftware.featuremanager.support.TestConstants.HTTP_STATUS_INTERNAL_SERVER_ERROR;
import static org.pdzsoftware.featuremanager.support.TestConstants.RESPONSE_STATUS_REASON;
import static org.pdzsoftware.featuremanager.support.TestConstants.UNEXPECTED_ERROR_MESSAGE;
import static org.pdzsoftware.featuremanager.support.TestConstants.UNEXPECTED_ERROR_PREFIX;
import static org.pdzsoftware.featuremanager.support.TestConstants.VALIDATION_FAILED_PREFIX;
import static org.pdzsoftware.featuremanager.support.TestConstants.VALIDATION_FIELD_USER_ID;
import static org.pdzsoftware.featuremanager.support.TestConstants.VALIDATION_MESSAGE_NOT_BLANK;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void handleValidationException_shouldReturnBadRequestWithFieldErrors() {
        MethodArgumentNotValidException exception = validationException(
                VALIDATION_FIELD_USER_ID,
                VALIDATION_MESSAGE_NOT_BLANK,
                HttpStatus.BAD_REQUEST);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidationException(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(HTTP_STATUS_BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains(VALIDATION_FAILED_PREFIX);
        assertThat(response.getBody().message()).contains(VALIDATION_FIELD_USER_ID);
    }

    @Test
    void handleResponseStatusException_shouldReturnConfiguredStatus() {
        ResponseStatusException exception = new ResponseStatusException(HttpStatus.FORBIDDEN, RESPONSE_STATUS_REASON);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleResponseStatusException(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(HTTP_STATUS_FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo(exception.getMessage());
    }

    @Test
    void handleGenericException_shouldReturnInternalServerError() {
        RuntimeException exception = new RuntimeException(UNEXPECTED_ERROR_MESSAGE);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGenericException(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(HTTP_STATUS_INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains(UNEXPECTED_ERROR_PREFIX);
        assertThat(response.getBody().message()).contains(UNEXPECTED_ERROR_MESSAGE);
    }

    private static MethodArgumentNotValidException validationException(
            String field,
            String message,
            HttpStatus status
    ) {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("request", field, message);

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of(fieldError));
        when(exception.getStatusCode()).thenReturn(status);

        return exception;
    }
}
