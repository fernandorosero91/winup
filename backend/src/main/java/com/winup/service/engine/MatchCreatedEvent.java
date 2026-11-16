package com.winup.service.engine;

import com.winup.model.match.MatchId;

import java.util.Objects;

public record MatchCreatedEvent(MatchId matchId, String home, String away) {
    public MatchCreatedEvent {
        Objects.requireNonNull(matchId, "El id del partido es obligatorio");
        if (home == null || home.isBlank() || away == null || away.isBlank()) {
            throw new IllegalArgumentException("Los equipos son obligatorios");
        }
    }
}
