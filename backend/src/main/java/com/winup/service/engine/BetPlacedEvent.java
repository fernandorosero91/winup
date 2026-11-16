package com.winup.service.engine;

import com.winup.model.match.Bet;
import com.winup.model.vo.MatchId;

import java.util.Objects;

public record BetPlacedEvent(MatchId matchId, Bet bet) {
    public BetPlacedEvent {
        Objects.requireNonNull(matchId, "El id del partido es obligatorio");
        Objects.requireNonNull(bet, "La apuesta es obligatoria");
    }
}
