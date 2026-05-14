package org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FeatureContributionMessage(
		@JsonProperty("feature_name") @NotBlank String featureName,
		@JsonProperty("feature_value") @NotNull Object featureValue,
		@JsonProperty("contribution") double contribution,
		@JsonProperty("direction") @NotBlank String direction
) {
}
