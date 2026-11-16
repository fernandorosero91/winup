package com.winup.service;

import com.winup.entity.MatchEntity;
import com.winup.model.MatchStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class MatchFactory {
    private final LeagueCatalog catalog;
    private final Clock clock;

    public MatchFactory(LeagueCatalog catalog, Clock clock) {
        this.catalog = catalog;
        this.clock = clock;
    }

    public MatchEntity create() {
        return create(ThreadLocalRandom.current());
    }

    public MatchEntity create(Random random) {
        return createFrom(catalog.random(random), random);
    }

    public MatchEntity createForSport(String sport) {
        return createForSport(sport, ThreadLocalRandom.current());
    }

    public MatchEntity createForSport(String sport, Random random) {
        MatchEntity match = createFrom(catalog.forSport(sport, random), random);
        match.setMinute(1);
        return match;
    }

    private MatchEntity createFrom(LeagueCatalog.League league, Random random) {
        int homeIndex = random.nextInt(league.teams().size());
        int awayIndex = random.nextInt(league.teams().size());
        while (awayIndex == homeIndex) {
            awayIndex = random.nextInt(league.teams().size());
        }

        var home = league.teams().get(homeIndex);
        var away = league.teams().get(awayIndex);
        BigDecimal localOdds = odds(random, 1.55, 2.60);
        BigDecimal drawOdds = odds(random, 3.10, 3.90);
        BigDecimal awayOdds = odds(random, 2.20, 4.60);
        Instant now = clock.instant();

        MatchEntity match = new MatchEntity();
        match.setId(UUID.randomUUID());
        match.setSport(league.sport());
        match.setLeague(league.name());
        match.setHomeTeam(home.name());
        match.setAwayTeam(away.name());
        match.setHomeShort(home.shortName());
        match.setAwayShort(away.shortName());
        match.setHomeScore(0);
        match.setAwayScore(0);
        match.setMinute(random.nextInt(0, 2));
        match.setStatus(MatchStatus.OPEN.code());
        match.setSource("RANDOM");
        match.setSimulated(true);
        match.setStartsAt(now);
        match.setLocalOdds(localOdds);
        match.setDrawOdds(drawOdds);
        match.setAwayOdds(awayOdds);
        match.setCreatedAt(now);
        match.setUpdatedAt(now);
        return match;
    }

    private BigDecimal odds(Random random, double min, double max) {
        double value = random.nextDouble(min, max);
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_EVEN);
    }
}
