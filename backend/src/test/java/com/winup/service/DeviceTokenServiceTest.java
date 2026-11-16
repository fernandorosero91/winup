package com.winup.service;

import com.winup.entity.DeviceTokenEntity;
import com.winup.repository.DeviceTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceTokenServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-12T22:00:00Z");

    @Mock
    private DeviceTokenRepository repository;

    @Test
    void should_register_a_new_device_token() {
        when(repository.findByToken("fcm-token")).thenReturn(Optional.empty());

        service().register("user-1", "fcm-token", "android");

        ArgumentCaptor<DeviceTokenEntity> captor = ArgumentCaptor.forClass(DeviceTokenEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo("user-1");
        assertThat(captor.getValue().getToken()).isEqualTo("fcm-token");
        assertThat(captor.getValue().getPlatform()).isEqualTo("android");
        assertThat(captor.getValue().getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void should_reuse_the_row_when_the_same_token_registers_again() {
        DeviceTokenEntity existing = new DeviceTokenEntity();
        existing.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        existing.setUserId("old-user");
        existing.setToken("fcm-token");
        when(repository.findByToken("fcm-token")).thenReturn(Optional.of(existing));

        service().register("new-user", "fcm-token", "android");

        assertThat(existing.getId()).isEqualTo(
                UUID.fromString("11111111-1111-1111-1111-111111111111")
        );
        assertThat(existing.getUserId()).isEqualTo("new-user");
        verify(repository).save(existing);
    }

    @Test
    void should_reject_a_blank_token() {
        assertThatThrownBy(() -> service().register("user-1", " ", "android"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private DeviceTokenService service() {
        return new DeviceTokenService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }
}
