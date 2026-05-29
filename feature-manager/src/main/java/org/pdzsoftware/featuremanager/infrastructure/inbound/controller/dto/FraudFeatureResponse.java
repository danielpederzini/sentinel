package org.pdzsoftware.featuremanager.infrastructure.inbound.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@Schema(description = "The full set of fraud features computed for a transaction")
public record FraudFeatureResponse(
        @Schema(description = "Unique transaction identifier", example = "txn-1029384756")
        String transactionId,
        @Schema(description = "Transaction amount", example = "149.90")
        BigDecimal amount,
        @Schema(description = "User's historical average transaction amount", example = "82.50")
        BigDecimal userAverageAmount,
        @Schema(description = "Total number of historical transactions for the user", example = "1284")
        long userHistoricalTransactionCount,
        @Schema(description = "Number of transactions by the user in the last 5 minutes", example = "2")
        long userTransactionCount5Min,
        @Schema(description = "Number of transactions by the user in the last hour", example = "7")
        long userTransactionCount1Hour,
        @Schema(description = "Seconds elapsed since the user's previous transaction", example = "340")
        long secondsSinceLastTransaction,
        @Schema(description = "Risk score associated with the merchant (0-1)", example = "0.23")
        float merchantRiskScore,
        @Schema(description = "Whether the device is known and trusted for the user", example = "true")
        boolean isDeviceTrusted,
        @Schema(description = "Whether the transaction country differs from the user's usual country", example = "false")
        boolean hasCountryMismatch,
        @Schema(description = "Ratio of this amount to the user's average amount", example = "1.82")
        float amountToAverageRatio,
        @Schema(description = "Hour of day the transaction occurred (0-23)", example = "14")
        int hourOfDay,
        @Schema(description = "Risk score associated with the originating IP (0-1)", example = "0.10")
        float ipRiskScore,
        @Schema(description = "Age of the card in days", example = "540")
        long cardAgeDays,
        @Schema(description = "Total transaction amount by the user in the last hour", example = "430.00")
        BigDecimal amountVelocity1Hour,
        @Schema(description = "Age of the user's account in days", example = "1200")
        long userAccountAgeDays,
        @Schema(description = "Day of week the transaction occurred (1=Monday)", example = "4")
        int dayOfWeek,
        @Schema(description = "Encoded merchant category", example = "12")
        int merchantCategory,
        @Schema(description = "Encoded card type", example = "1")
        int cardType,
        @Schema(description = "Distinct merchants transacted with in the last hour", example = "3")
        long distinctMerchantCount1Hour,
        @Schema(description = "Natural log of the transaction amount", example = "5.01")
        double logAmount,
        @Schema(description = "Natural log of seconds since last transaction", example = "5.83")
        double logSecondsSinceLastTransaction,
        @Schema(description = "Natural log of the 1-hour amount velocity", example = "6.07")
        double logVelocity1Hour,
        @Schema(description = "Amount multiplied by merchant risk score", example = "34.47")
        double amountTimesMerchantRisk,
        @Schema(description = "Product of merchant and IP risk scores", example = "0.023")
        double riskScoreProduct,
        @Schema(description = "Combined IP and device risk signal", example = "0.10")
        double ipDeviceRisk,
        @Schema(description = "Combined country-mismatch and IP risk signal", example = "0.0")
        double countryIpRisk,
        @Schema(description = "Interaction between velocity and amount", example = "642.30")
        double velocityAmountInteraction,
        @Schema(description = "Interaction between recency and velocity", example = "1.26")
        double recencyVelocity,
        @Schema(description = "Deviation of amount from the user's typical spend", example = "67.40")
        double amountDeviation,
        @Schema(description = "Whether the transaction occurred at night", example = "false")
        boolean isNight,
        @Schema(description = "Intensity measure of recent transaction velocity", example = "0.85")
        double velocityIntensity
) {
}
