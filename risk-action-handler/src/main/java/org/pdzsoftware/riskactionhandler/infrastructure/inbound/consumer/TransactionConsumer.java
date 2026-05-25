package org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pdzsoftware.riskactionhandler.application.usecase.PersistNotificationTaskUseCase;
import org.pdzsoftware.riskactionhandler.domain.enums.RiskLevel;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.TransactionScoredMessage;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionConsumer {
    private final PersistNotificationTaskUseCase persistNotificationTaskUseCase;

    @KafkaListener(
            topics = "#{@kafkaConsumerProperties.transactionsScoredTopic}",
            groupId = "#{@kafkaConsumerProperties.consumerGroupId}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(@Valid TransactionScoredMessage payload) {
        RiskLevel riskLevel = payload.predictionMessage().riskLevel();

        if (RiskLevel.HIGH.equals(riskLevel)) {
            log.info("Received HIGH risk transaction {} | fraudProbability: {}",
                    payload.transactionId(), payload.predictionMessage().fraudProbability());
            persistNotificationTaskUseCase.execute(payload);
        } else {
            log.debug("Received scored transaction {} | riskLevel: {} | fraudProbability: {}",
                    payload.transactionId(), riskLevel, payload.predictionMessage().fraudProbability());
        }
    }
}
