package com.winup.model.vo;

import com.winup.exception.ValidationException;

public record PasswordAttempt(String value) {
    private static final int MAX_LENGTH = 72;

    public PasswordAttempt {
        if (value == null || value.isBlank() || value.length() > MAX_LENGTH) {
            throw new ValidationException("INVALID_PASSWORD", "La contraseña es obligatoria");
        }
    }
}
