package com.winup.model.vo;

import com.winup.exception.ValidationException;

import java.util.Locale;
import java.util.regex.Pattern;

public record Email(String value) {
    private static final Pattern FORMAT = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final int MAX_LENGTH = 160;

    public Email {
        if (value == null) {
            throw new ValidationException("INVALID_EMAIL", "El correo es obligatorio");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > MAX_LENGTH || !FORMAT.matcher(normalized).matches()) {
            throw new ValidationException("INVALID_EMAIL", "El correo no es válido");
        }
        value = normalized;
    }
}
