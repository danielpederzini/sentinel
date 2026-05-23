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
import static org.pdzsoftware.featuremanager.support.TestConstants.BLANK_IP_ADDRESS;
import static org.pdzsoftware.featuremanager.support.TestConstants.IP_ADDRESS;
import static org.pdzsoftware.featuremanager.support.TestConstants.IP_RISK_SCORE_HIGH;
import static org.pdzsoftware.featuremanager.support.TestConstants.REPOSITORY_AVERAGE_AMOUNT;
import static org.pdzsoftware.featuremanager.support.TestConstants.REPOSITORY_AVERAGE_AMOUNT_STRING;
import static org.pdzsoftware.featuremanager.support.TestConstants.TRANSACTION_ID_1;
import static org.pdzsoftware.featuremanager.support.TestConstants.USER_ID;
import static org.pdzsoftware.featuremanager.support.TestConstants.USER_TRANSACTION_COUNT;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void save_shouldReturnPersistedId() {
        TransactionEntity entity = TransactionEntity.builder().id(TRANSACTION_ID_1).build();
        when(transactionRepository.save(entity)).thenReturn(entity);

        assertThat(transactionService.save(entity)).isEqualTo(TRANSACTION_ID_1);
        verify(transactionRepository).save(entity);
    }

    @Test
    void findAverageAmountByUserId_shouldReturnZero_whenRepositoryReturnsNull() {
        when(transactionRepository.findAverageAmountByUserId(USER_ID)).thenReturn(null);

        assertThat(transactionService.findAverageAmountByUserId(USER_ID)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void findAverageAmountByUserId_shouldReturnBigDecimal_whenRepositoryReturnsValue() {
        when(transactionRepository.findAverageAmountByUserId(USER_ID)).thenReturn(REPOSITORY_AVERAGE_AMOUNT);

        assertThat(transactionService.findAverageAmountByUserId(USER_ID))
                .isEqualByComparingTo(REPOSITORY_AVERAGE_AMOUNT_STRING);
    }

    @Test
    void countByUserId_shouldDelegateToRepository() {
        when(transactionRepository.countByUserId(USER_ID)).thenReturn(USER_TRANSACTION_COUNT);

        assertThat(transactionService.countByUserId(USER_ID)).isEqualTo(USER_TRANSACTION_COUNT);
    }

    @Test
    void findIpRiskScoreByIpAddress_shouldReturnZero_whenBlank() {
        assertThat(transactionService.findIpRiskScoreByIpAddress(BLANK_IP_ADDRESS)).isZero();
        assertThat(transactionService.findIpRiskScoreByIpAddress(null)).isZero();
    }

    @Test
    void findIpRiskScoreByIpAddress_shouldReturnScore_whenPresent() {
        when(transactionRepository.findIpRiskScoreByIpAddress(IP_ADDRESS)).thenReturn((double) IP_RISK_SCORE_HIGH);

        assertThat(transactionService.findIpRiskScoreByIpAddress(IP_ADDRESS)).isEqualTo(IP_RISK_SCORE_HIGH);
    }

    @Test
    void findIpRiskScoreByIpAddress_shouldReturnZero_whenRepositoryReturnsNull() {
        when(transactionRepository.findIpRiskScoreByIpAddress(IP_ADDRESS)).thenReturn(null);

        assertThat(transactionService.findIpRiskScoreByIpAddress(IP_ADDRESS)).isZero();
    }
}
