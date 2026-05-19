package org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto;

import lombok.Builder;

@Builder
public record PersistTransactionPredictionRequest(
        double fraudProbability,
        String riskLevel,
        String modelVersion
) {
    public static PersistTransactionPredictionRequest from(FraudPredictionResponse response) {
        return PersistTransactionPredictionRequest.builder()
                .fraudProbability(response.fraudProbability())
                .riskLevel(response.riskLevel().name())
                .modelVersion(response.modelVersion())
                .build();
    }
}
