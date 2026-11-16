package com.winup.repository;

import com.winup.entity.MovementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MovementRepository extends JpaRepository<MovementEntity, UUID> {
    List<MovementEntity> findByUserIdOrderByCreatedAtDesc(String userId);
}
