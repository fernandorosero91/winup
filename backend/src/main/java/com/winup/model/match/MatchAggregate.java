package com.winup.model.match;
import com.winup.model.vo.Dinero;
import com.winup.model.vo.MatchId;
import com.winup.model.vo.MatchResult;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class MatchAggregate {
    private final MatchId id;
    private final String home;
    private final String away;
    private final List<Bet> bets = new ArrayList<>();
    private MatchState state = new CreatedState();
    private MatchResult result = MatchResult.pending();

    public MatchAggregate(MatchId id, String home, String away) {
        this.id = id;
        this.home = home;
        this.away = away;
    }

    public void abrirApuestas() {
        state = state.abrir(this);
    }

    public void registrarApuesta(Bet bet) {
        state.registrarApuesta(this, bet);
    }

    public void finalizarPartido(MatchResult result) {
        this.result = result;
        state = state.finalizar(this, result);
    }

    public List<SettlementLine> liquidarGanancias(PayoutStrategy strategy) {
        if (!result.isFinal()) {
            throw new IllegalStateException("No se puede liquidar un partido sin resultado");
        }
        List<SettlementLine> lines = new ArrayList<>();
        for (Bet bet : bets) {
            boolean won = bet.selection().name().equals(result.winner());
            Dinero payout = won ? strategy.calcular(bet.stake(), bet.odds()) : new Dinero(BigDecimal.ZERO);
            lines.add(new SettlementLine(bet, won, payout));
        }
        state = state.liquidar(this);
        return List.copyOf(lines);
    }

    void addBet(Bet bet) {
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
}
