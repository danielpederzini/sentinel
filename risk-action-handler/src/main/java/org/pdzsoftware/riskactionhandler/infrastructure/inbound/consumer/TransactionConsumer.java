package org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.TransactionScoredMessage;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionConsumer {
	@KafkaListener(
			topics = "#{@kafkaConsumerProperties.transactionsScoredTopic}",
			groupId = "#{@kafkaConsumerProperties.consumerGroupId}",
			containerFactory = "kafkaListenerContainerFactory"
	)
	public void consume(@Valid TransactionScoredMessage payload,
	                    @Header(KafkaHeaders.RECEIVED_KEY) String messageKey) {
		log.info("Received scored transaction with key {}: {}", messageKey, payload);
	}
}
