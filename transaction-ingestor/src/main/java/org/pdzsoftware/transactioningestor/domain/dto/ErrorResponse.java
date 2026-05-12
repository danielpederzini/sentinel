package org.pdzsoftware.transactioningestor.domain.dto;

import lombok.Builder;
import org.springframework.http.HttpStatusCode;

@Builder
public record ErrorResponse(
        HttpStatusCode status,
        String message
) {
}
