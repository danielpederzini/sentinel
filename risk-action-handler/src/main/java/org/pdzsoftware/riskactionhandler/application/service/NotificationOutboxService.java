package org.pdzsoftware.riskactionhandler.application.service;

import lombok.RequiredArgsConstructor;
import org.pdzsoftware.riskactionhandler.domain.enums.NotificationStatus;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.entity.NotificationOutboxEntity;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.repository.NotificationOutboxRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationOutboxService {
    private final NotificationOutboxRepository notificationOutboxRepository;

    public void save(NotificationOutboxEntity entity) {
        notificationOutboxRepository.save(entity);
    }

    public List<NotificationOutboxEntity> findPending() {
        return notificationOutboxRepository.findByStatus(NotificationStatus.PENDING);
    }
}
