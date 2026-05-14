package org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ExplainabilityDetailsMessage(
		@JsonProperty("top_contributing_features")
		@NotNull @NotEmpty List<FeatureContributionMessage> topContributingFeatures
) {
}
