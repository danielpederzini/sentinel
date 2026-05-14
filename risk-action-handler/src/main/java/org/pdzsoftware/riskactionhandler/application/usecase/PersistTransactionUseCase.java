package org.pdzsoftware.riskactionhandler.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pdzsoftware.riskactionhandler.application.service.CardService;
import org.pdzsoftware.riskactionhandler.application.service.MerchantService;
import org.pdzsoftware.riskactionhandler.application.service.TransactionService;
import org.pdzsoftware.riskactionhandler.application.service.TrustedDeviceService;
import org.pdzsoftware.riskactionhandler.application.service.UserService;
import org.pdzsoftware.riskactionhandler.domain.exception.CardNotFoundException;
import org.pdzsoftware.riskactionhandler.domain.exception.DeviceNotFoundException;
import org.pdzsoftware.riskactionhandler.domain.exception.MerchantNotFoundException;
import org.pdzsoftware.riskactionhandler.domain.exception.UserNotFoundException;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.TransactionScoredMessage;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.entity.CardEntity;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.entity.MerchantEntity;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.entity.TransactionEntity;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.entity.TransactionFeatureVectorEntity;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.entity.TransactionPredictionEntity;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.entity.TrustedDeviceEntity;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class PersistTransactionUseCase implements VoidUseCase<TransactionScoredMessage> {
    private final UserService userService;
    private final MerchantService merchantService;
    private final CardService cardService;
    private final TrustedDeviceService trustedDeviceService;
    private final TransactionService transactionService;

    @Override
    public void execute(TransactionScoredMessage input) {
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

        TransactionFeatureVectorEntity featureEntity = TransactionFeatureVectorEntity.from(input);
        TransactionPredictionEntity predictionEntity = TransactionPredictionEntity.from(input);

        featureEntity.setTransaction(transactionEntity);
        predictionEntity.setTransaction(transactionEntity);

        transactionEntity.setFeatureVector(featureEntity);
        transactionEntity.setPrediction(predictionEntity);

        String savedId = transactionService.save(transactionEntity);
        log.info("Transaction {} persisted successfully", savedId);
    }

    private void checkIdsExist(TransactionScoredMessage input) {
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
            throw new DeviceNotFoundException(String.format(
                    "Device with id %s not found", input.deviceId()));
        }
    }
}
