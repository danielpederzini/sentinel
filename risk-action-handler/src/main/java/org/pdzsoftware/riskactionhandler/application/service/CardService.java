package org.pdzsoftware.riskactionhandler.application.service;

import lombok.RequiredArgsConstructor;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.repostiory.CardRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CardService {
    private final CardRepository cardRepository;

    public boolean existsById(String id) {
        return cardRepository.existsById(id);
    }
}
