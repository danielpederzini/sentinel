package org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.repository;

import org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.entity.TrustedDeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrustedDeviceRepository extends JpaRepository<TrustedDeviceEntity, String> {
}

