package org.pdzsoftware.featuremanager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeatureCacheService {
    private static final String USER_TRANSACTIONS_KEY_PREFIX = "user:transactions:";
    private static final String USER_LAST_TRANSACTION_KEY_PREFIX = "user:last-transaction:";
    private static final long FIVE_MINUTES_IN_SECONDS = 5L * 60L;
    private static final long ONE_HOUR_IN_SECONDS = 60L * 60L;

    private final RedisTemplate<String, Object> redisTemplate;

    public void recordUserTransaction(String userId, String transactionId, long timestamp) {
        String userTransactionKey = USER_TRANSACTIONS_KEY_PREFIX + userId;
        redisTemplate.opsForZSet().add(userTransactionKey, transactionId, timestamp);

        String lastTransactionKey = USER_LAST_TRANSACTION_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(lastTransactionKey, timestamp);
    }

    public long getUserTransactionCount5Min(String userId) {
        long now = System.currentTimeMillis() / 1000;
        long windowStart = now - FIVE_MINUTES_IN_SECONDS;
        return getTransactionCountInWindow(userId, windowStart, now);
    }

    public long getUserTransactionCount1Hour(String userId) {
        long now = System.currentTimeMillis() / 1000;
        long windowStart = now - ONE_HOUR_IN_SECONDS;
        return getTransactionCountInWindow(userId, windowStart, now);
    }

    private long getTransactionCountInWindow(String userId, long windowStart, long windowEnd) {
        String userTransactionKey = USER_TRANSACTIONS_KEY_PREFIX + userId;
        Long count = redisTemplate.opsForZSet().count(userTransactionKey, windowStart, windowEnd);
        return count != null ? count : 0L;
    }

    public long getSecondsSinceLastTransaction(String userId) {
        String lastTransactionKey = USER_LAST_TRANSACTION_KEY_PREFIX + userId;
        Object lastTimestamp = redisTemplate.opsForValue().get(lastTransactionKey);

        if (lastTimestamp == null) {
            return Long.MAX_VALUE;
        }

        long lastTransactionTime;
        if (lastTimestamp instanceof Long transactionTime) {
            lastTransactionTime = transactionTime;
        } else {
            lastTransactionTime = Long.parseLong(lastTimestamp.toString());
        }

        long now = System.currentTimeMillis() / 1000;
        return now - lastTransactionTime;
    }

    public void cleanupOldTransactions(String userId) {
        String userTransactionKey = USER_TRANSACTIONS_KEY_PREFIX + userId;
        long now = System.currentTimeMillis() / 1000;
        long cutoffTime = now - ONE_HOUR_IN_SECONDS;

        redisTemplate.opsForZSet().removeRangeByScore(userTransactionKey, 0, cutoffTime);
    }

    public void clearUserCache(String userId) {
        String userTransactionKey = USER_TRANSACTIONS_KEY_PREFIX + userId;
        String lastTransactionKey = USER_LAST_TRANSACTION_KEY_PREFIX + userId;
        redisTemplate.delete(userTransactionKey);
        redisTemplate.delete(lastTransactionKey);
    }
}




