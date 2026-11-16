package com.winup.service.sports.apifootball;

import com.fasterxml.jackson.databind.JsonNode;
import com.winup.config.ApiFootballProperties;
import com.winup.service.sports.ProviderFixture;
import com.winup.service.sports.ProviderHealth;
import com.winup.service.sports.ProviderOdds;
import com.winup.service.sports.SportQuery;
import com.winup.service.sports.SportsProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "winup.sports.provider", havingValue = "api-football")
public class ApiFootballAdapter implements SportsProvider {
    public static final String PROVIDER = "API_FOOTBALL";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneOffset.UTC);

    private final ApiFootballProperties properties;
    private final ApiFootballClient client;
    private final ApiFootballMapper mapper;

    public ApiFootballAdapter(
            ApiFootballProperties properties,
            ApiFootballClient client,
            ApiFootballMapper mapper
    ) {
        this.properties = properties;
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    public String id() {
        return PROVIDER;
    }

    @Override
    public List<ProviderFixture> fixtures(SportQuery query) {
        if (!query.liveOnly() && (properties.leagueId() <= 0 || properties.season() <= 0)) {
            throw new IllegalStateException(
                    "Configura WINUP_API_FOOTBALL_LEAGUE_ID y WINUP_API_FOOTBALL_SEASON"
            );
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/fixtures");
        if (query.liveOnly()) {
            builder.queryParam("live", "all");
        } else {
            builder.queryParam("league", properties.leagueId())
                    .queryParam("season", properties.season())
                    .queryParam("from", DAY.format(query.from()))
                    .queryParam("to", DAY.format(query.to()));
        }
        return client.get(builder.build().toUriString())
                .map(this::toFixtures)
                .orElseGet(List::of);
    }

    @Override
    public Optional<ProviderFixture> fixture(String externalId) {
        if (externalId == null || externalId.isBlank()) {
            return Optional.empty();
        }
        String path = UriComponentsBuilder.fromPath("/fixtures")
                .queryParam("id", externalId)
                .build()
                .toUriString();
        return client.get(path).flatMap(root -> {
            JsonNode response = root.path("response");
            if (!response.isArray() || response.isEmpty()) {
                return Optional.empty();
            }
            return mapper.toFixture(response.get(0));
        });
    }

    @Override
    public Optional<ProviderOdds> odds(String externalId) {
        if (externalId == null || externalId.isBlank()) {
            return Optional.empty();
        }
        String path = UriComponentsBuilder.fromPath("/odds")
                .queryParam("fixture", externalId)
                .build()
                .toUriString();
        return client.get(path).flatMap(mapper::toOdds);
    }

    @Override
    public ProviderHealth health() {
        return client.health();
    }

    private List<ProviderFixture> toFixtures(JsonNode root) {
        JsonNode response = root.path("response");
        if (!response.isArray()) {
            return List.of();
        }
        List<ProviderFixture> fixtures = new ArrayList<>();
        for (JsonNode node : response) {
            mapper.toFixture(node).ifPresent(fixtures::add);
        }
        return List.copyOf(fixtures);
    }
}
