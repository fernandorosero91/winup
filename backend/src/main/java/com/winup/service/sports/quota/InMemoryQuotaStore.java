package com.winup.service.sports.quota;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "winup.sports.quota.store", havingValue = "memory")
public class InMemoryQuotaStore implements QuotaStore {
    private final Map<String, State> states = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryQuotaStore(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Optional<QuotaSnapshot> acquire(String provider, LocalDate day, int defaultLimit, int stopPercent) {
        State state = state(provider);
        synchronized (state) {
            refresh(state, day, defaultLimit);
            if (!available(state, stopPercent)) {
                return Optional.empty();
            }
            state.used++;
            return Optional.of(snapshot(state));
        }
    }

    @Override
    public void recordResponse(String provider, LocalDate day, Integer limit, Integer remaining) {
        State state = state(provider);
        synchronized (state) {
            refresh(state, day, 0);
            if (limit != null && limit > 0) {
                state.limit = limit;
            }
            if (remaining != null && remaining >= 0) {
                state.remaining = remaining;
                state.used = Math.max(0, state.limit - remaining);
            }
        }
    }

    @Override
    public void openCircuit(String provider, LocalDate day, Instant until, int defaultLimit) {
        State state = state(provider);
        synchronized (state) {
            refresh(state, day, defaultLimit);
            state.openUntil = until;
        }
    }

    @Override
    public void closeCircuit(String provider, LocalDate day) {
        State state = state(provider);
        synchronized (state) {
            refresh(state, day, 0);
            state.openUntil = null;
        }
    }

    @Override
    public QuotaSnapshot load(String provider, LocalDate day, int defaultLimit) {
        State state = state(provider);
        synchronized (state) {
            refresh(state, day, defaultLimit);
            return snapshot(state);
        }
    }

    private boolean available(State state, int stopPercent) {
        if (state.openUntil != null && clock.instant().isBefore(state.openUntil)) {
            return false;
        }
        return state.used < Math.ceil(state.limit * stopPercent / 100.0);
    }

    private QuotaSnapshot snapshot(State state) {
        return new QuotaSnapshot(state.limit, state.used, state.remaining, state.openUntil);
    }

    private State state(String provider) {
        return states.computeIfAbsent(provider, key -> new State());
    }

    private void refresh(State state, LocalDate day, int defaultLimit) {
        if (!day.equals(state.day)) {
            state.day = day;
            state.limit = defaultLimit <= 0 ? state.limit : defaultLimit;
            state.used = 0;
            state.remaining = null;
            state.openUntil = null;
        }
    }

    private static final class State {
        private LocalDate day = LocalDate.MIN;
        private int limit;
        private int used;
        private Integer remaining;
        private Instant openUntil;
    }
}
