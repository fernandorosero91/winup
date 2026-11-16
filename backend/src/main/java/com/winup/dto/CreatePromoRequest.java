package com.winup.dto;

import java.time.Instant;

public record CreatePromoRequest(
        String title,
        String detail,
        Instant scheduledAt
) {
}
