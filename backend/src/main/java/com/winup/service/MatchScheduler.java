package com.winup.service;

import com.winup.entity.MatchEntity;
import com.winup.model.MatchStatus;
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
public class MatchScheduler {
    private static final Logger log = LoggerFactory.getLogger(MatchScheduler.class);

    private final MatchRepository matchRepository;
    private final EventPublisher eventPublisher;
    private final GeneratorLock generatorLock;
    private final Clock clock;
    private final boolean enabled;

    public MatchScheduler(
            MatchRepository matchRepository,
            EventPublisher eventPublisher,
            GeneratorLock generatorLock,
            Clock clock,
            @Value("${winup.scheduler.enabled:true}") boolean enabled
    ) {
        this.matchRepository = matchRepository;
        this.eventPublisher = eventPublisher;
        this.generatorLock = generatorLock;
        this.clock = clock;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${winup.scheduler.interval-ms:30000}")
    public void startDueMatches() {
        if (!enabled) {
            return;
        }
        generatorLock.run(this::startDue);
    }

    void startDue() {
        Instant now = clock.instant();
        List<MatchEntity> due = matchRepository
                .findByStatusAndMinuteAndStartsAtBefore(MatchStatus.OPEN.code(), 0, now);

        for (MatchEntity match : due) {
            if (!match.isSimulated()) {
                continue;
            }
            match.setMinute(1);
            match.setUpdatedAt(now);
            matchRepository.save(match);
            eventPublisher.scheduleSimulation(match);
            log.info("Partido {} inicia por calendario", match.getId());
        }
    }
}
