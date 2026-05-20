package org.pdzsoftware.riskactionhandler.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pdzsoftware.riskactionhandler.application.util.EmailContentBuilder;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.TransactionScoredMessage;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.client.EmailClient;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.client.LlmClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyRiskUseCase {
    private final LlmClient llmClient;
    private final EmailClient emailClient;

    public void execute(TransactionScoredMessage message) {
        try {
            String transactionId = message.transactionId();
            
            String llmExplanation = llmClient.getFraudExplanation(message);
            String emailContent = EmailContentBuilder.buildFraudAlertEmail(message, llmExplanation);
            String subject = "Fraud Alert — Transaction " + transactionId;

            emailClient.sendEmail(transactionId, subject, emailContent);
            log.info("Sent fraud alert notification for transaction {}", transactionId);
        } catch (Exception e) {
            log.error("Failed to send notification for transaction {}: {}",
                    message.transactionId(), e.getMessage(), e);
        }
    }
}
