package org.pdzsoftware.featuremanager.repostiory;

import org.pdzsoftware.featuremanager.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {
    @Query("select avg(t.amount) from TransactionEntity t where t.user.id = :userId")
    Double findAverageAmountByUserId(@Param("userId") String userId);
}
