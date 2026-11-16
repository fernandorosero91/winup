package com.winup.model.vo;

import com.winup.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OddsTest {
    @Test
    void should_normalize_a_valid_odd() {
        Odds odds = new Odds(new BigDecimal("1.8"));

        assertThat(odds.value()).isEqualByComparingTo("1.80");
    }

    @Test
    void should_accept_the_limits() {
        assertThat(new Odds(new BigDecimal("1.01")).value()).isEqualByComparingTo("1.01");
        assertThat(new Odds(new BigDecimal("100.00")).value()).isEqualByComparingTo("100.00");
    }

    @Test
    void should_reject_a_missing_odd() {
        assertThatThrownBy(() -> new Odds(null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("obligatoria");
    }

    @Test
    void should_reject_an_odd_out_of_range() {
        assertThatThrownBy(() -> new Odds(new BigDecimal("1.00")))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new Odds(new BigDecimal("100.01")))
                .isInstanceOf(ValidationException.class);
    }
}
