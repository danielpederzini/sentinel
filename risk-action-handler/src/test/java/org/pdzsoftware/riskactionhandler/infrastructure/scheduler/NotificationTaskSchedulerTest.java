package org.pdzsoftware.riskactionhandler.infrastructure.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pdzsoftware.riskactionhandler.application.usecase.NotifyRiskUseCase;
import org.pdzsoftware.riskactionhandler.domain.entity.NotificationTask;
import org.pdzsoftware.riskactionhandler.domain.enums.NotificationTaskStatus;
import org.pdzsoftware.riskactionhandler.infrastructure.config.properties.NotificationSchedulerProperties;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.repository.NotificationTaskRepository;
import org.pdzsoftware.riskactionhandler.support.TestFixtures;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.TRANSACTION_ID;

@ExtendWith(MockitoExtension.class)
class NotificationTaskSchedulerTest {

    @Mock
    private NotificationTaskRepository notificationTaskRepository;

    @Mock
    private NotifyRiskUseCase notifyRiskUseCase;

    @Mock
    private NotificationSchedulerProperties schedulerProperties;

    @InjectMocks
    private NotificationTaskScheduler notificationTaskScheduler;

    @Test
    void processNotificationTasks_shouldProcessRetryableTasks() {
        var task = NotificationTask.builder()
                .transactionId(TRANSACTION_ID)
                .status(NotificationTaskStatus.PENDING_LLM)
                .payload(TestFixtures.highRiskTransaction())
                .nextRetryAt(Instant.now().minusSeconds(10))
                .build();

        when(notificationTaskRepository.findByStatusInAndNextRetryAtLessThanEqual(anyList(), any(Instant.class)))
                .thenReturn(List.of(task));
        when(schedulerProperties.getBatchSize()).thenReturn(50);

        notificationTaskScheduler.processNotificationTasks();

        verify(notifyRiskUseCase).execute(task);
    }

    @Test
    void processNotificationTasks_shouldDoNothing_whenNoTasksReady() {
        when(notificationTaskRepository.findByStatusInAndNextRetryAtLessThanEqual(anyList(), any(Instant.class)))
                .thenReturn(List.of());

        notificationTaskScheduler.processNotificationTasks();

        verify(notifyRiskUseCase, never()).execute(any());
    }
}
