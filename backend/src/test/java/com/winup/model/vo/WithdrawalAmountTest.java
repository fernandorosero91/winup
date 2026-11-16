package com.winup.model.vo;

import com.winup.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WithdrawalAmountTest {
    @Test
    void should_accept_the_minimum_withdrawal() {
        WithdrawalAmount amount = new WithdrawalAmount(new BigDecimal("5000"));

        assertThat(amount.amount()).isEqualByComparingTo("5000.00");
    }

    @Test
    void should_reject_an_amount_below_the_minimum() {
        assertThatThrownBy(() -> new WithdrawalAmount(new BigDecimal("4999.99")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("5.000");
    }

    @Test
    void should_reject_an_amount_above_the_maximum() {
        assertThatThrownBy(() -> new WithdrawalAmount(new BigDecimal("500000.01")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("500.000");
    }

    @Test
    void should_reject_a_null_amount() {
        assertThatThrownBy(() -> new WithdrawalAmount(null))
                .isInstanceOf(ValidationException.class);
    }
}
