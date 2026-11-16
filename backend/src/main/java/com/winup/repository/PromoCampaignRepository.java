package com.winup.repository;

import com.winup.entity.PromoCampaignEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PromoCampaignRepository extends JpaRepository<PromoCampaignEntity, UUID> {
    List<PromoCampaignEntity> findAllByOrderByCreatedAtDesc();

    List<PromoCampaignEntity> findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
            String status,
            Instant now
    );

    @Modifying
    @Query("""
            update PromoCampaignEntity p
            set p.status = 'SENT', p.sentAt = :now
            where p.id = :id and p.status = 'SCHEDULED'
            """)
    int markSent(@Param("id") UUID id, @Param("now") Instant now);

    @Modifying
    @Query("""
            update PromoCampaignEntity p
            set p.status = 'CANCELLED'
            where p.id = :id and p.status = 'SCHEDULED'
            """)
    int cancel(@Param("id") UUID id);
}
