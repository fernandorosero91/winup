package com.winup.service.sports.replay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winup.service.sports.FixtureStatus;
import com.winup.service.sports.ProviderFixture;
import com.winup.service.sports.ProviderHealth;
import com.winup.service.sports.ProviderOdds;
import com.winup.service.sports.SportQuery;
import com.winup.service.sports.SportsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "winup.sports.provider", havingValue = "replay")
public class ReplaySportsAdapter implements SportsProvider {
    public static final String PROVIDER = "REPLAY";
    private static final Logger log = LoggerFactory.getLogger(ReplaySportsAdapter.class);

    private final List<ProviderFixture> fixtures;

    public ReplaySportsAdapter(
            ObjectMapper objectMapper,
            @Value("${winup.sports.replay-path:classpath:sports/replay-fixtures.json}") Resource resource
    ) {
        this.fixtures = load(objectMapper, resource);
        log.info("Replay deportivo cargado: {} partidos", fixtures.size());
    }

    @Override
    public String id() {
        return PROVIDER;
    }

    @Override
    public List<ProviderFixture> fixtures(SportQuery query) {
        return fixtures.stream()
                .filter(fixture -> query.matchesSport(fixture.sport()))
                .filter(fixture -> !fixture.startsAt().isBefore(query.from()))
                .filter(fixture -> !fixture.startsAt().isAfter(query.to()))
                .filter(fixture -> !query.liveOnly() || fixture.status() == FixtureStatus.LIVE)
                .sorted(Comparator.comparing(ProviderFixture::startsAt))
                .toList();
    }

    @Override
    public Optional<ProviderFixture> fixture(String externalId) {
        if (externalId == null || externalId.isBlank()) {
            return Optional.empty();
        }
        return fixtures.stream()
                .filter(fixture -> fixture.externalId().equals(externalId))
                .findFirst();
    }

    @Override
    public Optional<ProviderOdds> odds(String externalId) {
        return fixture(externalId).map(ProviderFixture::odds);
    }

    @Override
    public ProviderHealth health() {
        return new ProviderHealth(PROVIDER, true, -1, "Captura local determinista");
    }

    private List<ProviderFixture> load(ObjectMapper objectMapper, Resource resource) {
        try (InputStream input = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(input);
            JsonNode fixturesNode = root.path("fixtures");
            if (!fixturesNode.isArray() || fixturesNode.isEmpty()) {
                throw new IllegalStateException("El replay no contiene partidos");
            }
            List<ProviderFixture> parsed = objectMapper.convertValue(
                    fixturesNode,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ProviderFixture.class)
            );
            return List.copyOf(parsed);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo cargar la captura de replay", exception);
        }
    }
}
