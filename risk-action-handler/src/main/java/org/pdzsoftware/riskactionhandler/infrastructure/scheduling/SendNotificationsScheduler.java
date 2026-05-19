package org.pdzsoftware.riskactionhandler.infrastructure.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pdzsoftware.riskactionhandler.application.service.NotificationService;
import org.pdzsoftware.riskactionhandler.application.util.EmailContentBuilder;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.TransactionScoredMessage;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.client.EmailClient;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.client.LlmClient;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.document.NotificationDocument;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendNotificationsScheduler {
    private final NotificationService notificationService;
    private final LlmClient llmClient;
    private final EmailClient emailClient;

    @Scheduled(fixedDelayString = "${app.scheduling.send-notifications-delay-ms:60000}")
    public void sendPendingNotifications() {
        List<NotificationDocument> pending = notificationService.findPendingNotifications();
        if (pending.isEmpty()) {
            return;
        }

        log.info("Found {} pending notifications to send", pending.size());

        for (NotificationDocument notification : pending) {
            try {
                TransactionScoredMessage scoredPayload = notification.getScoredPayload();

                String llmExplanation = llmClient.getFraudExplanation(scoredPayload);
                String emailContent = EmailContentBuilder.buildFraudAlertEmail(scoredPayload, llmExplanation);
                String subject = "Fraud Alert — Transaction " + notification.getTransactionId();

                emailClient.sendEmail(notification.getTransactionId(), subject, emailContent);

                notificationService.markAsSent(notification);
                log.info("Sent notification for transaction {}", notification.getTransactionId());
            } catch (Exception e) {
                notificationService.markAsFailed(notification, e.getMessage());
                log.error("Failed to send notification for transaction {}: {}",
                        notification.getTransactionId(), e.getMessage());
            }
        }
    }
}
