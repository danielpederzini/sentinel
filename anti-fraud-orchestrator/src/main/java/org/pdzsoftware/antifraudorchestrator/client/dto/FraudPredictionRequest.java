package org.pdzsoftware.antifraudorchestrator.client.dto;

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
		@JsonProperty("card_age_days") long cardAgeDays
) {
	public static FraudPredictionRequest from(FraudFeatureResult features) {
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
				features.cardAgeDays()
		);
	}
}
