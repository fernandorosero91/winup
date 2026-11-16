package com.winup.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class MatchResultSimulatorTest {
    private static final BigDecimal LOW = new BigDecimal("1.30");
    private static final BigDecimal HIGH = new BigDecimal("9.00");

    private final MatchResultSimulator simulator = new MatchResultSimulator();

    @Test
    void should_return_a_consistent_three_way_result_for_football() {
        MatchResultSimulator.Result result = simulator.simulate(
                "Fútbol",
                new BigDecimal("1.80"),
                new BigDecimal("3.40"),
                new BigDecimal("4.50"),
                new Random(42)
        );

        assertThat(result.winner()).isIn("LOCAL", "EMPATE", "VISITANTE");
        if (result.winner().equals("LOCAL")) {
            assertThat(result.homeScore()).isGreaterThan(result.awayScore());
        }
        if (result.winner().equals("VISITANTE")) {
            assertThat(result.awayScore()).isGreaterThan(result.homeScore());
        }
        if (result.winner().equals("EMPATE")) {
            assertThat(result.homeScore()).isEqualTo(result.awayScore());
        }
    }

    @Test
    void should_favor_the_local_team_when_its_odds_are_lower() {
        int local = 0;

        for (int seed = 0; seed < 2_000; seed++) {
            MatchResultSimulator.Result result = simulator.simulate(
                    "Fútbol",
                    LOW,
                    new BigDecimal("7.00"),
                    HIGH,
                    new Random(seed)
            );
            if (result.winner().equals("LOCAL")) {
                local++;
            }
        }

        assertThat(local).isGreaterThan(1_300);
    }

    @Test
    void should_favor_the_away_team_when_its_odds_are_lower() {
        int away = 0;

        for (int seed = 0; seed < 2_000; seed++) {
            MatchResultSimulator.Result result = simulator.simulate(
                    "Fútbol",
                    HIGH,
                    new BigDecimal("7.00"),
                    LOW,
                    new Random(seed)
            );
            if (result.winner().equals("VISITANTE")) {
                away++;
            }
        }

        assertThat(away).isGreaterThan(1_300);
    }

    @Test
    void should_favor_a_draw_when_draw_odds_are_lower() {
        int draws = 0;

        for (int seed = 0; seed < 2_000; seed++) {
            MatchResultSimulator.Result result = simulator.simulate(
                    "Fútbol",
                    new BigDecimal("6.00"),
                    new BigDecimal("1.40"),
                    new BigDecimal("6.00"),
                    new Random(seed)
            );
            if (result.winner().equals("EMPATE")) {
                draws++;
            }
        }

        assertThat(draws).isGreaterThan(1_300);
    }

    @Test
    void should_never_return_a_draw_for_two_way_sports() {
        for (int seed = 0; seed < 200; seed++) {
            MatchResultSimulator.Result result = simulator.simulate(
                    "Baloncesto",
                    new BigDecimal("1.60"),
                    new BigDecimal("3.50"),
                    new BigDecimal("2.40"),
                    new Random(seed)
            );
            assertThat(result.homeScore()).isNotEqualTo(result.awayScore());
        }
    }

    @Test
    void should_score_tennis_within_three_games() {
        MatchResultSimulator.Result result = simulator.simulate(
                "Tenis",
                new BigDecimal("2.10"),
                null,
                new BigDecimal("1.75"),
                new Random(7)
        );

        assertThat(result.homeScore()).isBetween(0, 3);
        assertThat(result.awayScore()).isBetween(0, 3);
    }
}
