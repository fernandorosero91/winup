package com.winup.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "winup.sports.api-football")
public record ApiFootballProperties(
        String baseUrl,
        String apiKey,
        long leagueId,
        int season,
        int timeoutMs
) {
    public ApiFootballProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("WINUP_API_FOOTBALL_BASE_URL es obligatorio");
        }
        baseUrl = baseUrl.replaceAll("/+$", "");
        apiKey = apiKey == null ? "" : apiKey;
        timeoutMs = timeoutMs <= 0 ? 8000 : timeoutMs;
    }
}
