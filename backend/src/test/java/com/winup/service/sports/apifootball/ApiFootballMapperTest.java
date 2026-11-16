package com.winup.service.sports.apifootball;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winup.service.sports.FixtureStatus;
import com.winup.service.sports.ProviderFixture;
import com.winup.service.sports.ProviderOdds;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiFootballMapperTest {
    private final ObjectMapper json = new ObjectMapper();
    private final ApiFootballMapper mapper = new ApiFootballMapper();

    @Test
    void should_map_a_finished_fixture() throws Exception {
        JsonNode node = json.readTree(fixture("FT", 90, 2, 1));

        ProviderFixture fixture = mapper.toFixture(node).orElseThrow();

        assertThat(fixture.externalId()).isEqualTo("123");
        assertThat(fixture.league()).isEqualTo("La Liga");
        assertThat(fixture.home().name()).isEqualTo("Barcelona");
        assertThat(fixture.away().name()).isEqualTo("Real Madrid");
        assertThat(fixture.status()).isEqualTo(FixtureStatus.FINISHED);
        assertThat(fixture.score().home()).isEqualTo(2);
        assertThat(fixture.score().away()).isEqualTo(1);
        assertThat(fixture.score().minute()).isEqualTo(90);
        assertThat(fixture.odds()).isNull();
    }

    @Test
    void should_map_a_live_fixture() throws Exception {
        JsonNode node = json.readTree(fixture("2H", 65, 1, 0));

        ProviderFixture fixture = mapper.toFixture(node).orElseThrow();

        assertThat(fixture.status()).isEqualTo(FixtureStatus.LIVE);
        assertThat(fixture.score().minute()).isEqualTo(65);
    }

    @Test
    void should_map_unknown_status_as_scheduled() throws Exception {
        JsonNode node = json.readTree(fixture("NS", 0, 0, 0));

        assertThat(mapper.toFixture(node).orElseThrow().status()).isEqualTo(FixtureStatus.SCHEDULED);
    }

    @Test
    void should_ignore_a_fixture_without_id() throws Exception {
        JsonNode node = json.readTree("""
                {"fixture":{"date":"2026-11-09T20:00:00+00:00","status":{"short":"FT"}},
                 "league":{"name":"La Liga"},"teams":{"home":{"name":"A"},"away":{"name":"B"}},
                 "goals":{"home":0,"away":0}}
                """);

        assertThat(mapper.toFixture(node)).isEmpty();
    }

    @Test
    void should_ignore_a_broken_date() throws Exception {
        JsonNode node = json.readTree("""
                {"fixture":{"id":1,"date":"sin-fecha","status":{"short":"FT"}},
                 "league":{"name":"La Liga"},"teams":{"home":{"name":"A"},"away":{"name":"B"}},
                 "goals":{"home":0,"away":0}}
                """);

        assertThat(mapper.toFixture(node)).isEmpty();
    }

    @Test
    void should_map_the_match_winner_odds() throws Exception {
        JsonNode node = json.readTree("""
                {"response":[{"bookmakers":[{"bets":[
                  {"id":1,"values":[
                    {"value":"Home","odd":"1.80"},
                    {"value":"Draw","odd":"3.50"},
                    {"value":"Away","odd":"4.20"}
                  ]}
                ]}]}]}
                """);

        ProviderOdds odds = mapper.toOdds(node).orElseThrow();

        assertThat(odds.local()).isEqualByComparingTo("1.80");
        assertThat(odds.draw()).isEqualByComparingTo("3.50");
        assertThat(odds.away()).isEqualByComparingTo("4.20");
    }

    @Test
    void should_return_empty_without_a_match_winner_bet() throws Exception {
        JsonNode node = json.readTree("""
                {"response":[{"bookmakers":[{"bets":[{"id":2,"values":[]}]}]}]}
                """);

        assertThat(mapper.toOdds(node)).isEmpty();
        assertThat(mapper.toOdds(null)).isEmpty();
    }

    private String fixture(String status, int minute, int home, int away) {
        return """
                {"fixture":{"id":123,"date":"2026-11-09T20:00:00+00:00","status":{"short":"%s","elapsed":%d}},
                 "league":{"name":"La Liga"},
                 "teams":{"home":{"name":"Barcelona"},"away":{"name":"Real Madrid"}},
                 "goals":{"home":%d,"away":%d}}
                """.formatted(status, minute, home, away);
    }
}
