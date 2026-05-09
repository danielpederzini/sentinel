package org.pdzsoftware.featuremanager.application.dto;

import java.math.BigDecimal;

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
        int cardAgeDays
) {
}
