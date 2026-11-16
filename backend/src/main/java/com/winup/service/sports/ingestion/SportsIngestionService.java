package com.winup.service.sports.ingestion;

import com.winup.config.SportsIngestionOptions;
import com.winup.entity.MatchEntity;
import com.winup.entity.ProviderMatchEntity;
import com.winup.model.MatchStatus;
import com.winup.repository.MatchRepository;
import com.winup.repository.ProviderMatchRepository;
import com.winup.service.EventPublisher;
import com.winup.service.MatchOddsSnapshotService;
import com.winup.service.SettlementService;
import com.winup.service.sports.FixtureStatus;
import com.winup.service.sports.ProviderFixture;
import com.winup.service.sports.ProviderOdds;
import com.winup.service.sports.SportQuery;
import com.winup.service.sports.SportsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class SportsIngestionService {
    private static final Logger log = LoggerFactory.getLogger(SportsIngestionService.class);

    private final ObjectProvider<SportsProvider> providerProvider;
    private final MatchRepository matchRepository;
    private final ProviderMatchRepository providerMatchRepository;
    private final SettlementService settlementService;
    private final MatchCancellationService cancellationService;
    private final ProviderOddsFetcher oddsFetcher;
    private final MatchOddsSnapshotService snapshotService;
    private final EventPublisher eventPublisher;
    private final ProviderMatchMapper mapper;
    private final SportsIngestionOptions options;
    private final Clock clock;

    public SportsIngestionService(
            ObjectProvider<SportsProvider> providerProvider,
            MatchRepository matchRepository,
            ProviderMatchRepository providerMatchRepository,
            SettlementService settlementService,
            MatchCancellationService cancellationService,
            ProviderOddsFetcher oddsFetcher,
            MatchOddsSnapshotService snapshotService,
            EventPublisher eventPublisher,
            ProviderMatchMapper mapper,
            SportsIngestionOptions options,
            Clock clock
    ) {
        this.providerProvider = providerProvider;
        this.matchRepository = matchRepository;
        this.providerMatchRepository = providerMatchRepository;
        this.settlementService = settlementService;
        this.cancellationService = cancellationService;
        this.oddsFetcher = oddsFetcher;
        this.snapshotService = snapshotService;
        this.eventPublisher = eventPublisher;
        this.mapper = mapper;
        this.options = options;
        this.clock = clock;
    }

    @Transactional
    public int ingest() {
        if (!options.enabled()) {
            return 0;
        }
        SportsProvider provider = activeProvider();
        if (provider == null) {
            return 0;
        }
        Instant now = clock.instant();
        SportQuery query = new SportQuery(
                options.sport(),
                now.minus(Duration.ofHours(options.lookbackHours())),
                now.plus(Duration.ofHours(options.lookaheadHours())),
                false
        );
        return ingest(provider, query, now, "Ingesta");
    }

    @Transactional
    public int ingestLive() {
        if (!options.enabled()) {
            return 0;
        }
        SportsProvider provider = activeProvider();
        if (provider == null) {
            return 0;
        }
        Instant now = clock.instant();
        SportQuery query = new SportQuery(
                options.sport(),
                now.minus(Duration.ofHours(1)),
                now.plus(Duration.ofHours(1)),
                true
        );
        return ingest(provider, query, now, "Ingesta en vivo");
    }

    private SportsProvider activeProvider() {
        SportsProvider provider = providerProvider.getIfAvailable();
        if (provider == null) {
            log.warn("Ingesta de deportes sin proveedor activo");
        }
        return provider;
    }

    private int ingest(SportsProvider provider, SportQuery query, Instant now, String label) {
        List<ProviderFixture> fixtures;
        try {
            fixtures = provider.fixtures(query);
        } catch (RuntimeException exception) {
            log.warn("El proveedor {} falló: {}", provider.id(), exception.getMessage());
            return 0;
        }
        if (fixtures == null) {
            return 0;
        }
        String providerId = provider.id();
        int ingested = 0;
        for (ProviderFixture fixture : fixtures) {
            if (ingest(provider, fixture, providerId, now)) {
                ingested++;
            }
        }
        log.info("{} {}: {} partidos", label, providerId, ingested);
        return ingested;
    }

    private boolean ingest(
            SportsProvider provider,
            ProviderFixture fixture,
            String providerId,
            Instant now
    ) {
        if (fixture.status() == FixtureStatus.CANCELLED) {
            return cancellationService.voidCancelled(providerId, fixture);
        }
        if (fixture.status() == FixtureStatus.POSTPONED) {
            return false;
        }
        String winner = fixture.finished() ? mapper.winner(fixture) : null;
        if (fixture.finished() && winner == null) {
            log.warn("Partido {} sin ganador válido; se omite", fixture.externalId());
            return false;
        }
        Optional<ProviderMatchEntity> mapping = providerMatchRepository
                .findByProviderAndExternalId(providerId, fixture.externalId());
        MatchEntity match = mapping
                .map(existing -> matchRepository.findById(existing.getMatchId()).orElse(null))
                .orElse(null);
        if (match != null) {
            MatchStatus status = MatchStatus.from(match.getStatus());
            if (status == MatchStatus.CLOSED || status == MatchStatus.VOID) {
                return false;
            }
        }
        ProviderOdds odds = fixture.odds();
        if (match == null && odds == null) {
            odds = oddsFetcher.fetch(provider, fixture.externalId());
        }
        if (match == null && odds == null) {
            log.info("Partido {} sin cuotas; se omite", fixture.externalId());
            return false;
        }
        boolean created = match == null;
        int previousHome = created ? 0 : match.getHomeScore();
        int previousAway = created ? 0 : match.getAwayScore();
        int previousMinute = created ? 0 : match.getMinute();
        match = mapper.map(fixture, providerId, now, match, odds);
        matchRepository.save(match);
        snapshotService.record(match);
        if (created) {
            eventPublisher.publishMatchCreated(match);
        } else if (winner == null && shouldNotify(fixture, match, previousHome, previousAway, previousMinute)) {
            eventPublisher.notifyDashboard(MatchStatus.OPEN, match, null);
        }

        ProviderMatchEntity providerMatch = created
                ? new ProviderMatchEntity()
                : mapping.orElseThrow();
        providerMatch.setMatchId(match.getId());
        providerMatch.setProvider(providerId);
        providerMatch.setExternalId(fixture.externalId());
        providerMatch.setLastSyncedAt(now);
        providerMatchRepository.save(providerMatch);

        if (winner != null) {
            eventPublisher.matchFinished(
                    match,
                    winner,
                    fixture.score().home(),
                    fixture.score().away()
            );
            settlementService.settle(match.getId(), winner, fixture.score().home(), fixture.score().away());
        }
        return true;
    }

    private boolean shouldNotify(
            ProviderFixture fixture,
            MatchEntity match,
            int previousHome,
            int previousAway,
            int previousMinute
    ) {
        if (fixture.status() == FixtureStatus.LIVE) {
            return true;
        }
        return previousHome != match.getHomeScore()
                || previousAway != match.getAwayScore()
                || previousMinute != match.getMinute();
    }
}
