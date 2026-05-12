package org.pdzsoftware.antifraudorchestrator.infrastructure.inbound.consumer;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.pdzsoftware.antifraudorchestrator.infrastructure.inbound.consumer.dto.TransactionCreatedMessage;
import org.pdzsoftware.antifraudorchestrator.application.usecase.ProcessTransactionUseCase;
import org.pdzsoftware.antifraudorchestrator.application.usecase.dto.ProcessTransactionInput;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionConsumer {
	private final ProcessTransactionUseCase processTransactionUseCase;

	@KafkaListener(
			topics = "#{@kafkaConsumerProperties.transactionsCreatedTopic}",
			groupId = "#{@kafkaConsumerProperties.consumerGroupId}",
			containerFactory = "kafkaListenerContainerFactory"
	)
	public void consume(@Valid TransactionCreatedMessage payload,
	                    @Header(KafkaHeaders.RECEIVED_KEY) String messageKey) {
		processTransactionUseCase.execute(new ProcessTransactionInput(payload, messageKey));
	}
}
