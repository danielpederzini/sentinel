package org.pdzsoftware.featuremanager.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.pdzsoftware.featuremanager.domain.enums.CardType;
import org.pdzsoftware.featuremanager.domain.enums.MerchantCategory;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cards")
public class CardEntity {
    @Id
    private String id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CardType type;

    @Column(nullable = false)
    private LocalDateTime creationDateTime;
}
