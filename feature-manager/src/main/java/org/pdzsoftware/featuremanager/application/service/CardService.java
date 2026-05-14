package org.pdzsoftware.featuremanager.application.service;

import lombok.RequiredArgsConstructor;
import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.entity.CardEntity;
import org.pdzsoftware.featuremanager.domain.exception.CardNotFoundException;
import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.repostiory.CardRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CardService {
    private final CardRepository cardRepository;

    @Cacheable(cacheNames = "cards", key = "#a0", condition = "#a0 != null")
    public CardEntity getByIdOrThrow(String id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException(String.format("Card with ID %s not found", id)));
    }
}
