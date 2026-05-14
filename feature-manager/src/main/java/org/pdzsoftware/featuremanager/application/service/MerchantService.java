package org.pdzsoftware.featuremanager.application.service;

import lombok.RequiredArgsConstructor;
import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.entity.MerchantEntity;
import org.pdzsoftware.featuremanager.domain.exception.MerchantNotFoundException;
import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.repostiory.MerchantRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MerchantService {
    private final MerchantRepository merchantRepository;

    @Cacheable(cacheNames = "merchants", key = "#a0", condition = "#a0 != null")
    public MerchantEntity getByIdOrThrow(String id) {
        return merchantRepository.findById(id)
                .orElseThrow(() -> new MerchantNotFoundException(String.format("Merchant with ID %s not found", id)));
    }
}
