package com.winup.model.vo;

import com.winup.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DepositAmountTest {
    @Test
    void should_accept_minimum_deposit() {
        assertThat(new DepositAmount(new BigDecimal("5000")).amount()).isEqualByComparingTo("5000.00");
    }

    @Test
    void should_accept_maximum_deposit() {
        assertThat(new DepositAmount(new BigDecimal("500000")).amount()).isEqualByComparingTo("500000.00");
    }

    @Test
    void should_reject_deposit_below_minimum() {
        assertThatThrownBy(() -> new DepositAmount(new BigDecimal("4999.99")))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.code()).isEqualTo("INVALID_DEPOSIT"));
    }

    @Test
    void should_reject_deposit_above_maximum() {
        assertThatThrownBy(() -> new DepositAmount(new BigDecimal("500000.01")))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.code()).isEqualTo("INVALID_DEPOSIT"));
    }
}
