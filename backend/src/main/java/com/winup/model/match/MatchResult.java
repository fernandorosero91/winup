package com.winup.model.match;

import java.util.Objects;
import java.util.Set;

public record MatchResult(String winner, int homeScore, int awayScore) {
    private static final Set<String> WINNERS = Set.of("LOCAL", "EMPATE", "VISITANTE");

    public MatchResult {
        Objects.requireNonNull(winner, "El ganador es obligatorio");
        if (homeScore < 0 || awayScore < 0) {
            throw new IllegalArgumentException("Los marcadores no pueden ser negativos");
        }
        if (!winner.isBlank() && !WINNERS.contains(winner)) {
            throw new IllegalArgumentException("Ganador inválido: " + winner);
        }
    }

    public static MatchResult pending() {
        return new MatchResult("", 0, 0);
    }

    public boolean isFinal() {
        return winner != null && !winner.isBlank();
    }
}
