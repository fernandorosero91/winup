package com.winup.model.vo;

import com.winup.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordAttemptTest {
    @Test
    void should_accept_any_non_blank_password() {
        assertThatCode(() -> new PasswordAttempt("wrong")).doesNotThrowAnyException();
    }

    @Test
    void should_reject_blank_password() {
        assertThatThrownBy(() -> new PasswordAttempt(" "))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.code()).isEqualTo("INVALID_PASSWORD"));
    }

    @Test
    void should_reject_password_longer_than_72_characters() {
        assertThatThrownBy(() -> new PasswordAttempt("x".repeat(73)))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.code()).isEqualTo("INVALID_PASSWORD"));
    }
}
