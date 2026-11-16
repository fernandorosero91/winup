package com.winup.service.sports;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record ProviderFixture(
        String externalId,
        String sport,
        String league,
        ProviderTeam home,
        ProviderTeam away,
        Instant startsAt,
        FixtureStatus status,
        ProviderScore score,
        ProviderOdds odds
) {
    public ProviderFixture {
        Objects.requireNonNull(externalId, "El identificador externo es obligatorio");
        Objects.requireNonNull(sport, "El deporte es obligatorio");
        Objects.requireNonNull(league, "La liga es obligatoria");
        Objects.requireNonNull(home, "El local es obligatorio");
        Objects.requireNonNull(away, "El visitante es obligatorio");
        Objects.requireNonNull(startsAt, "La fecha es obligatoria");
        Objects.requireNonNull(status, "El estado es obligatorio");
        Objects.requireNonNull(score, "El marcador es obligatorio");
    }

    public boolean finished() {
        return status == FixtureStatus.FINISHED;
    }

    public boolean drawMarket() {
        return odds != null && odds.draw() != null && odds.draw().signum() > 0;
    }

    public BigDecimal oddsFor(String selection) {
        if (odds == null) {
            return null;
        }
        return switch (selection) {
            case "LOCAL" -> odds.local();
            case "VISITANTE" -> odds.away();
            default -> odds.draw();
        };
    }
}
