package com.winup.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminPaymentDto(
        UUID id,
        String userId,
        String status,
        @JsonSerialize(using = ToStringSerializer.class) BigDecimal amountCop,
        String provider,
        Instant createdAt
) {
}
