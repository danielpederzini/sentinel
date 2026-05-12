package org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.producer.dto;

import lombok.Builder;
import org.pdzsoftware.antifraudorchestrator.domain.enums.RiskLevel;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto.ExplainabilityDetailsResponse;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto.FraudPredictionResponse;

@Builder
public record FraudPredictionMessage(
        double fraudProbability,
        RiskLevel riskLevel,
        String modelVersion,
        ExplainabilityDetailsResponse explainability
) {
    public static FraudPredictionMessage from(FraudPredictionResponse fraudPredictionResponse) {
        return FraudPredictionMessage.builder()
                .fraudProbability(fraudPredictionResponse.fraudProbability())
                .riskLevel(fraudPredictionResponse.riskLevel())
                .modelVersion(fraudPredictionResponse.modelVersion())
                .explainability(fraudPredictionResponse.explainability())
                .build();
    }
}
