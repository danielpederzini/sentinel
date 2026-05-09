package org.pdzsoftware.featuremanager.repostiory;

import org.pdzsoftware.featuremanager.entity.MerchantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantRepository extends JpaRepository<MerchantEntity, String> {
}

