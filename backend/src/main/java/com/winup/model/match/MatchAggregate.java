package com.winup.model.match;

import com.winup.model.vo.Dinero;
import com.winup.model.vo.MatchId;
import com.winup.model.vo.MatchResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class MatchAggregate {
    private final MatchId id;
    private final String home;
    private final String away;
    private final List<Bet> bets = new ArrayList<>();
    private MatchState state = new CreatedState();
    private MatchResult result = MatchResult.pending();

    public MatchAggregate(MatchId id, String home, String away) {
        this.id = Objects.requireNonNull(id, "El id del partido es obligatorio");
        this.home = requiredText(home, "El equipo local es obligatorio");
        this.away = requiredText(away, "El equipo visitante es obligatorio");
    }

    public void abrirApuestas() {
        state = state.abrir(this);
    }

    public void registrarApuesta(Bet bet) {
        state.registrarApuesta(this, Objects.requireNonNull(bet, "La apuesta es obligatoria"));
    }

    public void finalizarPartido(MatchResult result) {
        MatchResult nextResult = Objects.requireNonNull(result, "El resultado es obligatorio");
        MatchState nextState = state.finalizar(this, nextResult);
        this.result = nextResult;
        this.state = nextState;
    }

    public List<SettlementLine> liquidarGanancias(PayoutStrategy strategy) {
        if (!result.isFinal()) {
            throw new IllegalStateException("No se puede liquidar un partido sin resultado");
        }
        PayoutStrategy payout = Objects.requireNonNull(strategy, "La estrategia es obligatoria");
        MatchState nextState = state.liquidar(this);
        List<SettlementLine> lines = new ArrayList<>();
        for (Bet bet : bets) {
            boolean won = bet.selection().name().equals(result.winner());
            Dinero amount = won
                    ? payout.calcular(bet.stake(), bet.odds())
                    : new Dinero(BigDecimal.ZERO);
            lines.add(new SettlementLine(bet, won, amount));
        }
        state = nextState;
        return List.copyOf(lines);
    }

    void addBet(Bet bet) {
        boolean duplicated = bets.stream()
                .anyMatch(existing -> existing.id().equals(bet.id()));
        if (duplicated) {
            throw new IllegalArgumentException("La apuesta ya está registrada");
        }
        bets.add(bet);
    }

    public MatchId id() {
        return id;
    }

    public String home() {
        return home;
    }

    public String away() {
        return away;
    }

    public MatchState state() {
        return state;
    }

    public String stateCode() {
        return state.code();
    }

    public MatchResult result() {
        return result;
    }

    public List<Bet> bets() {
        return List.copyOf(bets);
    }

    private String requiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
