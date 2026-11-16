package com.winup.model.vo;

import com.winup.exception.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record DepositAmount(BigDecimal amount) {
    private static final BigDecimal MIN = new BigDecimal("5000.00");
    private static final BigDecimal MAX = new BigDecimal("500000.00");

    public DepositAmount {
        if (amount == null) {
            throw new ValidationException("INVALID_DEPOSIT", "El importe es obligatorio");
        }
        BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_EVEN);
        if (normalized.compareTo(MIN) < 0 || normalized.compareTo(MAX) > 0) {
            throw new ValidationException("INVALID_DEPOSIT", "El depósito debe estar entre 5.000 y 500.000 COP");
        }
        amount = normalized;
    }

    public static DepositAmount parse(String value) {
        try {
            return new DepositAmount(new BigDecimal(value));
        } catch (NumberFormatException exception) {
            throw new ValidationException("INVALID_DEPOSIT", "El importe no es válido");
        }
    }
}
