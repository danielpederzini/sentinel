package org.pdzsoftware.featuremanager.service;

import lombok.RequiredArgsConstructor;
import org.pdzsoftware.featuremanager.entity.UserEntity;
import org.pdzsoftware.featuremanager.repostiory.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public List<UserEntity> findAll() {
        return userRepository.findAll();
    }

    @Cacheable(cacheNames = "users", key = "#id")
    public Optional<UserEntity> findById(String id) {
        return userRepository.findById(id);
    }

    @Caching(
            put = {@CachePut(cacheNames = "users", key = "#user.id")},
            evict = {@CacheEvict(cacheNames = "usersExists", key = "#user.id")}
    )
    public UserEntity save(UserEntity user) {
        return userRepository.save(user);
    }

    @Cacheable(cacheNames = "usersExists", key = "#id")
    public boolean existsById(String id) {
        return userRepository.existsById(id);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "users", key = "#id"),
            @CacheEvict(cacheNames = "usersExists", key = "#id")
    })
    public void deleteById(String id) {
        userRepository.deleteById(id);
    }
}
