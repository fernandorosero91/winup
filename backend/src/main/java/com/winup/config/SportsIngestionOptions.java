package com.winup.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "winup.sports.ingestion")
public record SportsIngestionOptions(
        boolean enabled,
        String sport,
        int lookbackHours,
        int lookaheadHours
) {
    public SportsIngestionOptions {
        sport = sport == null || sport.isBlank() ? "Fútbol" : sport;
        lookbackHours = lookbackHours <= 0 ? 24 : lookbackHours;
        lookaheadHours = lookaheadHours <= 0 ? 48 : lookaheadHours;
    }
}
