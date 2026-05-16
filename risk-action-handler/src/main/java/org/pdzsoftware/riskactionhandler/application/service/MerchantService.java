package org.pdzsoftware.riskactionhandler.application.service;

import lombok.RequiredArgsConstructor;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.repository.MerchantRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MerchantService {
    private final MerchantRepository merchantRepository;

    public boolean existsById(String id) {
        return merchantRepository.existsById(id);
    }
}
