package org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FeatureContributionResponse(
        @JsonProperty("feature_name") String featureName,
        @JsonProperty("feature_value") Object featureValue,
        @JsonProperty("contribution") double contribution,
        @JsonProperty("direction") String direction
) {
}
