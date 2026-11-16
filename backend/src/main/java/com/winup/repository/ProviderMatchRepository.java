package com.winup.repository;

import com.winup.entity.ProviderMatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProviderMatchRepository extends JpaRepository<ProviderMatchEntity, UUID> {
    Optional<ProviderMatchEntity> findByProviderAndExternalId(String provider, String externalId);
}
