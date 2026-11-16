package com.winup.model.vo;

import java.util.Objects;
import java.util.UUID;

public record BetId(UUID value) {
    public BetId {
        Objects.requireNonNull(value, "El id de la apuesta es obligatorio");
    }

    public static BetId of(String value) {
        return new BetId(UUID.fromString(value));
    }
}
