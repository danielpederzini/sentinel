package org.pdzsoftware.antifraudorchestrator.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.FeatureManagerClient;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.InferenceEngineClient;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto.FraudFeatureResponse;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto.FraudPredictionResponse;
import org.pdzsoftware.antifraudorchestrator.infrastructure.inbound.consumer.dto.TransactionCreatedMessage;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.producer.dto.TransactionScoredMessage;
import org.pdzsoftware.antifraudorchestrator.domain.exception.TransactionOrchestrationException;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.producer.TransactionProducer;
import org.pdzsoftware.antifraudorchestrator.application.usecase.dto.ProcessTransactionInput;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessTransactionUseCase implements VoidUseCase<ProcessTransactionInput> {
	private final FeatureManagerClient featureManagerClient;
	private final InferenceEngineClient inferenceEngineClient;
	private final TransactionProducer transactionProducer;

	@Override
	public void execute(ProcessTransactionInput input) {
		TransactionCreatedMessage payload = input.payload();
		String messageKey = input.messageKey();
		try {
			FraudFeatureResponse features = featureManagerClient.calculateFraudFeatures(payload);
			FraudPredictionResponse prediction = inferenceEngineClient.scoreTransaction(features);
			TransactionScoredMessage scoredMessage = TransactionScoredMessage.from(payload, features, prediction);

			log.info("Processed transaction {} | key: {} | riskLevel: {} | fraudProbability: {} | modelVersion: {} | explainability: {}",
					payload.transactionId(),
					messageKey,
					prediction.riskLevel(),
					prediction.fraudProbability(),
					prediction.modelVersion(),
					prediction.explainability()
			);

			transactionProducer.publish(scoredMessage)
					.whenComplete((result, exception) -> handlePublishResult(exception, payload, messageKey));
		} catch (RuntimeException exception) {
			throw new TransactionOrchestrationException(String.format(
					"Failed to orchestrate transaction %s with key %s", payload.transactionId(), messageKey), exception);
		}
	}

	private static void handlePublishResult(Throwable exception, TransactionCreatedMessage payload, String messageKey) {
		if (exception != null) {
			log.error("Failed to publish scored transaction {} with key {}", payload.transactionId(), messageKey, exception);
		} else {
			log.info("Successfully published scored transaction {} with key {}", payload.transactionId(), messageKey);
		}
	}
}
