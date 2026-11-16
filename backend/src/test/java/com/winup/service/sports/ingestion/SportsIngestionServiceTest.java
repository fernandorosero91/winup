package com.winup.service.sports.ingestion;

import com.winup.config.SportsIngestionOptions;
import com.winup.entity.MatchEntity;
import com.winup.entity.ProviderMatchEntity;
import com.winup.repository.MatchRepository;
import com.winup.repository.ProviderMatchRepository;
import com.winup.model.MatchStatus;
import com.winup.service.EventPublisher;
import com.winup.service.MatchOddsSnapshotService;
import com.winup.service.SettlementService;
import com.winup.service.sports.FixtureStatus;
import com.winup.service.sports.ProviderFixture;
import com.winup.service.sports.ProviderOdds;
import com.winup.service.sports.ProviderScore;
import com.winup.service.sports.ProviderTeam;
import com.winup.service.sports.SportsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SportsIngestionServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-16T12:00:00Z");
    private static final UUID MATCH_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");

    @Mock
    private ObjectProvider<SportsProvider> providerProvider;

    @Mock
    private SportsProvider provider;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private ProviderMatchRepository providerMatchRepository;

    @Mock
    private SettlementService settlementService;

    @Mock
    private MatchCancellationService cancellationService;

    @Mock
    private ProviderOddsFetcher oddsFetcher;

    @Mock
    private MatchOddsSnapshotService snapshotService;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private ProviderMatchMapper mapper;

    private SportsIngestionService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new SportsIngestionService(
                providerProvider,
                matchRepository,
                providerMatchRepository,
                settlementService,
                cancellationService,
                oddsFetcher,
                snapshotService,
                eventPublisher,
                mapper,
                new SportsIngestionOptions(true, "Fútbol", 24, 48),
                clock
        );
    }

    @Test
    void should_do_nothing_when_the_ingestion_is_disabled() {
        SportsIngestionService disabled = new SportsIngestionService(
                providerProvider,
                matchRepository,
                providerMatchRepository,
                settlementService,
                cancellationService,
                oddsFetcher,
                snapshotService,
                eventPublisher,
                mapper,
                new SportsIngestionOptions(false, "Fútbol", 24, 48),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        assertThat(disabled.ingest()).isZero();
        verify(providerProvider, never()).getIfAvailable();
    }

    @Test
    void should_do_nothing_without_an_active_provider() {
        when(providerProvider.getIfAvailable()).thenReturn(null);

        assertThat(service.ingest()).isZero();
        verify(matchRepository, never()).save(any());
    }

    @Test
    void should_create_a_new_match_from_a_provider_fixture() {
        ProviderFixture fixture = fixture(FixtureStatus.SCHEDULED, 0, 0, odds());
        MatchEntity match = match("OPEN");
        when(providerProvider.getIfAvailable()).thenReturn(provider);
        when(provider.id()).thenReturn("REPLAY");
        when(provider.fixtures(any())).thenReturn(List.of(fixture));
        when(providerMatchRepository.findByProviderAndExternalId("REPLAY", "fixture-1"))
                .thenReturn(Optional.empty());
        when(mapper.map(fixture, "REPLAY", NOW, null, odds())).thenReturn(match);
        when(matchRepository.save(match)).thenReturn(match);

        assertThat(service.ingest()).isEqualTo(1);

        verify(providerMatchRepository).save(any(ProviderMatchEntity.class));
        verify(snapshotService).record(match);
        verify(eventPublisher).publishMatchCreated(match);
        verify(settlementService, never()).settle(any(), any(), anyInt(), anyInt());
    }

    @Test
    void should_update_an_existing_live_match() {
        ProviderFixture fixture = fixture(FixtureStatus.LIVE, 1, 0, odds());
        MatchEntity match = match("OPEN");
        ProviderMatchEntity mapping = mapping(match.getId());
        when(providerProvider.getIfAvailable()).thenReturn(provider);
        when(provider.id()).thenReturn("REPLAY");
        when(provider.fixtures(any())).thenReturn(List.of(fixture));
        when(providerMatchRepository.findByProviderAndExternalId("REPLAY", "fixture-1"))
                .thenReturn(Optional.of(mapping));
        when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));
        when(mapper.map(fixture, "REPLAY", NOW, match, odds())).thenReturn(match);
        when(matchRepository.save(match)).thenReturn(match);

        assertThat(service.ingest()).isEqualTo(1);

        verify(eventPublisher, never()).publishMatchCreated(any());
        verify(eventPublisher).notifyDashboard(MatchStatus.OPEN, match, null);
        verify(settlementService, never()).settle(any(), any(), anyInt(), anyInt());
    }

    @Test
    void should_run_a_live_ingestion_query() {
        ProviderFixture fixture = fixture(FixtureStatus.LIVE, 0, 0, odds());
        MatchEntity match = match("OPEN");
        when(providerProvider.getIfAvailable()).thenReturn(provider);
        when(provider.id()).thenReturn("REPLAY");
        when(provider.fixtures(any())).thenReturn(List.of(fixture));
        when(providerMatchRepository.findByProviderAndExternalId("REPLAY", "fixture-1"))
                .thenReturn(Optional.empty());
        when(mapper.map(fixture, "REPLAY", NOW, null, odds())).thenReturn(match);
        when(matchRepository.save(match)).thenReturn(match);

        assertThat(service.ingestLive()).isEqualTo(1);

        verify(eventPublisher).publishMatchCreated(match);
    }

    @Test
    void should_settle_a_finished_match_from_the_provider_result() {
        ProviderFixture fixture = fixture(FixtureStatus.FINISHED, 2, 1, odds());
        MatchEntity match = match("OPEN");
        ProviderMatchEntity mapping = mapping(match.getId());
        when(providerProvider.getIfAvailable()).thenReturn(provider);
        when(provider.id()).thenReturn("REPLAY");
        when(provider.fixtures(any())).thenReturn(List.of(fixture));
        when(providerMatchRepository.findByProviderAndExternalId("REPLAY", "fixture-1"))
                .thenReturn(Optional.of(mapping));
        when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));
        when(mapper.winner(fixture)).thenReturn("LOCAL");
        when(mapper.map(fixture, "REPLAY", NOW, match, odds())).thenReturn(match);
        when(matchRepository.save(match)).thenReturn(match);

        assertThat(service.ingest()).isEqualTo(1);

        verify(eventPublisher).matchFinished(match, "LOCAL", 2, 1);
        verify(settlementService).settle(MATCH_ID, "LOCAL", 2, 1);
    }

    @Test
    void should_skip_a_match_that_is_already_closed() {
        ProviderFixture fixture = fixture(FixtureStatus.LIVE, 1, 0, odds());
        MatchEntity match = match("CLOSED");
        when(providerProvider.getIfAvailable()).thenReturn(provider);
        when(provider.id()).thenReturn("REPLAY");
        when(provider.fixtures(any())).thenReturn(List.of(fixture));
        when(providerMatchRepository.findByProviderAndExternalId("REPLAY", "fixture-1"))
                .thenReturn(Optional.of(mapping(match.getId())));
        when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));

        assertThat(service.ingest()).isZero();

        verify(mapper, never()).map(any(), any(), any(), any(), any());
        verify(settlementService, never()).settle(any(), any(), anyInt(), anyInt());
    }

    @Test
    void should_skip_a_match_that_is_already_void() {
        ProviderFixture fixture = fixture(FixtureStatus.LIVE, 1, 0, odds());
        MatchEntity match = match("VOID");
        when(providerProvider.getIfAvailable()).thenReturn(provider);
        when(provider.id()).thenReturn("REPLAY");
        when(provider.fixtures(any())).thenReturn(List.of(fixture));
        when(providerMatchRepository.findByProviderAndExternalId("REPLAY", "fixture-1"))
                .thenReturn(Optional.of(mapping(match.getId())));
        when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));

        assertThat(service.ingest()).isZero();

        verify(mapper, never()).map(any(), any(), any(), any(), any());
        verify(matchRepository, never()).save(any());
    }

    @Test
    void should_skip_a_new_fixture_without_odds() {
        ProviderFixture fixture = fixture(FixtureStatus.SCHEDULED, 0, 0, null);
        when(providerProvider.getIfAvailable()).thenReturn(provider);
        when(provider.id()).thenReturn("API_FOOTBALL");
        when(provider.fixtures(any())).thenReturn(List.of(fixture));
        when(providerMatchRepository.findByProviderAndExternalId("API_FOOTBALL", "fixture-1"))
                .thenReturn(Optional.empty());
        when(oddsFetcher.fetch(provider, "fixture-1")).thenReturn(null);

        assertThat(service.ingest()).isZero();

        verify(mapper, never()).map(any(), any(), any(), any(), any());
        verify(matchRepository, never()).save(any());
    }

    @Test
    void should_fetch_odds_for_a_new_fixture_without_them() {
        ProviderFixture fixture = fixture(FixtureStatus.SCHEDULED, 0, 0, null);
        ProviderOdds odds = odds();
        MatchEntity match = match("OPEN");
        when(providerProvider.getIfAvailable()).thenReturn(provider);
        when(provider.id()).thenReturn("API_FOOTBALL");
        when(provider.fixtures(any())).thenReturn(List.of(fixture));
        when(providerMatchRepository.findByProviderAndExternalId("API_FOOTBALL", "fixture-1"))
                .thenReturn(Optional.empty());
        when(oddsFetcher.fetch(provider, "fixture-1")).thenReturn(odds);
        when(mapper.map(fixture, "API_FOOTBALL", NOW, null, odds)).thenReturn(match);
        when(matchRepository.save(match)).thenReturn(match);

        assertThat(service.ingest()).isEqualTo(1);

        verify(matchRepository).save(match);
    }

    @Test
    void should_return_zero_when_the_provider_fails() {
        when(providerProvider.getIfAvailable()).thenReturn(provider);
        when(provider.id()).thenReturn("REPLAY");
        when(provider.fixtures(any())).thenThrow(new IllegalStateException("caído"));

        assertThat(service.ingest()).isZero();

        verify(matchRepository, never()).save(any());
    }

    private ProviderFixture fixture(
            FixtureStatus status,
            int home,
            int away,
            ProviderOdds odds
    ) {
        return new ProviderFixture(
                "fixture-1",
                "Fútbol",
                "LaLiga",
                new ProviderTeam("Real Madrid", "RMA"),
                new ProviderTeam("Barcelona", "BAR"),
                Instant.parse("2026-11-09T20:00:00Z"),
                status,
                new ProviderScore(home, away, status == FixtureStatus.LIVE ? 65 : 0, "FT"),
                odds
        );
    }

    private ProviderOdds odds() {
        return new ProviderOdds(
                new BigDecimal("2.10"),
                new BigDecimal("3.40"),
                new BigDecimal("3.20")
        );
    }

    private MatchEntity match(String status) {
        MatchEntity match = new MatchEntity();
        match.setId(MATCH_ID);
        match.setStatus(status);
        return match;
    }

    private ProviderMatchEntity mapping(UUID matchId) {
        ProviderMatchEntity mapping = new ProviderMatchEntity();
        mapping.setMatchId(matchId);
        mapping.setProvider("REPLAY");
        mapping.setExternalId("fixture-1");
        return mapping;
    }
}
