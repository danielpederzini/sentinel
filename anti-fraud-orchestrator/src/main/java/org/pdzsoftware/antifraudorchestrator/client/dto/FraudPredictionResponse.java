package org.pdzsoftware.antifraudorchestrator.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.pdzsoftware.antifraudorchestrator.enums.RiskLevel;

public record FraudPredictionResponse(
		@JsonProperty("transaction_id") String transactionId,
		@JsonProperty("fraud_probability") double fraudProbability,
		@JsonProperty("risk_level") RiskLevel riskLevel,
		@JsonProperty("model_version") String modelVersion,
		@JsonProperty("explainability") ExplainabilityDetails explainability
) {
}
