package org.pdzsoftware.riskactionhandler.application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pdzsoftware.riskactionhandler.domain.exception.EmailClientException;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.client.EmailClient;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.client.LlmClient;
import org.pdzsoftware.riskactionhandler.support.TestFixtures;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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

    @InjectMocks
    private NotifyRiskUseCase notifyRiskUseCase;

    @Test
    void execute_shouldSendEmail_whenLlmAndMailSucceed() {
        var message = TestFixtures.highRiskTransaction();
        when(llmClient.getFraudExplanation(message)).thenReturn(LLM_EXPLANATION);

        notifyRiskUseCase.execute(message);

        verify(llmClient).getFraudExplanation(message);
        verify(emailClient).sendEmail(
                eq(TRANSACTION_ID),
                eq(EMAIL_SUBJECT_PREFIX + TRANSACTION_ID),
                contains(LLM_EXPLANATION));
    }

    @Test
    void execute_shouldNotPropagateException_whenEmailFails() {
        var message = TestFixtures.highRiskTransaction();
        when(llmClient.getFraudExplanation(message)).thenReturn(LLM_EXPLANATION);
        doThrow(new EmailClientException("failed", new RuntimeException()))
                .when(emailClient)
                .sendEmail(eq(TRANSACTION_ID), eq(EMAIL_SUBJECT_PREFIX + TRANSACTION_ID), anyString());

        notifyRiskUseCase.execute(message);

        verify(emailClient).sendEmail(
                eq(TRANSACTION_ID),
                eq(EMAIL_SUBJECT_PREFIX + TRANSACTION_ID),
                anyString());
    }
}
