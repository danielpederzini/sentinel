package org.pdzsoftware.featuremanager.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pdzsoftware.featuremanager.domain.enums.MerchantCategory;
import org.pdzsoftware.featuremanager.domain.exception.MerchantNotFoundException;
import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.entity.MerchantEntity;
import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.repository.MerchantRepository;
import org.pdzsoftware.featuremanager.support.TestFixtures;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantServiceTest {

    @Mock
    private MerchantRepository merchantRepository;

    @InjectMocks
    private MerchantService merchantService;

    @Test
    void getByIdOrThrow_shouldReturnMerchant_whenFound() {
        MerchantEntity merchant = TestFixtures.merchant("merchant-1", MerchantCategory.GROCERY, 0.3f);
        when(merchantRepository.findById("merchant-1")).thenReturn(Optional.of(merchant));

        MerchantEntity result = merchantService.getByIdOrThrow("merchant-1");

        assertThat(result).isSameAs(merchant);
        verify(merchantRepository).findById("merchant-1");
    }

    @Test
    void getByIdOrThrow_shouldThrow_whenNotFound() {
        when(merchantRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> merchantService.getByIdOrThrow("missing"))
                .isInstanceOf(MerchantNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void existsById_shouldDelegateToRepository() {
        when(merchantRepository.existsById("merchant-1")).thenReturn(true);

        assertThat(merchantService.existsById("merchant-1")).isTrue();
        verify(merchantRepository).existsById("merchant-1");
    }
}
