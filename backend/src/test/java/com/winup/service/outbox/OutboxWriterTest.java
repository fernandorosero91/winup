package com.winup.service.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winup.entity.OutboxEventEntity;
import com.winup.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxWriterTest {
    @Mock
    private OutboxEventRepository repository;

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-11-12T18:00:00Z"),
            ZoneOffset.UTC
    );

    @Test
    void should_store_a_kafka_event_with_key_and_payload() {
        OutboxWriter writer = new OutboxWriter(repository, new ObjectMapper(), clock);

        writer.kafka("match_events", "match-1", Map.of("type", "MATCH_CREATED"));

        ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(repository).save(captor.capture());
        OutboxEventEntity event = captor.getValue();
        assertThat(event.getChannel()).isEqualTo("KAFKA");
        assertThat(event.getDestination()).isEqualTo("match_events");
        assertThat(event.getMessageKey()).isEqualTo("match-1");
        assertThat(event.getPayload()).contains("MATCH_CREATED");
        assertThat(event.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void should_store_a_rabbit_event_with_delay() {
        OutboxWriter writer = new OutboxWriter(repository, new ObjectMapper(), clock);

        writer.rabbit("match.delayed", "match.simulate", Map.of("matchId", "m1"), 60000);

        ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(repository).save(captor.capture());
        OutboxEventEntity event = captor.getValue();
        assertThat(event.getChannel()).isEqualTo("RABBIT");
        assertThat(event.getRoutingKey()).isEqualTo("match.simulate");
        assertThat(event.getDelayMs()).isEqualTo(60000);
    }
}
