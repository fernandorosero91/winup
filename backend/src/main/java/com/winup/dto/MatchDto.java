package com.winup.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MatchDto(
        UUID id,
        String sport,
        String league,
        String home,
        String away,
        String homeShort,
        String awayShort,
        int homeScore,
        int awayScore,
        int minute,
        String status,
        String startsAt,
        OddsDto odds,
        boolean drawMarket,
        String source,
        boolean isSimulated
) {
}
