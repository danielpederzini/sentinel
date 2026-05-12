package org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.pdzsoftware.riskactionhandler.domain.enums.RiskLevel;

public record FraudPredictionMessage(
        double fraudProbability,
        @NotNull RiskLevel riskLevel,
        @NotBlank String modelVersion,
        @NotNull ExplainabilityDetailsMessage explainability
) {
}
