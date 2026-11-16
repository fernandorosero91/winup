package com.winup.mapper;

import com.winup.entity.BetEntity;
import com.winup.entity.MatchEntity;
import com.winup.dto.BetDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BetMapperImplTest {
    @Test
    void should_map_a_bet_and_its_match() {
        MatchEntity match = new MatchEntity();
        match.setId(UUID.randomUUID());
        match.setHomeTeam("Equipo A");
        match.setAwayTeam("Equipo B");

        BetEntity bet = new BetEntity();
        bet.setId(UUID.randomUUID());
        bet.setMatchId(match.getId());
        bet.setUserId("user-1");
        bet.setSelection("LOCAL");
        bet.setStake(new BigDecimal("100.00"));
        bet.setOdds(new BigDecimal("2.00"));
        bet.setStatus("PENDING");
        bet.setPayout(BigDecimal.ZERO);
        bet.setCreatedAt(Instant.parse("2026-11-12T18:00:00Z"));

        BetDto dto = new BetMapperImpl().toDto(bet, match);

        assertThat(dto.match()).isEqualTo("Equipo A vs Equipo B");
        assertThat(dto.stake()).isEqualByComparingTo("100.00");
        assertThat(dto.potential()).isEqualByComparingTo("200.00");
        assertThat(dto.status()).isEqualTo("PENDIENTE");
    }
}
