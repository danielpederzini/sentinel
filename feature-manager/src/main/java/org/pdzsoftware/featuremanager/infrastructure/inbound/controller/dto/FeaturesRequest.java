package org.pdzsoftware.featuremanager.infrastructure.inbound.controller.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record FeaturesRequest(
        @NotNull BigDecimal userAverageAmount,
        long userHistoricalTransactionCount,
        long userTransactionCount5Min,
        long userTransactionCount1Hour,
        long secondsSinceLastTransaction,
        float merchantRiskScore,
        boolean isDeviceTrusted,
        boolean hasCountryMismatch,
        float amountToAverageRatio,
        int hourOfDay,
        float ipRiskScore,
        long cardAgeDays,
        BigDecimal amountVelocity1Hour,
        long userAccountAgeDays,
        int dayOfWeek,
        int merchantCategory,
        int cardType,
        long distinctMerchantCount1Hour,
        double logAmount,
        double logSecondsSinceLastTransaction,
        double logVelocity1Hour,
        double amountTimesMerchantRisk,
        double riskScoreProduct,
        double ipDeviceRisk,
        double countryIpRisk,
        double velocityAmountInteraction,
        double recencyVelocity,
        double amountDeviation,
        boolean isNight,
        double velocityIntensity
) {
}
