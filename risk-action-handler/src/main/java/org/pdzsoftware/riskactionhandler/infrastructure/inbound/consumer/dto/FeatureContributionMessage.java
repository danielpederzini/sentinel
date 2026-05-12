package org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FeatureContributionMessage(
		@NotBlank String featureName,
		@NotNull Object featureValue,
		double contribution,
		@NotBlank String direction
) {
}
