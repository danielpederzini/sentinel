package org.pdzsoftware.featuremanager.exception;

import lombok.Builder;
import org.springframework.http.HttpStatusCode;

@Builder
public record ErrorResponse(
        HttpStatusCode status,
        String message
) {
}
