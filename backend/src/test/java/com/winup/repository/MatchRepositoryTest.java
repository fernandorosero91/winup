package com.winup.repository;

import com.winup.entity.MatchEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class MatchRepositoryTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private MatchRepository matchRepository;

    @Test
    void should_persist_a_match_and_read_it_back() {
        MatchEntity match = new MatchEntity();
        match.setId(UUID.randomUUID());
        match.setSport("Fútbol");
        match.setLeague("Test League");
        match.setHomeTeam("Equipo A");
        match.setAwayTeam("Equipo B");
        match.setHomeShort("A");
        match.setAwayShort("B");
        match.setHomeScore(0);
        match.setAwayScore(0);
        match.setMinute(1);
        match.setStatus("OPEN");
        match.setStartsAt(Instant.parse("2026-11-12T18:00:00Z"));
        match.setLocalOdds(new BigDecimal("2.00"));
        match.setDrawOdds(new BigDecimal("3.20"));
        match.setAwayOdds(new BigDecimal("3.80"));
        match.setSource("RANDOM");
        match.setSimulated(true);
        match.setCreatedAt(Instant.parse("2026-11-12T18:00:00Z"));
        match.setUpdatedAt(Instant.parse("2026-11-12T18:00:00Z"));

        matchRepository.save(match);

        assertThat(matchRepository.findById(match.getId())).isPresent();
    }
}
