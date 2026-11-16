package com.winup.model.vo;

import com.winup.exception.ValidationException;

public record PromoDetail(String value) {
    private static final int MAX_LENGTH = 280;

    public PromoDetail {
        if (value == null || value.isBlank()) {
            throw new ValidationException("INVALID_PROMO", "El mensaje es obligatorio");
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_LENGTH) {
            throw new ValidationException("INVALID_PROMO", "El mensaje es demasiado largo");
        }
        value = normalized;
    }
}
