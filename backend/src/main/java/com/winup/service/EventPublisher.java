package com.winup.service;

import com.winup.config.RabbitConfig;
import com.winup.entity.BetEntity;
import com.winup.entity.MatchEntity;
import com.winup.model.MatchStatus;
import com.winup.service.outbox.OutboxWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class EventPublisher {
    private final OutboxWriter outbox;
    private final long windowMs;

    public EventPublisher(
            OutboxWriter outbox,
            @Value("${winup.match.window-ms:60000}") long windowMs
    ) {
        this.outbox = outbox;
        this.windowMs = windowMs;
    }

    public void matchCreated(MatchEntity match) {
        publishMatchCreated(match);
        scheduleSimulation(match);
    }

    public void publishMatchCreated(MatchEntity match) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("matchId", match.getId().toString());
        payload.put("league", match.getLeague());
        payload.put("home", match.getHomeTeam());
        payload.put("away", match.getAwayTeam());
        sendKafka("MATCH_CREATED", match.getId(), payload);
    }

    public void scheduleSimulation(MatchEntity match) {
        outbox.rabbit(
                RabbitConfig.DELAYED_EXCHANGE,
                "match.simulate",
                Map.of("matchId", match.getId().toString()),
                simulationDelay(match)
        );
    }

    private long simulationDelay(MatchEntity match) {
        long jitter = Math.floorMod(match.getId().hashCode(), (int) Math.max(1, windowMs / 2));
        return windowMs + jitter;
    }

    public void betPlaced(BetEntity bet, MatchEntity match) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("betId", bet.getId().toString());
        payload.put("userId", bet.getUserId());
        payload.put("matchId", bet.getMatchId().toString());
        payload.put("selection", bet.getSelection());
        payload.put("stake", bet.getStake());
        payload.put("odds", bet.getOdds());
        payload.put("home", match.getHomeTeam());
        payload.put("away", match.getAwayTeam());
        sendKafka("BET_PLACED", bet.getMatchId(), payload);
    }

    public void matchFinished(MatchEntity match, String winner, int homeScore, int awayScore) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("matchId", match.getId().toString());
        payload.put("winner", winner);
        payload.put("homeScore", homeScore);
        payload.put("awayScore", awayScore);
        sendKafka("MATCH_FINISHED", match.getId(), payload);
    }

    public void notifyDashboard(MatchStatus status, MatchEntity match, String winner) {
        notifyDashboard(
                status,
                match.getId().toString(),
                match.getHomeTeam(),
                match.getAwayTeam(),
                match.getHomeScore(),
                match.getAwayScore(),
                match.getMinute(),
                winner
        );
    }

    public void notifyDashboard(
            MatchStatus status,
            String matchId,
            String home,
            String away,
            int homeScore,
            int awayScore,
            int minute,
            String winner
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("matchId", matchId);
        payload.put("status", status.code());
        payload.put("home", home);
        payload.put("away", away);
        payload.put("homeScore", homeScore);
        payload.put("awayScore", awayScore);
        payload.put("minute", minute);
        payload.put("winner", winner == null ? "" : winner);
        outbox.rabbit(RabbitConfig.NOTIFICATIONS_EXCHANGE, "notify.dashboard.update", payload, 0);
    }

    public void notifyEmailWin(BetEntity bet, MatchEntity match, BigDecimal winnings) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", bet.getUserId());
        payload.put("betId", bet.getId().toString());
        payload.put("matchId", match.getId().toString());
        payload.put("matchName", match.getHomeTeam() + " vs " + match.getAwayTeam());
        payload.put("stake", bet.getStake());
        payload.put("odds", bet.getOdds());
        payload.put("winnings", winnings);
        payload.put("result", "WIN");
        publishUserNotification("notify.email.win", payload);
    }

    public void notifyEmailLoss(BetEntity bet, MatchEntity match) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", bet.getUserId());
        payload.put("betId", bet.getId().toString());
        payload.put("matchId", match.getId().toString());
        payload.put("matchName", match.getHomeTeam() + " vs " + match.getAwayTeam());
        payload.put("stake", bet.getStake());
        payload.put("lost", bet.getStake());
        payload.put("result", "LOSS");
        publishUserNotification("notify.email.loss", payload);
    }

    public void promotion(String title, String detail) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "PROMO");
        payload.put("title", title);
        payload.put("detail", detail);
        publishUserNotification("notify.promo", payload);
    }

    private void publishUserNotification(String routingKey, Map<String, Object> payload) {
        outbox.rabbit(RabbitConfig.NOTIFICATIONS_EXCHANGE, routingKey, payload, 0);
        outbox.rabbit(RabbitConfig.FANOUT_EXCHANGE, "", payload, 0);
    }

    private void sendKafka(String type, UUID matchId, Map<String, Object> payload) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("eventType", type);
        event.put("eventVersion", 1);
        event.put("occurredAt", Instant.now().toString());
        event.put("producer", "winup-backend");
        event.put("matchId", matchId.toString());
        event.put("payload", payload);
        outbox.kafka("match_events", matchId.toString(), event);
    }
}
