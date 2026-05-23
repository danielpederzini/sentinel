package org.pdzsoftware.riskactionhandler.support;

import org.pdzsoftware.riskactionhandler.domain.enums.CountryCode;
import org.pdzsoftware.riskactionhandler.domain.enums.RiskLevel;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.ExplainabilityDetailsMessage;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.FeatureContributionMessage;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.FraudFeaturesMessage;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.FraudPredictionMessage;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.TransactionScoredMessage;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.client.dto.ChatCompletionChoice;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.client.dto.ChatCompletionResponse;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.client.dto.ChatMessage;

import java.math.BigDecimal;
import java.util.List;

import static org.pdzsoftware.riskactionhandler.support.TestConstants.AMOUNT_TO_AVERAGE_RATIO;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.CARD_ID;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.CREATION_DATE_TIME;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.DEVICE_ID;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.FEATURE_CONTRIBUTION;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.FEATURE_DIRECTION_INCREASE;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.FEATURE_NAME_AMOUNT;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.FEATURE_VALUE_AMOUNT;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.FRAUD_PROBABILITY_HIGH;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.FRAUD_PROBABILITY_LOW;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.IP_ADDRESS;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.IP_RISK_SCORE;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.LLM_EXPLANATION;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.MERCHANT_ID;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.MERCHANT_RISK_SCORE;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.MODEL_VERSION;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.TRANSACTION_AMOUNT;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.TRANSACTION_ID;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.USER_ID;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static TransactionScoredMessage transactionScoredMessage(RiskLevel riskLevel, double fraudProbability) {
        return new TransactionScoredMessage(
                TRANSACTION_ID,
                USER_ID,
                CARD_ID,
                MERCHANT_ID,
                DEVICE_ID,
                TRANSACTION_AMOUNT,
                CountryCode.US,
                IP_ADDRESS,
                CREATION_DATE_TIME,
                fraudFeaturesMessage(),
                fraudPredictionMessage(riskLevel, fraudProbability)
        );
    }

    public static TransactionScoredMessage highRiskTransaction() {
        return transactionScoredMessage(RiskLevel.HIGH, FRAUD_PROBABILITY_HIGH);
    }

    public static TransactionScoredMessage lowRiskTransaction() {
        return transactionScoredMessage(RiskLevel.LOW, FRAUD_PROBABILITY_LOW);
    }

    public static FraudFeaturesMessage fraudFeaturesMessage() {
        return new FraudFeaturesMessage(
                TRANSACTION_AMOUNT,
                1L,
                2L,
                600L,
                MERCHANT_RISK_SCORE,
                true,
                false,
                AMOUNT_TO_AVERAGE_RATIO,
                14,
                IP_RISK_SCORE,
                30L,
                BigDecimal.TEN,
                365L,
                3,
                1,
                0,
                1L,
                3.9,
                8.0,
                2.3,
                10.0,
                0.02,
                0.0,
                0.0,
                1.0,
                0.5,
                0.1,
                false,
                5.0
        );
    }

    public static FraudPredictionMessage fraudPredictionMessage(RiskLevel riskLevel, double fraudProbability) {
        return new FraudPredictionMessage(
                fraudProbability,
                riskLevel,
                MODEL_VERSION,
                explainabilityWithContributions()
        );
    }

    public static ExplainabilityDetailsMessage explainabilityWithContributions() {
        return new ExplainabilityDetailsMessage(List.of(
                new FeatureContributionMessage(
                        FEATURE_NAME_AMOUNT,
                        FEATURE_VALUE_AMOUNT,
                        FEATURE_CONTRIBUTION,
                        FEATURE_DIRECTION_INCREASE)
        ));
    }

    public static ChatCompletionResponse chatCompletionResponse(String content) {
        return new ChatCompletionResponse(List.of(
                new ChatCompletionChoice(ChatMessage.builder().role("assistant").content(content).build())
        ));
    }

    public static ChatCompletionResponse chatCompletionResponseWithExplanation() {
        return chatCompletionResponse(LLM_EXPLANATION);
    }

    public static TransactionScoredMessage highRiskTransactionWithoutExplainability() {
        FraudPredictionMessage prediction = new FraudPredictionMessage(
                FRAUD_PROBABILITY_HIGH,
                RiskLevel.HIGH,
                MODEL_VERSION,
                null);
        return new TransactionScoredMessage(
                TRANSACTION_ID,
                USER_ID,
                CARD_ID,
                MERCHANT_ID,
                DEVICE_ID,
                TRANSACTION_AMOUNT,
                CountryCode.US,
                IP_ADDRESS,
                CREATION_DATE_TIME,
                fraudFeaturesMessage(),
                prediction);
    }
}
