package com.winup.service.provider.random;

import com.winup.entity.MatchEntity;
import com.winup.model.MatchStatus;
import com.winup.model.SportCatalog;
import com.winup.repository.MatchRepository;
import com.winup.service.MatchFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class RandomAdapter {
    private final MatchRepository matchRepository;
    private final MatchFactory matchFactory;
    private final long seed;
    private final int targetCount;

    public RandomAdapter(
            MatchRepository matchRepository,
            MatchFactory matchFactory,
            @Value("${winup.random.seed:2026}") long seed,
            @Value("${winup.random.count:300}") int targetCount
    ) {
        this.matchRepository = matchRepository;
        this.matchFactory = matchFactory;
        this.seed = seed;
        this.targetCount = targetCount;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedOnStartup() {
        seed(targetCount);
    }

    @Transactional
    public int seed(int count) {
        return seed(count, false);
    }

    @Transactional
    public int seedExtra(int count) {
        return seed(count, true);
    }

    private int seed(int count, boolean force) {
        if (count <= 0 || (!force && matchRepository.count() >= count)) {
            return 0;
        }
        Random random = new Random(force ? seed + System.nanoTime() : seed);
        List<MatchEntity> matches = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            String sport = SportCatalog.SPORTS.get(index % SportCatalog.SPORTS.size());
            MatchEntity match = matchFactory.createForSport(sport, random);
            applyState(match, index, random);
            matches.add(match);
        }
        matchRepository.saveAll(matches);
        return matches.size();
    }

    private void applyState(MatchEntity match, int index, Random random) {
        int slot = index % 3;
        if (slot == 0) {
            match.setStatus(MatchStatus.OPEN.code());
            match.setMinute(1 + random.nextInt(85));
            match.setHomeScore(random.nextInt(0, 4));
            match.setAwayScore(random.nextInt(0, 4));
            match.setStartsAt(Instant.now().minusSeconds(random.nextInt(600, 5400)));
            return;
        }
        if (slot == 1) {
            match.setStatus(MatchStatus.OPEN.code());
            match.setMinute(0);
            match.setHomeScore(0);
            match.setAwayScore(0);
            match.setStartsAt(Instant.now().plusSeconds(random.nextInt(3600, 172800)));
            return;
        }
        match.setStatus(MatchStatus.CLOSED.code());
        match.setMinute(90);
        int homeScore = random.nextInt(0, 4);
        int awayScore = random.nextInt(0, 4);
        match.setHomeScore(homeScore);
        match.setAwayScore(awayScore);
        match.setWinner(winnerFor(match.getSport(), homeScore, awayScore));
        match.setStartsAt(Instant.now().minusSeconds(random.nextInt(7200, 86400)));
    }

    private String winnerFor(String sport, int homeScore, int awayScore) {
        if (homeScore == awayScore) {
            return SportCatalog.hasDraw(sport) ? "EMPATE" : "LOCAL";
        }
        return homeScore > awayScore ? "LOCAL" : "VISITANTE";
    }
}
