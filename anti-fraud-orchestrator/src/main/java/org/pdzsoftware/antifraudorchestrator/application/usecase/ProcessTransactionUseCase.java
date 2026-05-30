package org.pdzsoftware.antifraudorchestrator.application.usecase;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.FeatureManagerClient;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.InferenceEngineClient;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto.FraudFeatureResponse;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto.FraudPredictionResponse;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto.PersistTransactionRequest;
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
	private final MeterRegistry meterRegistry;

	@Override
	public void execute(ProcessTransactionInput input) {
		TransactionCreatedMessage payload = input.payload();
		String messageKey = input.messageKey();
		Timer.Sample sample = Timer.start(meterRegistry);
		try {
			FraudFeatureResponse features = featureManagerClient.calculateFraudFeatures(payload);
			FraudPredictionResponse prediction = inferenceEngineClient.scoreTransaction(features);

			PersistTransactionRequest persistRequest = PersistTransactionRequest.from(payload, features, prediction);
			featureManagerClient.persistTransaction(persistRequest);

			TransactionScoredMessage scoredMessage = TransactionScoredMessage.from(payload, features, prediction);

			meterRegistry.counter("transactions_processed",
					"risk_level", prediction.riskLevel().name(),
					"model_version", prediction.modelVersion()).increment();

			log.info("Processed transaction {} | riskLevel: {} | fraudProbability: {} | modelVersion: {}",
					payload.transactionId(),
					prediction.riskLevel(),
					prediction.fraudProbability(),
					prediction.modelVersion()
			);

			transactionProducer.publish(scoredMessage)
					.whenComplete((result, exception) -> handlePublishResult(exception, payload, messageKey));
		} catch (RuntimeException exception) {
			throw new TransactionOrchestrationException(String.format(
					"Failed to orchestrate transaction %s with key %s", payload.transactionId(), messageKey), exception);
		} finally {
			sample.stop(Timer.builder("transaction_orchestration_duration_seconds")
					.description("End-to-end orchestration time per transaction")
					.publishPercentileHistogram()
					.register(meterRegistry));
		}
	}

	private static void handlePublishResult(Throwable exception, TransactionCreatedMessage payload, String messageKey) {
		if (exception != null) {
			log.error("Failed to publish scored transaction {} with key {}", payload.transactionId(), messageKey, exception);
		}
	}
}
