package org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto;

import lombok.Builder;
import org.pdzsoftware.antifraudorchestrator.infrastructure.inbound.consumer.dto.TransactionCreatedMessage;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record PersistTransactionRequest(
        String transactionId,
        String userId,
        String cardId,
        String merchantId,
        String deviceId,
        BigDecimal amount,
        String countryCode,
        String ipAddress,
        LocalDateTime creationDateTime,
        PersistTransactionFeaturesRequest features,
        PersistTransactionPredictionRequest prediction
) {
    public static PersistTransactionRequest from(TransactionCreatedMessage message,
                                                  FraudFeatureResponse features,
                                                  FraudPredictionResponse prediction) {
        return PersistTransactionRequest.builder()
                .transactionId(message.transactionId())
                .userId(message.userId())
                .cardId(message.cardId())
                .merchantId(message.merchantId())
                .deviceId(message.deviceId())
                .amount(message.amount())
                .countryCode(message.countryCode().name())
                .ipAddress(message.ipAddress())
                .creationDateTime(message.creationDateTime())
                .features(PersistTransactionFeaturesRequest.from(features))
                .prediction(PersistTransactionPredictionRequest.from(prediction))
                .build();
    }
}
