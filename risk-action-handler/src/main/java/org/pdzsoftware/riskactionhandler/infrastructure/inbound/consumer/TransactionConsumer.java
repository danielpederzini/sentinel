package org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pdzsoftware.riskactionhandler.domain.enums.RiskLevel;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.TransactionScoredMessage;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.client.EmailClient;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.client.LlmClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionConsumer {
    private final LlmClient llmClient;
    private final EmailClient emailClient;

    @KafkaListener(
            topics = "#{@kafkaConsumerProperties.transactionsScoredTopic}",
            groupId = "#{@kafkaConsumerProperties.consumerGroupId}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(@Valid TransactionScoredMessage payload,
                        @Header(KafkaHeaders.RECEIVED_KEY) String messageKey) {
        if (RiskLevel.HIGH.equals(payload.predictionMessage().riskLevel())) {
            String fraudExplanation = llmClient.getFraudExplanation(payload);
            log.info("LLM explainability fraudExplanation for transaction {} with key {}: {}",
                    payload.transactionId(), messageKey, fraudExplanation);
            emailClient.sendEmail(
                    payload.transactionId(),
                    "pederzinidaniel@hotmail.com",
                    String.format("Suspected Fraud: %s", payload.transactionId()),
                    fraudExplanation
            );
        }
    }
}
