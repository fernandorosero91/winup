package com.winup.service.sports.footballdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winup.service.sports.FixtureStatus;
import com.winup.service.sports.ProviderFixture;
import com.winup.service.sports.ProviderOdds;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FootballdataMapperTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FootballdataMapper mapper = new FootballdataMapper();

    @Test
    void should_map_a_scheduled_match_with_inline_odds() throws Exception {
        JsonNode node = json("""
                {
                  "match_id": 3744623801,
                  "match_date": "2026-11-20 20:00:00",
                  "date_unix": 1795204800,
                  "status": "incomplete",
                  "minute": null,
                  "home_team": {"team_name": "Athletic Club Bilbao"},
                  "away_team": {"team_name": "RCD Espanyol"},
                  "score": {"home": 0, "away": 0},
                  "odds": {"match_winner": {"home": 1.5, "draw": 4.2, "away": 6.5}}
                }
                """);

        ProviderFixture fixture = mapper.toFixture(node, "La Liga").orElseThrow();

        assertThat(fixture.externalId()).isEqualTo("3744623801");
        assertThat(fixture.league()).isEqualTo("La Liga");
        assertThat(fixture.home().name()).isEqualTo("Athletic Club Bilbao");
        assertThat(fixture.away().name()).isEqualTo("RCD Espanyol");
        assertThat(fixture.status()).isEqualTo(FixtureStatus.SCHEDULED);
        assertThat(fixture.startsAt()).isEqualTo(java.time.Instant.ofEpochSecond(1795204800));
        assertThat(fixture.odds().local()).isEqualByComparingTo("1.5");
        assertThat(fixture.odds().draw()).isEqualByComparingTo("4.2");
        assertThat(fixture.odds().away()).isEqualByComparingTo("6.5");
    }

    @Test
    void should_map_a_finished_match_and_the_flat_odds_shape() throws Exception {
        JsonNode node = json("""
                {
                  "match_id": 3714519137,
                  "match_date": "2026-11-08 20:00:00",
                  "date_unix": 1794168000,
                  "status": "complete",
                  "minute": null,
                  "home_team": {"team_name": "Atlético Madrid"},
                  "away_team": {"team_name": "FC Barcelona"},
                  "score": {"home": 1, "away": 2},
                  "odds": {"home_win": 2.1, "draw": 3.2, "away_win": 3.4}
                }
                """);

        ProviderFixture fixture = mapper.toFixture(node, "La Liga").orElseThrow();

        assertThat(fixture.status()).isEqualTo(FixtureStatus.FINISHED);
        assertThat(fixture.score().home()).isEqualTo(1);
        assertThat(fixture.score().away()).isEqualTo(2);
        assertThat(fixture.odds().local()).isEqualByComparingTo("2.1");
        assertThat(fixture.odds().away()).isEqualByComparingTo("3.4");
    }

    @Test
    void should_ignore_zero_odds() throws Exception {
        JsonNode node = json("""
                {
                  "match_id": 1,
                  "match_date": "2026-11-08 20:00:00",
                  "date_unix": 1794168000,
                  "status": "incomplete",
                  "home_team": {"team_name": "A"},
                  "away_team": {"team_name": "B"},
                  "score": {"home": 0, "away": 0},
                  "odds": {"home_win": 0, "draw": 0, "away_win": 0}
                }
                """);

        ProviderFixture fixture = mapper.toFixture(node, "La Liga").orElseThrow();

        assertThat(fixture.odds()).isNull();
    }

    @Test
    void should_ignore_odds_when_the_draw_is_missing() throws Exception {
        JsonNode node = json("""
                {
                  "match_id": 2,
                  "match_date": "2026-11-08 20:00:00",
                  "date_unix": 1794168000,
                  "status": "incomplete",
                  "home_team": {"team_name": "A"},
                  "away_team": {"team_name": "B"},
                  "score": {"home": 0, "away": 0},
                  "odds": {"home_win": 1.5, "draw": 0, "away_win": 3.2}
                }
                """);

        ProviderFixture fixture = mapper.toFixture(node, "La Liga").orElseThrow();

        assertThat(fixture.odds()).isNull();
    }

    @Test
    void should_map_postponed_and_cancelled_statuses() throws Exception {
        assertThat(status("postponed")).isEqualTo(FixtureStatus.POSTPONED);
        assertThat(status("cancelled")).isEqualTo(FixtureStatus.CANCELLED);
        assertThat(status("live")).isEqualTo(FixtureStatus.LIVE);
    }

    @Test
    void should_read_odds_from_the_dedicated_response() throws Exception {
        JsonNode root = json("""
                {
                  "success": true,
                  "data": {
                    "match_id": 3744623801,
                    "odds": {"match_winner": {"home": 1.5, "draw": 4.2, "away": 6.5}}
                  }
                }
                """);

        ProviderOdds odds = mapper.oddsFromResponse(root).orElseThrow();

        assertThat(odds.local()).isEqualByComparingTo("1.5");
        assertThat(odds.draw()).isEqualByComparingTo("4.2");
        assertThat(odds.away()).isEqualByComparingTo("6.5");
    }

    private FixtureStatus status(String status) throws Exception {
        JsonNode node = json("""
                {
                  "match_id": 1,
                  "match_date": "2026-11-08 20:00:00",
                  "status": "%s",
                  "home_team": {"team_name": "A"},
                  "away_team": {"team_name": "B"},
                  "score": {"home": 0, "away": 0}
                }
                """.formatted(status));
        return mapper.toFixture(node, "La Liga").orElseThrow().status();
    }

    private JsonNode json(String value) throws Exception {
        return objectMapper.readTree(value);
    }
}
