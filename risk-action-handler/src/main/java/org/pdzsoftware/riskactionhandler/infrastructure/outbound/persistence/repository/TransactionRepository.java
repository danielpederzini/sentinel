package org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.repository;

import org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {
}
