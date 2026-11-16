package com.winup.service;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class MatchResultSimulatorTest {
    private final MatchResultSimulator simulator = new MatchResultSimulator();

    @Test
    void should_return_a_consistent_three_way_result_for_football() {
        MatchResultSimulator.Result result = simulator.simulate("Fútbol", new Random(42));

        assertThat(result.homeScore()).isGreaterThanOrEqualTo(0);
        assertThat(result.awayScore()).isGreaterThanOrEqualTo(0);
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
    void should_never_return_a_draw_for_two_way_sports() {
        for (int seed = 0; seed < 50; seed++) {
            MatchResultSimulator.Result result = simulator.simulate("Baloncesto", new Random(seed));
            assertThat(result.homeScore()).isNotEqualTo(result.awayScore());
        }
    }

    @Test
    void should_score_tennis_within_three_games() {
        MatchResultSimulator.Result result = simulator.simulate("Tenis", new Random(7));

        assertThat(result.homeScore()).isBetween(0, 3);
        assertThat(result.awayScore()).isBetween(0, 3);
    }
}
