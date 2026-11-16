package com.winup.model.vo;

import com.winup.exception.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Stake(BigDecimal amount) {
    private static final BigDecimal MIN = new BigDecimal("1000.00");
    private static final BigDecimal MAX = new BigDecimal("500000.00");

    public Stake {
        if (amount == null) {
            throw new ValidationException("INVALID_STAKE", "El importe es obligatorio");
        }
        BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_EVEN);
        if (normalized.compareTo(MIN) < 0 || normalized.compareTo(MAX) > 0) {
            throw new ValidationException("INVALID_STAKE", "Importe fuera de rango");
        }
        amount = normalized;
    }
}
