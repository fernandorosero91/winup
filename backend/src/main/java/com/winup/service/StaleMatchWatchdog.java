package com.winup.service;

import com.winup.entity.MatchEntity;
import com.winup.repository.MatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class StaleMatchWatchdog {
    private static final Logger log = LoggerFactory.getLogger(StaleMatchWatchdog.class);

    private final MatchRepository matchRepository;
    private final MatchResultSimulator resultSimulator;
    private final EventPublisher eventPublisher;
    private final GeneratorLock generatorLock;
    private final Clock clock;
    private final boolean enabled;
    private final long graceMs;

    public StaleMatchWatchdog(
            MatchRepository matchRepository,
            MatchResultSimulator resultSimulator,
            EventPublisher eventPublisher,
            GeneratorLock generatorLock,
            Clock clock,
            @Value("${winup.watchdog.enabled:true}") boolean enabled,
            @Value("${winup.watchdog.grace-ms:180000}") long graceMs
    ) {
        this.matchRepository = matchRepository;
        this.resultSimulator = resultSimulator;
        this.eventPublisher = eventPublisher;
        this.generatorLock = generatorLock;
        this.clock = clock;
        this.enabled = enabled;
        this.graceMs = graceMs;
    }

    @Scheduled(fixedDelayString = "${winup.watchdog.interval-ms:60000}")
    public void sweep() {
        if (!enabled) {
            return;
        }
        generatorLock.run(this::closeStaleMatches);
    }

    void closeStaleMatches() {
        Instant threshold = clock.instant().minusMillis(graceMs);
        List<MatchEntity> stale = matchRepository
                .findByStatusAndMinuteGreaterThanAndCreatedAtBefore("OPEN", 0, threshold);

        for (MatchEntity match : stale) {
            if (!match.isSimulated()) {
                continue;
            }
            MatchResultSimulator.Result result = resultSimulator.simulate(match.getSport());
            log.info(
                    "Watchdog cierra el partido atascado {}: {} {} - {}",
                    match.getId(),
                    result.winner(),
                    result.homeScore(),
                    result.awayScore()
            );
            eventPublisher.matchFinished(
                    match,
                    result.winner(),
                    result.homeScore(),
                    result.awayScore()
            );
        }
    }
}
