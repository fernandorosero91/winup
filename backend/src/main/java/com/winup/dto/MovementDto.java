package com.winup.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.util.UUID;

public record MovementDto(
        UUID id,
        String concept,
        String date,
        @JsonSerialize(using = ToStringSerializer.class) BigDecimal amount,
        String type
) {
}
