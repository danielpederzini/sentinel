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
import static org.pdzsoftware.featuremanager.support.TestConstants.CACHE_MEMBER_TXN_1_AMOUNT;
import static org.pdzsoftware.featuremanager.support.TestConstants.CACHE_MEMBER_TXN_1_MERCHANT;
import static org.pdzsoftware.featuremanager.support.TestConstants.CACHE_MEMBER_TXN_2_AMOUNT;
import static org.pdzsoftware.featuremanager.support.TestConstants.CACHE_MEMBER_TXN_2_MERCHANT;
import static org.pdzsoftware.featuremanager.support.TestConstants.CACHE_MEMBER_TXN_3_MERCHANT;
import static org.pdzsoftware.featuremanager.support.TestConstants.DISTINCT_MERCHANT_COUNT;
import static org.pdzsoftware.featuremanager.support.TestConstants.ERROR_MESSAGE_BAD_MEMBER;
import static org.pdzsoftware.featuremanager.support.TestConstants.ERROR_MESSAGE_INVALID_RANGE;
import static org.pdzsoftware.featuremanager.support.TestConstants.FIXED_EPOCH_SECONDS;
import static org.pdzsoftware.featuremanager.support.TestConstants.MERCHANT_ID;
import static org.pdzsoftware.featuremanager.support.TestConstants.REDIS_CLEANUP_KEY_COUNT;
import static org.pdzsoftware.featuremanager.support.TestConstants.REDIS_SCORE_MIN;
import static org.pdzsoftware.featuremanager.support.TestConstants.REDIS_TIME_TO_LAST;
import static org.pdzsoftware.featuremanager.support.TestConstants.SECONDS_AGO_FOR_DELTA_TEST;
import static org.pdzsoftware.featuremanager.support.TestConstants.SECONDS_SINCE_LAST_DEFAULT;
import static org.pdzsoftware.featuremanager.support.TestConstants.TRANSACTION_AMOUNT_SINGLE;
import static org.pdzsoftware.featuremanager.support.TestConstants.TRANSACTION_AMOUNT_SMALL;
import static org.pdzsoftware.featuremanager.support.TestConstants.TRANSACTION_COUNT_5MIN;
import static org.pdzsoftware.featuremanager.support.TestConstants.TRANSACTION_ID_1;
import static org.pdzsoftware.featuremanager.support.TestConstants.USER_ID;
import static org.pdzsoftware.featuremanager.support.TestConstants.AMOUNT_VELOCITY_SUM;
import static org.pdzsoftware.featuremanager.support.TestConstants.DELTA_TOLERANCE_LOWER;
import static org.pdzsoftware.featuremanager.support.TestConstants.DELTA_TOLERANCE_UPPER;
import static org.pdzsoftware.featuremanager.support.TestConstants.MILLIS_PER_SECOND;
import static org.pdzsoftware.featuremanager.support.TestConstants.userAmountsKey;
import static org.pdzsoftware.featuremanager.support.TestConstants.userLastTransactionKey;
import static org.pdzsoftware.featuremanager.support.TestConstants.userMerchantsKey;
import static org.pdzsoftware.featuremanager.support.TestConstants.userTransactionsKey;
import static org.pdzsoftware.featuremanager.support.TestConstants.fixedEpochMillis;

