package org.pdzsoftware.riskactionhandler.application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pdzsoftware.riskactionhandler.domain.entity.NotificationTask;
import org.pdzsoftware.riskactionhandler.domain.enums.NotificationTaskStatus;
import org.pdzsoftware.riskactionhandler.domain.exception.EmailClientException;
import org.pdzsoftware.riskactionhandler.domain.exception.LlmClientException;
import org.pdzsoftware.riskactionhandler.infrastructure.config.properties.NotificationSchedulerProperties;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.client.EmailClient;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.client.LlmClient;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.repository.NotificationTaskRepository;
import org.pdzsoftware.riskactionhandler.support.TestFixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.EMAIL_SUBJECT_PREFIX;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.LLM_EXPLANATION;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.TRANSACTION_ID;

@ExtendWith(MockitoExtension.class)
class NotifyRiskUseCaseTest {

    @Mock
    private LlmClient llmClient;

    @Mock
    private EmailClient emailClient;

    @Mock
    private NotificationTaskRepository notificationTaskRepository;

    @Mock
    private NotificationSchedulerProperties schedulerProperties;

    @InjectMocks
    private NotifyRiskUseCase notifyRiskUseCase;

    @Test
    void execute_shouldCompleteAllSteps_whenLlmAndEmailSucceed() {
        var task = pendingLlmTask();
        when(llmClient.getFraudExplanation(task.getPayload())).thenReturn(LLM_EXPLANATION);

        notifyRiskUseCase.execute(task);

        assertThat(task.getStatus()).isEqualTo(NotificationTaskStatus.COMPLETED);
        assertThat(task.getLlmExplanation()).isEqualTo(LLM_EXPLANATION);
        assertThat(task.getCompletedAt()).isNotNull();
        verify(emailClient).sendEmail(eq(TRANSACTION_ID), eq(EMAIL_SUBJECT_PREFIX + TRANSACTION_ID), anyString());
    }

    @Test
    void execute_shouldOnlySendEmail_whenTaskIsPendingEmail() {
        var task = pendingEmailTask();

        notifyRiskUseCase.execute(task);

        assertThat(task.getStatus()).isEqualTo(NotificationTaskStatus.COMPLETED);
        verify(llmClient, never()).getFraudExplanation(any());
        verify(emailClient).sendEmail(eq(TRANSACTION_ID), eq(EMAIL_SUBJECT_PREFIX + TRANSACTION_ID), anyString());
    }

    @Test
    void execute_shouldTransitionToFailedState_whenLlmFails() {
        var task = pendingLlmTask();
        when(llmClient.getFraudExplanation(task.getPayload()))
                .thenThrow(new LlmClientException("connection refused", new RuntimeException()));
        when(schedulerProperties.getMaxAttempts()).thenReturn(5);
        when(schedulerProperties.getBaseDelaySeconds()).thenReturn(30L);

        notifyRiskUseCase.execute(task);

        assertThat(task.getStatus()).isEqualTo(NotificationTaskStatus.PENDING_LLM);
        assertThat(task.getAttempts()).isEqualTo(1);
        assertThat(task.getNextRetryAt()).isNotNull();
        assertThat(task.getErrorMessage()).contains("connection refused");
        verify(notificationTaskRepository).save(task);
    }

    @Test
    void execute_shouldTransitionToFailedState_whenEmailFails() {
        var task = pendingEmailTask();
        doThrow(new EmailClientException("smtp down", new RuntimeException()))
                .when(emailClient).sendEmail(anyString(), anyString(), anyString());
        when(schedulerProperties.getMaxAttempts()).thenReturn(5);
        when(schedulerProperties.getBaseDelaySeconds()).thenReturn(30L);

        notifyRiskUseCase.execute(task);

        assertThat(task.getStatus()).isEqualTo(NotificationTaskStatus.PENDING_EMAIL);
        assertThat(task.getAttempts()).isEqualTo(1);
        assertThat(task.getErrorMessage()).contains("smtp down");
        verify(notificationTaskRepository).save(task);
    }

    @Test
    void execute_shouldDeadLetter_whenMaxAttemptsReached() {
        var task = pendingLlmTask();
        task.setAttempts(4);
        when(llmClient.getFraudExplanation(task.getPayload()))
                .thenThrow(new LlmClientException("still failing", new RuntimeException()));
        when(schedulerProperties.getMaxAttempts()).thenReturn(5);

        notifyRiskUseCase.execute(task);

        assertThat(task.getStatus()).isEqualTo(NotificationTaskStatus.DEAD_LETTER);
        assertThat(task.getAttempts()).isEqualTo(5);
        verify(notificationTaskRepository).save(task);
    }

    private NotificationTask pendingLlmTask() {
        return NotificationTask.builder()
                .transactionId(TRANSACTION_ID)
                .status(NotificationTaskStatus.PENDING_LLM)
                .payload(TestFixtures.highRiskTransaction())
                .attempts(0)
                .build();
    }

    private NotificationTask pendingEmailTask() {
        return NotificationTask.builder()
                .transactionId(TRANSACTION_ID)
                .status(NotificationTaskStatus.PENDING_EMAIL)
                .payload(TestFixtures.highRiskTransaction())
                .llmExplanation(LLM_EXPLANATION)
                .emailContent("<html>Fraud Alert</html>")
                .attempts(0)
                .build();
    }
}
