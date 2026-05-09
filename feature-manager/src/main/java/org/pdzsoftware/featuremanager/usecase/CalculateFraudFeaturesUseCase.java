package org.pdzsoftware.featuremanager.usecase;

import lombok.RequiredArgsConstructor;
import org.pdzsoftware.featuremanager.dto.FraudFeatureRequest;
import org.pdzsoftware.featuremanager.dto.FraudFeatureResult;
import org.pdzsoftware.featuremanager.entity.CardEntity;
import org.pdzsoftware.featuremanager.entity.MerchantEntity;
import org.pdzsoftware.featuremanager.entity.UserEntity;
import org.pdzsoftware.featuremanager.exception.MerchantNotFoundException;
import org.pdzsoftware.featuremanager.exception.UserNotFoundException;
import org.pdzsoftware.featuremanager.service.CardService;
import org.pdzsoftware.featuremanager.service.FeatureCacheService;
import org.pdzsoftware.featuremanager.service.MerchantService;
import org.pdzsoftware.featuremanager.service.TransactionService;
import org.pdzsoftware.featuremanager.service.TrustedDeviceService;
import org.pdzsoftware.featuremanager.service.UserService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

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
        UserEntity userEntity = userService.findById(input.userId()).orElseThrow(() ->
                new UserNotFoundException(String.format("User with ID %s not found", input.userId())));

        MerchantEntity merchantEntity = merchantService.findById(input.merchantId()).orElseThrow(() ->
                new MerchantNotFoundException(String.format("Merchant with ID %s not found", input.merchantId())));

        CardEntity cardEntity = cardService.findById(input.cardId()).orElseThrow(() ->
                new RuntimeException(String.format("Card with ID %s not found", input.cardId())));

        BigDecimal averageAmount = transactionService.findAverageAmountByUserId(input.userId());
        float amountToAverageRatio = input.amount().divide(averageAmount, RoundingMode.HALF_UP).floatValue();
        boolean hasCountryMismatch = !userEntity.getHomeCountryCode().equals(input.countryCode());
        boolean isDeviceTrusted = trustedDeviceService.existsById(input.deviceId());
        int hourOfDay = input.creationDateTime().getHour();
        long cardAgeDays = Duration.between(cardEntity.getCreationDateTime(), LocalDateTime.now()).toDays();

        long transactionCount5Min = featureCacheService.getUserTransactionCount5Min(input.userId());
        long transactionCount1Hour = featureCacheService.getUserTransactionCount1Hour(input.userId());
        long secondsSinceLastTransaction = featureCacheService.getSecondsSinceLastTransaction(input.userId());

        recordTransactionInCache(input);

        return FraudFeatureResult.builder()
                .transactionId(input.transactionId())
                .amount(input.amount())
                .userAverageAmount(averageAmount)
                .userTransactionCount5Min(transactionCount5Min)
                .userTransactionCound1Hour(transactionCount1Hour)
                .secondsSinceLastTransaction(secondsSinceLastTransaction)
                .merchantRiskScore(merchantEntity.getRiskScore())
                .isDeviceTrusted(isDeviceTrusted)
                .hasCountryMismatch(hasCountryMismatch)
                .amountToAverageRatio(amountToAverageRatio)
                .hourOfDay(hourOfDay)
                .cardAgeDays(cardAgeDays)
                .build();
    }

    private void recordTransactionInCache(FraudFeatureRequest input) {
        LocalDateTime transactionDateTime = input.creationDateTime();
        long transactionTimestamp = transactionDateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
        featureCacheService.recordUserTransaction(input.userId(), input.transactionId(), transactionTimestamp);
    }
}
