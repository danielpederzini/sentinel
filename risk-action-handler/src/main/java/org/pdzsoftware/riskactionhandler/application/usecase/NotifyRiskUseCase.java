package org.pdzsoftware.riskactionhandler.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pdzsoftware.riskactionhandler.application.service.NotificationService;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.FraudPredictionMessage;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.TransactionScoredMessage;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.document.NotificationDocument;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyRiskUseCase {
    private final NotificationService notificationService;

    public void execute(TransactionScoredMessage message) {
        FraudPredictionMessage prediction = message.predictionMessage();

        NotificationDocument notification = NotificationDocument.builder()
                .transactionId(message.transactionId())
                .riskLevel(prediction.riskLevel())
                .fraudProbability(prediction.fraudProbability())
                .scoredPayload(message)
                .build();

        notificationService.save(notification);
        log.info("Persisted PENDING notification for transaction {}", message.transactionId());
    }
}
