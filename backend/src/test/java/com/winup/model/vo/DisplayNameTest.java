package com.winup.model.vo;

import com.winup.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DisplayNameTest {
    @Test
    void should_trim_display_name() {
        assertThat(new DisplayName(" Alex ").value()).isEqualTo("Alex");
    }

    @Test
    void should_reject_too_short_name() {
        assertThatThrownBy(() -> new DisplayName("A"))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.code()).isEqualTo("INVALID_NAME"));
    }

    @Test
    void should_reject_too_long_name() {
        assertThatThrownBy(() -> new DisplayName("A".repeat(81)))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.code()).isEqualTo("INVALID_NAME"));
    }
}
