package org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PersistTransactionFeaturesRequest(
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
    public static PersistTransactionFeaturesRequest from(FraudFeatureResponse response) {
        return PersistTransactionFeaturesRequest.builder()
                .userAverageAmount(response.userAverageAmount())
                .userTransactionCount5Min(response.userTransactionCount5Min())
                .userTransactionCount1Hour(response.userTransactionCount1Hour())
                .secondsSinceLastTransaction(response.secondsSinceLastTransaction())
                .merchantRiskScore(response.merchantRiskScore())
                .isDeviceTrusted(response.isDeviceTrusted())
                .hasCountryMismatch(response.hasCountryMismatch())
                .amountToAverageRatio(response.amountToAverageRatio())
                .hourOfDay(response.hourOfDay())
                .ipRiskScore(response.ipRiskScore())
                .cardAgeDays(response.cardAgeDays())
                .amountVelocity1Hour(response.amountVelocity1Hour())
                .userAccountAgeDays(response.userAccountAgeDays())
                .dayOfWeek(response.dayOfWeek())
                .merchantCategory(response.merchantCategory())
                .cardType(response.cardType())
                .distinctMerchantCount1Hour(response.distinctMerchantCount1Hour())
                .logAmount(response.logAmount())
                .logSecondsSinceLastTransaction(response.logSecondsSinceLastTransaction())
                .logVelocity1Hour(response.logVelocity1Hour())
                .amountTimesMerchantRisk(response.amountTimesMerchantRisk())
                .riskScoreProduct(response.riskScoreProduct())
                .ipDeviceRisk(response.ipDeviceRisk())
                .countryIpRisk(response.countryIpRisk())
                .velocityAmountInteraction(response.velocityAmountInteraction())
                .recencyVelocity(response.recencyVelocity())
                .amountDeviation(response.amountDeviation())
                .isNight(response.isNight())
                .velocityIntensity(response.velocityIntensity())
                .build();
    }
}
