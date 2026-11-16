package com.winup.service.sports.ingestion;

import com.winup.service.GeneratorLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SportsIngestionScheduler {
    private final SportsIngestionService ingestionService;
    private final GeneratorLock generatorLock;
    private final boolean liveEnabled;

    public SportsIngestionScheduler(
            SportsIngestionService ingestionService,
            GeneratorLock generatorLock,
            @Value("${winup.sports.ingestion.live-enabled:false}") boolean liveEnabled
    ) {
        this.ingestionService = ingestionService;
        this.generatorLock = generatorLock;
        this.liveEnabled = liveEnabled;
    }

    @Scheduled(fixedDelayString = "${winup.sports.ingestion.interval-ms:600000}")
    public void ingest() {
        generatorLock.run(ingestionService::ingest);
    }

    @Scheduled(fixedDelayString = "${winup.sports.ingestion.live-interval-ms:600000}")
    public void ingestLive() {
        if (!liveEnabled) {
            return;
        }
        generatorLock.run(ingestionService::ingestLive);
    }
}
