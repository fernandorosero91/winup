package com.winup.repository;

import com.winup.entity.MatchOddsSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MatchOddsSnapshotRepository extends JpaRepository<MatchOddsSnapshotEntity, UUID> {
    Optional<MatchOddsSnapshotEntity> findTopByMatchIdOrderByCapturedAtDesc(UUID matchId);
}
