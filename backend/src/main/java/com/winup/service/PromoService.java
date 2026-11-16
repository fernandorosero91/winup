package com.winup.service;

import com.winup.entity.PromoCampaignEntity;
import com.winup.dto.CreatePromoRequest;
import com.winup.dto.PromoCampaignDto;
import com.winup.model.vo.PromoDetail;
import com.winup.model.vo.PromoTitle;
import com.winup.repository.PromoCampaignRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PromoService {
    private final PromoCampaignRepository repository;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    public PromoService(
            PromoCampaignRepository repository,
            EventPublisher eventPublisher,
            Clock clock
    ) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    public List<PromoCampaignDto> list() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public PromoCampaignDto create(CreatePromoRequest request, String createdBy) {
        String title = new PromoTitle(request.title()).value();
        String detail = new PromoDetail(request.detail()).value();
        Instant now = clock.instant();
        Instant scheduledAt = request.scheduledAt() == null ? now : request.scheduledAt();

        PromoCampaignEntity campaign = new PromoCampaignEntity();
        campaign.setId(UUID.randomUUID());
        campaign.setTitle(title);
        campaign.setDetail(detail);
        campaign.setScheduledAt(scheduledAt);
        campaign.setCreatedAt(now);
        campaign.setCreatedBy(createdBy);

        if (scheduledAt.isAfter(now)) {
            campaign.setStatus("SCHEDULED");
            return toDto(repository.save(campaign));
        }

        campaign.setStatus("SENT");
        campaign.setSentAt(now);
        PromoCampaignEntity saved = repository.save(campaign);
        eventPublisher.promotion(title, detail);
        return toDto(saved);
    }

    @Transactional
    public void dispatchDue() {
        Instant now = clock.instant();
        List<PromoCampaignEntity> due = repository
                .findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc("SCHEDULED", now);

        for (PromoCampaignEntity campaign : due) {
            if (repository.markSent(campaign.getId(), now) == 1) {
                eventPublisher.promotion(campaign.getTitle(), campaign.getDetail());
            }
        }
    }

    @Transactional
    public void cancel(UUID id) {
        if (repository.cancel(id) == 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Solo se pueden cancelar promociones programadas"
            );
        }
    }

    private PromoCampaignDto toDto(PromoCampaignEntity campaign) {
        return new PromoCampaignDto(
                campaign.getId(),
                campaign.getTitle(),
                campaign.getDetail(),
                campaign.getStatus(),
                campaign.getScheduledAt(),
                campaign.getCreatedAt(),
                campaign.getSentAt()
        );
    }
}
