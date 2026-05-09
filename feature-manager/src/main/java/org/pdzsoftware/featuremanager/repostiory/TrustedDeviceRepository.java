package org.pdzsoftware.featuremanager.repostiory;

import org.pdzsoftware.featuremanager.entity.TrustedDeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrustedDeviceRepository extends JpaRepository<TrustedDeviceEntity, String> {
}

