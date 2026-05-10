package org.pdzsoftware.featuremanager.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pdzsoftware.featuremanager.dto.FraudFeatureRequest;
import org.pdzsoftware.featuremanager.dto.FraudFeatureResult;
import org.pdzsoftware.featuremanager.entity.CardEntity;
import org.pdzsoftware.featuremanager.entity.MerchantEntity;
import org.pdzsoftware.featuremanager.entity.UserEntity;
import org.pdzsoftware.featuremanager.exception.FeatureCalculationException;
import org.pdzsoftware.featuremanager.service.CardService;
import org.pdzsoftware.featuremanager.service.FeatureCacheService;
import org.pdzsoftware.featuremanager.service.MerchantService;
import org.pdzsoftware.featuremanager.service.TransactionService;
import org.pdzsoftware.featuremanager.service.TrustedDeviceService;
import org.pdzsoftware.featuremanager.service.UserService;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class CalculateFraudFeaturesUseCase implements UseCase<FraudFeatureRequest, FraudFeatureResult> {
    private final UserService userService;
    private final MerchantService merchantService;
    private final CardService cardService;
    private final TrustedDeviceService trustedDeviceService;
    private final TransactionService transactionService;

    private final FeatureCacheService featureCacheService;

    @Override
    public FraudFeatureResult execute(FraudFeatureRequest input) {
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

            long secondsSinceLastTransaction = featureCacheService.getSecondsSinceLastTransaction(input.userId());
            long transactionCount5Min = featureCacheService.getUserTransactionCount5Min(input.userId());
            long transactionCount1Hour = featureCacheService.getUserTransactionCount1Hour(input.userId());

            featureCacheService.recordUserTransaction(input.userId(), input.transactionId());

            FraudFeatureResult fraudFeatureResult = FraudFeatureResult.builder()
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
                    .cardAgeDays(cardAgeDays)
                    .build();

            log.info("Calculated fraud features for transaction {}", input.transactionId());
            return fraudFeatureResult;
        } catch (ResponseStatusException | IllegalArgumentException exception) {
            throw new FeatureCalculationException(String.format(
                    "Failed to calculate fraud features for transaction %s", input.transactionId()), exception);
        }
    }

    private static float getAmountToAverageRatio(BigDecimal amount, BigDecimal averageAmount) {
        BigDecimal divisor = BigDecimal.ONE.max(averageAmount);
        return amount.divide(divisor, RoundingMode.HALF_UP).floatValue();
    }
}
