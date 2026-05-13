package org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ExplainabilityDetailsResponse(
        @JsonProperty("top_contributing_features") List<FeatureContributionResponse> topContributingFeatures
) {
}
