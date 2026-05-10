package org.pdzsoftware.featuremanager.service;

import lombok.RequiredArgsConstructor;
import org.pdzsoftware.featuremanager.entity.CardEntity;
import org.pdzsoftware.featuremanager.exception.CardNotFoundException;
import org.pdzsoftware.featuremanager.repostiory.CardRepository;
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
