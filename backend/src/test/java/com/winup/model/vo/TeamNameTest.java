package com.winup.model.vo;

import com.winup.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TeamNameTest {
    @Test
    void should_trim_the_name() {
        TeamName team = new TeamName("  Real Madrid  ");

        assertThat(team.value()).isEqualTo("Real Madrid");
    }

    @Test
    void should_compare_names_ignoring_case() {
        TeamName home = new TeamName("Real Madrid");
        TeamName away = new TeamName("real madrid");

        assertThat(home.sameAs(away)).isTrue();
        assertThat(home.sameAs(new TeamName("Barcelona"))).isFalse();
    }

    @Test
    void should_reject_a_blank_name() {
        assertThatThrownBy(() -> new TeamName("   "))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("obligatorio");
    }

    @Test
    void should_reject_a_name_that_is_too_long() {
        String longName = "A".repeat(81);

        assertThatThrownBy(() -> new TeamName(longName))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("largo");
    }
}
