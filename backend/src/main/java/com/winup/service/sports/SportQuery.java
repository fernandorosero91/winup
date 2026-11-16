package com.winup.service.sports;

import java.time.Instant;
import java.util.Objects;

public record SportQuery(String sport, Instant from, Instant to, boolean liveOnly) {
    public SportQuery {
        Objects.requireNonNull(sport, "El deporte es obligatorio");
        Objects.requireNonNull(from, "El inicio es obligatorio");
        Objects.requireNonNull(to, "El fin es obligatorio");
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("La ventana de consulta es inválida");
        }
    }

    public boolean matchesSport(String candidate) {
        return "TODOS".equalsIgnoreCase(sport) || sport.equalsIgnoreCase(candidate);
    }
}
