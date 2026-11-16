package com.winup.service.sports.replay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winup.service.sports.FixtureStatus;
import com.winup.service.sports.ProviderFixture;
import com.winup.service.sports.ProviderHealth;
import com.winup.service.sports.SportQuery;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReplaySportsAdapterTest {
    private static final Instant FROM = Instant.parse("2026-11-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-11-30T00:00:00Z");

    private final ReplaySportsAdapter adapter = new ReplaySportsAdapter(
            new ObjectMapper().findAndRegisterModules(),
            new ClassPathResource("sports/replay-fixtures-test.json")
    );

    @Test
    void should_expose_the_replay_provider_id() {
        assertThat(adapter.id()).isEqualTo("REPLAY");
    }

    @Test
    void should_load_all_fixtures_for_all_sports() {
        List<ProviderFixture> fixtures = adapter.fixtures(new SportQuery("TODOS", FROM, TO, false));

        assertThat(fixtures).hasSize(3);
        assertThat(fixtures).isSortedAccordingTo((left, right) -> left.startsAt().compareTo(right.startsAt()));
    }

    @Test
    void should_filter_by_sport() {
        List<ProviderFixture> fixtures = adapter.fixtures(new SportQuery("Fútbol", FROM, TO, false));

        assertThat(fixtures).hasSize(2);
        assertThat(fixtures).allMatch(fixture -> fixture.sport().equals("Fútbol"));
    }

    @Test
    void should_filter_live_fixtures() {
        List<ProviderFixture> fixtures = adapter.fixtures(new SportQuery("TODOS", FROM, TO, true));

        assertThat(fixtures).hasSize(1);
        assertThat(fixtures.getFirst().externalId()).isEqualTo("test-football-live");
        assertThat(fixtures.getFirst().status()).isEqualTo(FixtureStatus.LIVE);
    }

    @Test
    void should_filter_by_time_window() {
        List<ProviderFixture> fixtures = adapter.fixtures(new SportQuery(
                "TODOS",
                Instant.parse("2026-11-10T17:00:00Z"),
                Instant.parse("2026-11-10T19:00:00Z"),
                false
        ));

        assertThat(fixtures).hasSize(1);
        assertThat(fixtures.getFirst().externalId()).isEqualTo("test-football-live");
    }

    @Test
    void should_find_a_fixture_by_external_id() {
        ProviderFixture fixture = adapter.fixture("test-football-finished").orElseThrow();

        assertThat(fixture.finished()).isTrue();
        assertThat(fixture.score().home()).isEqualTo(2);
        assertThat(fixture.score().away()).isEqualTo(1);
        assertThat(fixture.drawMarket()).isTrue();
        assertThat(fixture.oddsFor("LOCAL")).isEqualByComparingTo("2.10");
    }

    @Test
    void should_return_empty_when_the_external_id_is_unknown() {
        assertThat(adapter.fixture("no-existe")).isEmpty();
        assertThat(adapter.fixture(null)).isEmpty();
    }

    @Test
    void should_expose_the_fixture_odds() {
        assertThat(adapter.odds("test-football-finished")).isPresent();
        assertThat(adapter.odds("test-football-finished").orElseThrow().draw()).isEqualByComparingTo("3.40");
        assertThat(adapter.odds("test-nba-scheduled").orElseThrow().draw()).isNull();
        assertThat(adapter.odds("no-existe")).isEmpty();
    }

    @Test
    void should_report_the_replay_health() {
        ProviderHealth health = adapter.health();

        assertThat(health.provider()).isEqualTo("REPLAY");
        assertThat(health.available()).isTrue();
        assertThat(health.remainingQuota()).isEqualTo(-1);
        assertThat(health.message()).isNotBlank();
    }
}
