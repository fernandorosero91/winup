package com.winup.service.sports.quota;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

public interface QuotaStore {
    Optional<QuotaSnapshot> acquire(String provider, LocalDate day, int defaultLimit, int stopPercent);

    void recordResponse(String provider, LocalDate day, Integer limit, Integer remaining);

    void openCircuit(String provider, LocalDate day, Instant until, int defaultLimit);

    void closeCircuit(String provider, LocalDate day);

    QuotaSnapshot load(String provider, LocalDate day, int defaultLimit);
}
