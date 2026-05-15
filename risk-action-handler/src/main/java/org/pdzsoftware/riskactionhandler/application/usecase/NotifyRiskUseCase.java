package org.pdzsoftware.riskactionhandler.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pdzsoftware.riskactionhandler.application.service.NotificationOutboxService;
import org.pdzsoftware.riskactionhandler.application.util.EmailContentBuilder;
import org.pdzsoftware.riskactionhandler.domain.enums.NotificationStatus;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.TransactionScoredMessage;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.client.LlmClient;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.entity.NotificationOutboxEntity;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.entity.TransactionEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyRiskUseCase implements VoidUseCase<TransactionScoredMessage> {
    private final LlmClient llmClient;
    private final NotificationOutboxService notificationOutboxService;

    @Override
    public void execute(TransactionScoredMessage input) {
        String transactionId = input.transactionId();

        String fraudExplanation = llmClient.getFraudExplanation(input);
        String emailContent = EmailContentBuilder.buildFraudAlertEmail(input, fraudExplanation);
        String emailSubject = String.format("Suspected Fraud: %s", transactionId);

        TransactionEntity transactionEntity = TransactionEntity.builder()
                .id(transactionId)
                .build();

        NotificationOutboxEntity outboxEntity = NotificationOutboxEntity.builder()
                .transaction(transactionEntity)
                .emailSubject(emailSubject)
                .emailContent(emailContent)
                .status(NotificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .attemptCount(0)
                .build();

        notificationOutboxService.save(outboxEntity);
        log.info("Queued fraud alert notification for transaction {}", transactionId);
    }
}
