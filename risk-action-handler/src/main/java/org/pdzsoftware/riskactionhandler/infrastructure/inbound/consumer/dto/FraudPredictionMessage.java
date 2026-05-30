package org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.pdzsoftware.riskactionhandler.domain.enums.RiskLevel;

import java.util.List;
import java.util.Optional;

public record FraudPredictionMessage(
        double fraudProbability,
        @NotNull RiskLevel riskLevel,
        @NotBlank String modelVersion,
        @NotNull ExplainabilityDetailsMessage explainability
) {
    public List<FeatureContributionMessage> topContributingFeatures() {
        return Optional.ofNullable(explainability)
                .map(ExplainabilityDetailsMessage::topContributingFeatures)
                .orElse(List.of());
    }
}
