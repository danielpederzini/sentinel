package org.pdzsoftware.featuremanager.entity;

import jakarta.persistence.*;
import lombok.*;
import org.pdzsoftware.featuremanager.enums.CountryCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "transactions")
public class TransactionEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CountryCode countryCode;

    @Column(nullable = false)
    private String ipAddress;

    @Column(nullable = false)
    private LocalDateTime creationDateTime;

    @ManyToOne(fetch = FetchType.LAZY)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    private TrustedDeviceEntity trustedDevice;

    @ManyToOne(fetch = FetchType.LAZY)
    private MerchantEntity merchant;

    @ManyToOne(fetch = FetchType.LAZY)
    private CardEntity card;
}
