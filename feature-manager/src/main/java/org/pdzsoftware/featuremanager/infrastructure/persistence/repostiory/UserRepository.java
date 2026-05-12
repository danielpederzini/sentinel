package org.pdzsoftware.featuremanager.infrastructure.persistence.repostiory;

import org.pdzsoftware.featuremanager.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, String> {
}

