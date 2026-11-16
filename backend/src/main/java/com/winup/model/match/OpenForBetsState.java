package com.winup.model.match;
import com.winup.model.vo.MatchResult;


public final class OpenForBetsState implements MatchState {
    @Override
    public String code() {
        return "OPEN_FOR_BETS";
    }

    @Override
    public void registrarApuesta(MatchAggregate match, Bet bet) {
        match.addBet(bet);
    }

    @Override
    public MatchState finalizar(MatchAggregate match, MatchResult result) {
        return new ClosedState();
    }
}
