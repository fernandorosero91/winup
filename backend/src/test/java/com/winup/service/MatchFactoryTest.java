package com.winup.service;

import com.winup.entity.MatchEntity;
import com.winup.model.SportCatalog;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class MatchFactoryTest {
    private final MatchFactory factory = new MatchFactory(
            new LeagueCatalog(),
            Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void should_create_a_match_for_a_specific_sport() {
        MatchEntity match = factory.createForSport("Fórmula 1");

        assertThat(match.getId()).isNotNull();
        assertThat(match.getSport()).isEqualTo("Fórmula 1");
        assertThat(match.getLeague()).isEqualTo("Fórmula 1");
        assertThat(match.getHomeTeam()).isNotEqualTo(match.getAwayTeam());
        assertThat(match.getStatus()).isEqualTo("OPEN");
        assertThat(match.getMinute()).isEqualTo(1);
    }

    @Test
    void should_create_matches_for_every_catalog_sport() {
        for (String sport : SportCatalog.SPORTS) {
            MatchEntity match = factory.createForSport(sport);

            assertThat(match.getSport()).isEqualTo(sport);
        }
    }
}
