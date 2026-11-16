package com.winup.service.sports.apifootball;

import com.fasterxml.jackson.databind.JsonNode;
import com.winup.service.sports.FixtureStatus;
import com.winup.service.sports.ProviderFixture;
import com.winup.service.sports.ProviderOdds;
import com.winup.service.sports.ProviderScore;
import com.winup.service.sports.ProviderTeam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "winup.sports.provider", havingValue = "api-football")
public class ApiFootballMapper {
    private static final int MATCH_WINNER_BET = 1;
    private static final Logger log = LoggerFactory.getLogger(ApiFootballMapper.class);

    public Optional<ProviderFixture> toFixture(JsonNode node) {
        try {
            JsonNode fixture = node.path("fixture");
            String externalId = fixture.path("id").asText();
            if (externalId.isBlank()) {
                return Optional.empty();
            }
            JsonNode status = fixture.path("status");
            String statusCode = status.path("short").asText("NS");
            JsonNode goals = node.path("goals");
            return Optional.of(new ProviderFixture(
                    externalId,
                    "Fútbol",
                    node.path("league").path("name").asText("Fútbol"),
                    new ProviderTeam(node.path("teams").path("home").path("name").asText("Local"), ""),
                    new ProviderTeam(node.path("teams").path("away").path("name").asText("Visitante"), ""),
                    OffsetDateTime.parse(fixture.path("date").asText()).toInstant(),
                    status(statusCode),
                    new ProviderScore(
                            goals.path("home").asInt(0),
                            goals.path("away").asInt(0),
                            status.path("elapsed").asInt(0),
                            statusCode
                    ),
                    null
            ));
        } catch (RuntimeException exception) {
            log.warn("Partido de API-Football ignorado: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    public Optional<ProviderOdds> toOdds(JsonNode root) {
        if (root == null) {
            return Optional.empty();
        }
        for (JsonNode item : root.path("response")) {
            for (JsonNode bookmaker : item.path("bookmakers")) {
                for (JsonNode bet : bookmaker.path("bets")) {
                    if (bet.path("id").asInt() != MATCH_WINNER_BET) {
                        continue;
                    }
                    Optional<ProviderOdds> odds = oddsOf(bet);
                    if (odds.isPresent()) {
                        return odds;
                    }
                }
            }
        }
        return Optional.empty();
    }

    private Optional<ProviderOdds> oddsOf(JsonNode bet) {
        BigDecimal home = null;
        BigDecimal draw = null;
        BigDecimal away = null;
        for (JsonNode value : bet.path("values")) {
            BigDecimal odd = decimal(value.path("odd").asText());
            if (odd == null) {
                continue;
            }
            switch (value.path("value").asText()) {
                case "Home" -> home = odd;
                case "Draw" -> draw = odd;
                case "Away" -> away = odd;
                default -> {
                }
            }
        }
        if (home == null || away == null) {
            return Optional.empty();
        }
        return Optional.of(new ProviderOdds(home, draw, away));
    }

    private BigDecimal decimal(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private FixtureStatus status(String code) {
        return switch (code) {
            case "1H", "HT", "2H", "ET", "BT", "P", "LIVE" -> FixtureStatus.LIVE;
            case "FT", "AET", "PEN", "AWD", "WO" -> FixtureStatus.FINISHED;
            case "PST", "SUSP", "INT" -> FixtureStatus.POSTPONED;
            case "CANC", "ABD" -> FixtureStatus.CANCELLED;
            default -> FixtureStatus.SCHEDULED;
        };
    }
}
