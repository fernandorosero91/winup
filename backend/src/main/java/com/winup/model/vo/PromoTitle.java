package com.winup.model.vo;

import com.winup.exception.ValidationException;

public record PromoTitle(String value) {
    private static final int MAX_LENGTH = 140;

    public PromoTitle {
        if (value == null || value.isBlank()) {
            throw new ValidationException("INVALID_PROMO", "El título es obligatorio");
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_LENGTH) {
            throw new ValidationException("INVALID_PROMO", "El título es demasiado largo");
        }
        value = normalized;
    }
}
