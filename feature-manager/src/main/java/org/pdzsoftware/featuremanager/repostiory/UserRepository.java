package org.pdzsoftware.featuremanager.repostiory;

import org.pdzsoftware.featuremanager.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, String> {
}

