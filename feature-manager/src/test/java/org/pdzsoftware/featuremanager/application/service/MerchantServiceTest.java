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
import static org.pdzsoftware.featuremanager.support.TestConstants.MERCHANT_ID;
import static org.pdzsoftware.featuremanager.support.TestConstants.MERCHANT_RISK_SCORE;
import static org.pdzsoftware.featuremanager.support.TestConstants.MISSING_ID;

@ExtendWith(MockitoExtension.class)
class MerchantServiceTest {

    @Mock
    private MerchantRepository merchantRepository;

    @InjectMocks
    private MerchantService merchantService;

    @Test
    void getByIdOrThrow_shouldReturnMerchant_whenFound() {
        MerchantEntity merchant = TestFixtures.merchant(MERCHANT_ID, MerchantCategory.GROCERY, MERCHANT_RISK_SCORE);
        when(merchantRepository.findById(MERCHANT_ID)).thenReturn(Optional.of(merchant));

        MerchantEntity result = merchantService.getByIdOrThrow(MERCHANT_ID);

        assertThat(result).isSameAs(merchant);
        verify(merchantRepository).findById(MERCHANT_ID);
    }

    @Test
    void getByIdOrThrow_shouldThrow_whenNotFound() {
        when(merchantRepository.findById(MISSING_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> merchantService.getByIdOrThrow(MISSING_ID))
                .isInstanceOf(MerchantNotFoundException.class)
                .hasMessageContaining(MISSING_ID);
    }

    @Test
    void existsById_shouldDelegateToRepository() {
        when(merchantRepository.existsById(MERCHANT_ID)).thenReturn(true);

        assertThat(merchantService.existsById(MERCHANT_ID)).isTrue();
        verify(merchantRepository).existsById(MERCHANT_ID);
    }
}
