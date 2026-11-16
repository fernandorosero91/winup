package com.winup.service.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record PayoutOrder(
        UUID withdrawalId,
        BigDecimal amountCop,
        String currency,
        String destination
) {
}
