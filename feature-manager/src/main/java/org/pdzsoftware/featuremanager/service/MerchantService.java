package org.pdzsoftware.featuremanager.service;

import lombok.RequiredArgsConstructor;
import org.pdzsoftware.featuremanager.entity.MerchantEntity;
import org.pdzsoftware.featuremanager.repostiory.MerchantRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MerchantService {
    private final MerchantRepository merchantRepository;

    public List<MerchantEntity> findAll() {
        return merchantRepository.findAll();
    }

    public Optional<MerchantEntity> findById(String id) {
        return merchantRepository.findById(id);
    }

    public MerchantEntity save(MerchantEntity merchant) {
        return merchantRepository.save(merchant);
    }

    public boolean existsById(String id) {
        return merchantRepository.existsById(id);
    }

    public void deleteById(String id) {
        merchantRepository.deleteById(id);
    }
}
