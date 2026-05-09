package org.pdzsoftware.featuremanager.service;

import lombok.RequiredArgsConstructor;
import org.pdzsoftware.featuremanager.entity.TrustedDeviceEntity;
import org.pdzsoftware.featuremanager.repostiory.TrustedDeviceRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TrustedDeviceService {
    private final TrustedDeviceRepository trustedDeviceRepository;

    public List<TrustedDeviceEntity> findAll() {
        return trustedDeviceRepository.findAll();
    }

    @Cacheable(cacheNames = "trustedDevices", key = "#id")
    public Optional<TrustedDeviceEntity> findById(String id) {
        return trustedDeviceRepository.findById(id);
    }

    @Caching(
            put = {@CachePut(cacheNames = "trustedDevices", key = "#trustedDevice.id")},
            evict = {@CacheEvict(cacheNames = "trustedDevicesExists", key = "#trustedDevice.id")}
    )
    public TrustedDeviceEntity save(TrustedDeviceEntity trustedDevice) {
        return trustedDeviceRepository.save(trustedDevice);
    }

    @Cacheable(cacheNames = "trustedDevicesExists", key = "#id")
    public boolean existsById(String id) {
        return trustedDeviceRepository.existsById(id);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "trustedDevices", key = "#id"),
            @CacheEvict(cacheNames = "trustedDevicesExists", key = "#id")
    })
    public void deleteById(String id) {
        trustedDeviceRepository.deleteById(id);
    }
}
