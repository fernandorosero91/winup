package com.winup.model.vo;

import com.winup.exception.ValidationException;

public record LeagueName(String value) {
    private static final int MAX_LENGTH = 80;

    public LeagueName {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() > MAX_LENGTH) {
            throw new ValidationException("INVALID_LEAGUE", "El nombre de la liga es demasiado largo");
        }
        value = normalized;
    }

    public String orDefault(String fallback) {
        return value.isBlank() ? fallback : value;
    }
}
