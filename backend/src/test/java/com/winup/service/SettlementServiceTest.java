package com.winup.service;

import com.winup.entity.BetEntity;
import com.winup.entity.MatchEntity;
import com.winup.entity.WalletEntity;
import com.winup.model.MatchStatus;
import com.winup.model.match.DecimalPayoutStrategy;
import com.winup.repository.BetRepository;
import com.winup.repository.MatchRepository;
import com.winup.repository.MovementRepository;
import com.winup.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {
    @Mock
    private MatchRepository matchRepository;

    @Mock
    private BetRepository betRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private MovementRepository movementRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Test
    void should_settle_pending_bets_when_the_match_was_already_closed() {
        MatchEntity match = match("LOCAL");
        BetEntity bet = bet("LOCAL", "1000.00", "2.00");
        WalletEntity wallet = new WalletEntity();
        wallet.setUserId("user-1");
        wallet.setBalance(new BigDecimal("50000.00"));

        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(betRepository.findByMatchIdAndStatus(match.getId(), "PENDING")).thenReturn(List.of(bet));
        when(walletRepository.findWithLockByUserId("user-1")).thenReturn(Optional.of(wallet));

        service().settle(match.getId(), "LOCAL", 2, 1);

        assertThat(bet.getStatus()).isEqualTo("WON");
        assertThat(bet.getPayout()).isEqualByComparingTo("2000.00");
        assertThat(wallet.getBalance()).isEqualByComparingTo("52000.00");
        verify(eventPublisher, never()).notifyDashboard(any(), any(), any());
    }

    @Test
    void should_close_the_match_and_settle_when_it_was_open() {
        MatchEntity match = match(null);
        match.setStatus("OPEN");
        BetEntity bet = bet("VISITANTE", "1000.00", "3.00");

        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(betRepository.findByMatchIdAndStatus(match.getId(), "PENDING")).thenReturn(List.of(bet));

        service().settle(match.getId(), "LOCAL", 2, 1);

        assertThat(match.getStatus()).isEqualTo("CLOSED");
        assertThat(match.getWinner()).isEqualTo("LOCAL");
        assertThat(bet.getStatus()).isEqualTo("LOST");
        verify(eventPublisher).notifyEmailLoss(bet, match);
        verify(eventPublisher).notifyDashboard(eq(MatchStatus.CLOSED), eq(match), eq("LOCAL"));
    }

    @Test
    void should_not_settle_bets_twice() {
        MatchEntity match = match("LOCAL");
        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(betRepository.findByMatchIdAndStatus(match.getId(), "PENDING")).thenReturn(List.of());

        service().settle(match.getId(), "LOCAL", 2, 1);

        verify(walletRepository, never()).findWithLockByUserId(any());
        verify(eventPublisher, never()).notifyDashboard(any(), any(), any());
    }

    @Test
    void should_void_pending_bets_and_refund_the_stake() {
        MatchEntity match = match(null);
        match.setStatus("OPEN");
        BetEntity bet = bet("LOCAL", "1000.00", "2.00");
        WalletEntity wallet = new WalletEntity();
        wallet.setUserId("user-1");
        wallet.setBalance(new BigDecimal("5000.00"));

        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(betRepository.findByMatchIdAndStatus(match.getId(), "PENDING")).thenReturn(List.of(bet));
        when(walletRepository.findWithLockByUserId("user-1")).thenReturn(Optional.of(wallet));

        assertThat(service().voidMatch(match.getId())).isTrue();

        assertThat(match.getStatus()).isEqualTo("VOID");
        assertThat(bet.getStatus()).isEqualTo("VOID");
        assertThat(bet.getPayout()).isEqualByComparingTo("1000.00");
        assertThat(wallet.getBalance()).isEqualByComparingTo("6000.00");
        verify(movementRepository).save(any());
        verify(eventPublisher).notifyDashboard(MatchStatus.VOID, match, null);
    }

    @Test
    void should_not_void_a_closed_match() {
        MatchEntity match = match("LOCAL");

        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));

        assertThat(service().voidMatch(match.getId())).isFalse();

        assertThat(match.getStatus()).isEqualTo("CLOSED");
        verify(matchRepository, never()).save(match);
        verify(betRepository, never()).findByMatchIdAndStatus(any(), any());
    }

    @Test
    void should_not_void_a_match_twice() {
        MatchEntity match = match(null);
        match.setStatus("VOID");

        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));

        assertThat(service().voidMatch(match.getId())).isFalse();

        verify(betRepository, never()).findByMatchIdAndStatus(any(), any());
    }

    @Test
    void should_ignore_settlement_for_a_void_match() {
        MatchEntity match = match(null);
        match.setStatus("VOID");

        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));

        service().settle(match.getId(), "LOCAL", 2, 1);

        verify(matchRepository, never()).save(match);
        verify(betRepository, never()).findByMatchIdAndStatus(any(), any());
        verify(eventPublisher, never()).notifyDashboard(any(), any(), isNull());
    }

    private SettlementService service() {
        return new SettlementService(
                matchRepository,
                betRepository,
                walletRepository,
                movementRepository,
                eventPublisher,
                new DecimalPayoutStrategy(),
                Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC)
        );
    }

    private MatchEntity match(String winner) {
        MatchEntity match = new MatchEntity();
        match.setId(UUID.randomUUID());
        match.setStatus("CLOSED");
        match.setWinner(winner);
        match.setHomeTeam("Local");
        match.setAwayTeam("Visitante");
        return match;
    }

    private BetEntity bet(String selection, String stake, String odds) {
        BetEntity bet = new BetEntity();
        bet.setId(UUID.randomUUID());
        bet.setUserId("user-1");
        bet.setSelection(selection);
        bet.setStake(new BigDecimal(stake));
        bet.setOdds(new BigDecimal(odds));
        bet.setStatus("PENDING");
        bet.setPayout(BigDecimal.ZERO);
        return bet;
    }
}
