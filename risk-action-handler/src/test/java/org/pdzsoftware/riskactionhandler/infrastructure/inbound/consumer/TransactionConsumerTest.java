package org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pdzsoftware.riskactionhandler.application.usecase.NotifyRiskUseCase;
import org.pdzsoftware.riskactionhandler.support.TestFixtures;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TransactionConsumerTest {

    @Mock
    private NotifyRiskUseCase notifyRiskUseCase;

    @InjectMocks
    private TransactionConsumer transactionConsumer;

    @Test
    void consume_shouldNotifyRisk_whenRiskLevelIsHigh() {
        var message = TestFixtures.highRiskTransaction();
        transactionConsumer.consume(message);

        verify(notifyRiskUseCase).execute(message);
    }

    @Test
    void consume_shouldSkipNotification_whenRiskLevelIsNotHigh() {
        transactionConsumer.consume(TestFixtures.lowRiskTransaction());

        verifyNoInteractions(notifyRiskUseCase);
    }
}
