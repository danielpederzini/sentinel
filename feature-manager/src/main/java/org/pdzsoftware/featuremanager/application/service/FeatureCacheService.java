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
    private static final long FIVE_MINUTES_IN_SECONDS = 5L * 60L;
    private static final long ONE_HOUR_IN_SECONDS = 60L * 60L;
    private static final long EPOCH_MILLIS_THRESHOLD = 10_000_000_000L;
    private static final long ONE_SECOND_IN_MILLIS = 1_000L;

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisProperties redisProperties;

    public void recordUserTransaction(String userId, String transactionId, BigDecimal amount) {
        recordUserTransaction(userId, transactionId, amount, System.currentTimeMillis());
    }

    public void recordUserTransaction(String userId, String transactionId, BigDecimal amount, long timestamp) {
        try {
            long timestampInSeconds = normalizeToEpochSeconds(timestamp);
            String userTransactionKey = USER_TRANSACTIONS_KEY_PREFIX + userId;
            redisTemplate.opsForZSet().add(userTransactionKey, buildTransactionMember(transactionId), timestampInSeconds);
            redisTemplate.expire(userTransactionKey, redisProperties.getTimeToLast());

            String amountsKey = USER_TRANSACTION_AMOUNTS_KEY_PREFIX + userId;
            redisTemplate.opsForZSet().add(amountsKey, buildAmountMember(transactionId, amount), timestampInSeconds);
            redisTemplate.expire(amountsKey, redisProperties.getTimeToLast());

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
                return Long.MAX_VALUE;
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

    public void cleanupOldTransactions(String userId) {
        try {
            String userTransactionKey = USER_TRANSACTIONS_KEY_PREFIX + userId;
            String amountsKey = USER_TRANSACTION_AMOUNTS_KEY_PREFIX + userId;
            long nowInSeconds = System.currentTimeMillis() / ONE_SECOND_IN_MILLIS;
            long cutoffTime = nowInSeconds - ONE_HOUR_IN_SECONDS;

            redisTemplate.opsForZSet().removeRangeByScore(userTransactionKey, 0, cutoffTime);
            redisTemplate.opsForZSet().removeRangeByScore(amountsKey, 0, cutoffTime);
        } catch (IllegalArgumentException exception) {
            throw new CacheUpdateException(String.format(
                    "Failed to clean up old transactions for user %s", userId), exception);
        }
    }

    private String buildTransactionMember(String transactionId) {
        boolean isTransactionIdMissing = StringUtils.isBlank(transactionId);
        String memberPrefix = isTransactionIdMissing ? UUID.randomUUID().toString() : transactionId;
        return String.format("%s:%s", memberPrefix, System.currentTimeMillis());
    }

    private String buildAmountMember(String transactionId, BigDecimal amount) {
        boolean isTransactionIdMissing = StringUtils.isBlank(transactionId);
        String memberPrefix = isTransactionIdMissing ? UUID.randomUUID().toString() : transactionId;
        return String.format("%s:%s", memberPrefix, amount.toPlainString());
    }

    private long normalizeToEpochSeconds(long timestamp) {
        return timestamp > EPOCH_MILLIS_THRESHOLD ? timestamp / ONE_SECOND_IN_MILLIS : timestamp;
    }
}
