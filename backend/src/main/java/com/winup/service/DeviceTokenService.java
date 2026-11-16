package com.winup.service;

import com.winup.entity.DeviceTokenEntity;
import com.winup.repository.DeviceTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class DeviceTokenService {
    private final DeviceTokenRepository repository;
    private final Clock clock;

    public DeviceTokenService(DeviceTokenRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public void register(String userId, String token, String platform) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("El token del dispositivo es obligatorio");
        }
        DeviceTokenEntity entity = repository.findByToken(token.trim())
                .orElseGet(DeviceTokenEntity::new);
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
            entity.setCreatedAt(clock.instant());
        }
        entity.setUserId(userId);
        entity.setToken(token.trim());
        entity.setPlatform(platform == null || platform.isBlank() ? "android" : platform.trim());
        repository.save(entity);
    }

    @Transactional
    public void unregister(String token) {
        repository.findByToken(token).ifPresent(repository::delete);
    }
}
