package org.pdzsoftware.riskactionhandler.support;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class TestConstants {

    public static final String CHAT_COMPLETIONS_ENDPOINT = "/chat/completions";
    public static final String LLM_BASE_URL = "http://llm";

    public static final String USER_ID = "user-1";
    public static final String CARD_ID = "card-1";
    public static final String MERCHANT_ID = "merchant-1";
    public static final String DEVICE_ID = "device-1";
    public static final String TRANSACTION_ID = "txn-1";
    public static final String IP_ADDRESS = "203.0.113.1";
    public static final String MODEL_VERSION = "v1";
    public static final String LLM_MODEL = "gpt-test";
    public static final String DESTINATION_EMAIL = "fraud-alerts@example.com";

    public static final String EMAIL_SUBJECT_PREFIX = "Fraud Alert — Transaction ";
    public static final String LLM_EXPLANATION = "Unusual velocity and country mismatch detected.";
    public static final String FEATURE_NAME_AMOUNT = "amount";
    public static final String FEATURE_VALUE_AMOUNT = "200";
    public static final String FEATURE_DIRECTION_INCREASE = "increases";
    public static final double FEATURE_CONTRIBUTION = 0.42;

    public static final BigDecimal TRANSACTION_AMOUNT = BigDecimal.valueOf(99.99);
    public static final LocalDateTime CREATION_DATE_TIME = LocalDateTime.of(2024, 6, 15, 12, 0);

    public static final double FRAUD_PROBABILITY_HIGH = 0.91;
    public static final double FRAUD_PROBABILITY_LOW = 0.05;

    public static final float MERCHANT_RISK_SCORE = 0.3f;
    public static final float IP_RISK_SCORE = 0.2f;
    public static final float AMOUNT_TO_AVERAGE_RATIO = 1.5f;

    public static final String LLM_ERROR_MESSAGE_PREFIX = "Failed to get LLM explainability details for transaction";
    public static final String LLM_NO_CHOICES_MESSAGE = "LLM returned no choices";
    public static final String EMAIL_ERROR_MESSAGE_PREFIX = "Failed to send email with ID";
    public static final String REST_CLIENT_FAILURE_MESSAGE = "connection refused";
    public static final String MAIL_SEND_FAILURE_MESSAGE = "smtp down";

    public static final String HTML_FRAUD_ALERT_HEADING = "Fraud Alert";
    public static final String HTML_AI_ANALYSIS_SECTION = "AI Analysis";

    private TestConstants() {
    }
}
