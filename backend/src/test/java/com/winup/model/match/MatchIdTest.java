package com.winup.model.match;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchIdTest {
    @Test
    void should_reject_a_null_id() {
        assertThatThrownBy(() -> new MatchId(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_parse_from_string() {
        UUID value = UUID.randomUUID();

        assertThat(MatchId.of(value.toString()).value()).isEqualTo(value);
    }
}
