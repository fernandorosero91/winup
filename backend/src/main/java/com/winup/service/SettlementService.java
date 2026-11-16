package com.winup.service;

import com.winup.entity.BetEntity;
import com.winup.entity.MatchEntity;
import com.winup.entity.MovementEntity;
import com.winup.entity.WalletEntity;
import com.winup.model.match.Dinero;
import com.winup.model.match.PayoutStrategy;
import com.winup.repository.BetRepository;
import com.winup.repository.MatchRepository;
import com.winup.repository.MovementRepository;
import com.winup.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    public SettlementService(
            MatchRepository matchRepository,
            BetRepository betRepository,
            WalletRepository walletRepository,
            MovementRepository movementRepository,
            EventPublisher eventPublisher,
            PayoutStrategy payoutStrategy
    ) {
        this.matchRepository = matchRepository;
        this.betRepository = betRepository;
        this.walletRepository = walletRepository;
        this.movementRepository = movementRepository;
        this.eventPublisher = eventPublisher;
        this.payoutStrategy = payoutStrategy;
    }

    @Transactional
    public void settle(UUID matchId, String winner, int homeScore, int awayScore) {
        MatchEntity match = matchRepository.findById(matchId).orElse(null);
        if (match == null) {
            return;
        }

        boolean closedNow = !"CLOSED".equals(match.getStatus());
        if (closedNow) {
            match.setStatus("CLOSED");
            match.setWinner(winner);
            match.setHomeScore(homeScore);
            match.setAwayScore(awayScore);
            match.setMinute(90);
            match.setUpdatedAt(Instant.now());
            matchRepository.save(match);
        } else if (match.getWinner() == null) {
            match.setWinner(winner);
            match.setHomeScore(homeScore);
            match.setAwayScore(awayScore);
            matchRepository.save(match);
        }

        String resultWinner = match.getWinner() == null ? winner : match.getWinner();
        settlePendingBets(match, resultWinner);

        if (closedNow) {
            eventPublisher.notifyDashboard("CLOSED", match, resultWinner);
        }
    }

    private void settlePendingBets(MatchEntity match, String winner) {
        for (BetEntity bet : betRepository.findByMatchIdAndStatus(match.getId(), "PENDING")) {
            boolean won = bet.getSelection().equals(winner);
            bet.setSettledAt(Instant.now());
            if (won) {
                BigDecimal payout = payoutStrategy
                        .calcular(new Dinero(bet.getStake()), bet.getOdds())
                        .amount();
                bet.setStatus("WON");
                bet.setPayout(payout);
                credit(bet, match, payout);
                eventPublisher.notifyEmailWin(bet, match, payout);
            } else {
                bet.setStatus("LOST");
                bet.setPayout(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN));
                eventPublisher.notifyEmailLoss(bet, match);
            }
            betRepository.save(bet);
        }
    }

    private void credit(BetEntity bet, MatchEntity match, BigDecimal payout) {
        WalletEntity wallet = walletRepository.findWithLockByUserId(bet.getUserId()).orElse(null);
        if (wallet == null) {
            return;
        }
        wallet.setBalance(wallet.getBalance().add(payout).setScale(2, RoundingMode.HALF_EVEN));
        walletRepository.save(wallet);

        MovementEntity movement = new MovementEntity();
        movement.setId(UUID.randomUUID());
        movement.setUserId(bet.getUserId());
        movement.setConcept("Ganancia " + match.getHomeTeam() + " vs " + match.getAwayTeam());
        movement.setAmount(payout);
        movement.setType("GANANCIA");
        movement.setCreatedAt(Instant.now());
        movementRepository.save(movement);
    }
}
