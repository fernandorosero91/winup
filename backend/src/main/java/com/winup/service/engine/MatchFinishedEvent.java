package com.winup.service.engine;

import com.winup.model.vo.MatchId;
import com.winup.model.vo.MatchResult;

import java.util.Objects;

public record MatchFinishedEvent(MatchId matchId, MatchResult result) {
    public MatchFinishedEvent {
        Objects.requireNonNull(matchId, "El id del partido es obligatorio");
        Objects.requireNonNull(result, "El resultado es obligatorio");
    }
}
