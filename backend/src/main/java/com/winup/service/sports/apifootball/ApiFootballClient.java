package com.winup.service.sports.apifootball;

import com.fasterxml.jackson.databind.JsonNode;
import com.winup.config.ApiFootballProperties;
import com.winup.service.sports.ProviderHealth;
import com.winup.service.sports.quota.ProviderQuotaGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "winup.sports.provider", havingValue = "api-football")
public class ApiFootballClient {
    private static final String API_KEY_HEADER = "x-apisports-key";
    private static final Logger log = LoggerFactory.getLogger(ApiFootballClient.class);

    private final RestTemplate restTemplate;
    private final ApiFootballProperties properties;
    private final ProviderQuotaGuard quotaGuard;

    @Autowired
    public ApiFootballClient(
            RestTemplateBuilder builder,
            ApiFootballProperties properties,
            ProviderQuotaGuard quotaGuard
    ) {
        this(
                builder
                        .rootUri(properties.baseUrl())
                        .setConnectTimeout(Duration.ofMillis(properties.timeoutMs()))
                        .setReadTimeout(Duration.ofMillis(properties.timeoutMs()))
                        .build(),
                properties,
                quotaGuard
        );
    }

    ApiFootballClient(
            RestTemplate restTemplate,
            ApiFootballProperties properties,
            ProviderQuotaGuard quotaGuard
    ) {
        if (properties.apiKey().isBlank()) {
            throw new IllegalStateException("WINUP_API_FOOTBALL_KEY es obligatorio");
        }
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.quotaGuard = quotaGuard;
    }

    public Optional<JsonNode> get(String path) {
        if (!quotaGuard.tryAcquire(ApiFootballAdapter.PROVIDER)) {
            return Optional.empty();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set(API_KEY_HEADER, properties.apiKey());
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    path,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    JsonNode.class
            );
            applyQuota(response.getHeaders());
            quotaGuard.recordSuccess(ApiFootballAdapter.PROVIDER);
            return Optional.ofNullable(response.getBody());
        } catch (HttpStatusCodeException exception) {
            if (exception.getStatusCode().value() == 429) {
                quotaGuard.recordRateLimited(ApiFootballAdapter.PROVIDER);
            } else {
                quotaGuard.recordFailure(ApiFootballAdapter.PROVIDER);
            }
            log.warn("API-Football respondió {} para {}", exception.getStatusCode().value(), path);
            return Optional.empty();
        } catch (RestClientException exception) {
            quotaGuard.recordFailure(ApiFootballAdapter.PROVIDER);
            log.warn("API-Football no disponible: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    public ProviderHealth health() {
        return quotaGuard.health(ApiFootballAdapter.PROVIDER);
    }

    private void applyQuota(HttpHeaders headers) {
        quotaGuard.recordResponse(
                ApiFootballAdapter.PROVIDER,
                intHeader(headers, "x-ratelimit-requests-limit", "x-ratelimit-limit"),
                intHeader(headers, "x-ratelimit-requests-remaining", "x-ratelimit-remaining")
        );
    }

    private Integer intHeader(HttpHeaders headers, String... names) {
        for (String name : names) {
            String value = headers.getFirst(name);
            if (value == null) {
                continue;
            }
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException exception) {
                return null;
            }
        }
        return null;
    }
}
