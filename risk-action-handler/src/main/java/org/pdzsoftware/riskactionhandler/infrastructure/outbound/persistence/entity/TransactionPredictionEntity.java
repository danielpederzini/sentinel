package org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.pdzsoftware.riskactionhandler.domain.enums.RiskLevel;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.FraudPredictionMessage;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.TransactionScoredMessage;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "transaction_predictions")
public class TransactionPredictionEntity {
    @Id
    @Column(name = "transaction_id")
    private String transactionId;

    @Column(nullable = false)
    private double fraudProbability;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    @Column(nullable = false)
    private String modelVersion;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private TransactionEntity transaction;

    public static TransactionPredictionEntity from(TransactionScoredMessage input) {
        FraudPredictionMessage predictionMessage = input.predictionMessage();

        return TransactionPredictionEntity.builder()
                .transactionId(input.transactionId())
                .fraudProbability(predictionMessage.fraudProbability())
                .riskLevel(predictionMessage.riskLevel())
                .modelVersion(predictionMessage.modelVersion())
                .build();
    }
}
