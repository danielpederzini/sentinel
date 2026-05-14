package org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.repostiory;

import org.pdzsoftware.riskactionhandler.domain.enums.NotificationStatus;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.entity.NotificationOutboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutboxEntity, Long> {
    List<NotificationOutboxEntity> findByStatus(NotificationStatus status);
}
