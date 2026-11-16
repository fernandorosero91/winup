package com.winup.model.match;
import com.winup.exception.BettingClosedException;
import com.winup.model.vo.BetId;
import com.winup.model.vo.BetSelection;
import com.winup.model.vo.Dinero;
import com.winup.model.vo.MatchId;
import com.winup.model.vo.MatchResult;
import com.winup.model.vo.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchAggregateTest {
    private final MatchId matchId = new MatchId(UUID.randomUUID());

    @Test
    void should_open_register_and_settle_a_bet() {
        MatchAggregate match = new MatchAggregate(matchId, "Equipo A", "Equipo B");
        match.abrirApuestas();
        match.registrarApuesta(bet(BetSelection.LOCAL, "100.00", "2.00"));

        match.finalizarPartido(new MatchResult("LOCAL", 2, 1));
        var lines = match.liquidarGanancias(new DecimalPayoutStrategy());

        assertThat(match.stateCode()).isEqualTo("SETTLED");
        assertThat(match.bets()).hasSize(1);
        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).won()).isTrue();
        assertThat(lines.get(0).payout().amount()).isEqualByComparingTo("200.00");
    }

    @Test
    void should_reject_a_bet_after_the_match_is_closed() {
        MatchAggregate match = new MatchAggregate(matchId, "Equipo A", "Equipo B");
        match.abrirApuestas();
        match.finalizarPartido(new MatchResult("LOCAL", 1, 0));

        assertThatThrownBy(() -> match.registrarApuesta(bet(BetSelection.LOCAL, "100.00", "2.00")))
                .isInstanceOf(BettingClosedException.class);
    }

    @Test
    void should_reject_blank_team_names() {
        assertThatThrownBy(() -> new MatchAggregate(matchId, " ", "Equipo B"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("local");
    }

    @Test
    void should_reject_a_duplicate_bet() {
        MatchAggregate match = new MatchAggregate(matchId, "Equipo A", "Equipo B");
        match.abrirApuestas();
        Bet bet = bet(BetSelection.LOCAL, "100.00", "2.00");

        match.registrarApuesta(bet);

        assertThatThrownBy(() -> match.registrarApuesta(bet))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("registrada");
        assertThat(match.bets()).hasSize(1);
    }

    @Test
    void should_not_overwrite_the_result_after_settlement() {
        MatchAggregate match = new MatchAggregate(matchId, "Equipo A", "Equipo B");
        match.abrirApuestas();
        match.registrarApuesta(bet(BetSelection.LOCAL, "100.00", "2.00"));
        match.finalizarPartido(new MatchResult("LOCAL", 2, 1));
        match.liquidarGanancias(new DecimalPayoutStrategy());

        assertThatThrownBy(() -> match.finalizarPartido(new MatchResult("VISITANTE", 0, 3)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(match.result().winner()).isEqualTo("LOCAL");
    }

    @Test
    void should_not_liquidate_twice() {
        MatchAggregate match = new MatchAggregate(matchId, "Equipo A", "Equipo B");
        match.abrirApuestas();
        match.finalizarPartido(new MatchResult("LOCAL", 1, 0));
        match.liquidarGanancias(new DecimalPayoutStrategy());

        assertThatThrownBy(() -> match.liquidarGanancias(new DecimalPayoutStrategy()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void should_not_liquidate_before_a_result() {
        MatchAggregate match = new MatchAggregate(matchId, "Equipo A", "Equipo B");
        match.abrirApuestas();

        assertThatThrownBy(() -> match.liquidarGanancias(new DecimalPayoutStrategy()))
                .isInstanceOf(IllegalStateException.class);
    }

    private Bet bet(BetSelection selection, String stake, String odds) {
        return new Bet(
                new BetId(UUID.randomUUID()),
                new UserId("user-1"),
                selection,
                new Dinero(new BigDecimal(stake)),
                new BigDecimal(odds)
        );
    }
}
