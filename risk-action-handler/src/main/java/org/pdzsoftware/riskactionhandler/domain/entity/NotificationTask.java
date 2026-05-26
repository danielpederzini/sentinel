package org.pdzsoftware.riskactionhandler.domain.entity;

import lombok.Builder;
import lombok.Data;
import org.pdzsoftware.riskactionhandler.domain.enums.NotificationTaskStatus;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.TransactionScoredMessage;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@Document(collection = "notification_tasks")
public class NotificationTask {
    @Id
    private String id;

    @Indexed(unique = true)
    private String transactionId;

    private NotificationTaskStatus status;

    private TransactionScoredMessage payload;

    private String llmExplanation;

    private String emailContent;

    @Builder.Default
    private int attempts = 0;

    private Instant lastAttemptAt;

    @Indexed
    private Instant nextRetryAt;

    private String errorMessage;

    private Instant createdAt;

    private Instant completedAt;
}
