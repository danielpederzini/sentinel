package org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.producer.dto;

import lombok.Builder;
import org.pdzsoftware.antifraudorchestrator.domain.enums.CountryCode;
import org.pdzsoftware.antifraudorchestrator.infrastructure.inbound.consumer.dto.TransactionCreatedMessage;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto.FraudFeatureResponse;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto.FraudPredictionResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record FraudFeaturesMessage(
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
    public static FraudFeaturesMessage from(FraudFeatureResponse fraudFeatureResponse) {
        return FraudFeaturesMessage.builder()
                .userAverageAmount(fraudFeatureResponse.userAverageAmount())
                .userTransactionCount5Min(fraudFeatureResponse.userTransactionCount5Min())
                .userTransactionCount1Hour(fraudFeatureResponse.userTransactionCount1Hour())
                .secondsSinceLastTransaction(fraudFeatureResponse.secondsSinceLastTransaction())
                .merchantRiskScore(fraudFeatureResponse.merchantRiskScore())
                .isDeviceTrusted(fraudFeatureResponse.isDeviceTrusted())
                .hasCountryMismatch(fraudFeatureResponse.hasCountryMismatch())
                .amountToAverageRatio(fraudFeatureResponse.amountToAverageRatio())
                .hourOfDay(fraudFeatureResponse.hourOfDay())
                .ipRiskScore(fraudFeatureResponse.ipRiskScore())
                .cardAgeDays(fraudFeatureResponse.cardAgeDays())
                .amountVelocity1Hour(fraudFeatureResponse.amountVelocity1Hour())
                .logAmount(fraudFeatureResponse.logAmount())
                .logSecondsSinceLastTransaction(fraudFeatureResponse.logSecondsSinceLastTransaction())
                .logVelocity1Hour(fraudFeatureResponse.logVelocity1Hour())
                .amountTimesMerchantRisk(fraudFeatureResponse.amountTimesMerchantRisk())
                .amountTimesIpRisk(fraudFeatureResponse.amountTimesIpRisk())
                .riskScoreProduct(fraudFeatureResponse.riskScoreProduct())
                .ipDeviceRisk(fraudFeatureResponse.ipDeviceRisk())
                .countryIpRisk(fraudFeatureResponse.countryIpRisk())
                .velocityAmountInteraction(fraudFeatureResponse.velocityAmountInteraction())
                .recencyVelocity(fraudFeatureResponse.recencyVelocity())
                .cardAgeAmountRatio(fraudFeatureResponse.cardAgeAmountRatio())
                .amountDeviation(fraudFeatureResponse.amountDeviation())
                .isNight(fraudFeatureResponse.isNight())
                .nightAmountRatio(fraudFeatureResponse.nightAmountRatio())
                .velocityIntensity(fraudFeatureResponse.velocityIntensity())
                .build();
    }
}
