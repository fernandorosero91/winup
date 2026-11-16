package com.winup.service.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winup.entity.OutboxEventEntity;
import com.winup.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxDispatcher {
    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);
    private static final int MAX_ATTEMPTS = 5;

    private final OutboxEventRepository repository;
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxDispatcher(
            OutboxEventRepository repository,
            KafkaTemplate<Object, Object> kafkaTemplate,
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${winup.outbox.interval-ms:500}")
    @Transactional
    public void dispatch() {
        Instant now = clock.instant();
        for (OutboxEventEntity event : repository
                .findTop50ByStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc("PENDING", now)) {
            try {
                send(event);
                event.setStatus("SENT");
                event.setSentAt(clock.instant());
            } catch (Exception exception) {
                event.setAttempts(event.getAttempts() + 1);
                event.setNextAttemptAt(clock.instant().plusSeconds(1L << Math.min(event.getAttempts(), 5)));
                if (event.getAttempts() >= MAX_ATTEMPTS) {
                    event.setStatus("DEAD");
                }
                log.warn("No se pudo publicar el evento de outbox {}: {}", event.getId(), exception.getMessage());
            }
            repository.save(event);
        }
    }

    private void send(OutboxEventEntity event) throws Exception {
        if ("KAFKA".equals(event.getChannel())) {
            kafkaTemplate
                    .send(event.getDestination(), event.getMessageKey(), event.getPayload())
                    .get(5, TimeUnit.SECONDS);
            return;
        }
        Object payload = objectMapper.readValue(event.getPayload(), Object.class);
        rabbitTemplate.convertAndSend(
                event.getDestination(),
                event.getRoutingKey(),
                payload,
                message -> {
                    if (event.getDelayMs() > 0) {
                        message.getMessageProperties().setHeader("x-delay", event.getDelayMs());
                    }
                    return message;
                }
        );
    }
}
