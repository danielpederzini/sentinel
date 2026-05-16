package org.pdzsoftware.riskactionhandler.application.service;

import lombok.RequiredArgsConstructor;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.entity.TransactionEntity;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.repository.TransactionRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;

    public String save(TransactionEntity transaction) {
        TransactionEntity savedTransaction = transactionRepository.save(transaction);
        return savedTransaction.getId();
    }
}
