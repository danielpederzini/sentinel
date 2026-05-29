package org.pdzsoftware.transactioningestor.infrastructure.inbound.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import org.pdzsoftware.transactioningestor.domain.enums.CountryCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "A transaction submitted for fraud analysis")
public record TransactionIngestionRequest(
        @Schema(description = "Client-supplied unique transaction identifier; generated if omitted", example = "txn-1029384756")
        String transactionId,
        @Schema(description = "Identifier of the user who initiated the transaction", example = "user-42")
        @NotBlank String userId,
        @Schema(description = "Identifier of the card used", example = "card-9981")
        @NotBlank String cardId,
        @Schema(description = "Identifier of the merchant", example = "merchant-512")
        @NotBlank String merchantId,
        @Schema(description = "Identifier of the device used, when available", example = "device-7f3a")
        String deviceId,
        @Schema(description = "Transaction amount; must be positive", example = "149.90")
        @NotNull @Positive BigDecimal amount,
        @Schema(description = "ISO country code where the transaction occurred", example = "US")
        @NotNull CountryCode countryCode,
        @Schema(description = "Originating IP address, when available", example = "203.0.113.7")
        String ipAddress,
        @Schema(description = "Timestamp when the transaction was created; must not be in the future", example = "2026-05-29T14:30:00")
        @NotNull @PastOrPresent LocalDateTime creationDateTime
) {
}
