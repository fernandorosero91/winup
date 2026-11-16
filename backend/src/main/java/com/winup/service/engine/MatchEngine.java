package com.winup.service.engine;

import com.winup.model.match.MatchAggregate;
import com.winup.model.match.MatchId;
import com.winup.model.match.MatchResult;
import com.winup.model.match.PayoutStrategy;
import com.winup.model.match.SettlementLine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class MatchEngine {
    private final Map<MatchId, MatchAggregate> matches = new ConcurrentHashMap<>();
    private final PayoutStrategy payoutStrategy;

    public MatchEngine(PayoutStrategy payoutStrategy) {
        this.payoutStrategy = payoutStrategy;
    }

    public void onCreated(MatchCreatedEvent event) {
        MatchAggregate aggregate = matches.computeIfAbsent(
                event.matchId(),
                id -> new MatchAggregate(id, event.home(), event.away())
        );
        if ("CREATED".equals(aggregate.stateCode())) {
            aggregate.abrirApuestas();
        }
    }

    public void onBet(BetPlacedEvent event) {
        MatchAggregate aggregate = matches.get(event.matchId());
        if (aggregate == null) {
            throw new IllegalStateException("Partido no reconstruido: " + event.matchId().value());
        }
        aggregate.registrarApuesta(event.bet());
    }

    public List<SettlementLine> onFinished(MatchFinishedEvent event) {
        MatchAggregate aggregate = matches.get(event.matchId());
        if (aggregate == null) {
            throw new IllegalStateException("Partido no reconstruido: " + event.matchId().value());
        }
        aggregate.finalizarPartido(event.result());
        return aggregate.liquidarGanancias(payoutStrategy);
    }

    public Optional<MatchAggregate> find(MatchId id) {
        return Optional.ofNullable(matches.get(id));
    }

    public int size() {
        return matches.size();
    }

    public void reset() {
        matches.clear();
    }

    public MatchResult resultOf(MatchId id) {
        MatchAggregate aggregate = matches.get(id);
        return aggregate == null ? MatchResult.pending() : aggregate.result();
    }
}
