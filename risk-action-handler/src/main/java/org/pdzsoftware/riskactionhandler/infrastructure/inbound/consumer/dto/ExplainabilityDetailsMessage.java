package org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ExplainabilityDetailsMessage(
		@NotNull @NotEmpty List<FeatureContributionMessage> topContributingFeatures
) {
}
