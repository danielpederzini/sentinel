package org.pdzsoftware.transactioningestor.domain.exception.handler;

import lombok.extern.slf4j.Slf4j;
import org.pdzsoftware.transactioningestor.domain.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidationException(WebExchangeBindException exception) {
        StringBuilder messageBuilder = new StringBuilder("Validation failed: ");
        exception.getBindingResult().getFieldErrors().forEach(error ->
                messageBuilder.append(String.format("[%s: %s] ", error.getField(), error.getDefaultMessage()))
        );
        String message = messageBuilder.toString().trim();

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(exception.getStatusCode())
                .message(message)
                .build();

        log.warn("Input validation error: {}", message, exception);
        return Mono.just(ResponseEntity.status(exception.getStatusCode()).body(errorResponse));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleResponseStatusException(ResponseStatusException exception) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(exception.getStatusCode())
                .message(exception.getReason() != null ? exception.getReason() : exception.getMessage())
                .build();

        log.warn("Error occurred with status {}: {}", exception.getStatusCode(), exception.getMessage(), exception);
        return Mono.just(ResponseEntity.status(exception.getStatusCode()).body(errorResponse));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(Exception exception) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .message(String.format("An unexpected error occurred: %s", exception.getMessage()))
                .build();

        log.warn("Unexpected error occurred: {}", exception.getMessage(), exception);
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
    }
}
