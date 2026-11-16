package com.winup.repository;

import com.winup.entity.PaymentEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentEventRepository extends JpaRepository<PaymentEventEntity, UUID> {
    boolean existsByProviderAndProviderEventId(String provider, String providerEventId);
}
