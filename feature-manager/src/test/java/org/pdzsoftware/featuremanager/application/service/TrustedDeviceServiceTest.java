package org.pdzsoftware.featuremanager.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.repository.TrustedDeviceRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pdzsoftware.featuremanager.support.TestConstants.BLANK_DEVICE_ID;
import static org.pdzsoftware.featuremanager.support.TestConstants.DEVICE_ID;

@ExtendWith(MockitoExtension.class)
class TrustedDeviceServiceTest {

    @Mock
    private TrustedDeviceRepository trustedDeviceRepository;

    @InjectMocks
    private TrustedDeviceService trustedDeviceService;

    @Test
    void existsById_shouldReturnFalse_whenIdBlank() {
        assertThat(trustedDeviceService.existsById("")).isFalse();
        assertThat(trustedDeviceService.existsById(BLANK_DEVICE_ID)).isFalse();
        assertThat(trustedDeviceService.existsById(null)).isFalse();

        verify(trustedDeviceRepository, never()).existsById(any());
    }

    @Test
    void existsById_shouldDelegateToRepository_whenIdPresent() {
        when(trustedDeviceRepository.existsById(DEVICE_ID)).thenReturn(true);

        assertThat(trustedDeviceService.existsById(DEVICE_ID)).isTrue();
        verify(trustedDeviceRepository).existsById(DEVICE_ID);
    }
}
