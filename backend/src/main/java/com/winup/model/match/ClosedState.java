package com.winup.model.match;

public final class ClosedState implements MatchState {
    @Override
    public String code() {
        return "CLOSED";
    }

    @Override
    public MatchState liquidar(MatchAggregate match) {
        return new SettledState();
    }
}
