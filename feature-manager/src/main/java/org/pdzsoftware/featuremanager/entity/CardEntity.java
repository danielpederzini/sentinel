package org.pdzsoftware.featuremanager.entity;

import jakarta.persistence.*;
import lombok.*;
import org.pdzsoftware.featuremanager.enums.CardType;

import java.time.LocalDateTime;
import java.util.List;

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

    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TransactionEntity> transactions;
}
