package com.winup.model.match;

import java.util.Objects;

public record UserId(String value) {
    public UserId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El usuario es obligatorio");
        }
        value = value.trim();
    }

    public static UserId of(String value) {
        return new UserId(Objects.requireNonNull(value));
    }
}
