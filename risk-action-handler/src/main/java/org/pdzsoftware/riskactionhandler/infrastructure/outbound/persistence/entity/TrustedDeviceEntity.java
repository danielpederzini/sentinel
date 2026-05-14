package org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.pdzsoftware.riskactionhandler.domain.enums.DeviceType;
import org.springframework.util.StringUtils;

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

    public static TrustedDeviceEntity fromId(String id) {
        return TrustedDeviceEntity.builder()
                .id(id)
                .build();
    }

    public static TrustedDeviceEntity fromNullableId(String id) {
        return StringUtils.hasText(id) ? fromId(id) : null;
    }
}
