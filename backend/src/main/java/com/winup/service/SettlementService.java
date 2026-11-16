package com.winup.service;

import com.winup.entity.BetEntity;
import com.winup.entity.MatchEntity;
import com.winup.entity.MovementEntity;
import com.winup.entity.WalletEntity;
import com.winup.model.BetStatus;
import com.winup.model.MatchStatus;
import com.winup.model.vo.Dinero;
import com.winup.model.match.PayoutStrategy;
import com.winup.repository.BetRepository;
import com.winup.repository.MatchRepository;
import com.winup.repository.MovementRepository;
import com.winup.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class SettlementService {
    private final MatchRepository matchRepository;
    private final BetRepository betRepository;
    private final WalletRepository walletRepository;
    private final MovementRepository movementRepository;
    private final EventPublisher eventPublisher;
    private final PayoutStrategy payoutStrategy;
    private final Clock clock;

    public SettlementService(
            MatchRepository matchRepository,
            BetRepository betRepository,
            WalletRepository walletRepository,
            MovementRepository movementRepository,
            EventPublisher eventPublisher,
            PayoutStrategy payoutStrategy,
            Clock clock
    ) {
        this.matchRepository = matchRepository;
        this.betRepository = betRepository;
        this.walletRepository = walletRepository;
        this.movementRepository = movementRepository;
        this.eventPublisher = eventPublisher;
        this.payoutStrategy = payoutStrategy;
        this.clock = clock;
    }

    @Transactional
    public void settle(UUID matchId, String winner, int homeScore, int awayScore) {
        MatchEntity match = matchRepository.findById(matchId).orElse(null);
        if (match == null || MatchStatus.from(match.getStatus()) == MatchStatus.VOID) {
            return;
        }

        Instant now = clock.instant();
        boolean closedNow = MatchStatus.from(match.getStatus()) != MatchStatus.CLOSED;
        if (closedNow) {
            match.setStatus(MatchStatus.CLOSED.code());
            match.setWinner(winner);
            match.setHomeScore(homeScore);
            match.setAwayScore(awayScore);
            match.setMinute(90);
            match.setUpdatedAt(now);
            matchRepository.save(match);
        } else if (match.getWinner() == null) {
            match.setWinner(winner);
            match.setHomeScore(homeScore);
            match.setAwayScore(awayScore);
            matchRepository.save(match);
        }

        String resultWinner = match.getWinner() == null ? winner : match.getWinner();
        settlePendingBets(match, resultWinner, now);

        if (closedNow) {
            eventPublisher.notifyDashboard(MatchStatus.CLOSED, match, resultWinner);
        }
    }

    @Transactional
    public boolean voidMatch(UUID matchId) {
        MatchEntity match = matchRepository.findById(matchId).orElse(null);
        if (match == null) {
            return false;
        }
        MatchStatus status = MatchStatus.from(match.getStatus());
        if (status == MatchStatus.CLOSED || status == MatchStatus.VOID) {
            return false;
        }

        Instant now = clock.instant();
        match.setStatus(MatchStatus.VOID.code());
        match.setWinner(null);
        match.setMinute(0);
        match.setUpdatedAt(now);
        matchRepository.save(match);

        for (BetEntity bet : betRepository.findByMatchIdAndStatus(match.getId(), BetStatus.PENDING.code())) {
            bet.setStatus(BetStatus.VOID.code());
            bet.setPayout(bet.getStake());
            bet.setSettledAt(now);
            refund(bet, match, now);
            betRepository.save(bet);
        }
        eventPublisher.notifyDashboard(MatchStatus.VOID, match, null);
        return true;
    }

    private void settlePendingBets(MatchEntity match, String winner, Instant now) {
        for (BetEntity bet : betRepository.findByMatchIdAndStatus(match.getId(), BetStatus.PENDING.code())) {
            boolean won = bet.getSelection().equals(winner);
            bet.setSettledAt(now);
            if (won) {
                BigDecimal payout = payoutStrategy
                        .calcular(new Dinero(bet.getStake()), bet.getOdds())
                        .amount();
                bet.setStatus(BetStatus.WON.code());
                bet.setPayout(payout);
                credit(bet, match, payout, now);
                eventPublisher.notifyEmailWin(bet, match, payout);
            } else {
                bet.setStatus(BetStatus.LOST.code());
                bet.setPayout(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN));
                eventPublisher.notifyEmailLoss(bet, match);
            }
            betRepository.save(bet);
        }
    }

    private void credit(BetEntity bet, MatchEntity match, BigDecimal payout, Instant now) {
        WalletEntity wallet = walletRepository.findWithLockByUserId(bet.getUserId()).orElse(null);
        if (wallet == null) {
            return;
        }
        wallet.setBalance(wallet.getBalance().add(payout).setScale(2, RoundingMode.HALF_EVEN));
        walletRepository.save(wallet);

        movement(bet, match, payout, "GANANCIA", "Ganancia ", now);
    }

    private void refund(BetEntity bet, MatchEntity match, Instant now) {
        WalletEntity wallet = walletRepository.findWithLockByUserId(bet.getUserId()).orElse(null);
        if (wallet == null) {
            return;
        }
        wallet.setBalance(wallet.getBalance().add(bet.getStake()).setScale(2, RoundingMode.HALF_EVEN));
        walletRepository.save(wallet);

        movement(bet, match, bet.getStake(), "REEMBOLSO", "Reembolso ", now);
    }

    private void movement(
            BetEntity bet,
            MatchEntity match,
            BigDecimal amount,
            String type,
            String prefix,
            Instant now
    ) {
        MovementEntity movement = new MovementEntity();
        movement.setId(UUID.randomUUID());
        movement.setUserId(bet.getUserId());
        movement.setConcept(prefix + match.getHomeTeam() + " vs " + match.getAwayTeam());
        movement.setAmount(amount);
        movement.setType(type);
        movement.setCreatedAt(now);
        movementRepository.save(movement);
    }
}
