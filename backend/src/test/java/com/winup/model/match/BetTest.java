package com.winup.model.match;
import com.winup.model.vo.BetId;
import com.winup.model.vo.Dinero;
import com.winup.model.vo.UserId;


import com.winup.model.vo.BetSelection;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BetTest {
    @Test
    void should_reject_an_odds_lower_than_one() {
        assertThatThrownBy(() -> new Bet(
                new BetId(UUID.randomUUID()),
                new UserId("user-1"),
                BetSelection.LOCAL,
                new Dinero(new BigDecimal("100.00")),
                new BigDecimal("0.99")
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_reject_a_missing_selection() {
        assertThatThrownBy(() -> new Bet(
                new BetId(UUID.randomUUID()),
                new UserId("user-1"),
                null,
                new Dinero(new BigDecimal("100.00")),
                new BigDecimal("2.00")
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_keep_the_stake_as_dinero() {
        Bet bet = new Bet(
                new BetId(UUID.randomUUID()),
                new UserId("user-1"),
                BetSelection.LOCAL,
                new Dinero(new BigDecimal("100.00")),
                new BigDecimal("2.00")
        );

        assertThat(bet.stake().amount()).isEqualByComparingTo("100.00");
    }
}
