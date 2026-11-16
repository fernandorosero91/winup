package com.winup.service.sports.ingestion;

import com.winup.config.SportsIngestionOptions;
import com.winup.repository.MatchRepository;
import com.winup.repository.ProviderMatchRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SportsIngestionCancellationTest {
    private static final Instant NOW = Instant.parse("2026-11-16T12:00:00Z");

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
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        when(providerProvider.getIfAvailable()).thenReturn(provider);
        when(provider.id()).thenReturn("REPLAY");
    }

    @Test
    void should_delegate_a_cancelled_fixture_to_the_cancellation_service() {
        ProviderFixture fixture = fixture();
        when(provider.fixtures(any())).thenReturn(List.of(fixture));
        when(cancellationService.voidCancelled("REPLAY", fixture)).thenReturn(true);

        assertThat(service.ingest()).isEqualTo(1);

        verify(cancellationService).voidCancelled("REPLAY", fixture);
        verify(matchRepository, never()).save(any());
        verify(settlementService, never()).settle(any(), any(), anyInt(), anyInt());
    }

    @Test
    void should_not_count_a_cancelled_fixture_without_a_mapping() {
        ProviderFixture fixture = fixture();
        when(provider.fixtures(any())).thenReturn(List.of(fixture));
        when(cancellationService.voidCancelled("REPLAY", fixture)).thenReturn(false);

        assertThat(service.ingest()).isZero();

        verify(matchRepository, never()).save(any());
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
                new ProviderOdds(
                        new BigDecimal("2.10"),
                        new BigDecimal("3.40"),
                        new BigDecimal("3.20")
                )
        );
    }
}
