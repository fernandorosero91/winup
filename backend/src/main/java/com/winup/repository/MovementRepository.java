package com.winup.repository;

import com.winup.entity.MovementEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface MovementRepository extends JpaRepository<MovementEntity, UUID> {
    List<MovementEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    Page<MovementEntity> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    long countByUserId(String userId);

    @Query("select coalesce(sum(m.amount), 0) from MovementEntity m where m.userId = :userId and m.amount > 0")
    BigDecimal totalIncome(@Param("userId") String userId);

    @Query("select coalesce(sum(m.amount), 0) from MovementEntity m where m.userId = :userId and m.amount < 0")
    BigDecimal totalExpenses(@Param("userId") String userId);
}
