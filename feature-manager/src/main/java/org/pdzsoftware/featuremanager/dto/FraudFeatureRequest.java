package org.pdzsoftware.featuremanager.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

public record FraudFeatureRequest(
        String transactionId,
        String userId,
        String cardId,
        String merchantId,
        String deviceId,
        BigDecimal amount,
        Locale.IsoCountryCode countryCode,
        String ipAddress,
        LocalDateTime creationDateTime
) {
}
