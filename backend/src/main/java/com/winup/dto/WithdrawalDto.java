package com.winup.dto;

import java.util.UUID;

public record WithdrawalDto(
        UUID id,
        String userId,
        String status,
        String amountCop,
        String provider,
        String message,
        String createdAt
) {
}
