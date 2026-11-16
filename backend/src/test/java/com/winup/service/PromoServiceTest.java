package com.winup.service;

import com.winup.entity.PromoCampaignEntity;
import com.winup.dto.CreatePromoRequest;
import com.winup.repository.PromoCampaignRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromoServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-12T22:00:00Z");

    @Mock
    private PromoCampaignRepository repository;

    @Mock
    private EventPublisher eventPublisher;

    @Test
    void should_schedule_a_future_promotion_without_publishing_it() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service().create(
                new CreatePromoRequest("Bono", "Deposita y recibe un bono", NOW.plusSeconds(3600)),
                "admin-1"
        );

        assertThat(result.status()).isEqualTo("SCHEDULED");
        assertThat(result.scheduledAt()).isEqualTo(NOW.plusSeconds(3600));
        verify(eventPublisher, never()).promotion(any(), any());
    }

    @Test
    void should_send_an_immediate_promotion_right_away() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service().create(
                new CreatePromoRequest("Bono", "Deposita y recibe un bono", null),
                "admin-1"
        );

        assertThat(result.status()).isEqualTo("SENT");
        verify(eventPublisher).promotion("Bono", "Deposita y recibe un bono");
    }

    @Test
    void should_publish_each_due_promotion_only_once() {
        PromoCampaignEntity due = campaign();
        when(repository.findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc("SCHEDULED", NOW))
                .thenReturn(List.of(due));
        when(repository.markSent(due.getId(), NOW)).thenReturn(1);

        service().dispatchDue();

        verify(eventPublisher).promotion("Bono", "Deposita y recibe un bono");
    }

    @Test
    void should_skip_a_promotion_claimed_by_another_replica() {
        PromoCampaignEntity due = campaign();
        when(repository.findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc("SCHEDULED", NOW))
                .thenReturn(List.of(due));
        when(repository.markSent(due.getId(), NOW)).thenReturn(0);

        service().dispatchDue();

        verify(eventPublisher, never()).promotion(any(), any());
    }

    @Test
    void should_reject_a_promotion_without_a_title() {
        assertThatThrownBy(() -> service().create(
                new CreatePromoRequest(" ", "Deposita y recibe un bono", null),
                "admin-1"
        )).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void should_cancel_only_scheduled_promotions() {
        UUID id = UUID.randomUUID();
        when(repository.cancel(id)).thenReturn(1);

        service().cancel(id);

        verify(repository, never()).deleteById(any());
    }

    @Test
    void should_reject_cancelling_a_promotion_that_was_already_sent() {
        UUID id = UUID.randomUUID();
        when(repository.cancel(id)).thenReturn(0);

        assertThatThrownBy(() -> service().cancel(id))
                .isInstanceOf(ResponseStatusException.class);
    }

    private PromoService service() {
        return new PromoService(repository, eventPublisher, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private PromoCampaignEntity campaign() {
        PromoCampaignEntity campaign = new PromoCampaignEntity();
        campaign.setId(UUID.randomUUID());
        campaign.setTitle("Bono");
        campaign.setDetail("Deposita y recibe un bono");
        campaign.setStatus("SCHEDULED");
        campaign.setScheduledAt(NOW.minusSeconds(60));
        campaign.setCreatedAt(NOW.minusSeconds(120));
        return campaign;
    }
}
