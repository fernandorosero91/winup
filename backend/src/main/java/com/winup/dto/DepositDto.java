package com.winup.dto;

import java.util.UUID;

public record DepositDto(
        UUID id,
        String status,
        String amountCop,
        String checkoutUrl,
        String provider
) {
}
