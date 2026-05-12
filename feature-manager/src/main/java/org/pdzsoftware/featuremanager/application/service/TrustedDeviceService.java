package org.pdzsoftware.featuremanager.application.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.pdzsoftware.featuremanager.infrastructure.persistence.repostiory.TrustedDeviceRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TrustedDeviceService {
    private final TrustedDeviceRepository trustedDeviceRepository;

    @Cacheable(cacheNames = "trustedDevicesExists", key = "#a0", condition = "#a0 != null")
    public boolean existsById(String id) {
        if (StringUtils.isBlank(id)) {
            return false;
        }
        return trustedDeviceRepository.existsById(id);
    }
}
