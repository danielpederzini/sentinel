package org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.document;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.pdzsoftware.riskactionhandler.domain.enums.NotificationStatus;
import org.pdzsoftware.riskactionhandler.domain.enums.RiskLevel;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.TransactionScoredMessage;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Document(collection = "notifications")
public class NotificationDocument {
    @Id
    private String id;

    private String transactionId;
    private RiskLevel riskLevel;
    private double fraudProbability;

    private TransactionScoredMessage scoredPayload;

    @Indexed
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime sentAt;

    @Builder.Default
    private int retryCount = 0;

    private String failureReason;
}
