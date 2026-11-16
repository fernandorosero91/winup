package com.winup.service.sports.footballdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.winup.config.FootballdataProperties;
import com.winup.service.sports.ProviderHealth;
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
@ConditionalOnProperty(name = "winup.sports.provider", havingValue = "footballdata")
public class FootballdataClient {
    private static final Logger log = LoggerFactory.getLogger(FootballdataClient.class);
    private static final double STOP_RATIO = 0.05;

    private final RestTemplate restTemplate;
    private final FootballdataProperties properties;
    private volatile int remaining = -1;
    private volatile int limit = -1;

    @Autowired
    public FootballdataClient(
            RestTemplateBuilder builder,
            FootballdataProperties properties
    ) {
        this(
                builder
                        .rootUri(properties.baseUrl())
                        .setConnectTimeout(Duration.ofMillis(properties.timeoutMs()))
                        .setReadTimeout(Duration.ofMillis(properties.timeoutMs()))
                        .build(),
                properties
        );
    }

    FootballdataClient(RestTemplate restTemplate, FootballdataProperties properties) {
        if (properties.apiKey().isBlank()) {
            throw new IllegalStateException("WINUP_FOOTBALLDATA_API_KEY es obligatorio");
        }
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public Optional<JsonNode> get(String path) {
        if (!canCall()) {
            return Optional.empty();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(properties.apiKey());
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    path,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    JsonNode.class
            );
            JsonNode body = response.getBody();
            applyUsage(body);
            if (body != null && !body.path("success").asBoolean(true)) {
                log.warn("Footballdata.io respondió con error: {}", body.path("error").toString());
                return Optional.empty();
            }
            return Optional.ofNullable(body);
        } catch (HttpStatusCodeException exception) {
            if (exception.getStatusCode().value() == 429) {
                remaining = 0;
            }
            log.warn(
                    "Footballdata.io respondió {} para {}",
                    exception.getStatusCode().value(),
                    path
            );
            return Optional.empty();
        } catch (RestClientException exception) {
            log.warn("Footballdata.io no disponible: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    public ProviderHealth health() {
        boolean available = canCall();
        String message;
        if (remaining < 0) {
            message = "Cuota sin consultar";
        } else if (available) {
            message = "Cuota disponible";
        } else {
            message = "Cuota al 5 % o agotada";
        }
        return new ProviderHealth("FOOTBALLDATA", available, Math.max(remaining, 0), message);
    }

    private boolean canCall() {
        if (remaining < 0 || limit <= 0) {
            return true;
        }
        return remaining > Math.max(5, limit * STOP_RATIO);
    }

    private void applyUsage(JsonNode body) {
        if (body == null) {
            return;
        }
        JsonNode meta = body.path("meta");
        int currentLimit = meta.path("requests_limit").asInt(-1);
        int currentRemaining = meta.path("requests_remaining").asInt(-1);
        if (currentLimit > 0) {
            limit = currentLimit;
        }
        if (currentRemaining >= 0) {
            remaining = currentRemaining;
        }
    }
}
