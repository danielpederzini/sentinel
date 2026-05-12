package org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.producer.dto;

import lombok.Builder;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto.FraudFeatureResponse;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto.FraudPredictionResponse;
import org.pdzsoftware.antifraudorchestrator.infrastructure.inbound.consumer.dto.TransactionCreatedMessage;
import org.pdzsoftware.antifraudorchestrator.domain.enums.CountryCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record TransactionScoredMessage(
        String transactionId,
        String userId,
        String cardId,
        String merchantId,
        String deviceId,
        BigDecimal amount,
        CountryCode countryCode,
        String ipAddress,
        LocalDateTime creationDateTime,
        FraudFeaturesMessage featuresMessage,
        FraudPredictionMessage predictionMessage
) {
    public static TransactionScoredMessage from(TransactionCreatedMessage transactionCreatedMessage,
                                                FraudFeatureResponse fraudFeatureResponse,
                                                FraudPredictionResponse fraudPredictionResponse) {
        return TransactionScoredMessage.builder()
                .transactionId(transactionCreatedMessage.transactionId())
                .userId(transactionCreatedMessage.userId())
                .cardId(transactionCreatedMessage.cardId())
                .merchantId(transactionCreatedMessage.merchantId())
                .deviceId(transactionCreatedMessage.deviceId())
                .amount(transactionCreatedMessage.amount())
                .countryCode(transactionCreatedMessage.countryCode())
                .ipAddress(transactionCreatedMessage.ipAddress())
                .creationDateTime(transactionCreatedMessage.creationDateTime())
                .featuresMessage(FraudFeaturesMessage.from(fraudFeatureResponse))
                .predictionMessage(FraudPredictionMessage.from(fraudPredictionResponse))
                .build();
    }
}
