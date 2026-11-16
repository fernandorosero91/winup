package com.winup.model.vo;

import java.util.Objects;
import java.util.UUID;

public record MatchId(UUID value) {
    public MatchId {
        Objects.requireNonNull(value, "El id del partido es obligatorio");
    }

    public static MatchId of(String value) {
        return new MatchId(UUID.fromString(value));
    }
}
