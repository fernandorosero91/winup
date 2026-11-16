package com.winup.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminLedgerDto(
        UUID id,
        UUID transactionId,
        String account,
        @JsonSerialize(using = ToStringSerializer.class) BigDecimal debit,
        @JsonSerialize(using = ToStringSerializer.class) BigDecimal credit,
        String currency,
        String concept,
        Instant createdAt
) {
}
