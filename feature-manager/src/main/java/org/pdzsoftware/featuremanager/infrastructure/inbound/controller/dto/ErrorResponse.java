package org.pdzsoftware.featuremanager.infrastructure.inbound.controller.dto;

import lombok.Builder;
import org.springframework.http.HttpStatusCode;

@Builder
public record ErrorResponse(
        HttpStatusCode status,
        String message
) {
}
