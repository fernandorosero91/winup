package com.winup.service;

import com.winup.entity.NotificationClearEntity;
import com.winup.repository.NotificationClearRepository;
import com.winup.repository.NotificationReadRepository;
import com.winup.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationCleanupServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-16T12:00:00Z");

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationReadRepository readRepository;

    @Mock
    private NotificationClearRepository clearRepository;

    @Test
    void should_mark_the_clear_time_and_delete_user_notifications() {
        when(clearRepository.findById("user-1")).thenReturn(Optional.empty());

        service().clear("user-1");

        ArgumentCaptor<NotificationClearEntity> captor =
                ArgumentCaptor.forClass(NotificationClearEntity.class);
        verify(clearRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo("user-1");
        assertThat(captor.getValue().getClearedAt()).isEqualTo(NOW);
        verify(notificationRepository).deleteByUserId("user-1");
        verify(readRepository).deleteByUserId("user-1");
    }

    @Test
    void should_update_an_existing_clear_marker() {
        NotificationClearEntity existing = new NotificationClearEntity();
        existing.setUserId("user-1");
        existing.setClearedAt(Instant.parse("2026-11-01T00:00:00Z"));
        when(clearRepository.findById("user-1")).thenReturn(Optional.of(existing));

        service().clear("user-1");

        assertThat(existing.getClearedAt()).isEqualTo(NOW);
        verify(clearRepository).save(existing);
    }

    private NotificationCleanupService service() {
        return new NotificationCleanupService(
                notificationRepository,
                readRepository,
                clearRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }
}
