package com.winup.service.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winup.entity.OutboxEventEntity;
import com.winup.repository.OutboxEventRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Component
public class OutboxWriter {
    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxWriter(OutboxEventRepository repository, ObjectMapper objectMapper, Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void kafka(String topic, String key, Object payload) {
        save("KAFKA", topic, null, key, payload, 0);
    }

    public void rabbit(String exchange, String routingKey, Object payload, long delayMs) {
        save("RABBIT", exchange, routingKey, null, payload, delayMs);
    }

    private void save(
            String channel,
            String destination,
            String routingKey,
            String messageKey,
            Object payload,
            long delayMs
    ) {
        try {
            Instant now = clock.instant();
            OutboxEventEntity event = new OutboxEventEntity();
            event.setId(UUID.randomUUID());
            event.setChannel(channel);
            event.setDestination(destination);
            event.setRoutingKey(routingKey);
            event.setMessageKey(messageKey);
            event.setPayload(objectMapper.writeValueAsString(payload));
            event.setDelayMs(delayMs);
            event.setStatus("PENDING");
            event.setAttempts(0);
            event.setNextAttemptAt(now);
            event.setCreatedAt(now);
            repository.save(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo serializar el evento de outbox", exception);
        }
    }
}
