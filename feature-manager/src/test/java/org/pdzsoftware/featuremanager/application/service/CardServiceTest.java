package org.pdzsoftware.featuremanager.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pdzsoftware.featuremanager.domain.enums.CardType;
import org.pdzsoftware.featuremanager.domain.exception.CardNotFoundException;
import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.entity.CardEntity;
import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.repository.CardRepository;
import org.pdzsoftware.featuremanager.support.TestFixtures;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pdzsoftware.featuremanager.support.TestConstants.CARD_ID;
import static org.pdzsoftware.featuremanager.support.TestConstants.MISSING_ID;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private CardService cardService;

    @Test
    void getByIdOrThrow_shouldReturnCard_whenFound() {
        CardEntity card = TestFixtures.card(CARD_ID, CardType.CREDIT, LocalDateTime.now());
        when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));

        CardEntity result = cardService.getByIdOrThrow(CARD_ID);

        assertThat(result).isSameAs(card);
        verify(cardRepository).findById(CARD_ID);
    }

    @Test
    void getByIdOrThrow_shouldThrow_whenNotFound() {
        when(cardRepository.findById(MISSING_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getByIdOrThrow(MISSING_ID))
                .isInstanceOf(CardNotFoundException.class)
                .hasMessageContaining(MISSING_ID);
    }

    @Test
    void existsById_shouldDelegateToRepository() {
        when(cardRepository.existsById(CARD_ID)).thenReturn(false);

        assertThat(cardService.existsById(CARD_ID)).isFalse();
        verify(cardRepository).existsById(CARD_ID);
    }
}
