package org.pdzsoftware.antifraudorchestrator.consumer;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pdzsoftware.antifraudorchestrator.client.FeatureManagerClient;
import org.pdzsoftware.antifraudorchestrator.client.InferenceEngineClient;
import org.pdzsoftware.antifraudorchestrator.client.dto.FraudFeatureResult;
import org.pdzsoftware.antifraudorchestrator.client.dto.FraudPredictionResponse;
import org.pdzsoftware.antifraudorchestrator.dto.TransactionCreatedMessage;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionConsumer {
	private final FeatureManagerClient featureManagerClient;
	private final InferenceEngineClient inferenceEngineClient;

	@KafkaListener(
			topics = "#{@kafkaProperties.transactionsCreatedTopic}",
			groupId = "#{@kafkaProperties.consumerGroupId}",
			containerFactory = "kafkaListenerContainerFactory"
	)
	public void consume(@Valid TransactionCreatedMessage payload,
	                    @Header(KafkaHeaders.RECEIVED_KEY) String messageKey) {
		try {
			FraudFeatureResult features = featureManagerClient.calculateFraudFeatures(payload);
			FraudPredictionResponse prediction = inferenceEngineClient.scoreTransaction(features);

			log.info(
					"Processed transactionId={} key={} riskLevel={} fraudProbability={} modelVersion={}",
					payload.transactionId(),
					messageKey,
					prediction.riskLevel(),
					prediction.fraudProbability(),
					prediction.modelVersion()
			);
		} catch (Exception exception) {
			log.error("Failed to orchestrate transactionId={} key={}", payload.transactionId(), messageKey, exception);
		}
	}
}
