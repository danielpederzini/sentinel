package org.pdzsoftware.featuremanager.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.entity.TransactionEntity;
import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.repository.TransactionRepository;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void save_shouldReturnPersistedId() {
        TransactionEntity entity = TransactionEntity.builder().id("txn-1").build();
        when(transactionRepository.save(entity)).thenReturn(entity);

        assertThat(transactionService.save(entity)).isEqualTo("txn-1");
        verify(transactionRepository).save(entity);
    }

    @Test
    void findAverageAmountByUserId_shouldReturnZero_whenRepositoryReturnsNull() {
        when(transactionRepository.findAverageAmountByUserId("user-1")).thenReturn(null);

        assertThat(transactionService.findAverageAmountByUserId("user-1")).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void findAverageAmountByUserId_shouldReturnBigDecimal_whenRepositoryReturnsValue() {
        when(transactionRepository.findAverageAmountByUserId("user-1")).thenReturn(42.5);

        assertThat(transactionService.findAverageAmountByUserId("user-1")).isEqualByComparingTo("42.5");
    }

    @Test
    void countByUserId_shouldDelegateToRepository() {
        when(transactionRepository.countByUserId("user-1")).thenReturn(7L);

        assertThat(transactionService.countByUserId("user-1")).isEqualTo(7L);
    }

    @Test
    void findIpRiskScoreByIpAddress_shouldReturnZero_whenBlank() {
        assertThat(transactionService.findIpRiskScoreByIpAddress("")).isZero();
        assertThat(transactionService.findIpRiskScoreByIpAddress(null)).isZero();
    }

    @Test
    void findIpRiskScoreByIpAddress_shouldReturnScore_whenPresent() {
        when(transactionRepository.findIpRiskScoreByIpAddress("203.0.113.1")).thenReturn(0.75);

        assertThat(transactionService.findIpRiskScoreByIpAddress("203.0.113.1")).isEqualTo(0.75f);
    }

    @Test
    void findIpRiskScoreByIpAddress_shouldReturnZero_whenRepositoryReturnsNull() {
        when(transactionRepository.findIpRiskScoreByIpAddress("203.0.113.1")).thenReturn(null);

        assertThat(transactionService.findIpRiskScoreByIpAddress("203.0.113.1")).isZero();
    }
}
