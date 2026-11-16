package com.winup.model.match;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Dinero(BigDecimal amount) {
    public Dinero {
        Objects.requireNonNull(amount, "El importe es obligatorio");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("El importe no puede ser negativo");
        }
        amount = amount.setScale(2, RoundingMode.HALF_EVEN);
    }

    public Dinero mas(Dinero other) {
        return new Dinero(amount.add(other.amount));
    }

    public Dinero por(BigDecimal factor) {
        return new Dinero(amount.multiply(factor));
    }
}
