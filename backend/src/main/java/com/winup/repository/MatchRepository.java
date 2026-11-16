package com.winup.repository;

import com.winup.entity.MatchEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MatchRepository extends JpaRepository<MatchEntity, UUID> {
    List<MatchEntity> findAllByOrderByCreatedAtDesc();

    long countByStatus(String status);

    long countBySport(String sport);

    long countBySportAndStatus(String sport, String status);

    long countBySportAndStatusAndMinute(String sport, String status, int minute);

    long countBySportAndStatusAndMinuteGreaterThan(String sport, String status, int minute);

    long countByStatusAndMinuteGreaterThan(String status, int minute);

    List<MatchEntity> findByStatusAndMinuteGreaterThanAndCreatedAtBefore(
            String status,
            int minute,
            Instant createdAt
    );

    List<MatchEntity> findByStatusAndMinuteAndStartsAtBefore(
            String status,
            int minute,
            Instant startsAt
    );

    @Query("select distinct m.sport from MatchEntity m where m.status = :status")
    List<String> findDistinctSportsByStatus(@Param("status") String status);

    @Query("select distinct m.sport from MatchEntity m where m.status = 'OPEN' and m.minute > 0")
    List<String> findDistinctLiveSports();

    @Query("""
            select m from MatchEntity m
            where (:sport = 'TODOS' or m.sport = :sport)
              and (
                :status = 'TODOS'
                or (:status = 'EN VIVO' and m.status = 'OPEN' and m.minute > 0)
                or (:status = 'PRÓXIMO' and m.status = 'OPEN' and m.minute = 0)
                or (:status = 'FINALIZADO' and m.status = 'CLOSED')
              )
            """)
    Page<MatchEntity> search(
            @Param("status") String status,
            @Param("sport") String sport,
            Pageable pageable
    );
}
