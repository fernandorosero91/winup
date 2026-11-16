package com.winup.service.engine;

import com.winup.model.match.Bet;
import com.winup.model.match.BetId;
import com.winup.model.match.DecimalPayoutStrategy;
import com.winup.model.match.Dinero;
import com.winup.model.match.MatchId;
import com.winup.model.match.MatchResult;
import com.winup.model.match.UserId;
import com.winup.model.vo.BetSelection;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchEngineTest {
    private final MatchId matchId = new MatchId(UUID.randomUUID());

    @Test
    void should_reconstruct_state_from_the_three_events() {
        MatchEngine engine = new MatchEngine(new DecimalPayoutStrategy());

        engine.onCreated(new MatchCreatedEvent(matchId, "Equipo A", "Equipo B"));
        engine.onBet(new BetPlacedEvent(matchId, bet(BetSelection.LOCAL)));
        var lines = engine.onFinished(new MatchFinishedEvent(
                matchId,
                new MatchResult("LOCAL", 2, 1)
        ));

        assertThat(engine.find(matchId)).isPresent();
        assertThat(engine.find(matchId).orElseThrow().stateCode()).isEqualTo("SETTLED");
        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).payout().amount()).isEqualByComparingTo("200.00");
    }

    @Test
    void should_reject_a_bet_when_the_engine_has_already_closed_the_match() {
        MatchEngine engine = new MatchEngine(new DecimalPayoutStrategy());
        engine.onCreated(new MatchCreatedEvent(matchId, "Equipo A", "Equipo B"));
        engine.onFinished(new MatchFinishedEvent(matchId, new MatchResult("LOCAL", 1, 0)));

        assertThatThrownBy(() -> engine.onBet(new BetPlacedEvent(matchId, bet(BetSelection.LOCAL))))
                .isInstanceOf(com.winup.exception.BettingClosedException.class);
    }

    @Test
    void should_fail_when_the_match_was_never_created() {
        MatchEngine engine = new MatchEngine(new DecimalPayoutStrategy());

        assertThatThrownBy(() -> engine.onBet(new BetPlacedEvent(matchId, bet(BetSelection.LOCAL))))
                .isInstanceOf(IllegalStateException.class);
    }

    private Bet bet(BetSelection selection) {
        return new Bet(
                new BetId(UUID.randomUUID()),
                new UserId("user-1"),
                selection,
                new Dinero(new BigDecimal("100.00")),
                new BigDecimal("2.00")
        );
    }
}
