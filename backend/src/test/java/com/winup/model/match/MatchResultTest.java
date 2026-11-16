package com.winup.model.match;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchResultTest {
    @Test
    void should_accept_a_final_result() {
        assertThat(new MatchResult("LOCAL", 2, 1).isFinal()).isTrue();
    }

    @Test
    void should_accept_a_pending_result() {
        assertThat(MatchResult.pending().isFinal()).isFalse();
    }

    @Test
    void should_reject_an_invalid_winner() {
        assertThatThrownBy(() -> new MatchResult("LOCALX", 1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_reject_negative_scores() {
        assertThatThrownBy(() -> new MatchResult("LOCAL", -1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
