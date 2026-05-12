package org.pdzsoftware.featuremanager.infrastructure.inbound.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import org.pdzsoftware.featuremanager.domain.enums.CountryCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FraudFeatureRequest(
        @NotBlank String transactionId,
        @NotBlank String userId,
        @NotBlank String cardId,
        @NotBlank String merchantId,
        String deviceId,
        @NotNull @Positive BigDecimal amount,
        @NotNull CountryCode countryCode,
        String ipAddress,
        @NotNull @PastOrPresent LocalDateTime creationDateTime
) {
}
