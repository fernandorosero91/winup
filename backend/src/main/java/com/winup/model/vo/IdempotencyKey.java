package com.winup.model.vo;

import com.winup.exception.ValidationException;

public record IdempotencyKey(String value) {
    private static final int MAX_LENGTH = 120;

    public IdempotencyKey {
        if (value == null || value.isBlank()) {
            throw new ValidationException(
                    "INVALID_IDEMPOTENCY_KEY",
                    "La clave de idempotencia es obligatoria"
            );
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_LENGTH) {
            throw new ValidationException(
                    "INVALID_IDEMPOTENCY_KEY",
                    "La clave de idempotencia es demasiado larga"
            );
        }
        value = normalized;
    }
}
