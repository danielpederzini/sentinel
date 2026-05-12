package org.pdzsoftware.featuremanager.infrastructure.persistence.repostiory;

import org.pdzsoftware.featuremanager.infrastructure.persistence.entity.MerchantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantRepository extends JpaRepository<MerchantEntity, String> {
}

