package org.pdzsoftware.featuremanager.service;

import lombok.RequiredArgsConstructor;
import org.pdzsoftware.featuremanager.entity.CardEntity;
import org.pdzsoftware.featuremanager.repostiory.CardRepository;
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

    public Optional<CardEntity> findById(String id) {
        return cardRepository.findById(id);
    }

    public CardEntity save(CardEntity card) {
        return cardRepository.save(card);
    }

    public boolean existsById(String id) {
        return cardRepository.existsById(id);
    }

    public void deleteById(String id) {
        cardRepository.deleteById(id);
    }
}
