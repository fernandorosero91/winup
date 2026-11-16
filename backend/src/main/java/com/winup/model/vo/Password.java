package com.winup.model.vo;

import com.winup.exception.ValidationException;

import java.util.regex.Pattern;

public record Password(String value) {
    private static final Pattern LETTER = Pattern.compile("[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]");
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 72;

    public Password {
        if (value == null || value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new ValidationException(
                    "WEAK_PASSWORD",
                    "La contraseña debe tener entre 8 y 72 caracteres"
            );
        }
        if (!LETTER.matcher(value).find()) {
            throw new ValidationException(
                    "WEAK_PASSWORD",
                    "La contraseña debe incluir al menos una letra"
            );
        }
        if (!DIGIT.matcher(value).find()) {
            throw new ValidationException(
                    "WEAK_PASSWORD",
                    "La contraseña debe incluir al menos un número"
            );
        }
    }
}
