package org.pdzsoftware.featuremanager.repostiory;

import org.pdzsoftware.featuremanager.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {
}
