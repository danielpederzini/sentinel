package org.pdzsoftware.featuremanager.service;

import lombok.RequiredArgsConstructor;
import org.pdzsoftware.featuremanager.entity.TransactionEntity;
import org.pdzsoftware.featuremanager.repostiory.TransactionRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;

    public List<TransactionEntity> findAll() {
        return transactionRepository.findAll();
    }

    public Optional<TransactionEntity> findById(String id) {
        return transactionRepository.findById(id);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "userAverageAmounts", key = "#transaction.user.id", condition = "#transaction.user != null"),
            @CacheEvict(cacheNames = "userAverageAmounts", allEntries = true, condition = "#transaction.user == null")
    })
    public TransactionEntity save(TransactionEntity transaction) {
        return transactionRepository.save(transaction);
    }

    public boolean existsById(String id) {
        return transactionRepository.existsById(id);
    }

    @CacheEvict(cacheNames = "userAverageAmounts", allEntries = true)
    public void deleteById(String id) {
        transactionRepository.deleteById(id);
    }

    @Cacheable(cacheNames = "userAverageAmounts", key = "#userId")
    public BigDecimal findAverageAmountByUserId(String userId) {
        Double average = transactionRepository.findAverageAmountByUserId(userId);
        return average == null ? BigDecimal.ZERO : BigDecimal.valueOf(average);
    }
}
