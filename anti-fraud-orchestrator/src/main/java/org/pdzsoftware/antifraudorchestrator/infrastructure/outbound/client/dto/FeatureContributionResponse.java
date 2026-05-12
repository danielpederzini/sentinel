package org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FeatureContributionResponse(
		@JsonProperty("feature_name") String featureName,
		@JsonProperty("feature_value") Object featureValue,
		@JsonProperty("contribution") double contribution,
		@JsonProperty("direction") String direction
) {
	@Override
	public String toString() {
		return String.format(
				"FeatureContribution{featureName='%s', featureValue=%s, contribution=%.6f, direction='%s'}",
				featureName,
				featureValue,
				contribution,
				direction
		);
	}
}
