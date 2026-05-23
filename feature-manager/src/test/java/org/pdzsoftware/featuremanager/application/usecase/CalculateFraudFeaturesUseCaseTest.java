package org.pdzsoftware.featuremanager.application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pdzsoftware.featuremanager.application.service.CardService;
import org.pdzsoftware.featuremanager.application.service.FeatureCacheService;
import org.pdzsoftware.featuremanager.application.service.MerchantService;
import org.pdzsoftware.featuremanager.application.service.TransactionService;
import org.pdzsoftware.featuremanager.application.service.TrustedDeviceService;
import org.pdzsoftware.featuremanager.application.service.UserService;
import org.pdzsoftware.featuremanager.domain.enums.CardType;
import org.pdzsoftware.featuremanager.domain.enums.CountryCode;
import org.pdzsoftware.featuremanager.domain.enums.MerchantCategory;
import org.pdzsoftware.featuremanager.domain.exception.FeatureCalculationException;
import org.pdzsoftware.featuremanager.infrastructure.inbound.controller.dto.FraudFeatureRequest;
import org.pdzsoftware.featuremanager.infrastructure.inbound.controller.dto.FraudFeatureResponse;
import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.entity.CardEntity;
import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.entity.MerchantEntity;
import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.entity.UserEntity;
import org.pdzsoftware.featuremanager.support.TestFixtures;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pdzsoftware.featuremanager.support.TestConstants.AMOUNT_TO_AVERAGE_RATIO_COLD_START;
import static org.pdzsoftware.featuremanager.support.TestConstants.AMOUNT_VELOCITY_STUB;
import static org.pdzsoftware.featuremanager.support.TestConstants.CARD_AGE_DAYS;
import static org.pdzsoftware.featuremanager.support.TestConstants.COLD_START_AVERAGE_AMOUNT;
import static org.pdzsoftware.featuremanager.support.TestConstants.COUNTRY_IP_RISK_MIN;
import static org.pdzsoftware.featuremanager.support.TestConstants.CREATION_AFTERNOON;
import static org.pdzsoftware.featuremanager.support.TestConstants.CREATION_LATE_NIGHT;
import static org.pdzsoftware.featuremanager.support.TestConstants.CREATION_MORNING;
import static org.pdzsoftware.featuremanager.support.TestConstants.DISTINCT_MERCHANT_COUNT_STUB;
import static org.pdzsoftware.featuremanager.support.TestConstants.ERROR_MESSAGE_DB_DOWN;
import static org.pdzsoftware.featuremanager.support.TestConstants.HISTORICAL_TRANSACTION_COUNT;
import static org.pdzsoftware.featuremanager.support.TestConstants.HISTORICAL_TRANSACTION_COUNT_NONE;
import static org.pdzsoftware.featuremanager.support.TestConstants.HISTORICAL_TRANSACTION_COUNT_STUB;
import static org.pdzsoftware.featuremanager.support.TestConstants.IP_RISK_SCORE_STUB;
import static org.pdzsoftware.featuremanager.support.TestConstants.MERCHANT_ID;
import static org.pdzsoftware.featuremanager.support.TestConstants.MERCHANT_RISK_SCORE;
import static org.pdzsoftware.featuremanager.support.TestConstants.NIGHT_HOUR;
import static org.pdzsoftware.featuremanager.support.TestConstants.SECONDS_SINCE_LAST_STUB;
import static org.pdzsoftware.featuremanager.support.TestConstants.TRANSACTION_AMOUNT_LARGE;
import static org.pdzsoftware.featuremanager.support.TestConstants.TRANSACTION_AMOUNT_MEDIUM;
import static org.pdzsoftware.featuremanager.support.TestConstants.TRANSACTION_AMOUNT_SMALL;
import static org.pdzsoftware.featuremanager.support.TestConstants.TRANSACTION_COUNT_1HOUR_STUB;
import static org.pdzsoftware.featuremanager.support.TestConstants.TRANSACTION_COUNT_5MIN_STUB;
import static org.pdzsoftware.featuremanager.support.TestConstants.TRANSACTION_ID_1;
import static org.pdzsoftware.featuremanager.support.TestConstants.TRANSACTION_ID_2;
import static org.pdzsoftware.featuremanager.support.TestConstants.TRANSACTION_ID_3;
import static org.pdzsoftware.featuremanager.support.TestConstants.TRANSACTION_ID_FAIL;
import static org.pdzsoftware.featuremanager.support.TestConstants.USER_AGE_YEARS;
import static org.pdzsoftware.featuremanager.support.TestConstants.USER_ID;

@ExtendWith(MockitoExtension.class)
class CalculateFraudFeaturesUseCaseTest {

    @Mock
    private UserService userService;
    @Mock
    private MerchantService merchantService;
    @Mock
    private CardService cardService;
    @Mock
    private TrustedDeviceService trustedDeviceService;
    @Mock
    private TransactionService transactionService;
    @Mock
    private FeatureCacheService featureCacheService;

    @InjectMocks
    private CalculateFraudFeaturesUseCase calculateFraudFeaturesUseCase;

