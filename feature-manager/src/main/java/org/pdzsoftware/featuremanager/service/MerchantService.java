package org.pdzsoftware.featuremanager.service;

import lombok.RequiredArgsConstructor;
import org.pdzsoftware.featuremanager.entity.MerchantEntity;
import org.pdzsoftware.featuremanager.repostiory.MerchantRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
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

    @Cacheable(cacheNames = "merchants", key = "#id")
    public Optional<MerchantEntity> findById(String id) {
        return merchantRepository.findById(id);
    }

    @Caching(
            put = {@CachePut(cacheNames = "merchants", key = "#merchant.id")},
            evict = {@CacheEvict(cacheNames = "merchantsExists", key = "#merchant.id")}
    )
    public MerchantEntity save(MerchantEntity merchant) {
        return merchantRepository.save(merchant);
    }

    @Cacheable(cacheNames = "merchantsExists", key = "#id")
    public boolean existsById(String id) {
        return merchantRepository.existsById(id);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "merchants", key = "#id"),
            @CacheEvict(cacheNames = "merchantsExists", key = "#id")
    })
    public void deleteById(String id) {
        merchantRepository.deleteById(id);
    }
}
