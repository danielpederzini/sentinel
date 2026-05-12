package org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import org.pdzsoftware.riskactionhandler.domain.enums.CountryCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionScoredMessage(
        @NotBlank String transactionId,
        @NotBlank String userId,
        @NotBlank String cardId,
        @NotBlank String merchantId,
        String deviceId,
        @NotNull @Positive BigDecimal amount,
        @NotNull CountryCode countryCode,
        String ipAddress,
        @NotNull @PastOrPresent LocalDateTime creationDateTime,
        @NotNull FraudFeaturesMessage featuresMessage,
        @NotNull FraudPredictionMessage predictionMessage
) {
}
