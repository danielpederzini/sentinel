package org.pdzsoftware.featuremanager.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.pdzsoftware.featuremanager.domain.enums.MerchantCategory;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "merchants")
public class MerchantEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private float riskScore;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MerchantCategory category;

    @Column(nullable = false)
    private LocalDateTime creationDateTime;
}
