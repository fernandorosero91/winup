package com.winup.service.sports.footballdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.winup.config.FootballdataProperties;
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
@ConditionalOnProperty(name = "winup.sports.provider", havingValue = "footballdata")
public class FootballdataAdapter implements SportsProvider {
    public static final String PROVIDER = "FOOTBALLDATA";
    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 5;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneOffset.UTC);

    private final FootballdataProperties properties;
    private final FootballdataClient client;
    private final FootballdataMapper mapper;
    private volatile long resolvedSeasonId;

    public FootballdataAdapter(
            FootballdataProperties properties,
            FootballdataClient client,
            FootballdataMapper mapper
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
        if (properties.leagueId() <= 0) {
            throw new IllegalStateException("WINUP_FOOTBALLDATA_LEAGUE_ID es obligatorio");
        }
        List<ProviderFixture> fixtures = new ArrayList<>();
        int page = 1;
        while (page <= MAX_PAGES) {
            JsonNode root = client.get(matchesPath(query, page)).orElse(null);
            if (root == null) {
                break;
            }
            JsonNode data = root.path("data");
            String league = leagueName(data);
            JsonNode matches = data.path("matches");
            if (!matches.isArray()) {
                break;
            }
            for (JsonNode match : matches) {
                mapper.toFixture(match, league).ifPresent(fixtures::add);
            }
            int totalPages = root.path("meta").path("pagination").path("total_pages").asInt(1);
            if (page >= totalPages) {
                break;
            }
            page++;
        }
        return List.copyOf(fixtures);
    }

    @Override
    public Optional<ProviderFixture> fixture(String externalId) {
        if (externalId == null || externalId.isBlank()) {
            return Optional.empty();
        }
        return client.get("/matches/" + externalId + languageQuery())
                .map(root -> root.path("data"))
                .flatMap(data -> mapper.toFixture(data, leagueName(data)));
    }

    @Override
    public Optional<ProviderOdds> odds(String externalId) {
        if (externalId == null || externalId.isBlank()) {
            return Optional.empty();
        }
        return client.get("/matches/" + externalId + "/odds" + languageQuery())
                .flatMap(mapper::oddsFromResponse);
    }

    @Override
    public ProviderHealth health() {
        return client.health();
    }

    private String matchesPath(SportQuery query, int page) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromPath("/leagues/" + properties.leagueId() + "/matches")
                .queryParam("limit", PAGE_SIZE)
                .queryParam("page", page)
                .queryParam("lang", properties.language());
        if (query.liveOnly()) {
            return builder.queryParam("status", "live").build().toUriString();
        }
        return builder
                .queryParam("season_id", seasonId())
                .queryParam("from", DAY.format(query.from()))
                .queryParam("to", DAY.format(query.to()))
                .build()
                .toUriString();
    }

    private long seasonId() {
        if (properties.seasonId() > 0) {
            return properties.seasonId();
        }
        if (resolvedSeasonId > 0) {
            return resolvedSeasonId;
        }
        JsonNode root = client
                .get("/leagues/" + properties.leagueId() + "/seasons" + languageQuery())
                .orElseThrow(() -> new IllegalStateException(
                        "No se pudo resolver la temporada de Footballdata.io"
                ));
        JsonNode seasons = root.path("data").path("seasons");
        long best = 0;
        for (JsonNode season : seasons) {
            long year = season.path("year").asLong(0);
            if (season.path("is_current").asBoolean(false) && year > best) {
                best = year;
            }
        }
        if (best <= 0) {
            for (JsonNode season : seasons) {
                best = Math.max(best, season.path("year").asLong(0));
            }
        }
        if (best <= 0) {
            throw new IllegalStateException("Footballdata.io no devolvió temporadas");
        }
        resolvedSeasonId = best;
        return best;
    }

    private String leagueName(JsonNode data) {
        JsonNode league = data.path("league");
        String name = league.path("competition_name").asText("");
        if (name.isBlank()) {
            name = league.path("name").asText("");
        }
        return name.isBlank() ? "Fútbol" : name;
    }

    private String languageQuery() {
        return "?lang=" + properties.language();
    }
}
