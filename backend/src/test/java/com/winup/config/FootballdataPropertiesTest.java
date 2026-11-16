package com.winup.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FootballdataPropertiesTest {
    @Test
    void should_remove_trailing_slashes_and_default_language() {
        FootballdataProperties properties = new FootballdataProperties(
                "https://footballdata.io/api/v1///",
                null,
                10,
                117065,
                "",
                0
        );

        assertThat(properties.baseUrl()).isEqualTo("https://footballdata.io/api/v1");
        assertThat(properties.apiKey()).isEmpty();
        assertThat(properties.language()).isEqualTo("es");
        assertThat(properties.timeoutMs()).isEqualTo(8000);
    }

    @Test
    void should_reject_a_missing_base_url() {
        assertThatThrownBy(() -> new FootballdataProperties(" ", "key", 10, 0, "es", 5000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BASE_URL");
    }
}
