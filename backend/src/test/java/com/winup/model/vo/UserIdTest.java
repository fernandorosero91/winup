package com.winup.model.vo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserIdTest {
    @Test
    void should_reject_a_blank_id() {
        assertThatThrownBy(() -> new UserId("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_trim_the_id() {
        assertThat(new UserId(" user-1 ").value()).isEqualTo("user-1");
    }
}
