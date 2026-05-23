package org.pdzsoftware.featuremanager.application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pdzsoftware.featuremanager.application.service.CardService;
import org.pdzsoftware.featuremanager.application.service.MerchantService;
import org.pdzsoftware.featuremanager.application.service.TransactionService;
import org.pdzsoftware.featuremanager.application.service.TrustedDeviceService;
import org.pdzsoftware.featuremanager.application.service.UserService;
import org.pdzsoftware.featuremanager.domain.exception.CardNotFoundException;
import org.pdzsoftware.featuremanager.domain.exception.MerchantNotFoundException;
import org.pdzsoftware.featuremanager.domain.exception.UserNotFoundException;
import org.pdzsoftware.featuremanager.infrastructure.inbound.controller.dto.PersistTransactionRequest;
import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.entity.TransactionEntity;
import org.pdzsoftware.featuremanager.support.TestFixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pdzsoftware.featuremanager.support.TestConstants.CARD_ID;
import static org.pdzsoftware.featuremanager.support.TestConstants.DEVICE_ID;
import static org.pdzsoftware.featuremanager.support.TestConstants.MERCHANT_ID;
import static org.pdzsoftware.featuremanager.support.TestConstants.TRANSACTION_ID_1;
import static org.pdzsoftware.featuremanager.support.TestConstants.TRANSACTION_ID_2;
import static org.pdzsoftware.featuremanager.support.TestConstants.TRANSACTION_ID_3;
import static org.pdzsoftware.featuremanager.support.TestConstants.TRANSACTION_ID_4;
import static org.pdzsoftware.featuremanager.support.TestConstants.TRANSACTION_ID_5;
import static org.pdzsoftware.featuremanager.support.TestConstants.USER_ID;

@ExtendWith(MockitoExtension.class)
class PersistTransactionUseCaseTest {

    @Mock
    private UserService userService;
    @Mock
    private MerchantService merchantService;
    @Mock
    private CardService cardService;
    @Mock
    private TrustedDeviceService trustedDeviceService;
    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private PersistTransactionUseCase persistTransactionUseCase;

    @Test
    void execute_shouldPersistTransaction_whenAllIdsExist() {
        PersistTransactionRequest request = TestFixtures.persistTransactionRequest(TRANSACTION_ID_1);
        when(userService.existsById(USER_ID)).thenReturn(true);
        when(merchantService.existsById(MERCHANT_ID)).thenReturn(true);
        when(cardService.existsById(CARD_ID)).thenReturn(true);
        when(trustedDeviceService.existsById(DEVICE_ID)).thenReturn(true);
        when(transactionService.save(any(TransactionEntity.class))).thenReturn(TRANSACTION_ID_1);

        String savedId = persistTransactionUseCase.execute(request);

        assertThat(savedId).isEqualTo(TRANSACTION_ID_1);

        ArgumentCaptor<TransactionEntity> captor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactionService).save(captor.capture());
        TransactionEntity saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(TRANSACTION_ID_1);
        assertThat(saved.getFeatureVector()).isNotNull();
        assertThat(saved.getPrediction()).isNotNull();
        assertThat(saved.getFeatureVector().getTransaction()).isSameAs(saved);
        assertThat(saved.getPrediction().getTransaction()).isSameAs(saved);
    }

    @Test
    void execute_shouldThrowUserNotFound_whenUserMissing() {
        PersistTransactionRequest request = TestFixtures.persistTransactionRequest(TRANSACTION_ID_2);
        when(userService.existsById(USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> persistTransactionUseCase.execute(request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(USER_ID);
    }

    @Test
    void execute_shouldThrowMerchantNotFound_whenMerchantMissing() {
        PersistTransactionRequest request = TestFixtures.persistTransactionRequest(TRANSACTION_ID_3);
        when(userService.existsById(USER_ID)).thenReturn(true);
        when(merchantService.existsById(MERCHANT_ID)).thenReturn(false);

        assertThatThrownBy(() -> persistTransactionUseCase.execute(request))
                .isInstanceOf(MerchantNotFoundException.class);
    }

    @Test
    void execute_shouldThrowCardNotFound_whenCardMissing() {
        PersistTransactionRequest request = TestFixtures.persistTransactionRequest(TRANSACTION_ID_4);
        when(userService.existsById(USER_ID)).thenReturn(true);
        when(merchantService.existsById(MERCHANT_ID)).thenReturn(true);
        when(cardService.existsById(CARD_ID)).thenReturn(false);

        assertThatThrownBy(() -> persistTransactionUseCase.execute(request))
                .isInstanceOf(CardNotFoundException.class);
    }

    @Test
    void execute_shouldThrow_whenDeviceIdProvidedButNotFound() {
        PersistTransactionRequest request = TestFixtures.persistTransactionRequest(TRANSACTION_ID_5);
        when(userService.existsById(USER_ID)).thenReturn(true);
        when(merchantService.existsById(MERCHANT_ID)).thenReturn(true);
        when(cardService.existsById(CARD_ID)).thenReturn(true);
        when(trustedDeviceService.existsById(DEVICE_ID)).thenReturn(false);

        assertThatThrownBy(() -> persistTransactionUseCase.execute(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(DEVICE_ID);
    }
}
