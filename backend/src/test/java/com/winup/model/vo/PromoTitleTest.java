package com.winup.model.vo;

import com.winup.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromoTitleTest {
    @Test
    void should_trim_a_valid_title() {
        assertThat(new PromoTitle("  Bono  ").value()).isEqualTo("Bono");
    }

    @Test
    void should_reject_a_blank_title() {
        assertThatThrownBy(() -> new PromoTitle("   "))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("obligatorio");
    }

    @Test
    void should_reject_a_title_that_is_too_long() {
        assertThatThrownBy(() -> new PromoTitle("A".repeat(141)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("largo");
    }
}
