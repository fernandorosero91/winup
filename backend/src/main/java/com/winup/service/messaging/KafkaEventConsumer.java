package com.winup.service.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winup.entity.ProcessedEventEntity;
import com.winup.model.match.Bet;
import com.winup.model.match.BetId;
import com.winup.exception.BettingClosedException;
import com.winup.model.match.Dinero;
import com.winup.model.match.MatchId;
import com.winup.model.match.MatchResult;
import com.winup.model.match.UserId;
import com.winup.model.vo.BetSelection;
import com.winup.service.engine.BetPlacedEvent;
import com.winup.service.engine.MatchCreatedEvent;
import com.winup.service.engine.MatchEngine;
import com.winup.service.engine.MatchFinishedEvent;
import com.winup.repository.ProcessedEventRepository;
import com.winup.service.EventPublisher;
import com.winup.service.SettlementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@ConditionalOnProperty(name = "winup.role", havingValue = "engine", matchIfMissing = true)
@Component
public class KafkaEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(KafkaEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processedEventRepository;
    private final SettlementService settlementService;
    private final MatchEngine matchEngine;
    private final EventPublisher eventPublisher;

    public KafkaEventConsumer(
            ObjectMapper objectMapper,
            ProcessedEventRepository processedEventRepository,
            SettlementService settlementService,
            MatchEngine matchEngine,
            EventPublisher eventPublisher
    ) {
        this.objectMapper = objectMapper;
        this.processedEventRepository = processedEventRepository;
        this.settlementService = settlementService;
        this.matchEngine = matchEngine;
        this.eventPublisher = eventPublisher;
    }

    @KafkaListener(
            id = "match-events-listener",
            topics = "match_events",
            groupId = "winup-engine",
            autoStartup = "false"
    )
    public void consume(String message, Acknowledgment acknowledgment) {
        try {
            JsonNode root = objectMapper.readTree(message);
            UUID eventId = UUID.fromString(root.get("eventId").asText());
            if (processedEventRepository.existsById(eventId)) {
                acknowledgment.acknowledge();
                return;
            }

            String type = root.get("eventType").asText();
            JsonNode payload = root.get("payload");
            if ("MATCH_CREATED".equals(type)) {
                MatchId matchId = MatchId.of(payload.get("matchId").asText());
                matchEngine.onCreated(new MatchCreatedEvent(
                        matchId,
                        payload.path("home").asText(),
                        payload.path("away").asText()
                ));
                eventPublisher.notifyDashboard(
                        "OPEN",
                        matchId.value().toString(),
                        payload.path("home").asText(),
                        payload.path("away").asText(),
                        0,
                        0,
                        0,
                        ""
                );
            } else if ("BET_PLACED".equals(type)) {
                processBet(payload);
            } else if ("MATCH_FINISHED".equals(type)) {
                UUID matchId = UUID.fromString(payload.get("matchId").asText());
                MatchResult result = new MatchResult(
                        payload.get("winner").asText(),
                        payload.get("homeScore").asInt(),
                        payload.get("awayScore").asInt()
                );
                try {
                    matchEngine.onFinished(new MatchFinishedEvent(new MatchId(matchId), result));
                } catch (IllegalStateException exception) {
                    log.warn("El engine no ten\u00eda el partido {}; se liquida desde la proyecci\u00f3n", matchId);
                }
                settlementService.settle(
                        matchId,
                        result.winner(),
                        result.homeScore(),
                        result.awayScore()
                );
            }

            ProcessedEventEntity processed = new ProcessedEventEntity();
            processed.setEventId(eventId);
            processed.setProcessedAt(Instant.now());
            processedEventRepository.save(processed);
            acknowledgment.acknowledge();
        } catch (Exception exception) {
            log.error("No se pudo procesar el evento de Kafka: {}", message, exception);
            throw new IllegalStateException("Evento Kafka no procesado", exception);
        }
    }

    private void processBet(JsonNode payload) {
        Bet bet = new Bet(
                BetId.of(payload.get("betId").asText()),
                UserId.of(payload.get("userId").asText()),
                BetSelection.from(payload.get("selection").asText()),
                new Dinero(new BigDecimal(payload.get("stake").asText())),
                new BigDecimal(payload.get("odds").asText())
        );
        try {
            matchEngine.onBet(new BetPlacedEvent(MatchId.of(payload.get("matchId").asText()), bet));
        } catch (BettingClosedException exception) {
            log.warn("Apuesta rechazada por el agregado: {}", exception.getMessage());
        } catch (IllegalStateException exception) {
            log.warn("Apuesta sin partido reconstruido: {}", exception.getMessage());
        }
    }
}
