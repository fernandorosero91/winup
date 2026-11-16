package com.winup.service;

import com.winup.entity.LedgerEntryEntity;
import com.winup.entity.PaymentIntentEntity;
import com.winup.entity.WithdrawalEntity;
import com.winup.repository.LedgerEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {
    private static final UUID TRANSACTION = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final Instant NOW = Instant.parse("2026-11-13T20:00:00Z");

    @Mock
    private LedgerEntryRepository ledgerRepository;

    @InjectMocks
    private LedgerService ledgerService;

    @Test
    void should_record_a_balanced_deposit() {
        ledgerService.recordDeposit(intent(), new BigDecimal("50000.00"), NOW);

        List<LedgerEntryEntity> entries = capturedEntries();
        assertThat(entries).hasSize(2);
        assertThat(account(entries, "PROVIDER_CLEARING:SIMULATOR").getDebit()).isEqualByComparingTo("50000.00");
        assertThat(account(entries, "USER_AVAILABLE:user-1").getCredit()).isEqualByComparingTo("50000.00");
        assertBalanced(entries);
    }

    @Test
    void should_record_a_balanced_withdrawal_hold() {
        ledgerService.recordWithdrawalHold(withdrawal(), new BigDecimal("30000.00"), NOW);

        List<LedgerEntryEntity> entries = capturedEntries();
        assertThat(account(entries, "USER_AVAILABLE:user-1").getDebit()).isEqualByComparingTo("30000.00");
        assertThat(account(entries, "USER_HELD:user-1").getCredit()).isEqualByComparingTo("30000.00");
        assertBalanced(entries);
    }

    @Test
    void should_record_a_balanced_withdrawal_payout() {
        ledgerService.recordWithdrawalPaid(withdrawal(), new BigDecimal("30000.00"), NOW);

        List<LedgerEntryEntity> entries = capturedEntries();
        assertThat(account(entries, "USER_HELD:user-1").getDebit()).isEqualByComparingTo("30000.00");
        assertThat(account(entries, "PROVIDER_CLEARING:SIMULATOR").getCredit()).isEqualByComparingTo("30000.00");
        assertBalanced(entries);
    }

    @Test
    void should_record_a_balanced_withdrawal_release() {
        ledgerService.recordWithdrawalRelease(withdrawal(), new BigDecimal("30000.00"), NOW);

        List<LedgerEntryEntity> entries = capturedEntries();
        assertThat(account(entries, "USER_HELD:user-1").getDebit()).isEqualByComparingTo("30000.00");
        assertThat(account(entries, "USER_AVAILABLE:user-1").getCredit()).isEqualByComparingTo("30000.00");
        assertBalanced(entries);
    }

    private List<LedgerEntryEntity> capturedEntries() {
        ArgumentCaptor<LedgerEntryEntity> entries = ArgumentCaptor.forClass(LedgerEntryEntity.class);
        verify(ledgerRepository, times(2)).save(entries.capture());
        return entries.getAllValues();
    }

    private LedgerEntryEntity account(List<LedgerEntryEntity> entries, String account) {
        return entries.stream()
                .filter(entry -> account.equals(entry.getAccount()))
                .findFirst()
                .orElseThrow();
    }

    private void assertBalanced(List<LedgerEntryEntity> entries) {
        BigDecimal debits = entries.stream().map(LedgerEntryEntity::getDebit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credits = entries.stream().map(LedgerEntryEntity::getCredit).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(debits).isEqualByComparingTo(credits);
    }

    private PaymentIntentEntity intent() {
        PaymentIntentEntity intent = new PaymentIntentEntity();
        intent.setId(TRANSACTION);
        intent.setUserId("user-1");
        intent.setProvider("SIMULATOR");
        return intent;
    }

    private WithdrawalEntity withdrawal() {
        WithdrawalEntity withdrawal = new WithdrawalEntity();
        withdrawal.setId(TRANSACTION);
        withdrawal.setUserId("user-1");
        withdrawal.setProvider("SIMULATOR");
        return withdrawal;
    }
}
