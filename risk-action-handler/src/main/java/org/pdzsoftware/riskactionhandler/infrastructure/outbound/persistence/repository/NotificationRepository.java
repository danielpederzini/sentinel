package org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.repository;

import org.pdzsoftware.riskactionhandler.domain.enums.NotificationStatus;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.document.NotificationDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NotificationRepository extends MongoRepository<NotificationDocument, String> {
    List<NotificationDocument> findByStatus(NotificationStatus status);
}
