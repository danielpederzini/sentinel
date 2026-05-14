package org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pdzsoftware.riskactionhandler.application.usecase.NotifyRiskUseCase;
import org.pdzsoftware.riskactionhandler.application.usecase.PersistTransactionUseCase;
import org.pdzsoftware.riskactionhandler.domain.enums.RiskLevel;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.FraudPredictionMessage;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.TransactionScoredMessage;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionConsumer {
    private final NotifyRiskUseCase notifyRiskUseCase;
    private final PersistTransactionUseCase persistTransactionUseCase;

    @KafkaListener(
            topics = "#{@kafkaConsumerProperties.transactionsScoredTopic}",
            groupId = "#{@kafkaConsumerProperties.consumerGroupId}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(@Valid TransactionScoredMessage payload) {
        persistTransactionUseCase.execute(payload);

        FraudPredictionMessage predictionMessage = payload.predictionMessage();
        boolean isHighRisk = RiskLevel.HIGH.equals(predictionMessage.riskLevel());

        if (isHighRisk) {
            notifyRiskUseCase.execute(payload);
        }
    }
}
