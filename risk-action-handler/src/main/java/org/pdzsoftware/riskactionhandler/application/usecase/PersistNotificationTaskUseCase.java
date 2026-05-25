package org.pdzsoftware.riskactionhandler.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pdzsoftware.riskactionhandler.domain.entity.NotificationTask;
import org.pdzsoftware.riskactionhandler.domain.enums.NotificationTaskStatus;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.TransactionScoredMessage;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.repository.NotificationTaskRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class PersistNotificationTaskUseCase {
    private final NotificationTaskRepository notificationTaskRepository;

    public void execute(TransactionScoredMessage message) {
        String transactionId = message.transactionId();

        if (notificationTaskRepository.findByTransactionId(transactionId).isPresent()) {
            log.info("Notification task already exists for transaction {}, skipping", transactionId);
            return;
        }

        NotificationTask task = NotificationTask.builder()
                .transactionId(transactionId)
                .status(NotificationTaskStatus.PENDING_LLM)
                .payload(message)
                .attempts(0)
                .nextRetryAt(Instant.now())
                .createdAt(Instant.now())
                .build();

        notificationTaskRepository.save(task);
        log.info("Persisted notification task for transaction {}", transactionId);
    }
}
