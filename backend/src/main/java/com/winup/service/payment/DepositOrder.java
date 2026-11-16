package com.winup.service.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record DepositOrder(
        UUID intentId,
        BigDecimal amountCop,
        String currency,
        String description,
        String returnUrl
) {
    public DepositOrder(UUID intentId, BigDecimal amountCop, String currency, String description) {
        this(intentId, amountCop, currency, description, null);
    }
}
