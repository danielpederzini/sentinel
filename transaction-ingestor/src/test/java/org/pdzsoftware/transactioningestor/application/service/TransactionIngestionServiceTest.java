package org.pdzsoftware.transactioningestor.application.service;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pdzsoftware.transactioningestor.domain.exception.KafkaPublishException;
import org.pdzsoftware.transactioningestor.infrastructure.inbound.controller.dto.TransactionIngestionRequest;
import org.pdzsoftware.transactioningestor.infrastructure.inbound.controller.dto.TransactionIngestionResponse;
import org.pdzsoftware.transactioningestor.infrastructure.outbound.producer.TransactionProducer;
import org.pdzsoftware.transactioningestor.support.TestFixtures;
import org.springframework.kafka.support.SendResult;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pdzsoftware.transactioningestor.support.TestConstants.BLANK_TRANSACTION_ID;
import static org.pdzsoftware.transactioningestor.support.TestConstants.KAFKA_OFFSET;
import static org.pdzsoftware.transactioningestor.support.TestConstants.KAFKA_PARTITION;
import static org.pdzsoftware.transactioningestor.support.TestConstants.KAFKA_TOPIC_NAME;
import static org.pdzsoftware.transactioningestor.support.TestConstants.KAFKA_PUBLISH_ERROR_MESSAGE;
import static org.pdzsoftware.transactioningestor.support.TestConstants.TRANSACTION_ID;
import static org.pdzsoftware.transactioningestor.support.TestConstants.UNEXPECTED_ERROR_MESSAGE;

@ExtendWith(MockitoExtension.class)
class TransactionIngestionServiceTest {

    @Mock
    private TransactionProducer transactionProducer;

    @InjectMocks
    private TransactionIngestionService transactionIngestionService;

    @Test
    void ingest_shouldReturnExistingTransactionId_whenProvided() {
        TransactionIngestionRequest request = TestFixtures.ingestionRequestWithId();
        when(transactionProducer.publish(any(TransactionIngestionRequest.class)))
                .thenReturn(Mono.just(sendResult()));

        StepVerifier.create(transactionIngestionService.ingest(request))
                .assertNext(response -> assertThat(response.transactionId()).isEqualTo(TRANSACTION_ID))
                .verifyComplete();

        ArgumentCaptor<TransactionIngestionRequest> captor = ArgumentCaptor.forClass(TransactionIngestionRequest.class);
        verify(transactionProducer).publish(captor.capture());
        assertThat(captor.getValue().transactionId()).isEqualTo(TRANSACTION_ID);
    }

    @Test
    void ingest_shouldGenerateTransactionId_whenMissing() {
        TransactionIngestionRequest request = TestFixtures.ingestionRequest(BLANK_TRANSACTION_ID);
        when(transactionProducer.publish(any(TransactionIngestionRequest.class)))
                .thenReturn(Mono.just(sendResult()));

        StepVerifier.create(transactionIngestionService.ingest(request))
                .assertNext(response -> assertThat(response.transactionId()).isNotBlank())
                .verifyComplete();

        ArgumentCaptor<TransactionIngestionRequest> captor = ArgumentCaptor.forClass(TransactionIngestionRequest.class);
        verify(transactionProducer).publish(captor.capture());
        assertThat(captor.getValue().transactionId()).isNotBlank();
    }

    @Test
    void ingest_shouldMapPublishFailureToKafkaPublishException() {
        TransactionIngestionRequest request = TestFixtures.ingestionRequestWithId();
        when(transactionProducer.publish(any(TransactionIngestionRequest.class)))
                .thenReturn(Mono.error(new RuntimeException(UNEXPECTED_ERROR_MESSAGE)));

        StepVerifier.create(transactionIngestionService.ingest(request))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(KafkaPublishException.class);
                    assertThat(error).hasMessageContaining(KAFKA_PUBLISH_ERROR_MESSAGE);
                    assertThat(error.getCause()).hasMessage(UNEXPECTED_ERROR_MESSAGE);
                })
                .verify();
    }

    private static SendResult<String, TransactionIngestionRequest> sendResult() {
        TopicPartition topicPartition = new TopicPartition(KAFKA_TOPIC_NAME, KAFKA_PARTITION);
        RecordMetadata metadata = new RecordMetadata(
                topicPartition,
                0L,
                KAFKA_OFFSET,
                0L,
                0,
                0);
        return new SendResult<>(null, metadata);
    }
}
