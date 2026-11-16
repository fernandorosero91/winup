package com.winup.service.sports.replay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winup.service.sports.ProviderFixture;
import com.winup.service.sports.SportQuery;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReplayDeterminismTest {
    private static final SportQuery QUERY = new SportQuery(
            "TODOS",
            Instant.parse("2026-11-01T00:00:00Z"),
            Instant.parse("2026-11-30T00:00:00Z"),
            false
    );

    @Test
    void should_return_the_same_fixtures_on_every_load() {
        ReplaySportsAdapter first = adapter();
        ReplaySportsAdapter second = adapter();

        List<ProviderFixture> firstRun = first.fixtures(QUERY);
        List<ProviderFixture> secondRun = second.fixtures(QUERY);

        assertThat(firstRun).isEqualTo(secondRun);
        assertThat(first.fixtures(QUERY)).isEqualTo(firstRun);
    }

    @Test
    void should_return_an_immutable_snapshot() {
        List<ProviderFixture> fixtures = adapter().fixtures(QUERY);

        assertThatThrownBy(() -> fixtures.add(fixtures.getFirst()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private ReplaySportsAdapter adapter() {
        return new ReplaySportsAdapter(
                new ObjectMapper().findAndRegisterModules(),
                new ClassPathResource("sports/replay-fixtures-test.json")
        );
    }
}
