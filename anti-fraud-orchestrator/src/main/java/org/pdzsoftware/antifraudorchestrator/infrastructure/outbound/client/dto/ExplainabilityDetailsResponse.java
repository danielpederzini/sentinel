package org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ExplainabilityDetailsResponse(
		@JsonProperty("top_contributing_features") List<FeatureContributionResponse> topContributingFeatures
) {
	@Override
	public String toString() {
		List<FeatureContributionResponse> features = topContributingFeatures == null ? List.of() : topContributingFeatures;
		return String.format("ExplainabilityDetails{topContributingFeatures=%s}", features);
	}
}
