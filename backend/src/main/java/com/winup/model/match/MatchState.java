package com.winup.model.match;
import com.winup.model.vo.MatchResult;


import com.winup.exception.BettingClosedException;

public sealed interface MatchState permits CreatedState, OpenForBetsState, ClosedState, SettledState {
    String code();

    default MatchState abrir(MatchAggregate match) {
        throw new IllegalStateException("El partido no puede abrirse desde " + code());
    }

    default void registrarApuesta(MatchAggregate match, Bet bet) {
        throw new BettingClosedException("El partido no admite apuestas en estado " + code());
    }

    default MatchState finalizar(MatchAggregate match, MatchResult result) {
        throw new IllegalStateException("El partido no puede cerrarse desde " + code());
    }

    default MatchState liquidar(MatchAggregate match) {
        throw new IllegalStateException("El partido no puede liquidarse desde " + code());
    }
}
