package com.winup.model.vo;

import com.winup.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StakeTest {
    @Test
    void should_accept_minimum_stake() {
        assertThat(new Stake(new BigDecimal("500")).amount()).isEqualByComparingTo("500.00");
    }

    @Test
    void should_accept_maximum_stake() {
        assertThat(new Stake(new BigDecimal("500000")).amount()).isEqualByComparingTo("500000.00");
    }

    @Test
    void should_reject_stake_below_minimum() {
        assertThatThrownBy(() -> new Stake(new BigDecimal("499.99")))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.code()).isEqualTo("INVALID_STAKE"));
    }

    @Test
    void should_reject_stake_above_maximum() {
        assertThatThrownBy(() -> new Stake(new BigDecimal("500000.01")))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.code()).isEqualTo("INVALID_STAKE"));
    }

    @Test
    void should_reject_null_stake() {
        assertThatThrownBy(() -> new Stake((BigDecimal) null))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.code()).isEqualTo("INVALID_STAKE"));
    }

    @Test
    void should_normalize_scale_to_two_decimals() {
        assertThat(new Stake(new BigDecimal("500.555")).amount().scale()).isEqualTo(2);
    }
}
