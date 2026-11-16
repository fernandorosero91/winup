package com.winup.repository;

import com.winup.entity.LedgerEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntryEntity, UUID> {
    List<LedgerEntryEntity> findByTransactionId(UUID transactionId);

    List<LedgerEntryEntity> findTop100ByOrderByCreatedAtDesc();

    @Query("select coalesce(sum(l.debit), 0) from LedgerEntryEntity l")
    BigDecimal sumDebits();

    @Query("select coalesce(sum(l.credit), 0) from LedgerEntryEntity l")
    BigDecimal sumCredits();
}
