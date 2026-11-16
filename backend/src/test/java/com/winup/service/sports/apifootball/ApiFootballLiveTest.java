package com.winup.service.sports.apifootball;

import com.winup.config.ApiFootballProperties;
import com.winup.config.SportsQuotaProperties;
import com.winup.service.sports.ProviderFixture;
import com.winup.service.sports.SportQuery;
import com.winup.service.sports.quota.InMemoryQuotaStore;
import com.winup.service.sports.quota.ProviderQuotaGuard;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("live")
@EnabledIfEnvironmentVariable(named = "WINUP_API_FOOTBALL_KEY", matches = ".+")
class ApiFootballLiveTest {
    @Test
    void should_fetch_fixtures_from_the_real_api() {
        String baseUrl = System.getenv().getOrDefault(
                "WINUP_API_FOOTBALL_BASE_URL",
                "https://v3.football.api-sports.io"
        );
        ApiFootballProperties properties = new ApiFootballProperties(
                baseUrl,
                System.getenv("WINUP_API_FOOTBALL_KEY"),
                Long.parseLong(System.getenv().getOrDefault("WINUP_API_FOOTBALL_LEAGUE_ID", "140")),
                Integer.parseInt(System.getenv().getOrDefault("WINUP_API_FOOTBALL_SEASON", "2026")),
                15000
        );
        RestTemplate restTemplate = new RestTemplateBuilder()
                .rootUri(properties.baseUrl())
                .setConnectTimeout(Duration.ofMillis(properties.timeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.timeoutMs()))
                .build();
        Clock clock = Clock.systemUTC();
        ProviderQuotaGuard guard = new ProviderQuotaGuard(
                clock,
                new SportsQuotaProperties(100),
                new InMemoryQuotaStore(clock)
        );
        ApiFootballAdapter adapter = new ApiFootballAdapter(
                properties,
                new ApiFootballClient(restTemplate, properties, guard),
                new ApiFootballMapper()
        );
        Instant now = Instant.now();
        Instant from = instant("WINUP_API_FOOTBALL_SMOKE_FROM", now.minus(Duration.ofDays(7)));
        Instant to = instant("WINUP_API_FOOTBALL_SMOKE_TO", now);

        List<ProviderFixture> fixtures = adapter.fixtures(new SportQuery(
                "Fútbol",
                from,
                to,
                false
        ));

        assertThat(fixtures).isNotEmpty();
        ProviderFixture first = fixtures.getFirst();
        assertThat(first.externalId()).isNotBlank();
        assertThat(first.league()).isNotBlank();
        assertThat(first.home().name()).isNotBlank();
        assertThat(adapter.fixture(first.externalId())).isPresent();
        assertThat(guard.health(ApiFootballAdapter.PROVIDER).available()).isTrue();
    }

    private Instant instant(String name, Instant fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : Instant.parse(value);
    }
}
