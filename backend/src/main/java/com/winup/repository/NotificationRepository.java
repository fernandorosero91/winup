package com.winup.repository;

import com.winup.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {
    List<NotificationEntity> findByUserIdOrUserIdIsNullOrderByCreatedAtDesc(String userId);

    boolean existsByBetIdAndKind(UUID betId, String kind);
}
