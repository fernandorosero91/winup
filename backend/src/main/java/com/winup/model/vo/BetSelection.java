package com.winup.model.vo;

import com.winup.exception.ValidationException;

import java.util.Locale;

public enum BetSelection {
    LOCAL,
    EMPATE,
    VISITANTE;

    public static BetSelection from(String value) {
        if (value == null) {
            throw new ValidationException("INVALID_SELECTION", "La selección es obligatoria");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ValidationException("INVALID_SELECTION", "Selección inválida");
        }
    }
}
