package org.pdzsoftware.featuremanager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "transaction_feature_vectors")
public class TransactionFeatureVectorEntity {
    @Id
    @Column(name = "transaction_id")
    private String transactionId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private BigDecimal userAverageAmount;

    @Column(nullable = false)
    private long userTransactionCount5Min;

    @Column(nullable = false)
    private long userTransactionCount1Hour;

    @Column(nullable = false)
    private long secondsSinceLastTransaction;

    @Column(nullable = false)
    private float merchantRiskScore;

    @Column(nullable = false)
    private boolean isDeviceTrusted;

    @Column(nullable = false)
    private boolean hasCountryMismatch;

    @Column(nullable = false)
    private float amountToAverageRatio;

    @Column(nullable = false)
    private int hourOfDay;

    @Column(nullable = false)
    private float ipRiskScore;

    @Column(nullable = false)
    private long cardAgeDays;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private TransactionEntity transaction;
}
