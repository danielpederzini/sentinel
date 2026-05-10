package org.pdzsoftware.featuremanager.service;

import lombok.RequiredArgsConstructor;
import org.pdzsoftware.featuremanager.repostiory.TransactionRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;

    @Cacheable(cacheNames = "userAverageAmounts", key = "#a0", condition = "#a0 != null")
    public BigDecimal findAverageAmountByUserId(String userId) {
        Double average = transactionRepository.findAverageAmountByUserId(userId);
        return average == null ? BigDecimal.ZERO : BigDecimal.valueOf(average);
    }
}
