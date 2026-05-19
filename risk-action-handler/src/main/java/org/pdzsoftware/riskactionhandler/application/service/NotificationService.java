package org.pdzsoftware.riskactionhandler.application.service;

import lombok.RequiredArgsConstructor;
import org.pdzsoftware.riskactionhandler.domain.enums.NotificationStatus;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.document.NotificationDocument;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public NotificationDocument save(NotificationDocument notification) {
        return notificationRepository.save(notification);
    }

    public List<NotificationDocument> findPendingNotifications() {
        return notificationRepository.findByStatus(NotificationStatus.PENDING);
    }

    public void markAsSent(NotificationDocument notification) {
        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    public void markAsFailed(NotificationDocument notification, String reason) {
        notification.setStatus(NotificationStatus.FAILED);
        notification.setRetryCount(notification.getRetryCount() + 1);
        notification.setFailureReason(reason);
        notificationRepository.save(notification);
    }
}
