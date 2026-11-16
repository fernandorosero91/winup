package com.winup.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.util.UUID;

public record NotificationDto(
        UUID id,
        String kind,
        String title,
        String detail,
        @JsonSerialize(using = ToStringSerializer.class) BigDecimal amount,
        String date,
        UUID betId,
        UUID matchId
) {
}
