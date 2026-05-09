package org.pdzsoftware.featuremanager.service;

import lombok.RequiredArgsConstructor;
import org.pdzsoftware.featuremanager.entity.CardEntity;
import org.pdzsoftware.featuremanager.repostiory.CardRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CardService {
    private final CardRepository cardRepository;

    @Cacheable(cacheNames = "cards", key = "#a0", condition = "#a0 != null")
    public Optional<CardEntity> findById(String id) {
        return cardRepository.findById(id);
    }

    @Cacheable(cacheNames = "cardsExists", key = "#a0", condition = "#a0 != null")
    public boolean existsById(String id) {
        return cardRepository.existsById(id);
    }
}
