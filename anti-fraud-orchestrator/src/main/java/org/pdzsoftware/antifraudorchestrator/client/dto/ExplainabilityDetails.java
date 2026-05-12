package org.pdzsoftware.antifraudorchestrator.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ExplainabilityDetails(
		@JsonProperty("top_contributing_features") List<FeatureContribution> topContributingFeatures
) {
	@Override
	public String toString() {
		List<FeatureContribution> features = topContributingFeatures == null ? List.of() : topContributingFeatures;
		return String.format("ExplainabilityDetails{topContributingFeatures=%s}", features);
	}
}
