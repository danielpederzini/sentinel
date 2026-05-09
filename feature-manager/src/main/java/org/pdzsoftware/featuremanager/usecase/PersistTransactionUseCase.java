package org.pdzsoftware.featuremanager.usecase;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.pdzsoftware.featuremanager.dto.TransactionPersistenceRequest;
import org.pdzsoftware.featuremanager.dto.TransactionPersistenceResult;
import org.pdzsoftware.featuremanager.entity.TransactionEntity;
import org.pdzsoftware.featuremanager.exception.CardNotFoundException;
import org.pdzsoftware.featuremanager.exception.DeviceNotFoundException;
import org.pdzsoftware.featuremanager.exception.MerchantNotFoundException;
import org.pdzsoftware.featuremanager.exception.UserNotFoundException;
import org.pdzsoftware.featuremanager.mapper.TransactionMapper;
import org.pdzsoftware.featuremanager.service.CardService;
import org.pdzsoftware.featuremanager.service.FeatureCacheService;
import org.pdzsoftware.featuremanager.service.MerchantService;
import org.pdzsoftware.featuremanager.service.TransactionService;
import org.pdzsoftware.featuremanager.service.TrustedDeviceService;
import org.pdzsoftware.featuremanager.service.UserService;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
public class PersistTransactionUseCase implements UseCase<TransactionPersistenceRequest, TransactionPersistenceResult> {
    private final UserService userService;
    private final MerchantService merchantService;
    private final CardService cardService;
    private final TrustedDeviceService trustedDeviceService;
    private final TransactionService transactionService;
    private final FeatureCacheService featureCacheService;

    @Override
    public TransactionPersistenceResult execute(TransactionPersistenceRequest input) {
        validateReferences(input);

        TransactionEntity transactionEntity = TransactionMapper.toEntity(input);
        TransactionEntity persistedTransaction = transactionService.save(transactionEntity);

        recordTransactionInCache(input);

        return TransactionMapper.toResult(persistedTransaction);
    }

    private void validateReferences(TransactionPersistenceRequest input) {
        if (!userService.existsById(input.userId())) {
            throw new UserNotFoundException(String.format("User with ID %s not found", input.userId()));
        }

        if (!cardService.existsById(input.cardId())) {
            throw new CardNotFoundException(String.format("Card with ID %s not found", input.cardId()));
        }

        if (!merchantService.existsById(input.merchantId())) {
            throw new MerchantNotFoundException(String.format("Merchant with ID %s not found", input.merchantId()));
        }

        boolean hasDeviceId = !StringUtils.isBlank(input.deviceId());
        if (hasDeviceId && !trustedDeviceService.existsById(input.deviceId())) {
            throw new DeviceNotFoundException(String.format("Trusted device with ID %s not found", input.deviceId()));
        }
    }

    private void recordTransactionInCache(TransactionPersistenceRequest input) {
        long transactionTimestamp = input.creationDateTime().toInstant(ZoneOffset.UTC).toEpochMilli();
        featureCacheService.recordUserTransaction(input.userId(), input.transactionId(), transactionTimestamp);
    }
}
