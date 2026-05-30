package org.pdzsoftware.riskactionhandler.infrastructure.outbound.repository;

import org.pdzsoftware.riskactionhandler.domain.entity.NotificationTask;
import org.pdzsoftware.riskactionhandler.domain.enums.NotificationTaskStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTaskRepository extends MongoRepository<NotificationTask, String> {
    Optional<NotificationTask> findByTransactionId(String transactionId);

    List<NotificationTask> findByStatusInAndNextRetryAtLessThanEqual(
            List<NotificationTaskStatus> statuses,
            Instant now
    );

    long countByStatus(NotificationTaskStatus status);
}
