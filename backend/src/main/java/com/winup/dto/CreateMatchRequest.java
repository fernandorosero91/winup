package com.winup.dto;

import java.math.BigDecimal;

public record CreateMatchRequest(
        String sport,
        String league,
        String home,
        String away,
        BigDecimal localOdds,
        BigDecimal drawOdds,
        BigDecimal awayOdds,
        Integer minute
) {
}
