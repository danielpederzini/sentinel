package org.pdzsoftware.featuremanager.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pdzsoftware.featuremanager.infrastructure.inbound.controller.dto.FraudFeatureRequest;
import org.pdzsoftware.featuremanager.infrastructure.inbound.controller.dto.FraudFeatureResponse;
import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.entity.CardEntity;
import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.entity.MerchantEntity;
import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.entity.UserEntity;
import org.pdzsoftware.featuremanager.domain.exception.FeatureCalculationException;
import org.pdzsoftware.featuremanager.application.service.CardService;
import org.pdzsoftware.featuremanager.application.service.FeatureCacheService;
import org.pdzsoftware.featuremanager.application.service.MerchantService;
import org.pdzsoftware.featuremanager.application.service.TransactionService;
import org.pdzsoftware.featuremanager.application.service.TrustedDeviceService;
import org.pdzsoftware.featuremanager.application.service.UserService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class CalculateFraudFeaturesUseCase implements UseCase<FraudFeatureRequest, FraudFeatureResponse> {
    private final UserService userService;
    private final MerchantService merchantService;
    private final CardService cardService;
    private final TrustedDeviceService trustedDeviceService;
    private final TransactionService transactionService;

    private final FeatureCacheService featureCacheService;

    @Override
    public FraudFeatureResponse execute(FraudFeatureRequest input) {
        try {
            UserEntity userEntity = userService.getByIdOrThrow(input.userId());
            MerchantEntity merchantEntity = merchantService.getByIdOrThrow(input.merchantId());
            CardEntity cardEntity = cardService.getByIdOrThrow(input.cardId());

            BigDecimal averageAmount = transactionService.findAverageAmountByUserId(input.userId());
            float amountToAverageRatio = getAmountToAverageRatio(input.amount(), averageAmount);
            boolean hasCountryMismatch = !userEntity.getHomeCountryCode().equals(input.countryCode());
            boolean isDeviceTrusted = trustedDeviceService.existsById(input.deviceId());
            int hourOfDay = input.creationDateTime().getHour();
            long cardAgeDays = Duration.between(cardEntity.getCreationDateTime(), LocalDateTime.now()).toDays();
            float ipRiskScore = transactionService.findIpRiskScoreByIpAddress(input.ipAddress());

            long secondsSinceLastTransaction = featureCacheService.getSecondsSinceLastTransaction(input.userId());
            long transactionCount5Min = featureCacheService.getUserTransactionCount5Min(input.userId());
            long transactionCount1Hour = featureCacheService.getUserTransactionCount1Hour(input.userId());

            featureCacheService.recordUserTransaction(input.userId(), input.transactionId());

            FraudFeatureResponse fraudFeatureResponse = FraudFeatureResponse.builder()
                    .transactionId(input.transactionId())
                    .amount(input.amount())
                    .userAverageAmount(averageAmount)
                    .userTransactionCount5Min(transactionCount5Min)
                    .userTransactionCount1Hour(transactionCount1Hour)
                    .secondsSinceLastTransaction(secondsSinceLastTransaction)
                    .merchantRiskScore(merchantEntity.getRiskScore())
                    .isDeviceTrusted(isDeviceTrusted)
                    .hasCountryMismatch(hasCountryMismatch)
                    .amountToAverageRatio(amountToAverageRatio)
                    .hourOfDay(hourOfDay)
                    .ipRiskScore(ipRiskScore)
                    .cardAgeDays(cardAgeDays)
                    .build();

            log.info("Calculated fraud features for transaction {}", input.transactionId());
            return fraudFeatureResponse;
        } catch (RuntimeException exception) {
            throw new FeatureCalculationException(String.format(
                    "Failed to calculate fraud features for transaction %s", input.transactionId()), exception);
        }
    }

    private static float getAmountToAverageRatio(BigDecimal amount, BigDecimal averageAmount) {
        BigDecimal divisor = BigDecimal.ONE.max(averageAmount);
        return amount.divide(divisor, RoundingMode.HALF_UP).floatValue();
    }
}
