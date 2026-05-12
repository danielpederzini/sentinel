package org.pdzsoftware.antifraudorchestrator.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pdzsoftware.antifraudorchestrator.client.FeatureManagerClient;
import org.pdzsoftware.antifraudorchestrator.client.InferenceEngineClient;
import org.pdzsoftware.antifraudorchestrator.client.dto.FraudFeatureResult;
import org.pdzsoftware.antifraudorchestrator.client.dto.FraudPredictionResponse;
import org.pdzsoftware.antifraudorchestrator.dto.TransactionCreatedMessage;
import org.pdzsoftware.antifraudorchestrator.exception.TransactionOrchestrationException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessTransactionUseCase implements VoidUseCase<ProcessTransactionInput> {
	private final FeatureManagerClient featureManagerClient;
	private final InferenceEngineClient inferenceEngineClient;

	@Override
	public void execute(ProcessTransactionInput input) {
		TransactionCreatedMessage payload = input.payload();
		String messageKey = input.messageKey();
		try {
			FraudFeatureResult features = featureManagerClient.calculateFraudFeatures(payload);
			FraudPredictionResponse prediction = inferenceEngineClient.scoreTransaction(features);

			log.info("Processed transaction {} | key: {} | riskLevel: {} | fraudProbability: {} | modelVersion: {} | explainability: {}",
					payload.transactionId(),
					messageKey,
					prediction.riskLevel(),
					prediction.fraudProbability(),
					prediction.modelVersion(),
					prediction.explainability()
			);
		} catch (RuntimeException exception) {
			throw new TransactionOrchestrationException(String.format(
					"Failed to orchestrate transaction %s with key %s", payload.transactionId(), messageKey), exception);
		}
	}
}
