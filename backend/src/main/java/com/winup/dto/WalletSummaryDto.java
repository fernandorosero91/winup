package com.winup.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;

public record WalletSummaryDto(
        @JsonSerialize(using = ToStringSerializer.class) BigDecimal balance,
        @JsonSerialize(using = ToStringSerializer.class) BigDecimal heldBalance,
        String currency,
        @JsonSerialize(using = ToStringSerializer.class) BigDecimal income,
        @JsonSerialize(using = ToStringSerializer.class) BigDecimal expenses,
        long movementCount
) {
}
