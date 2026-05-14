package org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.repostiory;

import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.entity.CardEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<CardEntity, String> {
}

