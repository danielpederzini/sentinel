package org.pdzsoftware.featuremanager.infrastructure.inbound.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.pdzsoftware.featuremanager.domain.enums.RiskLevel;

public record PredictionRequest(
        double fraudProbability,
        @NotNull RiskLevel riskLevel,
        @NotBlank String modelVersion
) {
}
