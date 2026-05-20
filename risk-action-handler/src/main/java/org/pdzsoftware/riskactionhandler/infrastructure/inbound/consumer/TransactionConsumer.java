package org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pdzsoftware.riskactionhandler.application.usecase.NotifyRiskUseCase;
import org.pdzsoftware.riskactionhandler.domain.enums.RiskLevel;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.TransactionScoredMessage;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionConsumer {
    private final NotifyRiskUseCase notifyRiskUseCase;

    @KafkaListener(
            topics = "#{@kafkaConsumerProperties.transactionsScoredTopic}",
            groupId = "#{@kafkaConsumerProperties.consumerGroupId}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(@Valid TransactionScoredMessage payload) {
        log.info("Received scored transaction {} | riskLevel: {} | fraudProbability: {}",
                payload.transactionId(),
                payload.predictionMessage().riskLevel(),
                payload.predictionMessage().fraudProbability());

        if (RiskLevel.HIGH.equals(payload.predictionMessage().riskLevel())) {
            notifyRiskUseCase.execute(payload);
        }
    }
}
