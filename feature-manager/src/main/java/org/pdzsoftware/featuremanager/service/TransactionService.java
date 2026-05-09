package org.pdzsoftware.featuremanager.service;

import lombok.RequiredArgsConstructor;
import org.pdzsoftware.featuremanager.entity.TransactionEntity;
import org.pdzsoftware.featuremanager.repostiory.TransactionRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;

    @Caching(evict = {
            @CacheEvict(cacheNames = "userAverageAmounts", key = "#a0.user.id", condition = "#a0 != null && #a0.user != null"),
            @CacheEvict(cacheNames = "userAverageAmounts", allEntries = true, condition = "#a0 == null || #a0.user == null")
    })
    public TransactionEntity save(TransactionEntity transaction) {
        return transactionRepository.save(transaction);
    }

    @Cacheable(cacheNames = "userAverageAmounts", key = "#a0", condition = "#a0 != null")
    public BigDecimal findAverageAmountByUserId(String userId) {
        Double average = transactionRepository.findAverageAmountByUserId(userId);
        return average == null ? BigDecimal.ZERO : BigDecimal.valueOf(average);
    }
}
