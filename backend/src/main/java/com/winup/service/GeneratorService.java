package com.winup.service;

import com.winup.entity.MatchEntity;
import com.winup.model.SportCatalog;
import com.winup.repository.MatchRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class GeneratorService {
    private final MatchService matchService;
    private final MatchRepository matchRepository;
    private final EventPublisher eventPublisher;
    private final GeneratorLock generatorLock;
    private final boolean enabled;
    private final int livePerSport;
    private final int maxPerTick;

    public GeneratorService(
            MatchService matchService,
            MatchRepository matchRepository,
            EventPublisher eventPublisher,
            GeneratorLock generatorLock,
            @Value("${winup.generator.enabled:true}") boolean enabled,
            @Value("${winup.generator.live-per-sport:5}") int livePerSport,
            @Value("${winup.generator.max-per-tick:42}") int maxPerTick
    ) {
        this.matchService = matchService;
        this.matchRepository = matchRepository;
        this.eventPublisher = eventPublisher;
        this.generatorLock = generatorLock;
        this.enabled = enabled;
        this.livePerSport = livePerSport;
        this.maxPerTick = maxPerTick;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        if (!enabled) {
            return;
        }
        ensureAllSports();
    }

    @Scheduled(fixedDelayString = "${winup.generator.sports-interval-ms:30000}")
    public void ensureAllSports() {
        if (!enabled) {
            return;
        }
        generatorLock.run(() -> replenish(targetLive()));
    }

    @Scheduled(fixedRateString = "${winup.generator.interval-ms:5000}")
    public void tick() {
        if (!enabled) {
            return;
        }
        generatorLock.run(() -> replenish(maxPerTick));
    }

    public int targetLive() {
        return livePerSport * SportCatalog.SPORTS.size();
    }

    private void replenish(int limit) {
        int created = 0;
        for (String sport : SportCatalog.SPORTS) {
            if (created >= limit) {
                return;
            }
            long live = countLive(sport);
            while (live < livePerSport && created < limit) {
                publishNewMatch(sport);
                live++;
                created++;
            }
        }
    }

    private long countLive(String sport) {
        return matchRepository.countBySportAndStatusAndMinuteGreaterThan(sport, "OPEN", 0);
    }

    private void publishNewMatch(String sport) {
        MatchEntity match = matchService.createForSport(sport);
        eventPublisher.matchCreated(match);
    }
}
