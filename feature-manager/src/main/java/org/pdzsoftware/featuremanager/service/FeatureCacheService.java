package org.pdzsoftware.featuremanager.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.pdzsoftware.featuremanager.config.RedisProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeatureCacheService {
    private static final String USER_TRANSACTIONS_KEY_PREFIX = "user:transactions:";
    private static final String USER_LAST_TRANSACTION_KEY_PREFIX = "user:last-transaction:";
    private static final long FIVE_MINUTES_IN_SECONDS = 5L * 60L;
    private static final long ONE_HOUR_IN_SECONDS = 60L * 60L;
    private static final long EPOCH_MILLIS_THRESHOLD = 10_000_000_000L;
    private static final long ONE_SECOND_IN_MILLIS = 1_000L;

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisProperties redisProperties;

    public void recordUserTransaction(String userId, String transactionId) {
        recordUserTransaction(userId, transactionId, System.currentTimeMillis());
    }

    public void recordUserTransaction(String userId, String transactionId, long timestamp) {
        long timestampInSeconds = normalizeToEpochSeconds(timestamp);
        String userTransactionKey = USER_TRANSACTIONS_KEY_PREFIX + userId;
        redisTemplate.opsForZSet().add(userTransactionKey, buildTransactionMember(transactionId), timestampInSeconds);
        redisTemplate.expire(userTransactionKey, redisProperties.getTimeToLast());

        String lastTransactionKey = USER_LAST_TRANSACTION_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(lastTransactionKey, String.valueOf(timestampInSeconds), redisProperties.getTimeToLast());
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
        String userTransactionKey = USER_TRANSACTIONS_KEY_PREFIX + userId;
        Long count = redisTemplate.opsForZSet().count(userTransactionKey, windowStart, windowEnd);
        return count != null ? count : 0L;
    }

    public long getSecondsSinceLastTransaction(String userId) {
        String lastTransactionKey = USER_LAST_TRANSACTION_KEY_PREFIX + userId;
        String lastTimestamp = redisTemplate.opsForValue().get(lastTransactionKey);

        if (lastTimestamp == null) {
            return Long.MAX_VALUE;
        }

        long lastTransactionTime = Long.parseLong(lastTimestamp);

        long nowInSeconds = System.currentTimeMillis() / ONE_SECOND_IN_MILLIS;
        return nowInSeconds - lastTransactionTime;
    }

    public void cleanupOldTransactions(String userId) {
        String userTransactionKey = USER_TRANSACTIONS_KEY_PREFIX + userId;
        long nowInSeconds = System.currentTimeMillis() / ONE_SECOND_IN_MILLIS;
        long cutoffTime = nowInSeconds - ONE_HOUR_IN_SECONDS;

        redisTemplate.opsForZSet().removeRangeByScore(userTransactionKey, 0, cutoffTime);
    }

    private String buildTransactionMember(String transactionId) {
        boolean isTransactionIdMissing = StringUtils.isBlank(transactionId);
        String memberPrefix = isTransactionIdMissing ? UUID.randomUUID().toString() : transactionId;
        return String.format("%s:%s", memberPrefix, System.currentTimeMillis());
    }

    private long normalizeToEpochSeconds(long timestamp) {
        return timestamp > EPOCH_MILLIS_THRESHOLD ? timestamp / ONE_SECOND_IN_MILLIS : timestamp;
    }

    public void clearUserCache(String userId) {
        String userTransactionKey = USER_TRANSACTIONS_KEY_PREFIX + userId;
        String lastTransactionKey = USER_LAST_TRANSACTION_KEY_PREFIX + userId;
        redisTemplate.delete(userTransactionKey);
        redisTemplate.delete(lastTransactionKey);
    }
}




