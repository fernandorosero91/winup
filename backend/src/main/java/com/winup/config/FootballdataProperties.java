package com.winup.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "winup.sports.footballdata")
public record FootballdataProperties(
        String baseUrl,
        String apiKey,
        long leagueId,
        long seasonId,
        String language,
        int timeoutMs
) {
    public FootballdataProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("WINUP_FOOTBALLDATA_BASE_URL es obligatorio");
        }
        baseUrl = baseUrl.replaceAll("/+$", "");
        apiKey = apiKey == null ? "" : apiKey;
        language = language == null || language.isBlank() ? "es" : language;
        timeoutMs = timeoutMs <= 0 ? 8000 : timeoutMs;
    }
}
