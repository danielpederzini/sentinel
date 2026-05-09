package org.pdzsoftware.featuremanager.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.pdzsoftware.featuremanager.domain.enums.CardType;
import org.pdzsoftware.featuremanager.domain.enums.DeviceType;

import java.time.LocalDateTime;

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
}
