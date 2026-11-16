package com.winup.service.payment;

import java.math.BigDecimal;

public record ProviderPayment(
        String reference,
        String externalReference,
        BigDecimal amount,
        String currency,
        String status
) {
}
