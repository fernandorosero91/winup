package com.winup.dto;

import java.time.Instant;
import java.util.UUID;

public record PromoCampaignDto(
        UUID id,
        String title,
        String detail,
        String status,
        Instant scheduledAt,
        Instant createdAt,
        Instant sentAt
) {
}
