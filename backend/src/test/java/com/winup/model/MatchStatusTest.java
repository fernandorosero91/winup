package com.winup.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchStatusTest {
    @Test
    void should_map_an_open_match_to_upcoming_or_live() {
        assertThat(MatchStatus.OPEN.label(0)).isEqualTo("PRÓXIMO");
        assertThat(MatchStatus.OPEN.label(68)).isEqualTo("EN VIVO");
    }

    @Test
    void should_map_the_finished_and_void_labels() {
        assertThat(MatchStatus.CLOSED.label(90)).isEqualTo("FINALIZADO");
        assertThat(MatchStatus.VOID.label(0)).isEqualTo("ANULADO");
    }

    @Test
    void should_parse_the_persisted_codes() {
        assertThat(MatchStatus.from("OPEN")).isEqualTo(MatchStatus.OPEN);
        assertThat(MatchStatus.from("CLOSED")).isEqualTo(MatchStatus.CLOSED);
        assertThat(MatchStatus.from("VOID")).isEqualTo(MatchStatus.VOID);
    }

    @Test
    void should_reject_an_unknown_code() {
        assertThatThrownBy(() -> MatchStatus.from("CANCELLED"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CANCELLED");
    }
}
