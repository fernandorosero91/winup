package com.winup.service.engine;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winup.model.match.Bet;
import com.winup.model.vo.BetId;
import com.winup.exception.BettingClosedException;
import com.winup.model.vo.Dinero;
import com.winup.model.vo.MatchId;
import com.winup.model.vo.MatchResult;
import com.winup.model.vo.UserId;
import com.winup.model.vo.BetSelection;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ConditionalOnProperty(name = "winup.role", havingValue = "engine", matchIfMissing = true)
@Component
public class MatchEngineReplay {
    private static final Logger log = LoggerFactory.getLogger(MatchEngineReplay.class);

    private final ConsumerFactory<Object, Object> consumerFactory;
    private final ObjectMapper objectMapper;
    private final MatchEngine matchEngine;
    private final KafkaListenerEndpointRegistry registry;
    private final boolean enabled;

    public MatchEngineReplay(
            ConsumerFactory<Object, Object> consumerFactory,
            ObjectMapper objectMapper,
            MatchEngine matchEngine,
            KafkaListenerEndpointRegistry registry,
            @Value("${winup.replay.enabled:true}") boolean enabled
    ) {
        this.consumerFactory = consumerFactory;
        this.objectMapper = objectMapper;
        this.matchEngine = matchEngine;
        this.registry = registry;
        this.enabled = enabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void replay() {
        try {
            if (enabled) {
                rebuild();
            }
        } catch (Exception exception) {
            log.error("No se pudo reconstruir el engine desde Kafka", exception);
        } finally {
            var container = registry.getListenerContainer("match-events-listener");
            if (container != null) {
                container.start();
            }
        }
    }

    private void rebuild() {
        int applied = 0;
        try (Consumer<Object, Object> consumer = consumerFactory.createConsumer(
                "winup-replay-" + UUID.randomUUID()
        )) {
            List<PartitionInfo> partitions = consumer.partitionsFor("match_events");
            List<TopicPartition> assignments = partitions.stream()
                    .map(partition -> new TopicPartition("match_events", partition.partition()))
                    .toList();
            consumer.assign(assignments);
            consumer.seekToBeginning(assignments);
            Map<TopicPartition, Long> endOffsets = consumer.endOffsets(assignments);

            boolean done = false;
            while (!done) {
                ConsumerRecords<Object, Object> records = consumer.poll(Duration.ofSeconds(1));
                for (ConsumerRecord<Object, Object> record : records) {
                    apply(record.value());
                    applied++;
                }
                done = assignments.stream()
                        .allMatch(topic -> consumer.position(topic) >= endOffsets.getOrDefault(topic, 0L));
            }
        }
        log.info("Engine reconstruido desde Kafka: {} eventos", applied);
    }

    private void apply(Object value) {
        if (value == null) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(value.toString());
            JsonNode payload = root.get("payload");
            String type = root.get("eventType").asText();
            if ("MATCH_CREATED".equals(type)) {
                matchEngine.onCreated(new MatchCreatedEvent(
                        MatchId.of(payload.get("matchId").asText()),
                        payload.path("home").asText(),
                        payload.path("away").asText()
                ));
            } else if ("BET_PLACED".equals(type)) {
                registerBet(payload);
            } else if ("MATCH_FINISHED".equals(type)) {
                finish(payload);
            }
        } catch (Exception exception) {
            log.warn("Evento ignorado durante replay: {}", exception.getMessage());
        }
    }

    private void registerBet(JsonNode payload) {
        Bet bet = new Bet(
                BetId.of(payload.get("betId").asText()),
                UserId.of(payload.get("userId").asText()),
                BetSelection.from(payload.get("selection").asText()),
                new Dinero(new BigDecimal(payload.get("stake").asText())),
                new BigDecimal(payload.get("odds").asText())
        );
        try {
            matchEngine.onBet(new BetPlacedEvent(MatchId.of(payload.get("matchId").asText()), bet));
        } catch (BettingClosedException | IllegalStateException exception) {
            log.warn("Apuesta ignorada durante replay: {}", exception.getMessage());
        }
    }

    private void finish(JsonNode payload) {
        try {
            matchEngine.onFinished(new MatchFinishedEvent(
                    MatchId.of(payload.get("matchId").asText()),
                    new MatchResult(
                            payload.get("winner").asText(),
                            payload.get("homeScore").asInt(),
                            payload.get("awayScore").asInt()
                    )
            ));
        } catch (IllegalStateException exception) {
            log.warn("Finalizacion ignorada durante replay: {}", exception.getMessage());
        }
    }
}
