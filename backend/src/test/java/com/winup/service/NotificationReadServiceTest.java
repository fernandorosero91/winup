package com.winup.service;

import com.winup.repository.NotificationReadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationReadServiceTest {
    @Mock
    private NotificationReadRepository repository;

    @Test
    void should_store_only_notifications_that_were_not_read_yet() {
        when(repository.findNotificationIdsByUserId("user-1"))
                .thenReturn(List.of("bet-1"));

        new NotificationReadService(repository).markRead(
                "user-1",
                List.of("bet-1", "bet-2", "bet-2", " ")
        );

        verify(repository, times(1)).insertIfAbsent(any(UUID.class), eq("user-1"), eq("bet-2"));
        verify(repository, never()).insertIfAbsent(any(UUID.class), eq("user-1"), eq("bet-1"));
    }

    @Test
    void should_ignore_an_empty_request() {
        new NotificationReadService(repository).markRead("user-1", List.of());

        verify(repository, never()).findNotificationIdsByUserId("user-1");
        verify(repository, never()).insertIfAbsent(any(), any(), any());
    }

    @Test
    void should_return_the_persisted_ids_of_the_user() {
        when(repository.findNotificationIdsByUserId("user-1"))
                .thenReturn(List.of("bet-1", "bet-2"));

        List<String> ids = new NotificationReadService(repository).readIds("user-1");

        org.assertj.core.api.Assertions.assertThat(ids).containsExactly("bet-1", "bet-2");
    }
}
