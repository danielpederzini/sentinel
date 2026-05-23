package org.pdzsoftware.antifraudorchestrator.support;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class TestConstants {

    public static final String FRAUD_FEATURES_ENDPOINT = "/api/v1/fraud-features";
    public static final String PERSIST_TRANSACTION_ENDPOINT = "/api/v1/transactions";
    public static final String INFERENCE_SCORE_ENDPOINT = "/transaction/score";

    public static final String USER_ID = "user-1";
    public static final String CARD_ID = "card-1";
    public static final String MERCHANT_ID = "merchant-1";
    public static final String DEVICE_ID = "device-1";
    public static final String TRANSACTION_ID = "txn-1";
    public static final String MESSAGE_KEY = "txn-1";
    public static final String IP_ADDRESS = "203.0.113.1";
    public static final String MODEL_VERSION = "v1";

    public static final String KAFKA_TRANSACTIONS_SCORED_TOPIC = "transactions.scored";
    public static final String KAFKA_TOPIC_NAME = "transactions.scored";
    public static final int KAFKA_PARTITION = 0;
    public static final int KAFKA_OFFSET = 7;

    public static final BigDecimal TRANSACTION_AMOUNT = BigDecimal.valueOf(99.99);
    public static final LocalDateTime CREATION_DATE_TIME = LocalDateTime.of(2024, 6, 15, 12, 0);

    public static final float MERCHANT_RISK_SCORE = 0.3f;
    public static final float IP_RISK_SCORE = 0.2f;
    public static final float AMOUNT_TO_AVERAGE_RATIO = 1.5f;
    public static final double FRAUD_PROBABILITY = 0.12;

    public static final String FEATURE_MANAGER_ERROR_MESSAGE = "Failed to fetch fraud features from Feature Manager";
    public static final String INFERENCE_ERROR_MESSAGE = "Failed to score transaction with Inference Engine";
    public static final String ORCHESTRATION_ERROR_PREFIX = "Failed to orchestrate transaction";
    public static final String REST_CLIENT_FAILURE_MESSAGE = "connection refused";
    public static final String EMPTY_RESPONSE_MESSAGE = "Feature Manager returned an empty response body";

    private TestConstants() {
    }
}
