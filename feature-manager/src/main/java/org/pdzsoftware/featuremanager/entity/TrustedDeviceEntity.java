package org.pdzsoftware.featuremanager.entity;

import jakarta.persistence.*;
import lombok.*;
import org.pdzsoftware.featuremanager.enums.DeviceType;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "trusted_devices")
public class TrustedDeviceEntity {
    @Id
    private String id;

    @Column
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DeviceType type;

    @Column(nullable = false)
    private LocalDateTime creationDateTime;

    @OneToMany(mappedBy = "trustedDevice", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TransactionEntity> transactions;
}
