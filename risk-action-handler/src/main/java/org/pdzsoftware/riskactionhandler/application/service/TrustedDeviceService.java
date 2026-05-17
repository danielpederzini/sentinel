package org.pdzsoftware.riskactionhandler.application.service;

import lombok.RequiredArgsConstructor;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.repository.TrustedDeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TrustedDeviceService {
    private final TrustedDeviceRepository trustedDeviceRepository;

    public boolean existsById(String id) {
        if (!StringUtils.hasText(id)) {
            return false;
        }
        return trustedDeviceRepository.existsById(id);
    }
}
