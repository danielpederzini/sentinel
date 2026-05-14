package org.pdzsoftware.riskactionhandler.infrastructure.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pdzsoftware.riskactionhandler.application.service.NotificationOutboxService;
import org.pdzsoftware.riskactionhandler.domain.enums.NotificationStatus;
import org.pdzsoftware.riskactionhandler.domain.exception.EmailClientException;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.client.EmailClient;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.entity.NotificationOutboxEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationOutboxScheduler {
    private final NotificationOutboxService notificationOutboxService;
    private final EmailClient emailClient;

    @Scheduled(cron = "0 */5 * * * *")
    public void sendPendingNotifications() {
        List<NotificationOutboxEntity> pendingNotifications = notificationOutboxService.findPending();

        if (pendingNotifications.isEmpty()) {
            log.debug("No pending notifications to send");
            return;
        }

        log.info("Processing {} pending notification(s)", pendingNotifications.size());

        for (NotificationOutboxEntity notification : pendingNotifications) {
            try {
                String transactionId = notification.getTransaction().getId();

                emailClient.sendEmail(
                        transactionId,
                        notification.getEmailSubject(),
                        notification.getEmailContent()
                );

                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
                notificationOutboxService.save(notification);

                log.info("Sent fraud alert email for transaction {}", transactionId);
            } catch (EmailClientException exception) {
                notification.setStatus(NotificationStatus.FAILED);
                notification.setAttemptCount(notification.getAttemptCount() + 1);
                notification.setFailureReason(exception.getMessage());
                notificationOutboxService.save(notification);

                log.error("Failed to send notification for transaction {}: {}",
                        notification.getTransaction().getId(), exception.getMessage());
            }
        }
    }
}
