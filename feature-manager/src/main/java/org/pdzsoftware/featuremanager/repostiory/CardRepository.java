package org.pdzsoftware.featuremanager.repostiory;

import org.pdzsoftware.featuremanager.entity.CardEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<CardEntity, String> {
}

