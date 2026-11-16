package com.winup.service.sports.footballdata;

import com.winup.config.FootballdataProperties;
import com.winup.service.sports.ProviderFixture;
import com.winup.service.sports.SportQuery;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("live")
@EnabledIfEnvironmentVariable(named = "WINUP_FOOTBALLDATA_API_KEY", matches = ".+")
class FootballdataLiveTest {
    @Test
    void should_fetch_current_fixtures_from_the_real_api() {
        FootballdataProperties properties = new FootballdataProperties(
                env("WINUP_FOOTBALLDATA_BASE_URL", "https://footballdata.io/api/v1"),
                System.getenv("WINUP_FOOTBALLDATA_API_KEY"),
                Long.parseLong(env("WINUP_FOOTBALLDATA_LEAGUE_ID", "10")),
                Long.parseLong(env("WINUP_FOOTBALLDATA_SEASON_ID", "0")),
                env("WINUP_FOOTBALLDATA_LANGUAGE", "es"),
                15000
        );
        RestTemplate restTemplate = new RestTemplateBuilder()
                .rootUri(properties.baseUrl())
                .setConnectTimeout(Duration.ofMillis(properties.timeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.timeoutMs()))
                .build();
        FootballdataAdapter adapter = new FootballdataAdapter(
                properties,
                new FootballdataClient(restTemplate, properties),
                new FootballdataMapper()
        );
        Instant now = Instant.now();

        List<ProviderFixture> fixtures = adapter.fixtures(new SportQuery(
                "Fútbol",
                now.minus(Duration.ofDays(1)),
                now.plus(Duration.ofDays(7)),
                false
        ));

        assertThat(fixtures).isNotEmpty();
        ProviderFixture first = fixtures.getFirst();
        assertThat(first.externalId()).isNotBlank();
        assertThat(first.league()).isNotBlank();
        assertThat(first.home().name()).isNotBlank();
        assertThat(adapter.fixture(first.externalId())).isPresent();
        assertThat(adapter.health().available()).isTrue();
    }

    private String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
