package com.winup.repository;

import com.winup.entity.PaymentIntentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentIntentRepository extends JpaRepository<PaymentIntentEntity, UUID> {
    Optional<PaymentIntentEntity> findByIdempotencyKey(String idempotencyKey);

    Optional<PaymentIntentEntity> findByExternalReference(String externalReference);
}
