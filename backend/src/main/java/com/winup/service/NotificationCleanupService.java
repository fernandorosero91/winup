package com.winup.service;

import com.winup.entity.NotificationClearEntity;
import com.winup.repository.NotificationClearRepository;
import com.winup.repository.NotificationReadRepository;
import com.winup.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class NotificationCleanupService {
    private final NotificationRepository notificationRepository;
    private final NotificationReadRepository readRepository;
    private final NotificationClearRepository clearRepository;
    private final Clock clock;

    public NotificationCleanupService(
            NotificationRepository notificationRepository,
            NotificationReadRepository readRepository,
            NotificationClearRepository clearRepository,
            Clock clock
    ) {
        this.notificationRepository = notificationRepository;
        this.readRepository = readRepository;
        this.clearRepository = clearRepository;
        this.clock = clock;
    }

    @Transactional
    public void clear(String userId) {
        Instant now = clock.instant();
        NotificationClearEntity clear = clearRepository.findById(userId)
                .orElseGet(() -> {
                    NotificationClearEntity created = new NotificationClearEntity();
                    created.setUserId(userId);
                    return created;
                });
        clear.setClearedAt(now);
        clearRepository.save(clear);
        notificationRepository.deleteByUserId(userId);
        readRepository.deleteByUserId(userId);
    }
}
