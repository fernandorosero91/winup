package com.winup.mapper;

import com.winup.dto.BetDto;
import com.winup.entity.BetEntity;
import com.winup.entity.MatchEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BetMapperImplTest {
    @Test
    void should_map_a_pending_bet_without_outcome() {
        MatchEntity match = match("Equipo A", "Equipo B", "OPEN", null, 0, 0);
        BetEntity bet = bet("LOCAL", "PENDING", "2.00");

        BetDto dto = new BetMapperImpl().toDto(bet, match);

        assertThat(dto.match()).isEqualTo("Equipo A vs Equipo B");
        assertThat(dto.stake()).isEqualByComparingTo("100.00");
        assertThat(dto.potential()).isEqualByComparingTo("200.00");
        assertThat(dto.status()).isEqualTo("PENDIENTE");
        assertThat(dto.outcome().finished()).isFalse();
        assertThat(dto.outcome().winnerName()).isNull();
    }

    @Test
    void should_map_the_winner_and_score_of_a_closed_match() {
        MatchEntity match = match("Equipo A", "Equipo B", "CLOSED", "VISITANTE", 1, 3);
        BetEntity bet = bet("VISITANTE", "WON", "3.00");

        BetDto dto = new BetMapperImpl().toDto(bet, match);

        assertThat(dto.status()).isEqualTo("GANADA");
        assertThat(dto.outcome().finished()).isTrue();
        assertThat(dto.outcome().winner()).isEqualTo("VISITANTE");
        assertThat(dto.outcome().winnerName()).isEqualTo("Equipo B");
        assertThat(dto.outcome().homeScore()).isEqualTo(1);
        assertThat(dto.outcome().awayScore()).isEqualTo(3);
    }

    @Test
    void should_map_a_draw_with_the_word_empate() {
        MatchEntity match = match("Equipo A", "Equipo B", "CLOSED", "EMPATE", 2, 2);
        BetEntity bet = bet("EMPATE", "WON", "3.20");

        BetDto dto = new BetMapperImpl().toDto(bet, match);

        assertThat(dto.outcome().winnerName()).isEqualTo("Empate");
        assertThat(dto.outcome().homeScore()).isEqualTo(2);
        assertThat(dto.outcome().awayScore()).isEqualTo(2);
    }

    @Test
    void should_not_break_when_the_match_is_missing() {
        BetEntity bet = bet("LOCAL", "PENDING", "2.00");

        BetDto dto = new BetMapperImpl().toDto(bet, null);

        assertThat(dto.match()).isEqualTo("Partido");
        assertThat(dto.outcome().finished()).isFalse();
        assertThat(dto.outcome().winnerName()).isNull();
    }

    private MatchEntity match(
            String home,
            String away,
            String status,
            String winner,
            int homeScore,
            int awayScore
    ) {
        MatchEntity match = new MatchEntity();
        match.setId(UUID.randomUUID());
        match.setHomeTeam(home);
        match.setAwayTeam(away);
        match.setStatus(status);
        match.setWinner(winner);
        match.setHomeScore(homeScore);
        match.setAwayScore(awayScore);
        return match;
    }

    private BetEntity bet(String selection, String status, String odds) {
        BetEntity bet = new BetEntity();
        bet.setId(UUID.randomUUID());
        bet.setUserId("user-1");
        bet.setSelection(selection);
        bet.setStake(new BigDecimal("100.00"));
        bet.setOdds(new BigDecimal(odds));
        bet.setStatus(status);
        bet.setPayout(BigDecimal.ZERO);
        bet.setCreatedAt(Instant.parse("2026-11-12T18:00:00Z"));
        return bet;
    }
}
