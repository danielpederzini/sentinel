package org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pdzsoftware.riskactionhandler.domain.enums.RiskLevel;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.TransactionScoredMessage;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.client.LlmClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionConsumer {
	private static final Random RANDOM = new Random();
	private final LlmClient llmClient;

	@KafkaListener(
			topics = "#{@kafkaConsumerProperties.transactionsScoredTopic}",
			groupId = "#{@kafkaConsumerProperties.consumerGroupId}",
			containerFactory = "kafkaListenerContainerFactory"
	)
	public void consume(@Valid TransactionScoredMessage payload,
	                    @Header(KafkaHeaders.RECEIVED_KEY) String messageKey) {
		if (RiskLevel.HIGH.equals(payload.predictionMessage().riskLevel())) {
			String response = llmClient.explainFraud(payload);
			log.info("LLM explainability response for transaction {} with key {}: {}", payload.transactionId(), messageKey, response);
		}
	}
}
