package org.pdzsoftware.featuremanager.infrastructure.inbound.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import org.pdzsoftware.featuremanager.domain.enums.CountryCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Transaction details used as input for fraud feature calculation")
public record FraudFeatureRequest(
        @Schema(description = "Unique transaction identifier", example = "txn-1029384756")
        @NotBlank String transactionId,
        @Schema(description = "Identifier of the user", example = "user-42")
        @NotBlank String userId,
        @Schema(description = "Identifier of the card", example = "card-9981")
        @NotBlank String cardId,
        @Schema(description = "Identifier of the merchant", example = "merchant-512")
        @NotBlank String merchantId,
        @Schema(description = "Identifier of the device, when available", example = "device-7f3a")
        String deviceId,
        @Schema(description = "Transaction amount; must be positive", example = "149.90")
        @NotNull @Positive BigDecimal amount,
        @Schema(description = "ISO country code where the transaction occurred", example = "US")
        @NotNull CountryCode countryCode,
        @Schema(description = "Originating IP address, when available", example = "203.0.113.7")
        String ipAddress,
        @Schema(description = "Transaction creation timestamp; must not be in the future", example = "2026-05-29T14:30:00")
        @NotNull @PastOrPresent LocalDateTime creationDateTime
) {
}
