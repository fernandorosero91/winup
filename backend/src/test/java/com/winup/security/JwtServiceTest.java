package com.winup.security;

import com.winup.entity.UserEntity;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {
    private static final String SECRET = "0123456789012345678901234567890123456789";

    @Test
    void should_issue_a_token_with_user_claims() {
        JwtService service = new JwtService(
                SECRET,
                120,
                Clock.fixed(Instant.parse("2026-11-11T18:00:00Z"), ZoneOffset.UTC)
        );
        UserEntity user = new UserEntity();
        user.setId("user-1");
        user.setEmail("alex@winup.app");
        user.setDisplayName("Alex");

        Claims claims = service.parse(service.issue(user));

        assertThat(claims.getSubject()).isEqualTo("user-1");
        assertThat(claims.get("email", String.class)).isEqualTo("alex@winup.app");
        assertThat(claims.get("name", String.class)).isEqualTo("Alex");
    }

    @Test
    void should_reject_a_secret_shorter_than_32_bytes() {
        assertThatThrownBy(() -> new JwtService("short", 120, Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class);
    }
}
