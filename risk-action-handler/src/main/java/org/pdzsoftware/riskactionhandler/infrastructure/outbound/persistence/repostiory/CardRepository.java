package org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.repostiory;

import org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.entity.CardEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<CardEntity, String> {
}

