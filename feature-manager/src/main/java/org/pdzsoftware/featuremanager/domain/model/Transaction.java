package org.pdzsoftware.featuremanager.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Transaction {
    private String id;
    private BigDecimal amount;
    private Locale.IsoCountryCode countryCode;
    private String ipAddress;
    private LocalDateTime creationDateTime;
}
