package org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.repository;

import org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.entity.MerchantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantRepository extends JpaRepository<MerchantEntity, String> {
}

