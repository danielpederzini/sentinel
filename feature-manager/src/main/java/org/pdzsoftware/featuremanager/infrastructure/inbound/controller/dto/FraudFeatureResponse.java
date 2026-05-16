package org.pdzsoftware.featuremanager.infrastructure.inbound.controller.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record FraudFeatureResponse(
        String transactionId,
        BigDecimal amount,
        BigDecimal userAverageAmount,
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
        double logAmount,
        double logSecondsSinceLastTransaction,
        double logVelocity1Hour,
        double amountTimesMerchantRisk,
        double amountTimesIpRisk,
        double riskScoreProduct,
        double ipDeviceRisk,
        double countryIpRisk,
        double velocityAmountInteraction,
        double recencyVelocity,
        double cardAgeAmountRatio,
        double amountDeviation,
        boolean isNight,
        double nightAmountRatio,
        double velocityIntensity
) {
}
