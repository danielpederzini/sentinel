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

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private CardService cardService;

    @Test
    void getByIdOrThrow_shouldReturnCard_whenFound() {
        CardEntity card = TestFixtures.card("card-1", CardType.CREDIT, LocalDateTime.now());
        when(cardRepository.findById("card-1")).thenReturn(Optional.of(card));

        CardEntity result = cardService.getByIdOrThrow("card-1");

        assertThat(result).isSameAs(card);
        verify(cardRepository).findById("card-1");
    }

    @Test
    void getByIdOrThrow_shouldThrow_whenNotFound() {
        when(cardRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getByIdOrThrow("missing"))
                .isInstanceOf(CardNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void existsById_shouldDelegateToRepository() {
        when(cardRepository.existsById("card-1")).thenReturn(false);

        assertThat(cardService.existsById("card-1")).isFalse();
        verify(cardRepository).existsById("card-1");
    }
}
