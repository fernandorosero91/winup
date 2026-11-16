package com.winup.repository;

import com.winup.entity.NotificationReadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificationReadRepository extends JpaRepository<NotificationReadEntity, UUID> {
    @Query("select n.notificationId from NotificationReadEntity n where n.userId = :userId")
    List<String> findNotificationIdsByUserId(@Param("userId") String userId);

    @Modifying
    @Query(value = """
            insert into notification_read (id, user_id, notification_id, read_at)
            values (:id, :userId, :notificationId, now())
            on conflict (user_id, notification_id) do nothing
            """, nativeQuery = true)
    void insertIfAbsent(
            @Param("id") UUID id,
            @Param("userId") String userId,
            @Param("notificationId") String notificationId
    );
}
