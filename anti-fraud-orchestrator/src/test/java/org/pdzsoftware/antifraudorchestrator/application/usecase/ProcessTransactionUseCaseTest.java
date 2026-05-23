package org.pdzsoftware.antifraudorchestrator.application.usecase;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pdzsoftware.antifraudorchestrator.application.usecase.dto.ProcessTransactionInput;
import org.pdzsoftware.antifraudorchestrator.domain.exception.TransactionOrchestrationException;
import org.pdzsoftware.antifraudorchestrator.infrastructure.inbound.consumer.dto.TransactionCreatedMessage;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.FeatureManagerClient;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.InferenceEngineClient;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto.FraudFeatureResponse;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto.FraudPredictionResponse;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto.PersistTransactionRequest;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.producer.TransactionProducer;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.producer.dto.TransactionScoredMessage;
import org.pdzsoftware.antifraudorchestrator.support.TestFixtures;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.pdzsoftware.antifraudorchestrator.support.TestConstants.KAFKA_OFFSET;
import static org.pdzsoftware.antifraudorchestrator.support.TestConstants.KAFKA_PARTITION;
import static org.pdzsoftware.antifraudorchestrator.support.TestConstants.KAFKA_TOPIC_NAME;
import static org.pdzsoftware.antifraudorchestrator.support.TestConstants.MESSAGE_KEY;
import static org.pdzsoftware.antifraudorchestrator.support.TestConstants.ORCHESTRATION_ERROR_PREFIX;
import static org.pdzsoftware.antifraudorchestrator.support.TestConstants.REST_CLIENT_FAILURE_MESSAGE;
import static org.pdzsoftware.antifraudorchestrator.support.TestConstants.TRANSACTION_ID;

@ExtendWith(MockitoExtension.class)
class ProcessTransactionUseCaseTest {

    @Mock
    private FeatureManagerClient featureManagerClient;
    @Mock
    private InferenceEngineClient inferenceEngineClient;
    @Mock
    private TransactionProducer transactionProducer;

    @InjectMocks
    private ProcessTransactionUseCase processTransactionUseCase;

    @Test
    void execute_shouldOrchestrateFeatureScoringPersistenceAndPublish() {
        TransactionCreatedMessage payload = TestFixtures.transactionCreatedMessage();
        ProcessTransactionInput input = new ProcessTransactionInput(payload, MESSAGE_KEY);
        FraudFeatureResponse features = TestFixtures.fraudFeatureResponse();
        FraudPredictionResponse prediction = TestFixtures.fraudPredictionResponse();

        when(featureManagerClient.calculateFraudFeatures(payload)).thenReturn(features);
        when(inferenceEngineClient.scoreTransaction(features)).thenReturn(prediction);
        when(transactionProducer.publish(any(TransactionScoredMessage.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult()));

        processTransactionUseCase.execute(input);

        InOrder inOrder = inOrder(featureManagerClient, inferenceEngineClient, transactionProducer);
        inOrder.verify(featureManagerClient).calculateFraudFeatures(payload);
        inOrder.verify(inferenceEngineClient).scoreTransaction(features);
        inOrder.verify(featureManagerClient).persistTransaction(any(PersistTransactionRequest.class));
        inOrder.verify(transactionProducer).publish(any(TransactionScoredMessage.class));
    }

    @Test
    void execute_shouldWrapFailuresInTransactionOrchestrationException() {
        TransactionCreatedMessage payload = TestFixtures.transactionCreatedMessage();
        ProcessTransactionInput input = new ProcessTransactionInput(payload, MESSAGE_KEY);
        when(featureManagerClient.calculateFraudFeatures(payload))
                .thenThrow(new RuntimeException(REST_CLIENT_FAILURE_MESSAGE));

        assertThatThrownBy(() -> processTransactionUseCase.execute(input))
                .isInstanceOf(TransactionOrchestrationException.class)
                .hasMessageContaining(ORCHESTRATION_ERROR_PREFIX)
                .hasMessageContaining(TRANSACTION_ID)
                .hasMessageContaining(MESSAGE_KEY)
                .hasRootCauseMessage(REST_CLIENT_FAILURE_MESSAGE);

        verifyNoInteractions(inferenceEngineClient, transactionProducer);
    }

    private static SendResult<String, TransactionScoredMessage> sendResult() {
        TopicPartition topicPartition = new TopicPartition(KAFKA_TOPIC_NAME, KAFKA_PARTITION);
        RecordMetadata metadata = new RecordMetadata(topicPartition, 0L, KAFKA_OFFSET, 0L, 0, 0);
        return new SendResult<>(null, metadata);
    }
}
