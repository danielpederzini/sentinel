package org.pdzsoftware.featuremanager.support;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class TestConstants {

    public static final String USER_ID = "user-1";
    public static final String CARD_ID = "card-1";
    public static final String MERCHANT_ID = "merchant-1";
    public static final String DEVICE_ID = "device-1";
    public static final String MISSING_ID = "missing";

    public static final String TRANSACTION_ID_1 = "txn-1";
    public static final String TRANSACTION_ID_2 = "txn-2";
    public static final String TRANSACTION_ID_3 = "txn-3";
    public static final String TRANSACTION_ID_4 = "txn-4";
    public static final String TRANSACTION_ID_5 = "txn-5";
    public static final String TRANSACTION_ID_FAIL = "txn-fail";

    public static final String IP_ADDRESS = "203.0.113.1";
    public static final String BLANK_IP_ADDRESS = "";
    public static final String BLANK_DEVICE_ID = "   ";
    public static final String USER_EMAIL = "user@example.com";
    public static final String MERCHANT_EMAIL = "merchant@example.com";
    public static final String MODEL_VERSION = "v1";

    public static final String REDIS_USER_TRANSACTIONS_PREFIX = "user:transactions:";
    public static final String REDIS_USER_AMOUNTS_PREFIX = "user:transaction-amounts:";
    public static final String REDIS_USER_MERCHANTS_PREFIX = "user:transaction-merchants:";
    public static final String REDIS_USER_LAST_TRANSACTION_PREFIX = "user:last-transaction:";

    public static final String CACHE_MEMBER_TXN_1_AMOUNT = "txn-1:10.50";
    public static final String CACHE_MEMBER_TXN_2_AMOUNT = "txn-2:20";
    public static final String CACHE_MEMBER_TXN_1_MERCHANT = "txn-1:merchant-a";
    public static final String CACHE_MEMBER_TXN_2_MERCHANT = "txn-2:merchant-a";
    public static final String CACHE_MEMBER_TXN_3_MERCHANT = "txn-3:merchant-b";

    public static final String ERROR_MESSAGE_DB_DOWN = "db down";
    public static final String ERROR_MESSAGE_BAD_MEMBER = "bad member";
    public static final String ERROR_MESSAGE_INVALID_RANGE = "invalid range";

    public static final LocalDate USER_BIRTH_DATE = LocalDate.of(1990, 1, 1);
    public static final LocalDateTime CREATION_AFTERNOON = LocalDateTime.of(2024, 6, 15, 14, 0);
    public static final LocalDateTime CREATION_MORNING = LocalDateTime.of(2024, 6, 15, 10, 0);
    public static final LocalDateTime CREATION_LATE_NIGHT = LocalDateTime.of(2024, 6, 15, 23, 0);

    public static final int NIGHT_HOUR = 23;
    public static final int USER_AGE_YEARS = 2;
    public static final int CARD_AGE_DAYS = 30;
    public static final int MERCHANT_AGE_YEARS = 1;
    public static final int PERSIST_CREATED_HOURS_AGO = 1;

    public static final long FIXED_EPOCH_SECONDS = 1_700_000_000L;
    public static final int MILLIS_PER_SECOND = 1_000;
    public static final long SECONDS_SINCE_LAST_DEFAULT = 30L * 24L * 3600L;
    public static final long SECONDS_SINCE_LAST_STUB = 600L;
    public static final long SECONDS_AGO_FOR_DELTA_TEST = 120L;
    public static final long DELTA_TOLERANCE_LOWER = 115L;
    public static final long DELTA_TOLERANCE_UPPER = 125L;

    public static final Duration REDIS_TIME_TO_LAST = Duration.ofMinutes(10);
    public static final double REDIS_SCORE_MIN = 0.0;
    public static final int REDIS_CLEANUP_KEY_COUNT = 3;

    public static final long TRANSACTION_COUNT_5MIN = 3L;
    public static final long TRANSACTION_COUNT_1HOUR_STUB = 2L;
    public static final long TRANSACTION_COUNT_5MIN_STUB = 1L;
    public static final long DISTINCT_MERCHANT_COUNT = 2L;
    public static final long DISTINCT_MERCHANT_COUNT_STUB = 1L;
    public static final long HISTORICAL_TRANSACTION_COUNT = 3L;
    public static final long HISTORICAL_TRANSACTION_COUNT_STUB = 1L;
    public static final long HISTORICAL_TRANSACTION_COUNT_NONE = 0L;
    public static final long USER_TRANSACTION_COUNT = 7L;

    public static final BigDecimal COLD_START_AVERAGE_AMOUNT = BigDecimal.valueOf(100);
    public static final BigDecimal TRANSACTION_AMOUNT_LARGE = BigDecimal.valueOf(200);
    public static final BigDecimal TRANSACTION_AMOUNT_MEDIUM = BigDecimal.valueOf(50);
    public static final BigDecimal TRANSACTION_AMOUNT_SMALL = BigDecimal.TEN;
    public static final BigDecimal TRANSACTION_AMOUNT_SINGLE = BigDecimal.ONE;
    public static final BigDecimal AMOUNT_VELOCITY_SUM = new BigDecimal("30.50");
    public static final BigDecimal AMOUNT_VELOCITY_STUB = BigDecimal.valueOf(75);
    public static final BigDecimal PERSIST_TRANSACTION_AMOUNT = BigDecimal.valueOf(50);

    public static final float AMOUNT_TO_AVERAGE_RATIO_COLD_START = 2.0f;
    public static final float MERCHANT_RISK_SCORE = 0.5f;
    public static final float MERCHANT_RISK_SCORE_LOW = 0.2f;
    public static final float IP_RISK_SCORE_STUB = 0.4f;
    public static final float IP_RISK_SCORE_HIGH = 0.75f;
    public static final float IP_RISK_SCORE_LOW = 0.1f;
    public static final double COUNTRY_IP_RISK_MIN = 0.0;
    public static final double FRAUD_PROBABILITY = 0.15;

    public static final double REPOSITORY_AVERAGE_AMOUNT = 42.5;
    public static final String REPOSITORY_AVERAGE_AMOUNT_STRING = "42.5";

    public static final int FEATURE_HOUR_OF_DAY = 14;
    public static final int FEATURE_DAY_OF_WEEK = 3;
    public static final int FEATURE_MERCHANT_CATEGORY = 1;
    public static final int FEATURE_CARD_TYPE = 0;
    public static final long FEATURE_SECONDS_SINCE_LAST = 3600L;
    public static final long FEATURE_CARD_AGE_DAYS = 30L;
    public static final long FEATURE_USER_ACCOUNT_AGE_DAYS = 365L;
    public static final long FEATURE_DISTINCT_MERCHANT_COUNT = 1L;
    public static final long FEATURE_HISTORICAL_COUNT = 5L;
    public static final long FEATURE_TXN_COUNT_5MIN = 1L;
    public static final long FEATURE_TXN_COUNT_1HOUR = 2L;

    public static final double FEATURE_LOG_AMOUNT = 3.9;
    public static final double FEATURE_LOG_SECONDS = 8.0;
    public static final double FEATURE_LOG_VELOCITY = 2.3;
    public static final double FEATURE_AMOUNT_TIMES_MERCHANT_RISK = 10.0;
    public static final double FEATURE_RISK_SCORE_PRODUCT = 0.02;
    public static final double FEATURE_VELOCITY_AMOUNT_INTERACTION = 1.0;
    public static final double FEATURE_RECENCY_VELOCITY = 0.5;
    public static final double FEATURE_AMOUNT_DEVIATION = 0.1;
    public static final double FEATURE_VELOCITY_INTENSITY = 5.0;
    public static final float FEATURE_AMOUNT_TO_AVERAGE_RATIO = 0.5f;

    private TestConstants() {
    }

    public static String userTransactionsKey(String userId) {
        return REDIS_USER_TRANSACTIONS_PREFIX + userId;
    }

    public static String userAmountsKey(String userId) {
        return REDIS_USER_AMOUNTS_PREFIX + userId;
    }

    public static String userMerchantsKey(String userId) {
        return REDIS_USER_MERCHANTS_PREFIX + userId;
    }

    public static String userLastTransactionKey(String userId) {
        return REDIS_USER_LAST_TRANSACTION_PREFIX + userId;
    }

    public static long fixedEpochMillis() {
        return FIXED_EPOCH_SECONDS * MILLIS_PER_SECOND;
    }
}
