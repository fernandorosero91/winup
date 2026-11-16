package com.winup.model.vo;

import com.winup.exception.ValidationException;

public record DisplayName(String value) {
    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 80;

    public DisplayName {
        if (value == null) {
            throw new ValidationException("INVALID_NAME", "El nombre es obligatorio");
        }
        String normalized = value.trim();
        if (normalized.length() < MIN_LENGTH || normalized.length() > MAX_LENGTH) {
            throw new ValidationException(
                    "INVALID_NAME",
                    "El nombre debe tener entre 2 y 80 caracteres"
            );
        }
        value = normalized;
    }
}
