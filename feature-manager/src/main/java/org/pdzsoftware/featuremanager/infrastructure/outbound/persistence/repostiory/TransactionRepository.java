package org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.repostiory;

import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {
    @Query("select avg(t.amount) from TransactionEntity t where t.user.id = :userId")
    Double findAverageAmountByUserId(@Param("userId") String userId);

    @Query("""
            select coalesce(
                sum(case
                        when tp.riskLevel = org.pdzsoftware.featuremanager.domain.enums.RiskLevel.HIGH then 1.0
                        when tp.riskLevel = org.pdzsoftware.featuremanager.domain.enums.RiskLevel.MEDIUM then 0.5
                        else 0.0
                    end) / nullif(count(t), 0),
                0.0
            )
            from TransactionEntity t
            left join t.prediction tp
            where t.ipAddress = :ipAddress
            """)
    Double findIpRiskScoreByIpAddress(@Param("ipAddress") String ipAddress);
}
