package org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto;

import org.pdzsoftware.antifraudorchestrator.infrastructure.inbound.consumer.dto.TransactionCreatedMessage;
import org.pdzsoftware.antifraudorchestrator.domain.enums.CountryCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FraudFeatureRequest(
		String transactionId,
		String userId,
		String cardId,
		String merchantId,
		String deviceId,
		BigDecimal amount,
		CountryCode countryCode,
		String ipAddress,
		LocalDateTime creationDateTime
) {
	public static FraudFeatureRequest from(TransactionCreatedMessage message) {
		return new FraudFeatureRequest(
				message.transactionId(),
				message.userId(),
				message.cardId(),
				message.merchantId(),
				message.deviceId(),
				message.amount(),
				message.countryCode(),
				message.ipAddress(),
				message.creationDateTime()
		);
	}
}
