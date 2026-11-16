package com.winup.model.vo;

import com.winup.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordTest {
    @Test
    void should_accept_password_with_letter_and_digit() {
        assertThatCode(() -> new Password("Winup2026")).doesNotThrowAnyException();
    }

    @Test
    void should_reject_short_password() {
        assertThatThrownBy(() -> new Password("Ab1"))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.code()).isEqualTo("WEAK_PASSWORD"));
    }

    @Test
    void should_reject_password_without_digit() {
        assertThatThrownBy(() -> new Password("Password"))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.code()).isEqualTo("WEAK_PASSWORD"));
    }

    @Test
    void should_reject_password_without_letter() {
        assertThatThrownBy(() -> new Password("12345678"))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.code()).isEqualTo("WEAK_PASSWORD"));
    }

    @Test
    void should_reject_password_longer_than_72_characters() {
        assertThatThrownBy(() -> new Password("A1" + "x".repeat(71)))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.code()).isEqualTo("WEAK_PASSWORD"));
    }
}
