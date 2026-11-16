package com.winup.service.sports.footballdata;

import com.winup.config.FootballdataProperties;
import com.winup.service.sports.ProviderFixture;
import com.winup.service.sports.ProviderOdds;
import com.winup.service.sports.SportQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FootballdataAdapterTest {
    private static final String BASE = "https://footballdata.io/api/v1";
    private static final String KEY = "fd_test_key";
    private static final String MATCHES = BASE
            + "/leagues/10/matches?limit=100&page=1&lang=es&season_id=117065"
            + "&from=2026-11-16&to=2026-11-23";

    private final RestTemplate restTemplate = new RestTemplate();
    private final FootballdataProperties properties = new FootballdataProperties(
            BASE,
            KEY,
            10,
            117065,
            "es",
            5000
    );
    private final FootballdataClient client = new FootballdataClient(restTemplate, properties);
    private final FootballdataAdapter adapter = new FootballdataAdapter(
            properties,
            client,
            new FootballdataMapper()
    );

    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(BASE));
        server = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    void should_map_matches_with_inline_odds() {
        server.expect(requestTo(MATCHES))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + KEY))
                .andRespond(withSuccess(matchesResponse(), MediaType.APPLICATION_JSON));

        List<ProviderFixture> fixtures = adapter.fixtures(query());

        assertThat(fixtures).hasSize(1);
        ProviderFixture fixture = fixtures.getFirst();
        assertThat(fixture.externalId()).isEqualTo("3744623801");
        assertThat(fixture.league()).isEqualTo("La Liga");
        assertThat(fixture.odds().local()).isEqualByComparingTo("1.5");
        assertThat(client.health().remainingQuota()).isEqualTo(999);
        server.verify();
    }

    @Test
    void should_find_a_match_by_id() {
        server.expect(requestTo(BASE + "/matches/3744623801?lang=es"))
                .andRespond(withSuccess(singleMatchResponse(), MediaType.APPLICATION_JSON));

        ProviderFixture fixture = adapter.fixture("3744623801").orElseThrow();

        assertThat(fixture.home().name()).isEqualTo("Athletic Club Bilbao");
        server.verify();
    }

    @Test
    void should_fetch_the_dedicated_odds_endpoint() {
        server.expect(requestTo(BASE + "/matches/3744623801/odds?lang=es"))
                .andRespond(withSuccess(oddsResponse(), MediaType.APPLICATION_JSON));

        ProviderOdds odds = adapter.odds("3744623801").orElseThrow();

        assertThat(odds.local()).isEqualByComparingTo("1.5");
        assertThat(odds.draw()).isEqualByComparingTo("4.2");
        assertThat(odds.away()).isEqualByComparingTo("6.5");
        server.verify();
    }

    @Test
    void should_query_live_matches_by_status() {
        server.expect(requestTo(BASE + "/leagues/10/matches?limit=100&page=1&lang=es&status=live"))
                .andRespond(withSuccess(matchesResponse(), MediaType.APPLICATION_JSON));

        List<ProviderFixture> fixtures = adapter.fixtures(new SportQuery(
                "Fútbol",
                Instant.parse("2026-11-01T00:00:00Z"),
                Instant.parse("2026-11-30T00:00:00Z"),
                true
        ));

        assertThat(fixtures).hasSize(1);
        server.verify();
    }

    @Test
    void should_require_the_api_key() {
        FootballdataProperties withoutKey = new FootballdataProperties(
                BASE,
                "",
                10,
                117065,
                "es",
                5000
        );

        assertThatThrownBy(() -> new FootballdataClient(restTemplate, withoutKey))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("WINUP_FOOTBALLDATA_API_KEY");
    }

    private SportQuery query() {
        return new SportQuery(
                "Fútbol",
                Instant.parse("2026-11-16T00:00:00Z"),
                Instant.parse("2026-11-23T00:00:00Z"),
                false
        );
    }

    private String matchesResponse() {
        return """
                {"success":true,"data":{
                  "league":{"league_id":10,"competition_name":"La Liga"},
                  "matches":[{
                    "match_id":3744623801,
                    "match_date":"2026-11-20 20:00:00",
                    "date_unix":1795204800,
                    "status":"incomplete",
                    "home_team":{"team_name":"Athletic Club Bilbao"},
                    "away_team":{"team_name":"RCD Espanyol"},
                    "score":{"home":0,"away":0},
                    "odds":{"match_winner":{"home":1.5,"draw":4.2,"away":6.5}}
                  }]
                },"meta":{"requests_limit":1000,"requests_remaining":999,
                  "pagination":{"total_pages":1}}}
                """;
    }

    private String singleMatchResponse() {
        return """
                {"success":true,"data":{
                  "match_id":3744623801,
                  "match_date":"2026-11-20 20:00:00",
                  "date_unix":1795204800,
                  "status":"incomplete",
                  "home_team":{"team_name":"Athletic Club Bilbao"},
                  "away_team":{"team_name":"RCD Espanyol"},
                  "score":{"home":0,"away":0},
                  "league":{"competition_name":"La Liga"}
                },"meta":{"requests_limit":1000,"requests_remaining":998}}
                """;
    }

    private String oddsResponse() {
        return """
                {"success":true,"data":{
                  "match_id":3744623801,
                  "odds":{"match_winner":{"home":1.5,"draw":4.2,"away":6.5}}
                },"meta":{"requests_limit":1000,"requests_remaining":997}}
                """;
    }
}
