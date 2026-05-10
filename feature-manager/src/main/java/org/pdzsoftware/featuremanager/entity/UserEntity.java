package org.pdzsoftware.featuremanager.entity;

import jakarta.persistence.*;
import lombok.*;
import org.pdzsoftware.featuremanager.enums.CountryCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class UserEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CountryCode homeCountryCode;

    @Column(nullable = false)
    private LocalDateTime creationDateTime;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TransactionEntity> transactions;
}
