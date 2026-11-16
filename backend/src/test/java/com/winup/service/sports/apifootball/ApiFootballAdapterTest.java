package com.winup.service.sports.apifootball;

import com.winup.config.ApiFootballProperties;
import com.winup.service.sports.FixtureStatus;
import com.winup.service.sports.ProviderFixture;
import com.winup.service.sports.ProviderOdds;
import com.winup.service.sports.SportQuery;
import com.winup.service.sports.quota.InMemoryQuotaStore;
import com.winup.config.SportsQuotaProperties;
import com.winup.service.sports.quota.ProviderQuotaGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ApiFootballAdapterTest {
    private static final String BASE = "https://v3.football.api-sports.io";
    private static final String FIXTURES_URL = BASE
            + "/fixtures?league=140&season=2026&from=2026-11-01&to=2026-11-30";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ApiFootballProperties properties = new ApiFootballProperties(BASE, "test-key", 140, 2026, 5000);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC);
    private final ProviderQuotaGuard guard = new ProviderQuotaGuard(
            clock,
            new SportsQuotaProperties(100),
            new InMemoryQuotaStore(clock)
    );
    private final ApiFootballMapper mapper = new ApiFootballMapper();
    private final ApiFootballClient client = new ApiFootballClient(restTemplate, properties, guard);
    private final ApiFootballAdapter adapter = new ApiFootballAdapter(properties, client, mapper);

    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(BASE));
        server = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    void should_map_fixtures_and_quota_headers() {
        server.expect(requestTo(FIXTURES_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("x-apisports-key", "test-key"))
                .andRespond(withSuccess(finishedFixture(), MediaType.APPLICATION_JSON)
                        .headers(quotaHeaders(100, 99)));

        List<ProviderFixture> fixtures = adapter.fixtures(query());

        assertThat(fixtures).hasSize(1);
        ProviderFixture fixture = fixtures.getFirst();
        assertThat(fixture.externalId()).isEqualTo("123");
        assertThat(fixture.sport()).isEqualTo("Fútbol");
        assertThat(fixture.league()).isEqualTo("La Liga");
        assertThat(fixture.home().name()).isEqualTo("Barcelona");
        assertThat(fixture.away().name()).isEqualTo("Real Madrid");
        assertThat(fixture.status()).isEqualTo(FixtureStatus.FINISHED);
        assertThat(fixture.score().home()).isEqualTo(2);
        assertThat(fixture.score().away()).isEqualTo(1);
        assertThat(fixture.score().minute()).isEqualTo(90);
        assertThat(fixture.odds()).isNull();
        assertThat(guard.health(ApiFootballAdapter.PROVIDER).remainingQuota()).isEqualTo(99);
        server.verify();
    }

    @Test
    void should_query_live_fixtures_without_league() {
        server.expect(requestTo(BASE + "/fixtures?live=all"))
                .andRespond(withSuccess(liveFixture(), MediaType.APPLICATION_JSON));

        List<ProviderFixture> fixtures = adapter.fixtures(new SportQuery(
                "Fútbol",
                Instant.parse("2026-11-01T00:00:00Z"),
                Instant.parse("2026-11-30T00:00:00Z"),
                true
        ));

        assertThat(fixtures).hasSize(1);
        assertThat(fixtures.getFirst().status()).isEqualTo(FixtureStatus.LIVE);
        assertThat(fixtures.getFirst().score().minute()).isEqualTo(65);
        server.verify();
    }

    @Test
    void should_find_a_fixture_by_external_id() {
        server.expect(requestTo(BASE + "/fixtures?id=123"))
                .andRespond(withSuccess(finishedFixture(), MediaType.APPLICATION_JSON));

        ProviderFixture fixture = adapter.fixture("123").orElseThrow();

        assertThat(fixture.finished()).isTrue();
        assertThat(fixture.home().name()).isEqualTo("Barcelona");
        server.verify();
    }

    @Test
    void should_map_the_match_winner_odds() {
        server.expect(requestTo(BASE + "/odds?fixture=123"))
                .andRespond(withSuccess(oddsResponse(), MediaType.APPLICATION_JSON));

        ProviderOdds odds = adapter.odds("123").orElseThrow();

        assertThat(odds.local()).isEqualByComparingTo("1.80");
        assertThat(odds.draw()).isEqualByComparingTo("3.50");
        assertThat(odds.away()).isEqualByComparingTo("4.20");
        server.verify();
    }

    @Test
    void should_open_the_circuit_when_the_api_rate_limits() {
        server.expect(requestTo(FIXTURES_URL))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThat(adapter.fixtures(query())).isEmpty();
        assertThat(guard.canCall(ApiFootballAdapter.PROVIDER)).isFalse();
        assertThat(adapter.fixtures(query())).isEmpty();

        server.verify();
    }

    @Test
    void should_map_non_played_statuses() {
        String body = """
                {"response":[
                  {"fixture":{"id":200,"date":"2026-11-12T20:00:00+00:00","status":{"short":"NS","elapsed":null}},
                   "league":{"name":"La Liga"},"teams":{"home":{"name":"Sevilla"},"away":{"name":"Villarreal"}},
                   "goals":{"home":null,"away":null}},
                  {"fixture":{"id":201,"date":"2026-11-12T22:00:00+00:00","status":{"short":"CANC","elapsed":null}},
                   "league":{"name":"La Liga"},"teams":{"home":{"name":"Valencia"},"away":{"name":"Getafe"}},
                   "goals":{"home":null,"away":null}}
                ]}
                """;
        server.expect(requestTo(FIXTURES_URL))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        List<ProviderFixture> fixtures = adapter.fixtures(query());

        assertThat(fixtures).extracting(ProviderFixture::status)
                .containsExactly(FixtureStatus.SCHEDULED, FixtureStatus.CANCELLED);
        server.verify();
    }

    @Test
    void should_require_the_api_key() {
        ApiFootballProperties withoutKey = new ApiFootballProperties(BASE, "", 140, 2026, 5000);

        assertThatThrownBy(() -> new ApiFootballClient(restTemplate, withoutKey, guard))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("WINUP_API_FOOTBALL_KEY");
    }

    @Test
    void should_require_a_league_for_non_live_queries() {
        ApiFootballAdapter withoutLeague = new ApiFootballAdapter(
                new ApiFootballProperties(BASE, "test-key", 0, 0, 5000),
                new ApiFootballClient(
                        restTemplate,
                        new ApiFootballProperties(BASE, "test-key", 0, 0, 5000),
                        guard
                ),
                mapper
        );

        assertThatThrownBy(() -> withoutLeague.fixtures(query()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LEAGUE_ID");
    }

    private SportQuery query() {
        return new SportQuery(
                "Fútbol",
                Instant.parse("2026-11-01T00:00:00Z"),
                Instant.parse("2026-11-30T00:00:00Z"),
                false
        );
    }

    private HttpHeaders quotaHeaders(int limit, int remaining) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-ratelimit-requests-limit", String.valueOf(limit));
        headers.set("x-ratelimit-requests-remaining", String.valueOf(remaining));
        return headers;
    }

    private String finishedFixture() {
        return """
                {"response":[
                  {"fixture":{"id":123,"date":"2026-11-09T20:00:00+00:00","status":{"short":"FT","elapsed":90}},
                   "league":{"name":"La Liga"},
                   "teams":{"home":{"name":"Barcelona"},"away":{"name":"Real Madrid"}},
                   "goals":{"home":2,"away":1}}
                ],"errors":[]}
                """;
    }

    private String liveFixture() {
        return """
                {"response":[
                  {"fixture":{"id":124,"date":"2026-11-10T18:30:00+00:00","status":{"short":"2H","elapsed":65}},
                   "league":{"name":"Premier League"},
                   "teams":{"home":{"name":"Manchester City"},"away":{"name":"Liverpool"}},
                   "goals":{"home":1,"away":0}}
                ],"errors":[]}
                """;
    }

    private String oddsResponse() {
        return """
                {"response":[
                  {"bookmakers":[
                    {"bets":[
                      {"id":1,"name":"Match Winner","values":[
                        {"value":"Home","odd":"1.80"},
                        {"value":"Draw","odd":"3.50"},
                        {"value":"Away","odd":"4.20"}
                      ]}
                    ]}
                  ]}
                ],"errors":[]}
                """;
    }
}
