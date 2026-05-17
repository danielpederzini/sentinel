package org.pdzsoftware.riskactionhandler.application.service;

import lombok.RequiredArgsConstructor;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.persistence.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public boolean existsById(String id) {
        return userRepository.existsById(id);
    }
}