@ExtendWith(MockitoExtension.class)
class FeatureCacheServiceTest {

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
        lenient().when(redisProperties.getTimeToLast()).thenReturn(REDIS_TIME_TO_LAST);
    }

    @Test
    void recordUserTransaction_shouldWriteToRedisZSetsAndLastTransactionKey() {
        featureCacheService.recordUserTransaction(
                USER_ID, TRANSACTION_ID_1, TRANSACTION_AMOUNT_SMALL, MERCHANT_ID, fixedEpochMillis());

        verify(zSetOperations).add(eq(userTransactionsKey(USER_ID)), anyString(), eq((double) FIXED_EPOCH_SECONDS));
        verify(zSetOperations).add(eq(userAmountsKey(USER_ID)), anyString(), eq((double) FIXED_EPOCH_SECONDS));
        verify(zSetOperations).add(eq(userMerchantsKey(USER_ID)), anyString(), eq((double) FIXED_EPOCH_SECONDS));
        verify(valueOperations).set(
                eq(userLastTransactionKey(USER_ID)),
                eq(String.valueOf(FIXED_EPOCH_SECONDS)),
                eq(REDIS_TIME_TO_LAST));
        verify(redisTemplate).expire(userTransactionsKey(USER_ID), REDIS_TIME_TO_LAST);
    }

    @Test
    void recordUserTransaction_shouldWrapIllegalArgumentException() {
        doThrow(new IllegalArgumentException(ERROR_MESSAGE_BAD_MEMBER))
                .when(zSetOperations).add(anyString(), anyString(), anyDouble());

        assertThatThrownBy(() -> featureCacheService.recordUserTransaction(
                USER_ID, TRANSACTION_ID_1, TRANSACTION_AMOUNT_SINGLE, MERCHANT_ID, fixedEpochMillis()))
                .isInstanceOf(CacheUpdateException.class)
                .hasMessageContaining(TRANSACTION_ID_1);
    }

    @Test
    void getUserTransactionCount5Min_shouldReturnCountFromRedis() {
        when(zSetOperations.count(anyString(), anyDouble(), anyDouble())).thenReturn(TRANSACTION_COUNT_5MIN);

        assertThat(featureCacheService.getUserTransactionCount5Min(USER_ID)).isEqualTo(TRANSACTION_COUNT_5MIN);
    }

    @Test
    void getUserTransactionCount5Min_shouldReturnZero_whenCountIsNull() {
        when(zSetOperations.count(anyString(), anyDouble(), anyDouble())).thenReturn(null);

        assertThat(featureCacheService.getUserTransactionCount5Min(USER_ID)).isZero();
    }

    @Test
    void getSecondsSinceLastTransaction_shouldReturnDefault_whenNoLastTransaction() {
        when(valueOperations.get(userLastTransactionKey(USER_ID))).thenReturn(null);

        assertThat(featureCacheService.getSecondsSinceLastTransaction(USER_ID))
                .isEqualTo(SECONDS_SINCE_LAST_DEFAULT);
    }

    @Test
    void getSecondsSinceLastTransaction_shouldComputeDelta_whenLastTransactionExists() {
        long nowSeconds = System.currentTimeMillis() / MILLIS_PER_SECOND;
        long lastSeconds = nowSeconds - SECONDS_AGO_FOR_DELTA_TEST;
        when(valueOperations.get(userLastTransactionKey(USER_ID))).thenReturn(String.valueOf(lastSeconds));

        long result = featureCacheService.getSecondsSinceLastTransaction(USER_ID);

        assertThat(result).isBetween(DELTA_TOLERANCE_LOWER, DELTA_TOLERANCE_UPPER);
    }

    @Test
    void getAmountVelocity1Hour_shouldSumAmountsFromMembers() {
        Set<String> members = new LinkedHashSet<>();
        members.add(CACHE_MEMBER_TXN_1_AMOUNT);
        members.add(CACHE_MEMBER_TXN_2_AMOUNT);
        when(zSetOperations.rangeByScore(anyString(), anyDouble(), anyDouble())).thenReturn(members);

        assertThat(featureCacheService.getAmountVelocity1Hour(USER_ID)).isEqualByComparingTo(AMOUNT_VELOCITY_SUM);
    }

    @Test
    void getAmountVelocity1Hour_shouldReturnZero_whenNoMembers() {
        when(zSetOperations.rangeByScore(anyString(), anyDouble(), anyDouble())).thenReturn(Set.of());

        assertThat(featureCacheService.getAmountVelocity1Hour(USER_ID)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getDistinctMerchantCount1Hour_shouldCountUniqueMerchants() {
        Set<String> members = new LinkedHashSet<>();
        members.add(CACHE_MEMBER_TXN_1_MERCHANT);
        members.add(CACHE_MEMBER_TXN_2_MERCHANT);
        members.add(CACHE_MEMBER_TXN_3_MERCHANT);
        when(zSetOperations.rangeByScore(anyString(), anyDouble(), anyDouble())).thenReturn(members);

        assertThat(featureCacheService.getDistinctMerchantCount1Hour(USER_ID)).isEqualTo(DISTINCT_MERCHANT_COUNT);
    }

    @Test
    void getTransactionCountInWindow_shouldThrowCacheReadException_onIllegalArgument() {
        when(zSetOperations.count(anyString(), anyDouble(), anyDouble()))
                .thenThrow(new IllegalArgumentException(ERROR_MESSAGE_INVALID_RANGE));

        assertThatThrownBy(() -> featureCacheService.getUserTransactionCount1Hour(USER_ID))
                .isInstanceOf(CacheReadException.class)
                .hasMessageContaining(USER_ID);
    }

    @Test
    void cleanupOldTransactions_shouldRemoveScoresOlderThanOneHour() {
        featureCacheService.cleanupOldTransactions(USER_ID);

        ArgumentCaptor<Double> maxScoreCaptor = ArgumentCaptor.forClass(Double.class);
        verify(zSetOperations).removeRangeByScore(eq(userTransactionsKey(USER_ID)), eq(REDIS_SCORE_MIN), maxScoreCaptor.capture());
        verify(zSetOperations).removeRangeByScore(eq(userAmountsKey(USER_ID)), eq(REDIS_SCORE_MIN), maxScoreCaptor.capture());
        verify(zSetOperations).removeRangeByScore(eq(userMerchantsKey(USER_ID)), eq(REDIS_SCORE_MIN), maxScoreCaptor.capture());
        assertThat(maxScoreCaptor.getAllValues()).hasSize(REDIS_CLEANUP_KEY_COUNT);
    }
}
