package org.pdzsoftware.riskactionhandler.application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pdzsoftware.riskactionhandler.domain.entity.NotificationTask;
import org.pdzsoftware.riskactionhandler.domain.enums.NotificationTaskStatus;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.repository.NotificationTaskRepository;
import org.pdzsoftware.riskactionhandler.support.TestFixtures;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.TRANSACTION_ID;

@ExtendWith(MockitoExtension.class)
class PersistNotificationTaskUseCaseTest {

    @Mock
    private NotificationTaskRepository notificationTaskRepository;

    @InjectMocks
    private PersistNotificationTaskUseCase persistNotificationTaskUseCase;

    @Test
    void execute_shouldPersistNewTask_whenTransactionNotAlreadyProcessed() {
        var message = TestFixtures.highRiskTransaction();
        when(notificationTaskRepository.findByTransactionId(TRANSACTION_ID)).thenReturn(Optional.empty());

        persistNotificationTaskUseCase.execute(message);

        ArgumentCaptor<NotificationTask> captor = ArgumentCaptor.forClass(NotificationTask.class);
        verify(notificationTaskRepository).save(captor.capture());

        NotificationTask saved = captor.getValue();
        assertThat(saved.getTransactionId()).isEqualTo(TRANSACTION_ID);
        assertThat(saved.getStatus()).isEqualTo(NotificationTaskStatus.PENDING_LLM);
        assertThat(saved.getPayload()).isEqualTo(message);
        assertThat(saved.getAttempts()).isZero();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getNextRetryAt()).isNotNull();
    }

    @Test
    void execute_shouldSkipPersistence_whenTransactionAlreadyExists() {
        var message = TestFixtures.highRiskTransaction();
        when(notificationTaskRepository.findByTransactionId(TRANSACTION_ID))
                .thenReturn(Optional.of(NotificationTask.builder().transactionId(TRANSACTION_ID).build()));

        persistNotificationTaskUseCase.execute(message);

        verify(notificationTaskRepository, never()).save(any());
    }
}
