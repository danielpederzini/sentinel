package org.pdzsoftware.featuremanager.domain.model;

import lombok.*;
import org.pdzsoftware.featuremanager.domain.enums.MerchantCategory;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Merchant {
    private String id;
    private String email;
    private float riskScore;
    private MerchantCategory category;
    private LocalDateTime creationDateTime;
}
