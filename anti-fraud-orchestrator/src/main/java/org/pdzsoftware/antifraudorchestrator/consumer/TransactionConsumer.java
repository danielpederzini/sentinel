package org.pdzsoftware.antifraudorchestrator.consumer;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.pdzsoftware.antifraudorchestrator.dto.TransactionCreatedMessage;
import org.pdzsoftware.antifraudorchestrator.usecase.ProcessTransactionInput;
import org.pdzsoftware.antifraudorchestrator.usecase.ProcessTransactionUseCase;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionConsumer {
	private final ProcessTransactionUseCase processTransactionUseCase;

	@KafkaListener(
			topics = "#{@kafkaProperties.transactionsCreatedTopic}",
			groupId = "#{@kafkaProperties.consumerGroupId}",
			containerFactory = "kafkaListenerContainerFactory"
	)
	public void consume(@Valid TransactionCreatedMessage payload,
	                    @Header(KafkaHeaders.RECEIVED_KEY) String messageKey) {
		processTransactionUseCase.execute(new ProcessTransactionInput(payload, messageKey));
	}
}
