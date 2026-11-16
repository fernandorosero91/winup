package com.winup.service.payment;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReturnUrlPolicyTest {
    private final ReturnUrlPolicy policy = new ReturnUrlPolicy(
            "http://localhost:4200, http://127.0.0.1:8080, https://winup.test",
            "http://localhost:4200/cartera",
            "https://winup.test",
            "http://localhost:4200/cartera"
    );

    @Test
    void should_accept_url_from_allowed_origin() {
        String resolved = policy.resolve("http://127.0.0.1:8080/api/v1/payments/return");

        assertThat(resolved).isEqualTo("http://127.0.0.1:8080/api/v1/payments/return");
    }

    @Test
    void should_reject_url_from_unknown_origin() {
        String resolved = policy.resolve("https://sitio-desconocido.test/cartera");

        assertThat(resolved).isEqualTo("http://localhost:4200/cartera");
    }

    @Test
    void should_reject_url_without_http_scheme() {
        String resolved = policy.resolve("javascript:alert(1)");

        assertThat(resolved).isEqualTo("http://localhost:4200/cartera");
    }

    @Test
    void should_fall_back_when_url_is_blank() {
        String resolved = policy.resolve("   ");

        assertThat(resolved).isEqualTo("http://localhost:4200/cartera");
    }

    @Test
    void should_compare_origin_case_insensitive() {
        String resolved = policy.resolve("HTTP://LOCALHOST:4200/cartera");

        assertThat(resolved).isEqualTo("HTTP://LOCALHOST:4200/cartera");
    }

    @Test
    void should_fall_back_when_url_is_malformed() {
        String resolved = policy.resolve("http://[::1");

        assertThat(resolved).isEqualTo("http://localhost:4200/cartera");
    }

    @Test
    void should_route_app_target_through_public_return_page() {
        String resolved = policy.publicReturnUrl("http://127.0.0.1:8080/api/v1/payments/return");

        assertThat(resolved).isEqualTo("https://winup.test/api/v1/payments/return/app");
    }

    @Test
    void should_route_web_target_through_public_return_page() {
        String resolved = policy.publicReturnUrl("http://localhost:4200/cartera");

        assertThat(resolved).isEqualTo("https://winup.test/api/v1/payments/return/web");
    }

    @Test
    void should_keep_public_https_url() {
        String resolved = policy.publicReturnUrl("https://winup.test/cartera");

        assertThat(resolved).isEqualTo("https://winup.test/cartera");
    }

    @Test
    void should_route_rejected_target_as_web() {
        String resolved = policy.publicReturnUrl("https://sitio-desconocido.test/cartera");

        assertThat(resolved).isEqualTo("https://winup.test/api/v1/payments/return/web");
    }
}
