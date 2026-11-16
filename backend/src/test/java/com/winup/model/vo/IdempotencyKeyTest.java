package com.winup.model.vo;

import com.winup.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdempotencyKeyTest {
    @Test
    void should_trim_a_valid_key() {
        assertThat(new IdempotencyKey("  abc-123  ").value()).isEqualTo("abc-123");
    }

    @Test
    void should_reject_a_missing_key() {
        assertThatThrownBy(() -> new IdempotencyKey(null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("obligatoria");
        assertThatThrownBy(() -> new IdempotencyKey("   "))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void should_reject_a_key_that_is_too_long() {
        assertThatThrownBy(() -> new IdempotencyKey("A".repeat(121)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("larga");
    }
}
