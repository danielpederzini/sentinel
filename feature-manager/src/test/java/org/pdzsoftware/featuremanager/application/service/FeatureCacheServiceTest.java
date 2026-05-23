package org.pdzsoftware.featuremanager.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pdzsoftware.featuremanager.domain.exception.CacheReadException;
import org.pdzsoftware.featuremanager.domain.exception.CacheUpdateException;
import org.pdzsoftware.featuremanager.infrastructure.config.properties.RedisProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureCacheServiceTest {

    private static final long FIXED_EPOCH_SECONDS = 1_700_000_000L;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private RedisProperties redisProperties;

    @InjectMocks
    private FeatureCacheService featureCacheService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisProperties.getTimeToLast()).thenReturn(Duration.ofMinutes(10));
    }

    @Test
    void recordUserTransaction_shouldWriteToRedisZSetsAndLastTransactionKey() {
        long timestampMillis = FIXED_EPOCH_SECONDS * 1_000L;

        featureCacheService.recordUserTransaction("user-1", "txn-1", BigDecimal.TEN, "merchant-1", timestampMillis);

        verify(zSetOperations).add(eq("user:transactions:user-1"), anyString(), eq((double) FIXED_EPOCH_SECONDS));
        verify(zSetOperations).add(eq("user:transaction-amounts:user-1"), anyString(), eq((double) FIXED_EPOCH_SECONDS));
        verify(zSetOperations).add(eq("user:transaction-merchants:user-1"), anyString(), eq((double) FIXED_EPOCH_SECONDS));
        verify(valueOperations).set(eq("user:last-transaction:user-1"), eq(String.valueOf(FIXED_EPOCH_SECONDS)), eq(Duration.ofMinutes(10)));
        verify(redisTemplate).expire("user:transactions:user-1", Duration.ofMinutes(10));
    }

    @Test
    void recordUserTransaction_shouldWrapIllegalArgumentException() {
        doThrow(new IllegalArgumentException("bad member"))
                .when(zSetOperations).add(anyString(), anyString(), anyDouble());

        assertThatThrownBy(() -> featureCacheService.recordUserTransaction(
                "user-1", "txn-1", BigDecimal.ONE, "merchant-1", FIXED_EPOCH_SECONDS * 1_000L))
                .isInstanceOf(CacheUpdateException.class)
                .hasMessageContaining("txn-1");
    }

    @Test
    void getUserTransactionCount5Min_shouldReturnCountFromRedis() {
        when(zSetOperations.count(anyString(), anyDouble(), anyDouble())).thenReturn(3L);

        assertThat(featureCacheService.getUserTransactionCount5Min("user-1")).isEqualTo(3L);
    }

    @Test
    void getUserTransactionCount5Min_shouldReturnZero_whenCountIsNull() {
        when(zSetOperations.count(anyString(), anyDouble(), anyDouble())).thenReturn(null);

        assertThat(featureCacheService.getUserTransactionCount5Min("user-1")).isZero();
    }

    @Test
    void getSecondsSinceLastTransaction_shouldReturnDefault_whenNoLastTransaction() {
        when(valueOperations.get("user:last-transaction:user-1")).thenReturn(null);

        assertThat(featureCacheService.getSecondsSinceLastTransaction("user-1"))
                .isEqualTo(30L * 24L * 3600L);
    }

    @Test
    void getSecondsSinceLastTransaction_shouldComputeDelta_whenLastTransactionExists() {
        long nowSeconds = System.currentTimeMillis() / 1_000L;
        long lastSeconds = nowSeconds - 120L;
        when(valueOperations.get("user:last-transaction:user-1")).thenReturn(String.valueOf(lastSeconds));

        long result = featureCacheService.getSecondsSinceLastTransaction("user-1");

        assertThat(result).isBetween(115L, 125L);
    }

    @Test
    void getAmountVelocity1Hour_shouldSumAmountsFromMembers() {
        Set<String> members = new LinkedHashSet<>();
        members.add("txn-1:10.50");
        members.add("txn-2:20");
        when(zSetOperations.rangeByScore(anyString(), anyDouble(), anyDouble())).thenReturn(members);

        assertThat(featureCacheService.getAmountVelocity1Hour("user-1")).isEqualByComparingTo("30.50");
    }

    @Test
    void getAmountVelocity1Hour_shouldReturnZero_whenNoMembers() {
        when(zSetOperations.rangeByScore(anyString(), anyDouble(), anyDouble())).thenReturn(Set.of());

        assertThat(featureCacheService.getAmountVelocity1Hour("user-1")).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getDistinctMerchantCount1Hour_shouldCountUniqueMerchants() {
        Set<String> members = new LinkedHashSet<>();
        members.add("txn-1:merchant-a");
        members.add("txn-2:merchant-a");
        members.add("txn-3:merchant-b");
        when(zSetOperations.rangeByScore(anyString(), anyDouble(), anyDouble())).thenReturn(members);

        assertThat(featureCacheService.getDistinctMerchantCount1Hour("user-1")).isEqualTo(2L);
    }

    @Test
    void getTransactionCountInWindow_shouldThrowCacheReadException_onIllegalArgument() {
        when(zSetOperations.count(anyString(), anyDouble(), anyDouble()))
                .thenThrow(new IllegalArgumentException("invalid range"));

        assertThatThrownBy(() -> featureCacheService.getUserTransactionCount1Hour("user-1"))
                .isInstanceOf(CacheReadException.class)
                .hasMessageContaining("user-1");
    }

    @Test
    void cleanupOldTransactions_shouldRemoveScoresOlderThanOneHour() {
        featureCacheService.cleanupOldTransactions("user-1");

        ArgumentCaptor<Double> maxScoreCaptor = ArgumentCaptor.forClass(Double.class);
        verify(zSetOperations).removeRangeByScore(eq("user:transactions:user-1"), eq(0.0), maxScoreCaptor.capture());
        verify(zSetOperations).removeRangeByScore(eq("user:transaction-amounts:user-1"), eq(0.0), maxScoreCaptor.capture());
        verify(zSetOperations).removeRangeByScore(eq("user:transaction-merchants:user-1"), eq(0.0), maxScoreCaptor.capture());
        assertThat(maxScoreCaptor.getAllValues()).hasSize(3);
    }
}
