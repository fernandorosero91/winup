package com.winup.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.util.List;

public record WalletDto(
        @JsonSerialize(using = ToStringSerializer.class) BigDecimal balance,
        @JsonSerialize(using = ToStringSerializer.class) BigDecimal heldBalance,
        String currency,
        List<MovementDto> movements
) {
}
