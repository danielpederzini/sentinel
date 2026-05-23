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
        LocalDateTime created = LocalDateTime.of(2024, 6, 15, 14, 0);
        FraudFeatureRequest request = TestFixtures.fraudFeatureRequest(
                "txn-1", "user-1", CountryCode.US, created, BigDecimal.valueOf(200));

        stubHappyPath(request, CountryCode.US, created, BigDecimal.ZERO, 0L);

        FraudFeatureResponse response = calculateFraudFeaturesUseCase.execute(request);

        assertThat(response.userAverageAmount()).isEqualByComparingTo("100");
        assertThat(response.amountToAverageRatio()).isEqualTo(2.0f);
        verify(featureCacheService).recordUserTransaction(
                eq("user-1"), eq("txn-1"), eq(BigDecimal.valueOf(200)), eq("merchant-1"));
    }

    @Test
    void execute_shouldFlagCountryMismatch_whenTransactionCountryDiffersFromHome() {
        LocalDateTime created = LocalDateTime.of(2024, 6, 15, 10, 0);
        FraudFeatureRequest request = TestFixtures.fraudFeatureRequest(
                "txn-2", "user-1", CountryCode.BR, created, BigDecimal.valueOf(50));

        stubHappyPath(request, CountryCode.US, created, BigDecimal.valueOf(100), 3L);

        FraudFeatureResponse response = calculateFraudFeaturesUseCase.execute(request);

        assertThat(response.hasCountryMismatch()).isTrue();
        assertThat(response.countryIpRisk()).isGreaterThan(0.0);
    }

    @Test
    void execute_shouldMarkNightTransaction_whenHourIsLate() {
        LocalDateTime created = LocalDateTime.of(2024, 6, 15, 23, 0);
        FraudFeatureRequest request = TestFixtures.fraudFeatureRequest(
                "txn-3", "user-1", CountryCode.US, created, BigDecimal.TEN);

        stubHappyPath(request, CountryCode.US, created, BigDecimal.valueOf(100), 1L);

        FraudFeatureResponse response = calculateFraudFeaturesUseCase.execute(request);

        assertThat(response.isNight()).isTrue();
        assertThat(response.hourOfDay()).isEqualTo(23);
    }

    @Test
    void execute_shouldWrapRuntimeExceptions() {
        FraudFeatureRequest request = TestFixtures.fraudFeatureRequest(
                "txn-fail", "user-1", CountryCode.US, LocalDateTime.now(), BigDecimal.TEN);
        when(userService.getByIdOrThrow("user-1")).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> calculateFraudFeaturesUseCase.execute(request))
                .isInstanceOf(FeatureCalculationException.class)
                .hasMessageContaining("txn-fail")
                .hasRootCauseMessage("db down");
    }

    private void stubHappyPath(
            FraudFeatureRequest request,
            CountryCode homeCountry,
            LocalDateTime created,
            BigDecimal historicalAverage,
            long historicalCount
    ) {
        UserEntity user = TestFixtures.user(request.userId(), homeCountry, created.minusYears(2));
        MerchantEntity merchant = TestFixtures.merchant(request.merchantId(), MerchantCategory.TRAVEL, 0.5f);
        CardEntity card = TestFixtures.card(request.cardId(), CardType.DEBIT, created.minusDays(30));

        when(userService.getByIdOrThrow(request.userId())).thenReturn(user);
        when(merchantService.getByIdOrThrow(request.merchantId())).thenReturn(merchant);
        when(cardService.getByIdOrThrow(request.cardId())).thenReturn(card);
        when(transactionService.findAverageAmountByUserId(request.userId())).thenReturn(historicalAverage);
        when(transactionService.countByUserId(request.userId())).thenReturn(historicalCount);
        when(trustedDeviceService.existsById(request.deviceId())).thenReturn(true);
        when(transactionService.findIpRiskScoreByIpAddress(request.ipAddress())).thenReturn(0.4f);
        when(featureCacheService.getSecondsSinceLastTransaction(request.userId())).thenReturn(600L);
        when(featureCacheService.getUserTransactionCount5Min(request.userId())).thenReturn(1L);
        when(featureCacheService.getUserTransactionCount1Hour(request.userId())).thenReturn(2L);
        when(featureCacheService.getAmountVelocity1Hour(request.userId())).thenReturn(BigDecimal.valueOf(75));
        when(featureCacheService.getDistinctMerchantCount1Hour(request.userId())).thenReturn(1L);
    }
}
