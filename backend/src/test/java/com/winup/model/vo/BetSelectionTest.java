package com.winup.model.vo;

import com.winup.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BetSelectionTest {
    @Test
    void should_parse_local_selection() {
        assertThat(BetSelection.from(" local ")).isEqualTo(BetSelection.LOCAL);
    }

    @Test
    void should_parse_draw_selection() {
        assertThat(BetSelection.from("EMPATE")).isEqualTo(BetSelection.EMPATE);
    }

    @Test
    void should_reject_unknown_selection() {
        assertThatThrownBy(() -> BetSelection.from("GANADOR"))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.code()).isEqualTo("INVALID_SELECTION"));
    }
}
