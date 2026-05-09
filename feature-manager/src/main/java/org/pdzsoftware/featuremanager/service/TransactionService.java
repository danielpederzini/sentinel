package org.pdzsoftware.featuremanager.service;

import lombok.RequiredArgsConstructor;
import org.pdzsoftware.featuremanager.entity.TransactionEntity;
import org.pdzsoftware.featuremanager.repostiory.TransactionRepository;
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

    public TransactionEntity save(TransactionEntity transaction) {
        return transactionRepository.save(transaction);
    }

    public boolean existsById(String id) {
        return transactionRepository.existsById(id);
    }

    public void deleteById(String id) {
        transactionRepository.deleteById(id);
    }

        public BigDecimal findAverageAmountByUserId(String userId) {
        Double average = transactionRepository.findAverageAmountByUserId(userId);
        return average == null ? BigDecimal.ZERO : BigDecimal.valueOf(average);
    }
}
