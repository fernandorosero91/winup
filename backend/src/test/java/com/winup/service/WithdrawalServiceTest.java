package com.winup.service;

import com.winup.config.WithdrawalOptions;
import com.winup.dto.WithdrawalDto;
import com.winup.dto.WithdrawalRequest;
import com.winup.entity.WalletEntity;
import com.winup.entity.WithdrawalEntity;
import com.winup.repository.WalletRepository;
import com.winup.repository.WithdrawalRepository;
import com.winup.service.WithdrawalPayoutExecutor.PayoutOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WithdrawalServiceTest {
    private static final String USER = "user-1";
    private static final String KEY = "retiro-1";
    private static final UUID WITHDRAWAL_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock
    private WithdrawalRepository withdrawalRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WithdrawalPayoutExecutor payoutExecutor;

    private WalletEntity wallet;
    private WithdrawalService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-13T20:00:00Z"), ZoneOffset.UTC);
        service = new WithdrawalService(
                withdrawalRepository,
                walletRepository,
                payoutExecutor,
                new WithdrawalOptions(new BigDecimal("200000.00"), BigDecimal.ZERO),
                clock
        );
        wallet = wallet("100000.00");
    }

    @Test
    void should_pay_a_withdrawal_below_the_threshold() {
        when(withdrawalRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        when(walletRepository.findWithLockByUserId(USER)).thenReturn(Optional.of(wallet));
        when(payoutExecutor.pay(any())).thenReturn(new PayoutOutcome("PAID", "SIM-1", null));

        WithdrawalDto result = service.create(USER, new WithdrawalRequest("50000.00"), KEY);

        assertThat(result.status()).isEqualTo("PAID");
        assertThat(result.amountCop()).isEqualTo("50000.00");
        verify(payoutExecutor).hold(any(), any());
        verify(payoutExecutor).pay(any());
    }

    @Test
    void should_keep_the_hold_when_the_amount_requires_approval() {
        wallet = wallet("300000.00");
        when(withdrawalRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        when(walletRepository.findWithLockByUserId(USER)).thenReturn(Optional.of(wallet));

        WithdrawalDto result = service.create(USER, new WithdrawalRequest("250000.00"), KEY);

        assertThat(result.status()).isEqualTo("PENDING_APPROVAL");
        assertThat(result.message()).isEqualTo("Tu retiro quedó en revisión.");
        verify(payoutExecutor).hold(any(), any());
        verify(payoutExecutor, never()).pay(any());
    }

    @Test
    void should_reject_a_withdrawal_above_the_available_balance() {
        wallet = wallet("10000.00");
        when(withdrawalRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        when(walletRepository.findWithLockByUserId(USER)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> service.create(USER, new WithdrawalRequest("50000.00"), KEY))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Saldo insuficiente");

        verify(withdrawalRepository, never()).save(any());
        verify(payoutExecutor, never()).hold(any(), any());
    }

    @Test
    void should_return_the_existing_withdrawal_for_the_same_idempotency_key() {
        when(withdrawalRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.of(pendingApproval()));

        WithdrawalDto result = service.create(USER, new WithdrawalRequest("250000.00"), KEY);

        assertThat(result.id()).isEqualTo(WITHDRAWAL_ID);
        assertThat(result.status()).isEqualTo("PENDING_APPROVAL");
        verify(walletRepository, never()).findWithLockByUserId(any());
        verify(payoutExecutor, never()).pay(any());
    }

    @Test
    void should_pay_a_withdrawal_after_admin_approval() {
        WithdrawalEntity withdrawal = pendingApproval();
        when(withdrawalRepository.findWithLockById(WITHDRAWAL_ID)).thenReturn(Optional.of(withdrawal));
        when(payoutExecutor.pay(any())).thenReturn(new PayoutOutcome("PAID", "SIM-2", null));

        WithdrawalDto result = service.approve(WITHDRAWAL_ID, "admin-1");

        assertThat(result.status()).isEqualTo("PAID");
        assertThat(withdrawal.getApprovedBy()).isEqualTo("admin-1");
        verify(payoutExecutor).pay(withdrawal);
    }

    @Test
    void should_release_the_hold_when_admin_rejects() {
        WithdrawalEntity withdrawal = pendingApproval();
        when(withdrawalRepository.findWithLockById(WITHDRAWAL_ID)).thenReturn(Optional.of(withdrawal));
        when(payoutExecutor.release(any(), eq("REJECTED"), anyString()))
                .thenReturn(new PayoutOutcome("REJECTED", null, "Datos incompletos"));

        WithdrawalDto result = service.reject(WITHDRAWAL_ID, "admin-1", "Datos incompletos");

        assertThat(result.status()).isEqualTo("REJECTED");
        assertThat(result.message()).isEqualTo("Datos incompletos");
        verify(payoutExecutor).release(withdrawal, "REJECTED", "Datos incompletos");
        verify(payoutExecutor, never()).pay(any());
    }

    @Test
    void should_keep_processing_when_the_payout_is_pending() {
        when(withdrawalRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        when(walletRepository.findWithLockByUserId(USER)).thenReturn(Optional.of(wallet));
        when(payoutExecutor.pay(any())).thenReturn(new PayoutOutcome("PROCESSING", "POP-1", null));

        WithdrawalDto result = service.create(USER, new WithdrawalRequest("50000.00"), KEY);

        assertThat(result.status()).isEqualTo("PROCESSING");
    }

    private WalletEntity wallet(String balance) {
        WalletEntity entity = new WalletEntity();
        entity.setUserId(USER);
        entity.setBalance(new BigDecimal(balance));
        entity.setHeldBalance(new BigDecimal("0.00"));
        return entity;
    }

    private WithdrawalEntity pendingApproval() {
        WithdrawalEntity entity = new WithdrawalEntity();
        entity.setId(WITHDRAWAL_ID);
        entity.setUserId(USER);
        entity.setAmountCop(new BigDecimal("250000.00"));
        entity.setStatus("PENDING_APPROVAL");
        entity.setProvider("SIMULATOR");
        entity.setIdempotencyKey(KEY);
        entity.setCreatedAt(Instant.parse("2026-11-13T19:00:00Z"));
        entity.setUpdatedAt(Instant.parse("2026-11-13T19:00:00Z"));
        return entity;
    }
}
