package org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.repository;

import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, String> {
}

