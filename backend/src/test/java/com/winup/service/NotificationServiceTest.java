package com.winup.service;

import com.winup.entity.NotificationEntity;
import com.winup.repository.NotificationClearRepository;
import com.winup.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-12T22:00:00Z");
    private static final UUID BET_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MATCH_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private NotificationRepository repository;

    @Mock
    private NotificationClearRepository clearRepository;

    @Test
    void should_store_a_win_notification_for_the_user() {
        service().recordResult(
                "user-1",
                BET_ID,
                MATCH_ID,
                "Gen.G vs T1",
                true,
                new BigDecimal("9250.00")
        );

        ArgumentCaptor<NotificationEntity> captor = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(repository).save(captor.capture());
        NotificationEntity saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo("user-1");
        assertThat(saved.getKind()).isEqualTo("GANADA");
        assertThat(saved.getTitle()).isEqualTo("Apuesta ganada");
        assertThat(saved.getDetail()).contains("Gen.G vs T1").contains("9.250,00");
        assertThat(saved.getAmount()).isEqualByComparingTo("9250.00");
        assertThat(saved.getBetId()).isEqualTo(BET_ID);
        assertThat(saved.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void should_store_a_loss_notification_for_the_user() {
        service().recordResult(
                "user-1",
                BET_ID,
                MATCH_ID,
                "Valencia Basket vs Real Madrid",
                false,
                new BigDecimal("2000.00")
        );

        ArgumentCaptor<NotificationEntity> captor = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getKind()).isEqualTo("PERDIDA");
        assertThat(captor.getValue().getDetail()).isEqualTo(
                "Valencia Basket vs Real Madrid · perdiste $ 2.000,00"
        );
    }

    @Test
    void should_not_duplicate_a_result_notification_for_the_same_bet() {
        when(repository.existsByBetIdAndKind(BET_ID, "GANADA")).thenReturn(true);

        service().recordResult(
                "user-1",
                BET_ID,
                MATCH_ID,
                "Gen.G vs T1",
                true,
                new BigDecimal("9250.00")
        );

        verify(repository, never()).save(any());
    }

    @Test
    void should_store_a_global_promotion_for_every_user() {
        service().recordPromotion(
                "Bono de bienvenida",
                "Deposita y recibe un bono del 20 por ciento"
        );

        ArgumentCaptor<NotificationEntity> captor = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(repository).save(captor.capture());
        NotificationEntity saved = captor.getValue();
        assertThat(saved.getUserId()).isNull();
        assertThat(saved.getKind()).isEqualTo("PROMOCION");
        assertThat(saved.getTitle()).isEqualTo("Bono de bienvenida");
    }

    @Test
    void should_ignore_a_promotion_without_title() {
        service()
                .recordPromotion(" ", "texto");

        verify(repository, never()).save(any());
    }

    private NotificationService service() {
        return new NotificationService(
                repository,
                clearRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void should_list_notifications_with_the_bogota_time() {
        NotificationEntity notification = new NotificationEntity();
        notification.setId(UUID.randomUUID());
        notification.setUserId("user-1");
        notification.setKind("GANADA");
        notification.setTitle("Apuesta ganada");
        notification.setDetail("Gen.G vs T1 · $ 9.250,00 acreditados en tu cartera");
        notification.setAmount(new BigDecimal("9250.00"));
        notification.setCreatedAt(NOW);
        when(clearRepository.findById("user-1")).thenReturn(Optional.empty());
        when(repository.findVisible(eq("user-1"), any(Instant.class)))
                .thenReturn(List.of(notification));

        var result = service().list("user-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).kind()).isEqualTo("GANADA");
        assertThat(result.get(0).date()).isEqualTo("17:00");
        assertThat(result.get(0).amount()).isEqualByComparingTo("9250.00");
    }
}
