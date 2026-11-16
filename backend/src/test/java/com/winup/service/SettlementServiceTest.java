package com.winup.service;

import com.winup.entity.BetEntity;
import com.winup.entity.MatchEntity;
import com.winup.entity.WalletEntity;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        verify(eventPublisher).notifyDashboard(eq("CLOSED"), eq(match), eq("LOCAL"));
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

    private SettlementService service() {
        return new SettlementService(
                matchRepository,
                betRepository,
                walletRepository,
                movementRepository,
                eventPublisher,
                new DecimalPayoutStrategy()
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
