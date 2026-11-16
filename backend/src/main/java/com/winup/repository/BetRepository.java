package com.winup.repository;

import com.winup.entity.BetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BetRepository extends JpaRepository<BetEntity, UUID> {
    List<BetEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    List<BetEntity> findByMatchIdAndStatus(UUID matchId, String status);

    Optional<BetEntity> findByIdempotencyKey(String idempotencyKey);
}
