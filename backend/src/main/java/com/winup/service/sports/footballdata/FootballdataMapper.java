package com.winup.service.sports.footballdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.winup.service.sports.FixtureStatus;
import com.winup.service.sports.ProviderFixture;
import com.winup.service.sports.ProviderOdds;
import com.winup.service.sports.ProviderScore;
import com.winup.service.sports.ProviderTeam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "winup.sports.provider", havingValue = "footballdata")
public class FootballdataMapper {
    private static final DateTimeFormatter MATCH_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    public Optional<ProviderFixture> toFixture(JsonNode node, String leagueFallback) {
        String externalId = node.path("match_id").asText();
        if (externalId.isBlank()) {
            return Optional.empty();
        }
        Instant startsAt = start(node);
        if (startsAt == null) {
            return Optional.empty();
        }
        String statusCode = node.path("status").asText("incomplete");
        String league = firstNonBlank(
                node.path("league").path("competition_name").asText(""),
                node.path("league").path("name").asText(""),
                leagueFallback
        );
        return Optional.of(new ProviderFixture(
                externalId,
                "Fútbol",
                league,
                new ProviderTeam(node.path("home_team").path("team_name").asText("Local"), ""),
                new ProviderTeam(node.path("away_team").path("team_name").asText("Visitante"), ""),
                startsAt,
                status(statusCode),
                new ProviderScore(
                        node.path("score").path("home").asInt(0),
                        node.path("score").path("away").asInt(0),
                        node.path("minute").asInt(0),
                        statusCode
                ),
                odds(node).orElse(null)
        ));
    }

    public Optional<ProviderOdds> oddsFromResponse(JsonNode root) {
        if (root == null) {
            return Optional.empty();
        }
        return odds(root.path("data"));
    }

    public Optional<ProviderOdds> odds(JsonNode node) {
        JsonNode odds = node.path("odds");
        if (odds.isMissingNode() || odds.isNull()) {
            return Optional.empty();
        }
        JsonNode winner = odds.path("match_winner");
        if (winner.isObject()) {
            return build(
                    decimal(winner.path("home")),
                    decimal(winner.path("draw")),
                    decimal(winner.path("away"))
            );
        }
        return build(
                decimal(firstPresent(odds, "home_win", "home")),
                decimal(firstPresent(odds, "draw", "draw_win")),
                decimal(firstPresent(odds, "away_win", "away"))
        );
    }

    private Optional<ProviderOdds> build(BigDecimal home, BigDecimal draw, BigDecimal away) {
        if (home == null
                || draw == null
                || away == null
                || home.signum() <= 0
                || draw.signum() <= 0
                || away.signum() <= 0) {
            return Optional.empty();
        }
        return Optional.of(new ProviderOdds(home, draw, away));
    }

    private JsonNode firstPresent(JsonNode node, String first, String second) {
        return node.has(first) ? node.path(first) : node.path(second);
    }

    private BigDecimal decimal(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        try {
            return new BigDecimal(node.asText());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Instant start(JsonNode node) {
        long unix = node.path("date_unix").asLong(0);
        if (unix > 0) {
            return Instant.ofEpochSecond(unix);
        }
        String value = node.path("match_date").asText("");
        if (value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, MATCH_DATE).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "Fútbol";
    }

    private FixtureStatus status(String code) {
        String value = code.toLowerCase(Locale.ROOT);
        if (value.equals("complete")
                || value.equals("completed")
                || value.contains("finished")
                || value.equals("ft")) {
            return FixtureStatus.FINISHED;
        }
        if (value.contains("live")
                || value.contains("in_play")
                || value.contains("inprogress")
                || value.equals("ht")
                || value.equals("1h")
                || value.equals("2h")
                || value.contains("half")
                || value.contains("extra_time")
                || value.contains("penalt")) {
            return FixtureStatus.LIVE;
        }
        if (value.contains("postpon") || value.contains("suspend") || value.contains("interrupt")) {
            return FixtureStatus.POSTPONED;
        }
        if (value.contains("cancel") || value.contains("abandon")) {
            return FixtureStatus.CANCELLED;
        }
        return FixtureStatus.SCHEDULED;
    }
}
