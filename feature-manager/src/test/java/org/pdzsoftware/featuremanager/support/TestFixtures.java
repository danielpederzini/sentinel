package org.pdzsoftware.featuremanager.support;

import org.pdzsoftware.featuremanager.domain.enums.CardType;
import org.pdzsoftware.featuremanager.domain.enums.CountryCode;
import org.pdzsoftware.featuremanager.domain.enums.MerchantCategory;
import org.pdzsoftware.featuremanager.domain.enums.RiskLevel;
import org.pdzsoftware.featuremanager.infrastructure.inbound.controller.dto.FeaturesRequest;
import org.pdzsoftware.featuremanager.infrastructure.inbound.controller.dto.FraudFeatureRequest;
import org.pdzsoftware.featuremanager.infrastructure.inbound.controller.dto.PersistTransactionRequest;
import org.pdzsoftware.featuremanager.infrastructure.inbound.controller.dto.PredictionRequest;
import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.entity.CardEntity;
import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.entity.MerchantEntity;
import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.entity.UserEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static FraudFeatureRequest fraudFeatureRequest(
            String transactionId,
            String userId,
            CountryCode countryCode,
            LocalDateTime creationDateTime,
            BigDecimal amount
    ) {
        return new FraudFeatureRequest(
                transactionId,
                userId,
                "card-1",
                "merchant-1",
                "device-1",
                amount,
                countryCode,
                "203.0.113.1",
                creationDateTime
        );
    }

    public static UserEntity user(String id, CountryCode homeCountry, LocalDateTime created) {
        return UserEntity.builder()
                .id(id)
                .email("user@example.com")
                .birthDate(LocalDate.of(1990, 1, 1))
                .homeCountryCode(homeCountry)
                .creationDateTime(created)
                .build();
    }

    public static CardEntity card(String id, CardType type, LocalDateTime created) {
        return CardEntity.builder()
                .id(id)
                .type(type)
                .creationDateTime(created)
                .build();
    }

    public static MerchantEntity merchant(String id, MerchantCategory category, float riskScore) {
        return MerchantEntity.builder()
                .id(id)
                .email("merchant@example.com")
                .category(category)
                .riskScore(riskScore)
                .creationDateTime(LocalDateTime.now().minusYears(1))
                .build();
    }

    public static PersistTransactionRequest persistTransactionRequest(String transactionId) {
        return new PersistTransactionRequest(
                transactionId,
                "user-1",
                "card-1",
                "merchant-1",
                "device-1",
                BigDecimal.valueOf(50),
                CountryCode.US,
                "203.0.113.1",
                LocalDateTime.now().minusHours(1),
                new FeaturesRequest(
                        BigDecimal.valueOf(100),
                        5L,
                        1L,
                        2L,
                        3600L,
                        0.2f,
                        true,
                        false,
                        0.5f,
                        14,
                        0.1f,
                        30L,
                        BigDecimal.TEN,
                        365L,
                        3,
                        1,
                        0,
                        1L,
                        3.9,
                        8.0,
                        2.3,
                        10.0,
                        0.02,
                        0.0,
                        0.0,
                        1.0,
                        0.5,
                        0.1,
                        false,
                        5.0
                ),
                new PredictionRequest(0.15, RiskLevel.LOW, "v1")
        );
    }
}
