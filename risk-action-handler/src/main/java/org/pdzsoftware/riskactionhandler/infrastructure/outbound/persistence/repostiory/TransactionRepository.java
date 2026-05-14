package org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.repostiory;

import org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {
}
