package org.pdzsoftware.featuremanager.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pdzsoftware.featuremanager.application.service.CardService;
import org.pdzsoftware.featuremanager.application.service.MerchantService;
import org.pdzsoftware.featuremanager.application.service.TransactionService;
import org.pdzsoftware.featuremanager.application.service.TrustedDeviceService;
import org.pdzsoftware.featuremanager.application.service.UserService;
import org.pdzsoftware.featuremanager.domain.exception.CardNotFoundException;
import org.pdzsoftware.featuremanager.domain.exception.MerchantNotFoundException;
import org.pdzsoftware.featuremanager.domain.exception.TrustedDeviceNotFoundException;
import org.pdzsoftware.featuremanager.domain.exception.UserNotFoundException;
import org.pdzsoftware.featuremanager.infrastructure.inbound.controller.dto.FeaturesRequest;
import org.pdzsoftware.featuremanager.infrastructure.inbound.controller.dto.PersistTransactionRequest;
import org.pdzsoftware.featuremanager.infrastructure.inbound.controller.dto.PredictionRequest;
import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.entity.CardEntity;
import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.entity.MerchantEntity;
import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.entity.TransactionEntity;
import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.entity.TransactionFeatureVectorEntity;
import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.entity.TransactionPredictionEntity;
import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.entity.TrustedDeviceEntity;
import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class PersistTransactionUseCase {
    private final UserService userService;
    private final MerchantService merchantService;
    private final CardService cardService;
    private final TrustedDeviceService trustedDeviceService;
    private final TransactionService transactionService;

    public String execute(PersistTransactionRequest input) {
        checkIdsExist(input);

        UserEntity userEntity = UserEntity.fromId(input.userId());
        MerchantEntity merchantEntity = MerchantEntity.fromId(input.merchantId());
        CardEntity cardEntity = CardEntity.fromId(input.cardId());
        TrustedDeviceEntity deviceEntity = TrustedDeviceEntity.fromNullableId(input.deviceId());

        TransactionEntity transactionEntity = TransactionEntity.builder()
                .id(input.transactionId())
                .amount(input.amount())
                .countryCode(input.countryCode())
                .ipAddress(input.ipAddress())
                .creationDateTime(input.creationDateTime())
                .user(userEntity)
                .merchant(merchantEntity)
                .card(cardEntity)
                .trustedDevice(deviceEntity)
                .build();

        TransactionFeatureVectorEntity featureEntity = buildFeatureVector(input);
        TransactionPredictionEntity predictionEntity = buildPrediction(input);

        featureEntity.setTransaction(transactionEntity);
        predictionEntity.setTransaction(transactionEntity);

        transactionEntity.setFeatureVector(featureEntity);
        transactionEntity.setPrediction(predictionEntity);

        String savedId = transactionService.save(transactionEntity);
        log.debug("Transaction {} persisted successfully", savedId);
        return savedId;
    }

    private void checkIdsExist(PersistTransactionRequest input) {
        if (!userService.existsById(input.userId())) {
            throw new UserNotFoundException(String.format(
                    "User with id %s not found", input.userId()));
        }

        if (!merchantService.existsById(input.merchantId())) {
            throw new MerchantNotFoundException(String.format(
                    "Merchant with id %s not found", input.merchantId()));
        }

        if (!cardService.existsById(input.cardId())) {
            throw new CardNotFoundException(String.format(
                    "Card with id %s not found", input.cardId()));
        }

        if (StringUtils.hasText(input.deviceId()) && !trustedDeviceService.existsById(input.deviceId())) {
            throw new TrustedDeviceNotFoundException(String.format(
                    "Device with id %s not found", input.deviceId()));
        }
    }

    private TransactionFeatureVectorEntity buildFeatureVector(PersistTransactionRequest input) {
        FeaturesRequest features = input.features();

        return TransactionFeatureVectorEntity.builder()
                .transactionId(input.transactionId())
                .amount(input.amount())
                .userAverageAmount(features.userAverageAmount())
                .userHistoricalTransactionCount(features.userHistoricalTransactionCount())
                .userTransactionCount5Min(features.userTransactionCount5Min())
                .userTransactionCount1Hour(features.userTransactionCount1Hour())
                .secondsSinceLastTransaction(features.secondsSinceLastTransaction())
                .merchantRiskScore(features.merchantRiskScore())
                .isDeviceTrusted(features.isDeviceTrusted())
                .hasCountryMismatch(features.hasCountryMismatch())
                .amountToAverageRatio(features.amountToAverageRatio())
                .hourOfDay(features.hourOfDay())
                .ipRiskScore(features.ipRiskScore())
                .cardAgeDays(features.cardAgeDays())
                .amountVelocity1Hour(features.amountVelocity1Hour())
                .userAccountAgeDays(features.userAccountAgeDays())
                .dayOfWeek(features.dayOfWeek())
                .merchantCategory(features.merchantCategory())
                .cardType(features.cardType())
                .distinctMerchantCount1Hour(features.distinctMerchantCount1Hour())
                .logAmount(features.logAmount())
                .logSecondsSinceLastTransaction(features.logSecondsSinceLastTransaction())
                .logVelocity1Hour(features.logVelocity1Hour())
                .amountTimesMerchantRisk(features.amountTimesMerchantRisk())
                .riskScoreProduct(features.riskScoreProduct())
                .ipDeviceRisk(features.ipDeviceRisk())
                .countryIpRisk(features.countryIpRisk())
                .velocityAmountInteraction(features.velocityAmountInteraction())
                .recencyVelocity(features.recencyVelocity())
                .amountDeviation(features.amountDeviation())
                .isNight(features.isNight())
                .velocityIntensity(features.velocityIntensity())
                .build();
    }

    private TransactionPredictionEntity buildPrediction(PersistTransactionRequest input) {
        PredictionRequest prediction = input.prediction();

        return TransactionPredictionEntity.builder()
                .transactionId(input.transactionId())
                .fraudProbability(prediction.fraudProbability())
                .riskLevel(prediction.riskLevel())
                .modelVersion(prediction.modelVersion())
                .build();
    }
}
