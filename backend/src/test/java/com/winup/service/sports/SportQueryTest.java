package com.winup.service.sports;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SportQueryTest {
    private static final Instant FROM = Instant.parse("2026-11-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-11-30T00:00:00Z");

    @Test
    void should_match_all_sports_with_todos() {
        SportQuery query = new SportQuery("TODOS", FROM, TO, false);

        assertThat(query.matchesSport("Fútbol")).isTrue();
        assertThat(query.matchesSport("NBA")).isTrue();
    }

    @Test
    void should_match_a_sport_case_insensitive() {
        SportQuery query = new SportQuery("fútbol", FROM, TO, false);

        assertThat(query.matchesSport("Fútbol")).isTrue();
        assertThat(query.matchesSport("NBA")).isFalse();
    }

    @Test
    void should_reject_a_reversed_window() {
        assertThatThrownBy(() -> new SportQuery("Fútbol", TO, FROM, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_require_the_query_fields() {
        assertThatThrownBy(() -> new SportQuery(null, FROM, TO, false))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SportQuery("Fútbol", null, TO, false))
                .isInstanceOf(NullPointerException.class);
    }
}
