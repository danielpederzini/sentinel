package org.pdzsoftware.featuremanager.service;

import lombok.RequiredArgsConstructor;
import org.pdzsoftware.featuremanager.entity.TrustedDeviceEntity;
import org.pdzsoftware.featuremanager.repostiory.TrustedDeviceRepository;
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

    public Optional<TrustedDeviceEntity> findById(String id) {
        return trustedDeviceRepository.findById(id);
    }

    public TrustedDeviceEntity save(TrustedDeviceEntity trustedDevice) {
        return trustedDeviceRepository.save(trustedDevice);
    }

    public boolean existsById(String id) {
        return trustedDeviceRepository.existsById(id);
    }

    public void deleteById(String id) {
        trustedDeviceRepository.deleteById(id);
    }
}
