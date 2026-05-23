package org.pdzsoftware.featuremanager.support;

import org.pdzsoftware.featuremanager.domain.enums.CardType;
import org.pdzsoftware.featuremanager.domain.enums.CountryCode;
import org.pdzsoftware.featuremanager.domain.enums.MerchantCategory;
import org.pdzsoftware.featuremanager.domain.enums.RiskLevel;
import org.pdzsoftware.featuremanager.infrastructure.inbound.controller.dto.FeaturesRequest;
import org.pdzsoftware.featuremanager.infrastructure.inbound.controller.dto.FraudFeatureRequest;
import org.pdzsoftware.featuremanager.infrastructure.inbound.controller.dto.FraudFeatureResponse;
import org.pdzsoftware.featuremanager.infrastructure.inbound.controller.dto.PersistTransactionRequest;
import org.pdzsoftware.featuremanager.infrastructure.inbound.controller.dto.PredictionRequest;
import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.entity.CardEntity;
import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.entity.MerchantEntity;
import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.entity.UserEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.pdzsoftware.featuremanager.support.TestConstants.CARD_ID;
import static org.pdzsoftware.featuremanager.support.TestConstants.DEVICE_ID;
import static org.pdzsoftware.featuremanager.support.TestConstants.FEATURE_AMOUNT_DEVIATION;
import static org.pdzsoftware.featuremanager.support.TestConstants.FEATURE_AMOUNT_TIMES_MERCHANT_RISK;
import static org.pdzsoftware.featuremanager.support.TestConstants.FEATURE_AMOUNT_TO_AVERAGE_RATIO;
import static org.pdzsoftware.featuremanager.support.TestConstants.FEATURE_CARD_AGE_DAYS;
import static org.pdzsoftware.featuremanager.support.TestConstants.FEATURE_CARD_TYPE;
import static org.pdzsoftware.featuremanager.support.TestConstants.FEATURE_DAY_OF_WEEK;
import static org.pdzsoftware.featuremanager.support.TestConstants.FEATURE_DISTINCT_MERCHANT_COUNT;
import static org.pdzsoftware.featuremanager.support.TestConstants.FEATURE_HISTORICAL_COUNT;
import static org.pdzsoftware.featuremanager.support.TestConstants.FEATURE_HOUR_OF_DAY;
import static org.pdzsoftware.featuremanager.support.TestConstants.FEATURE_LOG_AMOUNT;
import static org.pdzsoftware.featuremanager.support.TestConstants.FEATURE_LOG_SECONDS;
import static org.pdzsoftware.featuremanager.support.TestConstants.FEATURE_LOG_VELOCITY;
import static org.pdzsoftware.featuremanager.support.TestConstants.FEATURE_MERCHANT_CATEGORY;
import static org.pdzsoftware.featuremanager.support.TestConstants.FEATURE_RECENCY_VELOCITY;
import static org.pdzsoftware.featuremanager.support.TestConstants.FEATURE_RISK_SCORE_PRODUCT;
import static org.pdzsoftware.featuremanager.support.TestConstants.FEATURE_SECONDS_SINCE_LAST;
import static org.pdzsoftware.featuremanager.support.TestConstants.FEATURE_TXN_COUNT_1HOUR;
import static org.pdzsoftware.featuremanager.support.TestConstants.FEATURE_TXN_COUNT_5MIN;
import static org.pdzsoftware.featuremanager.support.TestConstants.FEATURE_USER_ACCOUNT_AGE_DAYS;
import static org.pdzsoftware.featuremanager.support.TestConstants.FEATURE_VELOCITY_AMOUNT_INTERACTION;
import static org.pdzsoftware.featuremanager.support.TestConstants.FEATURE_VELOCITY_INTENSITY;
import static org.pdzsoftware.featuremanager.support.TestConstants.FRAUD_PROBABILITY;
import static org.pdzsoftware.featuremanager.support.TestConstants.IP_ADDRESS;
import static org.pdzsoftware.featuremanager.support.TestConstants.IP_RISK_SCORE_LOW;
import static org.pdzsoftware.featuremanager.support.TestConstants.MERCHANT_EMAIL;
import static org.pdzsoftware.featuremanager.support.TestConstants.MERCHANT_ID;
import static org.pdzsoftware.featuremanager.support.TestConstants.MERCHANT_RISK_SCORE_LOW;
import static org.pdzsoftware.featuremanager.support.TestConstants.MODEL_VERSION;
import static org.pdzsoftware.featuremanager.support.TestConstants.PERSIST_CREATED_HOURS_AGO;
import static org.pdzsoftware.featuremanager.support.TestConstants.PERSIST_TRANSACTION_AMOUNT;
import static org.pdzsoftware.featuremanager.support.TestConstants.COLD_START_AVERAGE_AMOUNT;
import static org.pdzsoftware.featuremanager.support.TestConstants.CREATION_AFTERNOON;
import static org.pdzsoftware.featuremanager.support.TestConstants.IP_RISK_SCORE_STUB;
import static org.pdzsoftware.featuremanager.support.TestConstants.MERCHANT_RISK_SCORE;
import static org.pdzsoftware.featuremanager.support.TestConstants.TRANSACTION_AMOUNT_LARGE;
import static org.pdzsoftware.featuremanager.support.TestConstants.TRANSACTION_ID_1;
import static org.pdzsoftware.featuremanager.support.TestConstants.COUNTRY_IP_RISK_MIN;
import static org.pdzsoftware.featuremanager.support.TestConstants.MERCHANT_AGE_YEARS;
import static org.pdzsoftware.featuremanager.support.TestConstants.USER_BIRTH_DATE;
import static org.pdzsoftware.featuremanager.support.TestConstants.USER_EMAIL;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static FraudFeatureRequest fraudFeatureRequestWithId() {
        return fraudFeatureRequest(
                TRANSACTION_ID_1,
                TestConstants.USER_ID,
                CountryCode.US,
                CREATION_AFTERNOON,
                TRANSACTION_AMOUNT_LARGE);
    }

    public static FraudFeatureResponse fraudFeatureResponse() {
        return FraudFeatureResponse.builder()
                .transactionId(TRANSACTION_ID_1)
                .amount(TRANSACTION_AMOUNT_LARGE)
                .userAverageAmount(COLD_START_AVERAGE_AMOUNT)
                .userHistoricalTransactionCount(FEATURE_HISTORICAL_COUNT)
                .userTransactionCount5Min(FEATURE_TXN_COUNT_5MIN)
                .userTransactionCount1Hour(FEATURE_TXN_COUNT_1HOUR)
                .secondsSinceLastTransaction(FEATURE_SECONDS_SINCE_LAST)
                .merchantRiskScore(MERCHANT_RISK_SCORE)
                .isDeviceTrusted(true)
                .hasCountryMismatch(false)
                .amountToAverageRatio(FEATURE_AMOUNT_TO_AVERAGE_RATIO)
                .hourOfDay(FEATURE_HOUR_OF_DAY)
                .ipRiskScore(IP_RISK_SCORE_STUB)
                .cardAgeDays(FEATURE_CARD_AGE_DAYS)
                .amountVelocity1Hour(BigDecimal.TEN)
                .userAccountAgeDays(FEATURE_USER_ACCOUNT_AGE_DAYS)
                .dayOfWeek(FEATURE_DAY_OF_WEEK)
                .merchantCategory(FEATURE_MERCHANT_CATEGORY)
                .cardType(FEATURE_CARD_TYPE)
                .distinctMerchantCount1Hour(FEATURE_DISTINCT_MERCHANT_COUNT)
                .logAmount(FEATURE_LOG_AMOUNT)
                .logSecondsSinceLastTransaction(FEATURE_LOG_SECONDS)
                .logVelocity1Hour(FEATURE_LOG_VELOCITY)
                .amountTimesMerchantRisk(FEATURE_AMOUNT_TIMES_MERCHANT_RISK)
                .riskScoreProduct(FEATURE_RISK_SCORE_PRODUCT)
                .ipDeviceRisk(COUNTRY_IP_RISK_MIN)
                .countryIpRisk(COUNTRY_IP_RISK_MIN)
                .velocityAmountInteraction(FEATURE_VELOCITY_AMOUNT_INTERACTION)
                .recencyVelocity(FEATURE_RECENCY_VELOCITY)
                .amountDeviation(FEATURE_AMOUNT_DEVIATION)
                .isNight(false)
                .velocityIntensity(FEATURE_VELOCITY_INTENSITY)
                .build();
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
                CARD_ID,
                MERCHANT_ID,
                DEVICE_ID,
                amount,
                countryCode,
                IP_ADDRESS,
                creationDateTime
        );
    }

    public static UserEntity user(String id, CountryCode homeCountry, LocalDateTime created) {
        return UserEntity.builder()
                .id(id)
                .email(USER_EMAIL)
                .birthDate(USER_BIRTH_DATE)
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
                .email(MERCHANT_EMAIL)
                .category(category)
                .riskScore(riskScore)
                .creationDateTime(LocalDateTime.now().minusYears(MERCHANT_AGE_YEARS))
                .build();
    }

    public static PersistTransactionRequest persistTransactionRequest(String transactionId) {
        return new PersistTransactionRequest(
                transactionId,
                TestConstants.USER_ID,
                CARD_ID,
                MERCHANT_ID,
                DEVICE_ID,
                PERSIST_TRANSACTION_AMOUNT,
                CountryCode.US,
                IP_ADDRESS,
                LocalDateTime.now().minusHours(PERSIST_CREATED_HOURS_AGO),
                new FeaturesRequest(
                        COLD_START_AVERAGE_AMOUNT,
                        FEATURE_HISTORICAL_COUNT,
                        FEATURE_TXN_COUNT_5MIN,
                        FEATURE_TXN_COUNT_1HOUR,
                        FEATURE_SECONDS_SINCE_LAST,
                        MERCHANT_RISK_SCORE_LOW,
                        true,
                        false,
                        FEATURE_AMOUNT_TO_AVERAGE_RATIO,
                        FEATURE_HOUR_OF_DAY,
                        IP_RISK_SCORE_LOW,
                        FEATURE_CARD_AGE_DAYS,
                        BigDecimal.TEN,
                        FEATURE_USER_ACCOUNT_AGE_DAYS,
                        FEATURE_DAY_OF_WEEK,
                        FEATURE_MERCHANT_CATEGORY,
                        FEATURE_CARD_TYPE,
                        FEATURE_DISTINCT_MERCHANT_COUNT,
                        FEATURE_LOG_AMOUNT,
                        FEATURE_LOG_SECONDS,
                        FEATURE_LOG_VELOCITY,
                        FEATURE_AMOUNT_TIMES_MERCHANT_RISK,
                        FEATURE_RISK_SCORE_PRODUCT,
                        COUNTRY_IP_RISK_MIN,
                        COUNTRY_IP_RISK_MIN,
                        FEATURE_VELOCITY_AMOUNT_INTERACTION,
                        FEATURE_RECENCY_VELOCITY,
                        FEATURE_AMOUNT_DEVIATION,
                        false,
                        FEATURE_VELOCITY_INTENSITY
                ),
                new PredictionRequest(FRAUD_PROBABILITY, RiskLevel.LOW, MODEL_VERSION)
        );
    }
}
