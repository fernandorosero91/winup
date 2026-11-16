package com.winup.service;

import com.winup.repository.NotificationReadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class NotificationReadService {
    private static final int MAX_IDS = 500;

    private final NotificationReadRepository repository;

    public NotificationReadService(NotificationReadRepository repository) {
        this.repository = repository;
    }

    public List<String> readIds(String userId) {
        return repository.findNotificationIdsByUserId(userId);
    }

    @Transactional
    public void markRead(String userId, List<String> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            return;
        }
        Set<String> existing = new HashSet<>(repository.findNotificationIdsByUserId(userId));
        notificationIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .map(String::trim)
                .distinct()
                .limit(MAX_IDS)
                .filter(id -> !existing.contains(id))
                .forEach(id -> repository.insertIfAbsent(UUID.randomUUID(), userId, id));
    }
}
