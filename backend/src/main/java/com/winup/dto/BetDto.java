package com.winup.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.util.UUID;

public record BetDto(
        UUID id,
        String match,
        String market,
        String selection,
        @JsonSerialize(using = ToStringSerializer.class) BigDecimal stake,
        BigDecimal odds,
        @JsonSerialize(using = ToStringSerializer.class) BigDecimal potential,
        String status,
        String placedAt
) {
}
