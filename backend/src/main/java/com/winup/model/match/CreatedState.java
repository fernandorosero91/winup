package com.winup.model.match;

public final class CreatedState implements MatchState {
    @Override
    public String code() {
        return "CREATED";
    }

    @Override
    public MatchState abrir(MatchAggregate match) {
        return new OpenForBetsState();
    }
}
