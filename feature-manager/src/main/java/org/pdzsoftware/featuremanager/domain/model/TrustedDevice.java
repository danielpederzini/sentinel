package org.pdzsoftware.featuremanager.domain.model;

import lombok.*;
import org.pdzsoftware.featuremanager.domain.enums.DeviceType;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TrustedDevice {
    private String id;
    private String name;
    private DeviceType type;
    private LocalDateTime creationDateTime;
}
