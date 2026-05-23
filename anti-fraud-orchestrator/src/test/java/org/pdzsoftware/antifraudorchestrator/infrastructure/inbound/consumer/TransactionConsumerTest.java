package org.pdzsoftware.antifraudorchestrator.infrastructure.inbound.consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pdzsoftware.antifraudorchestrator.application.usecase.ProcessTransactionUseCase;
import org.pdzsoftware.antifraudorchestrator.application.usecase.dto.ProcessTransactionInput;
import org.pdzsoftware.antifraudorchestrator.infrastructure.inbound.consumer.dto.TransactionCreatedMessage;
import org.pdzsoftware.antifraudorchestrator.support.TestFixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.pdzsoftware.antifraudorchestrator.support.TestConstants.MESSAGE_KEY;

@ExtendWith(MockitoExtension.class)
class TransactionConsumerTest {

    @Mock
    private ProcessTransactionUseCase processTransactionUseCase;

    @InjectMocks
    private TransactionConsumer transactionConsumer;

    @Test
    void consume_shouldDelegateToProcessTransactionUseCase() {
        TransactionCreatedMessage payload = TestFixtures.transactionCreatedMessage();

        transactionConsumer.consume(payload, MESSAGE_KEY);

        ArgumentCaptor<ProcessTransactionInput> captor = ArgumentCaptor.forClass(ProcessTransactionInput.class);
        verify(processTransactionUseCase).execute(captor.capture());
        ProcessTransactionInput input = captor.getValue();
        assertThat(input.payload()).isEqualTo(payload);
        assertThat(input.messageKey()).isEqualTo(MESSAGE_KEY);
    }
}
