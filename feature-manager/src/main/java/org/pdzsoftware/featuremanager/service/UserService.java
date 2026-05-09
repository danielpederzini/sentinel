package org.pdzsoftware.featuremanager.service;

import lombok.RequiredArgsConstructor;
import org.pdzsoftware.featuremanager.entity.UserEntity;
import org.pdzsoftware.featuremanager.repostiory.UserRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    @Cacheable(cacheNames = "users", key = "#a0", condition = "#a0 != null")
    public Optional<UserEntity> findById(String id) {
        return userRepository.findById(id);
    }

    @Cacheable(cacheNames = "usersExists", key = "#a0", condition = "#a0 != null")
    public boolean existsById(String id) {
        return userRepository.existsById(id);
    }
}
