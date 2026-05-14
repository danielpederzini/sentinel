package org.pdzsoftware.riskactionhandler.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.TransactionScoredMessage;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.client.EmailClient;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.client.EmailContentBuilder;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.client.LlmClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyRiskUseCase implements VoidUseCase<TransactionScoredMessage> {
    private final LlmClient llmClient;
    private final EmailClient emailClient;

    @Override
    public void execute(TransactionScoredMessage input) {
        String transactionId = input.transactionId();
        String fraudExplanation = llmClient.getFraudExplanation(input);
        String emailContent = EmailContentBuilder.buildFraudAlertEmail(input, fraudExplanation);

        emailClient.sendEmail(
                transactionId,
                String.format("Suspected Fraud: %s", transactionId),
                emailContent
        );

        log.info("Sent fraud alert email for transaction {}", transactionId);
    }
}