    @Test
    void execute_shouldUseColdStartAverage_whenHistoricalAverageIsZero() {
        FraudFeatureRequest request = TestFixtures.fraudFeatureRequest(
                TRANSACTION_ID_1, USER_ID, CountryCode.US, CREATION_AFTERNOON, TRANSACTION_AMOUNT_LARGE);

        stubHappyPath(request, CountryCode.US, CREATION_AFTERNOON, BigDecimal.ZERO, HISTORICAL_TRANSACTION_COUNT_NONE);

        FraudFeatureResponse response = calculateFraudFeaturesUseCase.execute(request);

        assertThat(response.userAverageAmount()).isEqualByComparingTo(COLD_START_AVERAGE_AMOUNT);
        assertThat(response.amountToAverageRatio()).isEqualTo(AMOUNT_TO_AVERAGE_RATIO_COLD_START);
        verify(featureCacheService).recordUserTransaction(
                eq(USER_ID), eq(TRANSACTION_ID_1), eq(TRANSACTION_AMOUNT_LARGE), eq(MERCHANT_ID));
    }

    @Test
    void execute_shouldFlagCountryMismatch_whenTransactionCountryDiffersFromHome() {
        FraudFeatureRequest request = TestFixtures.fraudFeatureRequest(
                TRANSACTION_ID_2, USER_ID, CountryCode.BR, CREATION_MORNING, TRANSACTION_AMOUNT_MEDIUM);

        stubHappyPath(request, CountryCode.US, CREATION_MORNING, COLD_START_AVERAGE_AMOUNT, HISTORICAL_TRANSACTION_COUNT);

        FraudFeatureResponse response = calculateFraudFeaturesUseCase.execute(request);

        assertThat(response.hasCountryMismatch()).isTrue();
        assertThat(response.countryIpRisk()).isGreaterThan(COUNTRY_IP_RISK_MIN);
    }

    @Test
    void execute_shouldMarkNightTransaction_whenHourIsLate() {
        FraudFeatureRequest request = TestFixtures.fraudFeatureRequest(
                TRANSACTION_ID_3, USER_ID, CountryCode.US, CREATION_LATE_NIGHT, TRANSACTION_AMOUNT_SMALL);

        stubHappyPath(request, CountryCode.US, CREATION_LATE_NIGHT, COLD_START_AVERAGE_AMOUNT, HISTORICAL_TRANSACTION_COUNT_STUB);

        FraudFeatureResponse response = calculateFraudFeaturesUseCase.execute(request);

        assertThat(response.isNight()).isTrue();
        assertThat(response.hourOfDay()).isEqualTo(NIGHT_HOUR);
    }

    @Test
    void execute_shouldWrapRuntimeExceptions() {
        FraudFeatureRequest request = TestFixtures.fraudFeatureRequest(
                TRANSACTION_ID_FAIL, USER_ID, CountryCode.US, LocalDateTime.now(), TRANSACTION_AMOUNT_SMALL);
        when(userService.getByIdOrThrow(USER_ID)).thenThrow(new RuntimeException(ERROR_MESSAGE_DB_DOWN));

        assertThatThrownBy(() -> calculateFraudFeaturesUseCase.execute(request))
                .isInstanceOf(FeatureCalculationException.class)
                .hasMessageContaining(TRANSACTION_ID_FAIL)
                .hasRootCauseMessage(ERROR_MESSAGE_DB_DOWN);
    }

    private void stubHappyPath(
            FraudFeatureRequest request,
            CountryCode homeCountry,
            LocalDateTime created,
            BigDecimal historicalAverage,
            long historicalCount
    ) {
        UserEntity user = TestFixtures.user(request.userId(), homeCountry, created.minusYears(USER_AGE_YEARS));
        MerchantEntity merchant = TestFixtures.merchant(request.merchantId(), MerchantCategory.TRAVEL, MERCHANT_RISK_SCORE);
        CardEntity card = TestFixtures.card(request.cardId(), CardType.DEBIT, created.minusDays(CARD_AGE_DAYS));

        when(userService.getByIdOrThrow(request.userId())).thenReturn(user);
        when(merchantService.getByIdOrThrow(request.merchantId())).thenReturn(merchant);
        when(cardService.getByIdOrThrow(request.cardId())).thenReturn(card);
        when(transactionService.findAverageAmountByUserId(request.userId())).thenReturn(historicalAverage);
        when(transactionService.countByUserId(request.userId())).thenReturn(historicalCount);
        when(trustedDeviceService.existsById(request.deviceId())).thenReturn(true);
        when(transactionService.findIpRiskScoreByIpAddress(request.ipAddress())).thenReturn(IP_RISK_SCORE_STUB);
        when(featureCacheService.getSecondsSinceLastTransaction(request.userId())).thenReturn(SECONDS_SINCE_LAST_STUB);
        when(featureCacheService.getUserTransactionCount5Min(request.userId())).thenReturn(TRANSACTION_COUNT_5MIN_STUB);
        when(featureCacheService.getUserTransactionCount1Hour(request.userId())).thenReturn(TRANSACTION_COUNT_1HOUR_STUB);
        when(featureCacheService.getAmountVelocity1Hour(request.userId())).thenReturn(AMOUNT_VELOCITY_STUB);
        when(featureCacheService.getDistinctMerchantCount1Hour(request.userId())).thenReturn(DISTINCT_MERCHANT_COUNT_STUB);
    }
}
