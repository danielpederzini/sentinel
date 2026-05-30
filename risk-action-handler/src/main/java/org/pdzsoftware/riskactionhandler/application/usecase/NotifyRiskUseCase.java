package org.pdzsoftware.riskactionhandler.application.usecase;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pdzsoftware.riskactionhandler.application.util.EmailContentBuilder;
import org.pdzsoftware.riskactionhandler.domain.entity.NotificationTask;
import org.pdzsoftware.riskactionhandler.domain.enums.NotificationTaskStatus;
import org.pdzsoftware.riskactionhandler.infrastructure.config.properties.NotificationSchedulerProperties;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.TransactionScoredMessage;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.client.EmailClient;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.client.LlmClient;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.repository.NotificationTaskRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyRiskUseCase {
    private static final String EMAIL_SUBJECT_PREFIX = "Fraud Alert — Transaction ";

    private final LlmClient llmClient;
    private final EmailClient emailClient;
    private final NotificationTaskRepository notificationTaskRepository;
    private final NotificationSchedulerProperties schedulerProperties;
    private final MeterRegistry meterRegistry;

    public void execute(NotificationTask task) {
        try {
            if (NotificationTaskStatus.PENDING_LLM.equals(task.getStatus())) {
                processLlmStep(task);
            }

            if (NotificationTaskStatus.PENDING_EMAIL.equals(task.getStatus())) {
                processEmailStep(task);
            }
        } catch (RuntimeException exception) {
            handleFailure(task, exception);
        }
    }

    private void processLlmStep(NotificationTask task) {
        TransactionScoredMessage message = task.getPayload();
        String llmExplanation = llmClient.getFraudExplanation(message);
        String emailContent = EmailContentBuilder.buildFraudAlertEmail(message, llmExplanation);

        task.setLlmExplanation(llmExplanation);
        task.setEmailContent(emailContent);
        task.setStatus(NotificationTaskStatus.PENDING_EMAIL);
        notificationTaskRepository.save(task);

        log.info("LLM step completed for transaction {}", task.getTransactionId());
    }

    private void processEmailStep(NotificationTask task) {
        String transactionId = task.getTransactionId();
        String subject = EMAIL_SUBJECT_PREFIX + transactionId;

        emailClient.sendEmail(transactionId, subject, task.getEmailContent());

        task.setStatus(NotificationTaskStatus.COMPLETED);
        task.setCompletedAt(Instant.now());
        task.setErrorMessage(null);
        notificationTaskRepository.save(task);

        meterRegistry.counter("risk_alerts_sent").increment();
        log.info("Sent fraud alert notification for transaction {}", transactionId);
    }

    private void handleFailure(NotificationTask task, RuntimeException exception) {
        int attempts = task.getAttempts() + 1;
        task.setAttempts(attempts);
        task.setLastAttemptAt(Instant.now());
        task.setErrorMessage(exception.getMessage());

        if (attempts >= schedulerProperties.getMaxAttempts()) {
            task.setStatus(NotificationTaskStatus.DEAD_LETTER);
            meterRegistry.counter("notifications_dead_lettered").increment();
            log.error("Notification task for transaction {} moved to DEAD_LETTER after {} attempts: {}",
                    task.getTransactionId(), attempts, exception.getMessage());
        } else {
            long delaySeconds = schedulerProperties.getBaseDelaySeconds() * (1L << (attempts - 1));
            task.setNextRetryAt(Instant.now().plusSeconds(delaySeconds));
            log.warn("Notification task for transaction {} failed (attempt {}), next retry at {}: {}",
                    task.getTransactionId(), attempts, task.getNextRetryAt(), exception.getMessage());
        }

        notificationTaskRepository.save(task);
    }
}
