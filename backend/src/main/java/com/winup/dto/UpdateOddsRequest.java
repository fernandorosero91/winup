package com.winup.dto;

import java.math.BigDecimal;

public record UpdateOddsRequest(
        BigDecimal localOdds,
        BigDecimal drawOdds,
        BigDecimal awayOdds
) {
}
