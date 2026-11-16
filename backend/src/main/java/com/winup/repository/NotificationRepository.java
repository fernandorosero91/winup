package com.winup.repository;

import com.winup.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {
    @Query("""
            select n from NotificationEntity n
            where (n.userId = :userId or n.userId is null)
              and n.createdAt > :clearedAt
            order by n.createdAt desc
            """)
    List<NotificationEntity> findVisible(
            @Param("userId") String userId,
            @Param("clearedAt") Instant clearedAt
    );

    boolean existsByBetIdAndKind(UUID betId, String kind);

    void deleteByUserId(String userId);
}
