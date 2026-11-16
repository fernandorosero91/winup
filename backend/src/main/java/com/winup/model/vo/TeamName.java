package com.winup.model.vo;

import com.winup.exception.ValidationException;

public record TeamName(String value) {
    private static final int MAX_LENGTH = 80;

    public TeamName {
        if (value == null || value.isBlank()) {
            throw new ValidationException("INVALID_TEAM", "El nombre del equipo es obligatorio");
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_LENGTH) {
            throw new ValidationException("INVALID_TEAM", "El nombre del equipo es demasiado largo");
        }
        value = normalized;
    }

    public boolean sameAs(TeamName other) {
        return other != null && value.equalsIgnoreCase(other.value);
    }
}
