package com.winup.service.sports.quota;

import com.winup.config.SportsQuotaProperties;
import com.winup.service.sports.ProviderHealth;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderQuotaGuardTest {
    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-16T12:00:00Z"));

    @Test
    void should_allow_calls_under_the_threshold() {
        ProviderQuotaGuard guard = guard();

        guard.recordResponse("API_FOOTBALL", 100, 99);

        assertThat(guard.canCall("API_FOOTBALL")).isTrue();
    }

    @Test
    void should_stop_at_ninety_five_percent() {
        ProviderQuotaGuard guard = guard();

        guard.recordResponse("API_FOOTBALL", 100, 5);

        assertThat(guard.canCall("API_FOOTBALL")).isFalse();
    }

    @Test
    void should_stop_after_the_local_limit_is_consumed() {
        ProviderQuotaGuard guard = guard();
        for (int call = 0; call < 95; call++) {
            assertThat(guard.tryAcquire("API_FOOTBALL")).isTrue();
        }

        assertThat(guard.tryAcquire("API_FOOTBALL")).isFalse();
    }

    @Test
    void should_open_the_circuit_on_a_rate_limit_and_recover() {
        ProviderQuotaGuard guard = guard();

        guard.recordRateLimited("API_FOOTBALL");

        assertThat(guard.canCall("API_FOOTBALL")).isFalse();
        assertThat(guard.health("API_FOOTBALL").available()).isFalse();

        guard.recordSuccess("API_FOOTBALL");

        assertThat(guard.canCall("API_FOOTBALL")).isTrue();
    }

    @Test
    void should_recover_after_the_cooldown() {
        ProviderQuotaGuard guard = guard();

        guard.recordRateLimited("API_FOOTBALL");
        clock.advance(Duration.ofMinutes(2));

        assertThat(guard.canCall("API_FOOTBALL")).isTrue();
    }

    @Test
    void should_reset_the_window_on_a_new_day() {
        ProviderQuotaGuard guard = guard();
        for (int call = 0; call < 95; call++) {
            assertThat(guard.tryAcquire("API_FOOTBALL")).isTrue();
        }
        assertThat(guard.tryAcquire("API_FOOTBALL")).isFalse();

        clock.advance(Duration.ofDays(1));

        assertThat(guard.canCall("API_FOOTBALL")).isTrue();
        ProviderHealth health = guard.health("API_FOOTBALL");
        assertThat(health.available()).isTrue();
        assertThat(health.remainingQuota()).isEqualTo(95);
    }

    private ProviderQuotaGuard guard() {
        return new ProviderQuotaGuard(clock, new SportsQuotaProperties(100), new InMemoryQuotaStore(clock));
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
