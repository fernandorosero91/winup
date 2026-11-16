package com.winup.service.sports;

import java.util.Objects;

public record ProviderTeam(String name, String shortName) {
    public ProviderTeam {
        Objects.requireNonNull(name, "El nombre del equipo es obligatorio");
    }
}
