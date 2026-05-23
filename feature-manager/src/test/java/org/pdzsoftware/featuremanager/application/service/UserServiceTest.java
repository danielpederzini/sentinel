package org.pdzsoftware.featuremanager.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pdzsoftware.featuremanager.domain.enums.CountryCode;
import org.pdzsoftware.featuremanager.domain.exception.UserNotFoundException;
import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.entity.UserEntity;
import org.pdzsoftware.featuremanager.infrastructure.outbound.persistence.repository.UserRepository;
import org.pdzsoftware.featuremanager.support.TestFixtures;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getByIdOrThrow_shouldReturnUser_whenFound() {
        UserEntity user = TestFixtures.user("user-1", CountryCode.US, LocalDateTime.now());
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        UserEntity result = userService.getByIdOrThrow("user-1");

        assertThat(result).isSameAs(user);
        verify(userRepository).findById("user-1");
    }

    @Test
    void getByIdOrThrow_shouldThrow_whenNotFound() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getByIdOrThrow("missing"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void existsById_shouldDelegateToRepository() {
        when(userRepository.existsById("user-1")).thenReturn(true);

        assertThat(userService.existsById("user-1")).isTrue();
        verify(userRepository).existsById("user-1");
    }
}
