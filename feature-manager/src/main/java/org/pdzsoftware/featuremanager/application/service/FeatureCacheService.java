package org.pdzsoftware.featuremanager.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.pdzsoftware.featuremanager.infrastructure.config.properties.RedisProperties;
import org.pdzsoftware.featuremanager.domain.exception.CacheReadException;
import org.pdzsoftware.featuremanager.domain.exception.CacheUpdateException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureCacheService {
    private static final String USER_TRANSACTIONS_KEY_PREFIX = "user:transactions:";
    private static final String USER_LAST_TRANSACTION_KEY_PREFIX = "user:last-transaction:";
    private static final String USER_TRANSACTION_AMOUNTS_KEY_PREFIX = "user:transaction-amounts:";
    private static final String USER_TRANSACTION_MERCHANTS_KEY_PREFIX = "user:transaction-merchants:";
    private static final long FIVE_MINUTES_IN_SECONDS = 5L * 60L;
    private static final long ONE_HOUR_IN_SECONDS = 60L * 60L;
    private static final long EPOCH_MILLIS_THRESHOLD = 10_000_000_000L;
    private static final long ONE_SECOND_IN_MILLIS = 1_000L;
    private static final long DEFAULT_SECONDS_SINCE_LAST = 30L * 24L * 3600L; // 30 days — matches training data

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisProperties redisProperties;

    public void recordUserTransaction(String userId, String transactionId, BigDecimal amount, String merchantId) {
        recordUserTransaction(userId, transactionId, amount, merchantId, System.currentTimeMillis());
    }

    public void recordUserTransaction(String userId, String transactionId, BigDecimal amount, String merchantId, long timestamp) {
        try {
            long timestampInSeconds = normalizeToEpochSeconds(timestamp);
            String userTransactionKey = USER_TRANSACTIONS_KEY_PREFIX + userId;
            redisTemplate.opsForZSet().add(userTransactionKey, buildMember(transactionId, String.valueOf(System.currentTimeMillis())), timestampInSeconds);
            redisTemplate.expire(userTransactionKey, redisProperties.getTimeToLast());

            String amountsKey = USER_TRANSACTION_AMOUNTS_KEY_PREFIX + userId;
            redisTemplate.opsForZSet().add(amountsKey, buildMember(transactionId, amount.toPlainString()), timestampInSeconds);
            redisTemplate.expire(amountsKey, redisProperties.getTimeToLast());

            String merchantsKey = USER_TRANSACTION_MERCHANTS_KEY_PREFIX + userId;
            redisTemplate.opsForZSet().add(merchantsKey, buildMember(transactionId, merchantId), timestampInSeconds);
            redisTemplate.expire(merchantsKey, redisProperties.getTimeToLast());

            String lastTransactionKey = USER_LAST_TRANSACTION_KEY_PREFIX + userId;
            redisTemplate.opsForValue().set(lastTransactionKey, String.valueOf(timestampInSeconds), redisProperties.getTimeToLast());
        } catch (IllegalArgumentException exception) {
            throw new CacheUpdateException(String.format(
                    "Failed to record transaction %s for user %s", transactionId, userId), exception);
        }
    }

    public long getUserTransactionCount5Min(String userId) {
        long nowInSeconds = System.currentTimeMillis() / ONE_SECOND_IN_MILLIS;
        long windowStart = nowInSeconds - FIVE_MINUTES_IN_SECONDS;
        return getTransactionCountInWindow(userId, windowStart, nowInSeconds);
    }

    public long getUserTransactionCount1Hour(String userId) {
        long nowInSeconds = System.currentTimeMillis() / ONE_SECOND_IN_MILLIS;
        long windowStart = nowInSeconds - ONE_HOUR_IN_SECONDS;
        return getTransactionCountInWindow(userId, windowStart, nowInSeconds);
    }

    private long getTransactionCountInWindow(String userId, long windowStart, long windowEnd) {
        try {
            String userTransactionKey = USER_TRANSACTIONS_KEY_PREFIX + userId;
            Long count = redisTemplate.opsForZSet().count(userTransactionKey, windowStart, windowEnd);
            return count != null ? count : 0L;
        } catch (IllegalArgumentException exception) {
            throw new CacheReadException(String.format(
                    "Failed to retrieve transaction count for user %s", userId), exception);
        }
    }

    public long getSecondsSinceLastTransaction(String userId) {
        try {
            String lastTransactionKey = USER_LAST_TRANSACTION_KEY_PREFIX + userId;
            String lastTimestamp = redisTemplate.opsForValue().get(lastTransactionKey);

            if (lastTimestamp == null) {
                return DEFAULT_SECONDS_SINCE_LAST;
            }

            long lastTransactionTime = Long.parseLong(lastTimestamp);

            long nowInSeconds = System.currentTimeMillis() / ONE_SECOND_IN_MILLIS;
            return nowInSeconds - lastTransactionTime;
        } catch (IllegalArgumentException exception) {
            throw new CacheReadException(String.format(
                    "Failed to retrieve last transaction time for user %s", userId), exception);
        }
    }

    public BigDecimal getAmountVelocity1Hour(String userId) {
        try {
            String amountsKey = USER_TRANSACTION_AMOUNTS_KEY_PREFIX + userId;
            long nowInSeconds = System.currentTimeMillis() / ONE_SECOND_IN_MILLIS;
            long windowStart = nowInSeconds - ONE_HOUR_IN_SECONDS;

            Set<String> members = redisTemplate.opsForZSet().rangeByScore(amountsKey, windowStart, nowInSeconds);
            if (members == null || members.isEmpty()) {
                return BigDecimal.ZERO;
            }

            BigDecimal total = BigDecimal.ZERO;
            for (String member : members) {
                int separatorIndex = member.lastIndexOf(':');
                if (separatorIndex >= 0 && separatorIndex < member.length() - 1) {
                    total = total.add(new BigDecimal(member.substring(separatorIndex + 1)));
                }
            }
            return total;
        } catch (IllegalArgumentException exception) {
            throw new CacheReadException(String.format(
                    "Failed to retrieve amount velocity for user %s", userId), exception);
        }
    }

    public long getDistinctMerchantCount1Hour(String userId) {
        try {
            String merchantsKey = USER_TRANSACTION_MERCHANTS_KEY_PREFIX + userId;
            long nowInSeconds = System.currentTimeMillis() / ONE_SECOND_IN_MILLIS;
            long windowStart = nowInSeconds - ONE_HOUR_IN_SECONDS;

            Set<String> members = redisTemplate.opsForZSet().rangeByScore(merchantsKey, windowStart, nowInSeconds);
            if (members == null || members.isEmpty()) {
                return 0L;
            }

            return members.stream()
                    .map(member -> {
                        int separatorIndex = member.lastIndexOf(':');
                        return separatorIndex >= 0 && separatorIndex < member.length() - 1
                                ? member.substring(separatorIndex + 1) : member;
                    })
                    .distinct()
                    .count();
        } catch (IllegalArgumentException exception) {
            throw new CacheReadException(String.format(
                    "Failed to retrieve distinct merchant count for user %s", userId), exception);
        }
    }

    public void cleanupOldTransactions(String userId) {
        try {
            String userTransactionKey = USER_TRANSACTIONS_KEY_PREFIX + userId;
            String amountsKey = USER_TRANSACTION_AMOUNTS_KEY_PREFIX + userId;
            String merchantsKey = USER_TRANSACTION_MERCHANTS_KEY_PREFIX + userId;
            long nowInSeconds = System.currentTimeMillis() / ONE_SECOND_IN_MILLIS;
            long cutoffTime = nowInSeconds - ONE_HOUR_IN_SECONDS;

            redisTemplate.opsForZSet().removeRangeByScore(userTransactionKey, 0, cutoffTime);
            redisTemplate.opsForZSet().removeRangeByScore(amountsKey, 0, cutoffTime);
            redisTemplate.opsForZSet().removeRangeByScore(merchantsKey, 0, cutoffTime);
        } catch (IllegalArgumentException exception) {
            throw new CacheUpdateException(String.format(
                    "Failed to clean up old transactions for user %s", userId), exception);
        }
    }

    private String buildMember(String transactionId, String suffix) {
        String memberPrefix = StringUtils.isBlank(transactionId) ? UUID.randomUUID().toString() : transactionId;
        return String.format("%s:%s", memberPrefix, suffix);
    }

    private long normalizeToEpochSeconds(long timestamp) {
        return timestamp > EPOCH_MILLIS_THRESHOLD ? timestamp / ONE_SECOND_IN_MILLIS : timestamp;
    }
}
