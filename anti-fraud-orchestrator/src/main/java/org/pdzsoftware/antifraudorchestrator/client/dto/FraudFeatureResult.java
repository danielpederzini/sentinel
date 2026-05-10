package org.pdzsoftware.antifraudorchestrator.client.dto;

import java.math.BigDecimal;

public record FraudFeatureResult(
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
		long cardAgeDays
) {
}
