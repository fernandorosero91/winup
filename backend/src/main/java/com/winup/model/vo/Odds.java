package com.winup.model.vo;

import com.winup.exception.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Odds(BigDecimal value) {
    private static final BigDecimal MIN = new BigDecimal("1.01");
    private static final BigDecimal MAX = new BigDecimal("100.00");

    public Odds {
        if (value == null) {
            throw new ValidationException("INVALID_ODDS", "La cuota es obligatoria");
        }
        BigDecimal normalized = value.setScale(2, RoundingMode.HALF_EVEN);
        if (normalized.compareTo(MIN) < 0 || normalized.compareTo(MAX) > 0) {
            throw new ValidationException("INVALID_ODDS", "La cuota debe estar entre 1.01 y 100.00");
        }
        value = normalized;
    }
}
