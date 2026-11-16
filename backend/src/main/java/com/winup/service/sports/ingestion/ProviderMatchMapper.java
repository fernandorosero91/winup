package com.winup.service.sports.ingestion;

import com.winup.entity.MatchEntity;
import com.winup.model.MatchStatus;
import com.winup.model.SportCatalog;
import com.winup.service.sports.ProviderFixture;
import com.winup.service.sports.ProviderOdds;
import com.winup.service.sports.ProviderTeam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Component
public class ProviderMatchMapper {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private static final int SHORT_NAME_LENGTH = 3;

    public MatchEntity map(
            ProviderFixture fixture,
            String providerId,
            Instant now,
            MatchEntity existing,
            ProviderOdds odds
    ) {
        MatchEntity match = existing == null ? new MatchEntity() : existing;
        if (existing == null) {
            match.setId(UUID.randomUUID());
            match.setCreatedAt(now);
        }
        match.setSport(fixture.sport());
        match.setLeague(fixture.league());
        match.setHomeTeam(fixture.home().name());
        match.setAwayTeam(fixture.away().name());
        match.setHomeShort(shortName(fixture.home()));
        match.setAwayShort(shortName(fixture.away()));
        match.setHomeScore(fixture.score().home());
        match.setAwayScore(fixture.score().away());
        match.setMinute(fixture.finished() ? 90 : fixture.score().minute());
        match.setStartsAt(fixture.startsAt());
        match.setStatus(MatchStatus.OPEN.code());
        match.setWinner(null);
        match.setSource(providerId);
        match.setSimulated(false);
        if (odds != null) {
            match.setLocalOdds(odds.local());
            match.setDrawOdds(odds.draw() == null ? ZERO : odds.draw());
            match.setAwayOdds(odds.away());
        }
        match.setUpdatedAt(now);
        return match;
    }

    public String winner(ProviderFixture fixture) {
        if (fixture.score().home() > fixture.score().away()) {
            return "LOCAL";
        }
        if (fixture.score().home() < fixture.score().away()) {
            return "VISITANTE";
        }
        return SportCatalog.hasDraw(fixture.sport()) ? "EMPATE" : null;
    }

    private String shortName(ProviderTeam team) {
        String shortName = team.shortName();
        if (shortName != null && !shortName.isBlank()) {
            return shortName;
        }
        String name = team.name();
        return name.length() <= SHORT_NAME_LENGTH
                ? name.toUpperCase(Locale.ROOT)
                : name.substring(0, SHORT_NAME_LENGTH).toUpperCase(Locale.ROOT);
    }
}
