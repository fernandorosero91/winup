package com.winup.model.vo;

import com.winup.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {
    @Test
    void should_normalize_email() {
        assertThat(new Email(" Alex@WinUp.App ").value()).isEqualTo("alex@winup.app");
    }

    @Test
    void should_reject_invalid_email() {
        assertThatThrownBy(() -> new Email("alex-at-winup"))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.code()).isEqualTo("INVALID_EMAIL"));
    }

    @Test
    void should_reject_null_email() {
        assertThatThrownBy(() -> new Email(null))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.code()).isEqualTo("INVALID_EMAIL"));
    }

    @Test
    void should_reject_email_longer_than_160_characters() {
        String longEmail = "a".repeat(151) + "@winup.app";

        assertThatThrownBy(() -> new Email(longEmail))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.code()).isEqualTo("INVALID_EMAIL"));
    }
}
