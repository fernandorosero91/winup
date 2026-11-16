package com.winup.model.vo;

import com.winup.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromoDetailTest {
    @Test
    void should_trim_a_valid_message() {
        assertThat(new PromoDetail("  Deposita y recibe un bono  ").value())
                .isEqualTo("Deposita y recibe un bono");
    }

    @Test
    void should_reject_a_blank_message() {
        assertThatThrownBy(() -> new PromoDetail(""))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("obligatorio");
    }

    @Test
    void should_reject_a_message_that_is_too_long() {
        assertThatThrownBy(() -> new PromoDetail("A".repeat(281)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("largo");
    }
}
