package com.winup.model.vo;

import com.winup.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LeagueNameTest {
    @Test
    void should_trim_the_league() {
        assertThat(new LeagueName("  LaLiga  ").value()).isEqualTo("LaLiga");
    }

    @Test
    void should_fall_back_when_the_league_is_blank() {
        assertThat(new LeagueName(null).orDefault("Fútbol")).isEqualTo("Fútbol");
        assertThat(new LeagueName("   ").orDefault("Fútbol")).isEqualTo("Fútbol");
    }

    @Test
    void should_reject_a_league_that_is_too_long() {
        assertThatThrownBy(() -> new LeagueName("A".repeat(81)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("largo");
    }
}
