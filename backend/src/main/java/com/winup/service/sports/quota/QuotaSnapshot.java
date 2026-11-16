package com.winup.service.sports.quota;

import java.time.Instant;

public record QuotaSnapshot(
        int limit,
        int used,
        Integer remaining,
        Instant openUntil
) {
}
