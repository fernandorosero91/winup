package com.winup.service.sports.quota;

import com.winup.config.SportsQuotaProperties;
import com.winup.service.sports.ProviderHealth;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

@Component
public class ProviderQuotaGuard {
    private static final double STOP_RATIO = 0.95;
    private static final int STOP_PERCENT = 95;
    private static final Duration COOLDOWN = Duration.ofMinutes(1);

    private final Clock clock;
    private final int defaultLimit;
    private final QuotaStore store;

    public ProviderQuotaGuard(
            Clock clock,
            SportsQuotaProperties properties,
            QuotaStore store
    ) {
        this.clock = clock;
        this.defaultLimit = properties.dailyLimit();
        this.store = store;
    }

    public boolean tryAcquire(String provider) {
        return store.acquire(provider, today(), defaultLimit, STOP_PERCENT).isPresent();
    }

    public boolean canCall(String provider) {
        return available(load(provider));
    }

    public void recordResponse(String provider, Integer limit, Integer remaining) {
        store.recordResponse(provider, today(), limit, remaining);
    }

    public void recordRateLimited(String provider) {
        store.openCircuit(provider, today(), clock.instant().plus(COOLDOWN), defaultLimit);
    }

    public void recordFailure(String provider) {
        recordRateLimited(provider);
    }

    public void recordSuccess(String provider) {
        store.closeCircuit(provider, today());
    }

    public ProviderHealth health(String provider) {
        QuotaSnapshot snapshot = load(provider);
        boolean available = available(snapshot);
        return new ProviderHealth(provider, available, remaining(snapshot), message(snapshot, available));
    }

    private QuotaSnapshot load(String provider) {
        return store.load(provider, today(), defaultLimit);
    }

    private boolean available(QuotaSnapshot snapshot) {
        Instant now = clock.instant();
        if (snapshot.openUntil() != null && now.isBefore(snapshot.openUntil())) {
            return false;
        }
        if (snapshot.remaining() != null) {
            return snapshot.remaining() > Math.floor(snapshot.limit() * (1 - STOP_RATIO));
        }
        return snapshot.used() < snapshot.limit() * STOP_RATIO;
    }

    private int remaining(QuotaSnapshot snapshot) {
        if (snapshot.remaining() != null) {
            return snapshot.remaining();
        }
        return Math.max(0, (int) Math.ceil(snapshot.limit() * STOP_RATIO) - snapshot.used());
    }

    private String message(QuotaSnapshot snapshot, boolean available) {
        Instant now = clock.instant();
        if (snapshot.openUntil() != null && now.isBefore(snapshot.openUntil())) {
            return "Circuito abierto por límite del proveedor";
        }
        return available ? "Cuota disponible" : "Cuota al 95 %";
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
