package org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pdzsoftware.riskactionhandler.application.usecase.PersistNotificationTaskUseCase;
import org.pdzsoftware.riskactionhandler.support.TestFixtures;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TransactionConsumerTest {

    @Mock
    private PersistNotificationTaskUseCase persistNotificationTaskUseCase;

    @InjectMocks
    private TransactionConsumer transactionConsumer;

    @Test
    void consume_shouldPersistTask_whenRiskLevelIsHigh() {
        var message = TestFixtures.highRiskTransaction();
        transactionConsumer.consume(message);

        verify(persistNotificationTaskUseCase).execute(message);
    }

    @Test
    void consume_shouldSkipPersistence_whenRiskLevelIsNotHigh() {
        transactionConsumer.consume(TestFixtures.lowRiskTransaction());

        verifyNoInteractions(persistNotificationTaskUseCase);
    }
}
