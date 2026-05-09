package org.pdzsoftware.featuremanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

public record TransactionPersistenceRequest(
        String transactionId,
        @NotBlank String userId,
        @NotBlank String cardId,
        @NotBlank String merchantId,
        @NotBlank String deviceId,
        @NotNull @Positive BigDecimal amount,
        @NotNull Locale.IsoCountryCode countryCode,
        String ipAddress,
        @NotNull @PastOrPresent LocalDateTime creationDateTime
) {
}
