package org.pdzsoftware.featuremanager.mapper;

import lombok.experimental.UtilityClass;
import org.pdzsoftware.featuremanager.dto.TransactionPersistenceRequest;
import org.pdzsoftware.featuremanager.dto.TransactionPersistenceResult;
import org.pdzsoftware.featuremanager.entity.CardEntity;
import org.pdzsoftware.featuremanager.entity.MerchantEntity;
import org.pdzsoftware.featuremanager.entity.TransactionEntity;
import org.pdzsoftware.featuremanager.entity.TrustedDeviceEntity;
import org.pdzsoftware.featuremanager.entity.UserEntity;

@UtilityClass
public final class TransactionMapper {
    public static TransactionEntity toEntity(TransactionPersistenceRequest request) {
        return TransactionEntity.builder()
                .id(request.transactionId())
                .amount(request.amount())
                .countryCode(request.countryCode())
                .ipAddress(request.ipAddress())
                .creationDateTime(request.creationDateTime())
                .user(UserEntity.builder()
                        .id(request.userId())
                        .build())
                .card(CardEntity.builder()
                        .id(request.cardId())
                        .build())
                .merchant(MerchantEntity.builder()
                        .id(request.merchantId())
                        .build())
                .trustedDevice(TrustedDeviceEntity.builder()
                        .id(request.deviceId())
                        .build())
                .build();
    }

    public static TransactionPersistenceResult toResult(TransactionEntity entity) {
        return new TransactionPersistenceResult(
                entity.getId()
        );
    }
}
