package org.pdzsoftware.featuremanager.infrastructure.inbound.controller.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.pdzsoftware.featuremanager.domain.enums.RiskLevel;

public record PredictionRequest(
        @DecimalMin("0.0") @DecimalMax("1.0") double fraudProbability,
        @NotNull RiskLevel riskLevel,
        @NotBlank String modelVersion
) {
}
