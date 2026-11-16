package com.winup.repository;

import com.winup.entity.DeviceTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceTokenRepository extends JpaRepository<DeviceTokenEntity, UUID> {
    List<DeviceTokenEntity> findByUserId(String userId);

    Optional<DeviceTokenEntity> findByToken(String token);
}
