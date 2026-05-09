package org.pdzsoftware.featuremanager.mapper;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
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
        UserEntity userEntity = UserEntity.builder()
                .id(request.userId())
                .build();

        CardEntity cardEntity = CardEntity.builder()
                .id(request.cardId())
                .build();

        MerchantEntity merchantEntity = MerchantEntity.builder()
                .id(request.merchantId())
                .build();

        TrustedDeviceEntity trustedDeviceEntity = null;
        if (!StringUtils.isBlank(request.deviceId())) {
            trustedDeviceEntity = TrustedDeviceEntity.builder()
                    .id(request.deviceId())
                    .build();
        }

        return TransactionEntity.builder()
                .id(request.transactionId())
                .amount(request.amount())
                .countryCode(request.countryCode())
                .ipAddress(request.ipAddress())
                .creationDateTime(request.creationDateTime())
                .user(userEntity)
                .card(cardEntity)
                .merchant(merchantEntity)
                .trustedDevice(trustedDeviceEntity)
                .build();
    }

    public static TransactionPersistenceResult toResult(TransactionEntity entity) {
        return new TransactionPersistenceResult(
                entity.getId()
        );
    }
}
