package com.winup.model.match;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DecimalPayoutStrategyTest {
    @Test
    void should_multiply_stake_by_odds_and_round_to_two_decimals() {
        Dinero payout = new DecimalPayoutStrategy()
                .calcular(new Dinero(new BigDecimal("100.00")), new BigDecimal("2.35"));

        assertThat(payout.amount()).isEqualByComparingTo("235.00");
    }
}
