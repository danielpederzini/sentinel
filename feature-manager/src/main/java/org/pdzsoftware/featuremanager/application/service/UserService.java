package org.pdzsoftware.featuremanager.application.service;

import lombok.RequiredArgsConstructor;
import org.pdzsoftware.featuremanager.infrastructure.persistence.entity.UserEntity;
import org.pdzsoftware.featuremanager.domain.exception.UserNotFoundException;
import org.pdzsoftware.featuremanager.infrastructure.persistence.repostiory.UserRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    @Cacheable(cacheNames = "users", key = "#a0", condition = "#a0 != null")
    public UserEntity getByIdOrThrow(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(String.format("User with ID %s not found", id)));
    }
}
