package org.pdzsoftware.antifraudorchestrator.support;

import org.pdzsoftware.antifraudorchestrator.domain.enums.CountryCode;
import org.pdzsoftware.antifraudorchestrator.domain.enums.RiskLevel;
import org.pdzsoftware.antifraudorchestrator.infrastructure.inbound.consumer.dto.TransactionCreatedMessage;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto.ExplainabilityDetailsResponse;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto.FraudFeatureResponse;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto.FraudPredictionResponse;

import java.math.BigDecimal;
import java.util.List;

import static org.pdzsoftware.antifraudorchestrator.support.TestConstants.AMOUNT_TO_AVERAGE_RATIO;
import static org.pdzsoftware.antifraudorchestrator.support.TestConstants.CARD_ID;
import static org.pdzsoftware.antifraudorchestrator.support.TestConstants.CREATION_DATE_TIME;
import static org.pdzsoftware.antifraudorchestrator.support.TestConstants.DEVICE_ID;
import static org.pdzsoftware.antifraudorchestrator.support.TestConstants.FRAUD_PROBABILITY;
import static org.pdzsoftware.antifraudorchestrator.support.TestConstants.IP_ADDRESS;
import static org.pdzsoftware.antifraudorchestrator.support.TestConstants.IP_RISK_SCORE;
import static org.pdzsoftware.antifraudorchestrator.support.TestConstants.MERCHANT_ID;
import static org.pdzsoftware.antifraudorchestrator.support.TestConstants.MERCHANT_RISK_SCORE;
import static org.pdzsoftware.antifraudorchestrator.support.TestConstants.MODEL_VERSION;
import static org.pdzsoftware.antifraudorchestrator.support.TestConstants.TRANSACTION_AMOUNT;
import static org.pdzsoftware.antifraudorchestrator.support.TestConstants.TRANSACTION_ID;
import static org.pdzsoftware.antifraudorchestrator.support.TestConstants.USER_ID;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static TransactionCreatedMessage transactionCreatedMessage() {
        return new TransactionCreatedMessage(
                TRANSACTION_ID,
                USER_ID,
                CARD_ID,
                MERCHANT_ID,
                DEVICE_ID,
                TRANSACTION_AMOUNT,
                CountryCode.US,
                IP_ADDRESS,
                CREATION_DATE_TIME
        );
    }

    public static FraudFeatureResponse fraudFeatureResponse() {
        return new FraudFeatureResponse(
                TRANSACTION_ID,
                TRANSACTION_AMOUNT,
                TRANSACTION_AMOUNT,
                5L,
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

    public static FraudPredictionResponse fraudPredictionResponse() {
        return new FraudPredictionResponse(
                TRANSACTION_ID,
                FRAUD_PROBABILITY,
                RiskLevel.LOW,
                MODEL_VERSION,
                new ExplainabilityDetailsResponse(List.of())
        );
    }
}
