package com.winup.service.sports;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderFixtureTest {
    @Test
    void should_expose_the_odds_of_each_selection() {
        ProviderFixture fixture = fixture(new ProviderOdds(
                new BigDecimal("2.10"),
                new BigDecimal("3.40"),
                new BigDecimal("3.20")
        ));

        assertThat(fixture.oddsFor("LOCAL")).isEqualByComparingTo("2.10");
        assertThat(fixture.oddsFor("EMPATE")).isEqualByComparingTo("3.40");
        assertThat(fixture.oddsFor("VISITANTE")).isEqualByComparingTo("3.20");
        assertThat(fixture.drawMarket()).isTrue();
        assertThat(fixture.finished()).isTrue();
    }

    @Test
    void should_hide_the_draw_market_when_there_is_no_draw_odds() {
        ProviderFixture fixture = fixture(new ProviderOdds(
                new BigDecimal("1.80"),
                null,
                new BigDecimal("2.05")
        ));

        assertThat(fixture.drawMarket()).isFalse();
        assertThat(fixture.oddsFor("EMPATE")).isNull();
    }

    @Test
    void should_allow_a_fixture_without_odds() {
        ProviderFixture fixture = fixture(null);

        assertThat(fixture.oddsFor("LOCAL")).isNull();
        assertThat(fixture.drawMarket()).isFalse();
    }

    @Test
    void should_reject_negative_scores() {
        assertThatThrownBy(() -> new ProviderScore(-1, 0, 0, "NS"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_reject_non_positive_odds() {
        assertThatThrownBy(() -> new ProviderOdds(
                BigDecimal.ZERO,
                null,
                new BigDecimal("2.00")
        ))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_require_a_team_name() {
        assertThatThrownBy(() -> new ProviderTeam(null, "RMA"))
                .isInstanceOf(NullPointerException.class);
    }

    private ProviderFixture fixture(ProviderOdds odds) {
        return new ProviderFixture(
                "test-1",
                "Fútbol",
                "LaLiga",
                new ProviderTeam("Real Madrid", "RMA"),
                new ProviderTeam("Barcelona", "BAR"),
                Instant.parse("2026-11-09T20:00:00Z"),
                FixtureStatus.FINISHED,
                new ProviderScore(2, 1, 90, "FT"),
                odds
        );
    }
}
