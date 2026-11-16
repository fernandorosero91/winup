package com.winup.model.vo;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DineroTest {
    @Test
    void should_reject_negative_amounts() {
        assertThatThrownBy(() -> new Dinero(new BigDecimal("-0.01")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_normalize_scale_to_two_decimals() {
        assertThat(new Dinero(new BigDecimal("100.555")).amount().scale()).isEqualTo(2);
    }

    @Test
    void should_multiply_by_odds() {
        assertThat(new Dinero(new BigDecimal("100.00"))
                .por(new BigDecimal("2.35"))
                .amount()).isEqualByComparingTo("235.00");
    }
}
