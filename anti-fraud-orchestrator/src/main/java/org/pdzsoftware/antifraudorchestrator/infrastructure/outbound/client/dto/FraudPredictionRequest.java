package org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FraudPredictionRequest(
		@JsonProperty("transaction_id") String transactionId,
		@JsonProperty("amount") double amount,
		@JsonProperty("user_average_amount") double userAverageAmount,
		@JsonProperty("user_transaction_count_5min") long userTransactionCount5Min,
		@JsonProperty("user_transaction_count_1hour") long userTransactionCount1Hour,
		@JsonProperty("seconds_since_last_transaction") long secondsSinceLastTransaction,
		@JsonProperty("merchant_risk_score") float merchantRiskScore,
		@JsonProperty("is_device_trusted") boolean isDeviceTrusted,
		@JsonProperty("has_country_mismatch") boolean hasCountryMismatch,
		@JsonProperty("amount_to_average_ratio") float amountToAverageRatio,
		@JsonProperty("hour_of_day") int hourOfDay,
		@JsonProperty("ip_risk_score") float ipRiskScore,
		@JsonProperty("card_age_days") long cardAgeDays,
		@JsonProperty("amount_velocity_1hour") double amountVelocity1Hour,
		@JsonProperty("log_amount") double logAmount,
		@JsonProperty("log_seconds_since") double logSecondsSinceLastTransaction,
		@JsonProperty("log_velocity_1hour") double logVelocity1Hour,
		@JsonProperty("amount_x_merchant_risk") double amountTimesMerchantRisk,
		@JsonProperty("amount_x_ip_risk") double amountTimesIpRisk,
		@JsonProperty("risk_score_product") double riskScoreProduct,
		@JsonProperty("ip_device_risk") double ipDeviceRisk,
		@JsonProperty("country_ip_risk") double countryIpRisk,
		@JsonProperty("velocity_amount_interaction") double velocityAmountInteraction,
		@JsonProperty("recency_velocity") double recencyVelocity,
		@JsonProperty("card_age_x_amount_ratio") double cardAgeAmountRatio,
		@JsonProperty("amount_deviation") double amountDeviation,
		@JsonProperty("is_night") boolean isNight,
		@JsonProperty("night_amount_ratio") double nightAmountRatio,
		@JsonProperty("velocity_intensity") double velocityIntensity
) {
	public static FraudPredictionRequest from(FraudFeatureResponse features) {
		return new FraudPredictionRequest(
				features.transactionId(),
				features.amount().doubleValue(),
				features.userAverageAmount().doubleValue(),
				features.userTransactionCount5Min(),
				features.userTransactionCount1Hour(),
				features.secondsSinceLastTransaction(),
				features.merchantRiskScore(),
				features.isDeviceTrusted(),
				features.hasCountryMismatch(),
				features.amountToAverageRatio(),
				features.hourOfDay(),
				features.ipRiskScore(),
				features.cardAgeDays(),
				features.amountVelocity1Hour().doubleValue(),
				features.logAmount(),
				features.logSecondsSinceLastTransaction(),
				features.logVelocity1Hour(),
				features.amountTimesMerchantRisk(),
				features.amountTimesIpRisk(),
				features.riskScoreProduct(),
				features.ipDeviceRisk(),
				features.countryIpRisk(),
				features.velocityAmountInteraction(),
				features.recencyVelocity(),
				features.cardAgeAmountRatio(),
				features.amountDeviation(),
				features.isNight(),
				features.nightAmountRatio(),
				features.velocityIntensity()
		);
	}
}
