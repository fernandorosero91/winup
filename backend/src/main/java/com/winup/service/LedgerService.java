package com.winup.service;

import com.winup.entity.LedgerEntryEntity;
import com.winup.entity.PaymentIntentEntity;
import com.winup.entity.WithdrawalEntity;
import com.winup.repository.LedgerEntryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class LedgerService {
    private static final String PROVIDER_CLEARING = "PROVIDER_CLEARING:";
    private static final String USER_AVAILABLE = "USER_AVAILABLE:";
    private static final String USER_HELD = "USER_HELD:";

    private final LedgerEntryRepository ledgerRepository;

    public LedgerService(LedgerEntryRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    public void recordDeposit(PaymentIntentEntity intent, BigDecimal amount, Instant now) {
        transfer(
                new EntryContext(intent.getId(), "Depósito", "PAYMENT_INTENT", intent.getId().toString(), now),
                PROVIDER_CLEARING + intent.getProvider(),
                USER_AVAILABLE + intent.getUserId(),
                amount
        );
    }

    public void recordWithdrawalHold(WithdrawalEntity withdrawal, BigDecimal amount, Instant now) {
        transfer(
                new EntryContext(withdrawal.getId(), "Retiro retenido", "WITHDRAWAL", withdrawal.getId().toString(), now),
                USER_AVAILABLE + withdrawal.getUserId(),
                USER_HELD + withdrawal.getUserId(),
                amount
        );
    }

    public void recordWithdrawalPaid(WithdrawalEntity withdrawal, BigDecimal amount, Instant now) {
        transfer(
                new EntryContext(withdrawal.getId(), "Retiro pagado", "WITHDRAWAL", withdrawal.getId().toString(), now),
                USER_HELD + withdrawal.getUserId(),
                PROVIDER_CLEARING + withdrawal.getProvider(),
                amount
        );
    }

    public void recordWithdrawalRelease(WithdrawalEntity withdrawal, BigDecimal amount, Instant now) {
        transfer(
                new EntryContext(withdrawal.getId(), "Retiro liberado", "WITHDRAWAL", withdrawal.getId().toString(), now),
                USER_HELD + withdrawal.getUserId(),
                USER_AVAILABLE + withdrawal.getUserId(),
                amount
        );
    }

    private void transfer(EntryContext context, String debitAccount, String creditAccount, BigDecimal amount) {
        BigDecimal normalized = amount.setScale(2, java.math.RoundingMode.HALF_EVEN);
        save(context, debitAccount, normalized, BigDecimal.ZERO.setScale(2));
        save(context, creditAccount, BigDecimal.ZERO.setScale(2), normalized);
    }

    private void save(EntryContext context, String account, BigDecimal debit, BigDecimal credit) {
        LedgerEntryEntity entry = new LedgerEntryEntity();
        entry.setId(UUID.randomUUID());
        entry.setTransactionId(context.transactionId());
        entry.setAccount(account);
        entry.setDebit(debit);
        entry.setCredit(credit);
        entry.setCurrency("COP");
        entry.setConcept(context.concept());
        entry.setRefType(context.refType());
        entry.setRefId(context.refId());
        entry.setCreatedAt(context.now());
        ledgerRepository.save(entry);
    }

    private record EntryContext(UUID transactionId, String concept, String refType, String refId, Instant now) {
    }
}
