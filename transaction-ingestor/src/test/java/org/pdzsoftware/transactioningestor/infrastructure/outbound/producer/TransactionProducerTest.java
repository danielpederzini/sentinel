package org.pdzsoftware.transactioningestor.infrastructure.outbound.producer;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pdzsoftware.transactioningestor.infrastructure.config.properties.KafkaProducerProperties;
import org.pdzsoftware.transactioningestor.infrastructure.inbound.controller.dto.TransactionIngestionRequest;
import org.pdzsoftware.transactioningestor.support.TestFixtures;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import reactor.test.StepVerifier;

import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pdzsoftware.transactioningestor.support.TestConstants.KAFKA_OFFSET;
import static org.pdzsoftware.transactioningestor.support.TestConstants.KAFKA_PARTITION;
import static org.pdzsoftware.transactioningestor.support.TestConstants.KAFKA_TRANSACTIONS_CREATED_TOPIC;
import static org.pdzsoftware.transactioningestor.support.TestConstants.KAFKA_TOPIC_NAME;
import static org.pdzsoftware.transactioningestor.support.TestConstants.TRANSACTION_ID;

@ExtendWith(MockitoExtension.class)
class TransactionProducerTest {

    @Mock
    private KafkaTemplate<String, TransactionIngestionRequest> kafkaTemplate;

    @Mock
    private KafkaProducerProperties kafkaProducerProperties;

    @InjectMocks
    private TransactionProducer transactionProducer;

    @Test
    void publish_shouldSendToConfiguredTopicWithTransactionKey() {
        TransactionIngestionRequest request = TestFixtures.ingestionRequestWithId();
        SendResult<String, TransactionIngestionRequest> sendResult = sendResult();
        when(kafkaProducerProperties.getTransactionsCreatedTopic()).thenReturn(KAFKA_TRANSACTIONS_CREATED_TOPIC);
        when(kafkaTemplate.send(KAFKA_TRANSACTIONS_CREATED_TOPIC, TRANSACTION_ID, request))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        StepVerifier.create(transactionProducer.publish(request))
                .expectNext(sendResult)
                .verifyComplete();

        verify(kafkaTemplate).send(KAFKA_TRANSACTIONS_CREATED_TOPIC, TRANSACTION_ID, request);
        verify(kafkaProducerProperties).getTransactionsCreatedTopic();
    }

    private static SendResult<String, TransactionIngestionRequest> sendResult() {
        TopicPartition topicPartition = new TopicPartition(KAFKA_TOPIC_NAME, KAFKA_PARTITION);
        RecordMetadata metadata = new RecordMetadata(topicPartition, 0L, KAFKA_OFFSET, 0L, 0, 0);
        return new SendResult<>(null, metadata);
    }
}
