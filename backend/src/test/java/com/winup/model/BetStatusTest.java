package com.winup.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BetStatusTest {
    @Test
    void should_map_the_persisted_codes_to_api_labels() {
        assertThat(BetStatus.PENDING.label()).isEqualTo("PENDIENTE");
        assertThat(BetStatus.WON.label()).isEqualTo("GANADA");
        assertThat(BetStatus.LOST.label()).isEqualTo("PERDIDA");
        assertThat(BetStatus.VOID.label()).isEqualTo("ANULADA");
    }

    @Test
    void should_expose_a_refund_only_for_the_void_status() {
        assertThat(BetStatus.VOID.refunded()).isTrue();
        assertThat(BetStatus.PENDING.refunded()).isFalse();
        assertThat(BetStatus.WON.refunded()).isFalse();
        assertThat(BetStatus.LOST.refunded()).isFalse();
    }

    @Test
    void should_parse_the_persisted_codes() {
        assertThat(BetStatus.from("PENDING")).isEqualTo(BetStatus.PENDING);
        assertThat(BetStatus.from("WON")).isEqualTo(BetStatus.WON);
        assertThat(BetStatus.from("LOST")).isEqualTo(BetStatus.LOST);
        assertThat(BetStatus.from("VOID")).isEqualTo(BetStatus.VOID);
    }

    @Test
    void should_reject_an_unknown_code() {
        assertThatThrownBy(() -> BetStatus.from("CANCELLED"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CANCELLED");
    }
}
