package org.pdzsoftware.featuremanager.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record FraudFeatureResult(
        String transactionId,
        BigDecimal amount,
        BigDecimal userAverageAmount,
        int userTransactionCount5Min,
        int userTransactionCound1Hour,
        long secondsSinceLastTransaction,
        float merchantRiskScore,
        boolean isDeviceTrusted,
        boolean hasCountryMismatch,
        float amountToAverageRatio,
        int hourOfDay,
        float ipRiskScore,
        long cardAgeDays
) {
}
