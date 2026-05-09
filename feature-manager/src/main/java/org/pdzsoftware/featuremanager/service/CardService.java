package org.pdzsoftware.featuremanager.service;

import lombok.RequiredArgsConstructor;
import org.pdzsoftware.featuremanager.entity.CardEntity;
import org.pdzsoftware.featuremanager.repostiory.CardRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CardService {
    private final CardRepository cardRepository;

    public List<CardEntity> findAll() {
        return cardRepository.findAll();
    }

    @Cacheable(cacheNames = "cards", key = "#id")
    public Optional<CardEntity> findById(String id) {
        return cardRepository.findById(id);
    }

    @Caching(
            put = {@CachePut(cacheNames = "cards", key = "#card.id")},
            evict = {@CacheEvict(cacheNames = "cardsExists", key = "#card.id")}
    )
    public CardEntity save(CardEntity card) {
        return cardRepository.save(card);
    }

    @Cacheable(cacheNames = "cardsExists", key = "#id")
    public boolean existsById(String id) {
        return cardRepository.existsById(id);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "cards", key = "#id"),
            @CacheEvict(cacheNames = "cardsExists", key = "#id")
    })
    public void deleteById(String id) {
        cardRepository.deleteById(id);
    }
}
