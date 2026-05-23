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
import static org.pdzsoftware.featuremanager.support.TestConstants.MISSING_ID;
import static org.pdzsoftware.featuremanager.support.TestConstants.USER_ID;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getByIdOrThrow_shouldReturnUser_whenFound() {
        UserEntity user = TestFixtures.user(USER_ID, CountryCode.US, LocalDateTime.now());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        UserEntity result = userService.getByIdOrThrow(USER_ID);

        assertThat(result).isSameAs(user);
        verify(userRepository).findById(USER_ID);
    }

    @Test
    void getByIdOrThrow_shouldThrow_whenNotFound() {
        when(userRepository.findById(MISSING_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getByIdOrThrow(MISSING_ID))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(MISSING_ID);
    }

    @Test
    void existsById_shouldDelegateToRepository() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);

        assertThat(userService.existsById(USER_ID)).isTrue();
        verify(userRepository).existsById(USER_ID);
    }
}
