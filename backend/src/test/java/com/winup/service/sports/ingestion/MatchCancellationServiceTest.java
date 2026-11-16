package com.winup.service.sports.ingestion;

import com.winup.entity.ProviderMatchEntity;
import com.winup.repository.ProviderMatchRepository;
import com.winup.service.SettlementService;
import com.winup.service.sports.FixtureStatus;
import com.winup.service.sports.ProviderFixture;
import com.winup.service.sports.ProviderScore;
import com.winup.service.sports.ProviderTeam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchCancellationServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-16T12:00:00Z");
    private static final UUID MATCH_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");

    @Mock
    private ProviderMatchRepository providerMatchRepository;

    @Mock
    private SettlementService settlementService;

    private MatchCancellationService service;

    @BeforeEach
    void setUp() {
        service = new MatchCancellationService(
                providerMatchRepository,
                settlementService,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void should_void_a_cancelled_match_that_was_already_ingested() {
        ProviderMatchEntity mapping = mapping();
        when(providerMatchRepository.findByProviderAndExternalId("REPLAY", "fixture-1"))
                .thenReturn(Optional.of(mapping));
        when(settlementService.voidMatch(MATCH_ID)).thenReturn(true);

        assertThat(service.voidCancelled("REPLAY", fixture())).isTrue();

        assertThat(mapping.getLastSyncedAt()).isEqualTo(NOW);
        verify(providerMatchRepository).save(mapping);
        verify(settlementService).voidMatch(MATCH_ID);
    }

    @Test
    void should_ignore_a_cancelled_fixture_without_a_mapping() {
        when(providerMatchRepository.findByProviderAndExternalId("REPLAY", "fixture-1"))
                .thenReturn(Optional.empty());

        assertThat(service.voidCancelled("REPLAY", fixture())).isFalse();

        verify(providerMatchRepository, never()).save(any());
        verify(settlementService, never()).voidMatch(any());
    }

    @Test
    void should_report_false_when_the_match_could_not_be_voided() {
        ProviderMatchEntity mapping = mapping();
        when(providerMatchRepository.findByProviderAndExternalId("REPLAY", "fixture-1"))
                .thenReturn(Optional.of(mapping));
        when(settlementService.voidMatch(MATCH_ID)).thenReturn(false);

        assertThat(service.voidCancelled("REPLAY", fixture())).isFalse();
    }

    private ProviderFixture fixture() {
        return new ProviderFixture(
                "fixture-1",
                "Fútbol",
                "LaLiga",
                new ProviderTeam("Real Madrid", "RMA"),
                new ProviderTeam("Barcelona", "BAR"),
                Instant.parse("2026-11-09T20:00:00Z"),
                FixtureStatus.CANCELLED,
                new ProviderScore(0, 0, 0, "CANC"),
                null
        );
    }

    private ProviderMatchEntity mapping() {
        ProviderMatchEntity mapping = new ProviderMatchEntity();
        mapping.setMatchId(MATCH_ID);
        mapping.setProvider("REPLAY");
        mapping.setExternalId("fixture-1");
        return mapping;
    }
}
