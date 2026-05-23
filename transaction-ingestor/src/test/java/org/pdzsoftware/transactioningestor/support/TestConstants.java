package org.pdzsoftware.transactioningestor.support;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class TestConstants {

    public static final String TRANSACTIONS_API_PATH = "/api/v1/transactions";

    public static final String USER_ID = "user-1";
    public static final String CARD_ID = "card-1";
    public static final String MERCHANT_ID = "merchant-1";
    public static final String DEVICE_ID = "device-1";
    public static final String TRANSACTION_ID = "txn-1";
    public static final String BLANK_TRANSACTION_ID = "   ";
    public static final String IP_ADDRESS = "203.0.113.1";

    public static final String KAFKA_TRANSACTIONS_CREATED_TOPIC = "transactions.created";
    public static final String KAFKA_TOPIC_NAME = "transactions.created";
    public static final int KAFKA_PARTITION = 0;
    public static final int KAFKA_OFFSET = 42;

    public static final BigDecimal TRANSACTION_AMOUNT = BigDecimal.valueOf(99.99);
    public static final LocalDateTime CREATION_DATE_TIME = LocalDateTime.of(2024, 6, 15, 12, 0);

    public static final String VALIDATION_FIELD_USER_ID = "userId";
    public static final String VALIDATION_MESSAGE_NOT_BLANK = "must not be blank";
    public static final String VALIDATION_FAILED_PREFIX = "Validation failed:";

    public static final String RESPONSE_STATUS_REASON = "Forbidden action";
    public static final String UNEXPECTED_ERROR_MESSAGE = "Something broke";
    public static final String KAFKA_PUBLISH_ERROR_MESSAGE = "Failed to publish transaction to Kafka";
    public static final String UNEXPECTED_ERROR_PREFIX = "An unexpected error occurred:";

    public static final int HTTP_STATUS_ACCEPTED = 202;
    public static final int HTTP_STATUS_BAD_REQUEST = 400;
    public static final int HTTP_STATUS_FORBIDDEN = 403;
    public static final int HTTP_STATUS_INTERNAL_SERVER_ERROR = 500;

    private TestConstants() {
    }
}
