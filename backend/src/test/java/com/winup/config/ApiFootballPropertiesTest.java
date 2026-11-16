package com.winup.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiFootballPropertiesTest {
    @Test
    void should_remove_trailing_slashes() {
        ApiFootballProperties properties = new ApiFootballProperties(
                "https://v3.football.api-sports.io///",
                "key",
                140,
                2024,
                5000
        );

        assertThat(properties.baseUrl()).isEqualTo("https://v3.football.api-sports.io");
    }

    @Test
    void should_use_a_default_timeout_and_empty_key() {
        ApiFootballProperties properties = new ApiFootballProperties(
                "https://v3.football.api-sports.io",
                null,
                140,
                2024,
                0
        );

        assertThat(properties.timeoutMs()).isEqualTo(8000);
        assertThat(properties.apiKey()).isEmpty();
    }

    @Test
    void should_reject_a_missing_base_url() {
        assertThatThrownBy(() -> new ApiFootballProperties("  ", "key", 140, 2024, 5000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BASE_URL");
    }
}
