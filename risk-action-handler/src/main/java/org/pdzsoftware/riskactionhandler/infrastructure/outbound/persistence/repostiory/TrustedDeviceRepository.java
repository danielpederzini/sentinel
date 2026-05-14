package org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.repostiory;

import org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.entity.TrustedDeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrustedDeviceRepository extends JpaRepository<TrustedDeviceEntity, String> {
}

