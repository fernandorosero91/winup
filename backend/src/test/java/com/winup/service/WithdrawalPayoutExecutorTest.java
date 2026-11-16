package com.winup.service;

import com.winup.entity.MovementEntity;
import com.winup.entity.WalletEntity;
import com.winup.entity.WithdrawalEntity;
import com.winup.repository.MovementRepository;
import com.winup.repository.WalletRepository;
import com.winup.repository.WithdrawalRepository;
import com.winup.service.WithdrawalPayoutExecutor.PayoutOutcome;
import com.winup.service.payment.PayoutGateway;
import com.winup.service.payment.PayoutOrder;
import com.winup.service.payment.PayoutResult;
import com.winup.service.payment.PayoutState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WithdrawalPayoutExecutorTest {
    private static final String USER = "user-1";
    private static final UUID WITHDRAWAL_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    @Mock
    private WithdrawalRepository withdrawalRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private MovementRepository movementRepository;

    @Mock
    private LedgerService ledgerService;

    @Mock
    private PayoutGateway payoutGateway;

    private WithdrawalPayoutExecutor executor;
    private WalletEntity wallet;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-13T20:00:00Z"), ZoneOffset.UTC);
        executor = new WithdrawalPayoutExecutor(
                withdrawalRepository,
                walletRepository,
                movementRepository,
                ledgerService,
                payoutGateway,
                clock
        );
        wallet = wallet("100000.00", "0.00");
    }

    @Test
    void should_hold_the_amount_and_record_the_provider() {
        WithdrawalEntity withdrawal = withdrawal("50000.00");

        when(payoutGateway.provider()).thenReturn("SIMULATOR");
        executor.hold(wallet, withdrawal);

        assertThat(wallet.getBalance()).isEqualByComparingTo("50000.00");
        assertThat(wallet.getHeldBalance()).isEqualByComparingTo("50000.00");
        assertThat(withdrawal.getProvider()).isEqualTo("SIMULATOR");
        verify(ledgerService).recordWithdrawalHold(any(), any(), any());
    }

    @Test
    void should_complete_a_paid_payout() {
        wallet = wallet("50000.00", "50000.00");
        WithdrawalEntity withdrawal = heldWithdrawal();
        when(payoutGateway.payout(any(PayoutOrder.class))).thenReturn(new PayoutResult("SIM-1", PayoutState.PAID));
        when(walletRepository.findWithLockByUserId(USER)).thenReturn(Optional.of(wallet));

        PayoutOutcome outcome = executor.pay(withdrawal);

        assertThat(outcome.status()).isEqualTo("PAID");
        assertThat(wallet.getHeldBalance()).isEqualByComparingTo("0.00");
        assertThat(withdrawal.getStatus()).isEqualTo("PAID");
        ArgumentCaptor<MovementEntity> movement = ArgumentCaptor.forClass(MovementEntity.class);
        verify(movementRepository).save(movement.capture());
        assertThat(movement.getValue().getAmount()).isEqualByComparingTo("-50000.00");
        assertThat(movement.getValue().getType()).isEqualTo("EGRESO");
        verify(ledgerService).recordWithdrawalPaid(any(), any(), any());
    }

    @Test
    void should_release_the_hold_when_the_payout_fails() {
        wallet = wallet("50000.00", "50000.00");
        WithdrawalEntity withdrawal = heldWithdrawal();
        when(payoutGateway.payout(any(PayoutOrder.class))).thenReturn(new PayoutResult(null, PayoutState.FAILED));
        when(walletRepository.findWithLockByUserId(USER)).thenReturn(Optional.of(wallet));

        PayoutOutcome outcome = executor.pay(withdrawal);

        assertThat(outcome.status()).isEqualTo("FAILED");
        assertThat(wallet.getBalance()).isEqualByComparingTo("100000.00");
        assertThat(wallet.getHeldBalance()).isEqualByComparingTo("0.00");
        verify(ledgerService).recordWithdrawalRelease(any(), any(), any());
        verify(movementRepository, never()).save(any());
    }

    @Test
    void should_keep_the_hold_when_the_payout_is_pending() {
        WithdrawalEntity withdrawal = heldWithdrawal();
        when(payoutGateway.payout(any(PayoutOrder.class))).thenReturn(new PayoutResult("POP-1", PayoutState.PENDING));

        PayoutOutcome outcome = executor.pay(withdrawal);

        assertThat(outcome.status()).isEqualTo("PROCESSING");
        assertThat(withdrawal.getStatus()).isEqualTo("PROCESSING");
        assertThat(withdrawal.getProviderRef()).isEqualTo("POP-1");
        verify(ledgerService, never()).recordWithdrawalPaid(any(), any(), any());
    }

    @Test
    void should_release_the_hold_on_rejection() {
        wallet = wallet("50000.00", "50000.00");
        WithdrawalEntity withdrawal = heldWithdrawal();
        when(walletRepository.findWithLockByUserId(USER)).thenReturn(Optional.of(wallet));

        PayoutOutcome outcome = executor.release(withdrawal, "REJECTED", "Datos incompletos");

        assertThat(outcome.status()).isEqualTo("REJECTED");
        assertThat(outcome.message()).isEqualTo("Datos incompletos");
        assertThat(wallet.getBalance()).isEqualByComparingTo("100000.00");
        assertThat(wallet.getHeldBalance()).isEqualByComparingTo("0.00");
        verify(ledgerService).recordWithdrawalRelease(any(), any(), any());
    }

    private WalletEntity wallet(String balance, String held) {
        WalletEntity entity = new WalletEntity();
        entity.setUserId(USER);
        entity.setBalance(new BigDecimal(balance));
        entity.setHeldBalance(new BigDecimal(held));
        return entity;
    }

    private WithdrawalEntity withdrawal(String amount) {
        WithdrawalEntity entity = new WithdrawalEntity();
        entity.setId(WITHDRAWAL_ID);
        entity.setUserId(USER);
        entity.setAmountCop(new BigDecimal(amount));
        entity.setStatus("PROCESSING");
        entity.setCreatedAt(Instant.parse("2026-11-13T19:00:00Z"));
        entity.setUpdatedAt(Instant.parse("2026-11-13T19:00:00Z"));
        return entity;
    }

    private WithdrawalEntity heldWithdrawal() {
        WithdrawalEntity entity = withdrawal("50000.00");
        entity.setProvider("SIMULATOR");
        return entity;
    }
}
