package com.winup.service;

import com.winup.entity.MovementEntity;
import com.winup.entity.WalletEntity;
import com.winup.entity.WithdrawalEntity;
import com.winup.exception.ValidationException;
import com.winup.repository.MovementRepository;
import com.winup.repository.WalletRepository;
import com.winup.repository.WithdrawalRepository;
import com.winup.service.payment.PayoutGateway;
import com.winup.service.payment.PayoutOrder;
import com.winup.service.payment.PayoutResult;
import com.winup.service.payment.PayoutState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class WithdrawalPayoutExecutor {
    private static final String PROCESSING = "PROCESSING";
    private static final String PAID = "PAID";
    private static final String FAILED = "FAILED";
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final WithdrawalRepository withdrawalRepository;
    private final WalletRepository walletRepository;
    private final MovementRepository movementRepository;
    private final LedgerService ledgerService;
    private final PayoutGateway payoutGateway;
    private final Clock clock;

    public WithdrawalPayoutExecutor(
            WithdrawalRepository withdrawalRepository,
            WalletRepository walletRepository,
            MovementRepository movementRepository,
            LedgerService ledgerService,
            PayoutGateway payoutGateway,
            Clock clock
    ) {
        this.withdrawalRepository = withdrawalRepository;
        this.walletRepository = walletRepository;
        this.movementRepository = movementRepository;
        this.ledgerService = ledgerService;
        this.payoutGateway = payoutGateway;
        this.clock = clock;
    }

    @Transactional
    public void hold(WalletEntity wallet, WithdrawalEntity withdrawal) {
        BigDecimal amount = withdrawal.getAmountCop();
        withdrawal.setProvider(payoutGateway.provider());
        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet.setHeldBalance(wallet.getHeldBalance().add(amount));
        walletRepository.save(wallet);
        ledgerService.recordWithdrawalHold(withdrawal, amount, clock.instant());
    }

    @Transactional
    public PayoutOutcome pay(WithdrawalEntity withdrawal) {
        PayoutResult result;
        try {
            result = payoutGateway.payout(new PayoutOrder(
                    withdrawal.getId(),
                    withdrawal.getAmountCop(),
                    "COP",
                    withdrawal.getDestination()
            ));
        } catch (RuntimeException exception) {
            result = new PayoutResult(null, PayoutState.FAILED);
        }
        return switch (result.state()) {
            case PAID -> complete(withdrawal, result.providerReference());
            case PENDING -> pending(withdrawal, result.providerReference());
            case FAILED -> release(
                    withdrawal,
                    FAILED,
                    "No se pudo procesar el retiro. El saldo fue liberado."
            );
        };
    }

    @Transactional
    public PayoutOutcome release(WithdrawalEntity withdrawal, String status, String message) {
        WalletEntity wallet = lockWallet(withdrawal.getUserId());
        BigDecimal amount = withdrawal.getAmountCop();
        wallet.setHeldBalance(wallet.getHeldBalance().subtract(amount).max(ZERO));
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        Instant now = clock.instant();
        ledgerService.recordWithdrawalRelease(withdrawal, amount, now);
        withdrawal.setStatus(status);
        withdrawal.setUpdatedAt(now);
        withdrawalRepository.save(withdrawal);
        return new PayoutOutcome(status, null, message);
    }

    private PayoutOutcome complete(WithdrawalEntity withdrawal, String providerReference) {
        WalletEntity wallet = lockWallet(withdrawal.getUserId());
        BigDecimal amount = withdrawal.getAmountCop();
        wallet.setHeldBalance(wallet.getHeldBalance().subtract(amount).max(ZERO));
        walletRepository.save(wallet);

        Instant now = clock.instant();
        MovementEntity movement = new MovementEntity();
        movement.setId(UUID.randomUUID());
        movement.setUserId(withdrawal.getUserId());
        movement.setConcept("Retiro " + withdrawal.getProvider());
        movement.setAmount(amount.negate());
        movement.setType("EGRESO");
        movement.setCreatedAt(now);
        movementRepository.save(movement);

        ledgerService.recordWithdrawalPaid(withdrawal, amount, now);
        withdrawal.setProviderRef(providerReference);
        withdrawal.setStatus(PAID);
        withdrawal.setUpdatedAt(now);
        withdrawalRepository.save(withdrawal);
        return new PayoutOutcome(PAID, providerReference, null);
    }

    private PayoutOutcome pending(WithdrawalEntity withdrawal, String providerReference) {
        withdrawal.setProviderRef(providerReference);
        withdrawal.setStatus(PROCESSING);
        withdrawal.setUpdatedAt(clock.instant());
        withdrawalRepository.save(withdrawal);
        return new PayoutOutcome(PROCESSING, providerReference, null);
    }

    private WalletEntity lockWallet(String userId) {
        return walletRepository.findWithLockByUserId(userId)
                .orElseThrow(() -> new ValidationException("WALLET_NOT_FOUND", "Cartera no encontrada"));
    }

    public record PayoutOutcome(String status, String providerReference, String message) {
    }
}
