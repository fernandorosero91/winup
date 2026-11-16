package com.winup.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PlaceBetRequest(UUID matchId, String selection, BigDecimal stake) {
}
