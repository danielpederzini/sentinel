package org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.producer;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pdzsoftware.antifraudorchestrator.infrastructure.config.properties.KafkaProducerProperties;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.producer.dto.TransactionScoredMessage;
import org.pdzsoftware.antifraudorchestrator.support.TestFixtures;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pdzsoftware.antifraudorchestrator.support.TestConstants.KAFKA_OFFSET;
import static org.pdzsoftware.antifraudorchestrator.support.TestConstants.KAFKA_PARTITION;
import static org.pdzsoftware.antifraudorchestrator.support.TestConstants.KAFKA_TOPIC_NAME;
import static org.pdzsoftware.antifraudorchestrator.support.TestConstants.KAFKA_TRANSACTIONS_SCORED_TOPIC;
import static org.pdzsoftware.antifraudorchestrator.support.TestConstants.TRANSACTION_ID;

@ExtendWith(MockitoExtension.class)
class TransactionProducerTest {

    @Mock
    private KafkaTemplate<String, TransactionScoredMessage> kafkaTemplate;

    @Mock
    private KafkaProducerProperties kafkaProducerProperties;

    @InjectMocks
    private TransactionProducer transactionProducer;

    @Test
    void publish_shouldSendToConfiguredTopicWithTransactionKey() throws ExecutionException, InterruptedException {
        TransactionScoredMessage message = TransactionScoredMessage.from(
                TestFixtures.transactionCreatedMessage(),
                TestFixtures.fraudFeatureResponse(),
                TestFixtures.fraudPredictionResponse());
        SendResult<String, TransactionScoredMessage> sendResult = sendResult();
        when(kafkaProducerProperties.getTransactionsScoredTopic()).thenReturn(KAFKA_TRANSACTIONS_SCORED_TOPIC);
        when(kafkaTemplate.send(KAFKA_TRANSACTIONS_SCORED_TOPIC, TRANSACTION_ID, message))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        SendResult<String, TransactionScoredMessage> result = transactionProducer.publish(message).get();

        assertThat(result).isSameAs(sendResult);
        verify(kafkaTemplate).send(KAFKA_TRANSACTIONS_SCORED_TOPIC, TRANSACTION_ID, message);
        verify(kafkaProducerProperties).getTransactionsScoredTopic();
    }

    private static SendResult<String, TransactionScoredMessage> sendResult() {
        TopicPartition topicPartition = new TopicPartition(KAFKA_TOPIC_NAME, KAFKA_PARTITION);
        RecordMetadata metadata = new RecordMetadata(topicPartition, 0L, KAFKA_OFFSET, 0L, 0, 0);
        return new SendResult<>(null, metadata);
    }
}
