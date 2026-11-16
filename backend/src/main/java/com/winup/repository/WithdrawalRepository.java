package com.winup.repository;

import com.winup.entity.WithdrawalEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WithdrawalRepository extends JpaRepository<WithdrawalEntity, UUID> {
    Optional<WithdrawalEntity> findByIdempotencyKey(String idempotencyKey);

    List<WithdrawalEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    List<WithdrawalEntity> findAllByOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WithdrawalEntity w where w.id = :id")
    Optional<WithdrawalEntity> findWithLockById(@Param("id") UUID id);

    @Query("""
            select coalesce(sum(w.amountCop), 0) from WithdrawalEntity w
            where w.userId = :userId and w.createdAt >= :since
            and w.status in ('PENDING_APPROVAL', 'PROCESSING', 'PAID')
            """)
    BigDecimal sumActiveSince(@Param("userId") String userId, @Param("since") Instant since);
}
