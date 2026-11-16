package com.winup.service.sports;

import java.math.BigDecimal;
import java.util.Objects;

public record ProviderOdds(BigDecimal local, BigDecimal draw, BigDecimal away) {
    public ProviderOdds {
        Objects.requireNonNull(local, "La cuota local es obligatoria");
        Objects.requireNonNull(away, "La cuota visitante es obligatoria");
        if (local.signum() <= 0 || away.signum() <= 0) {
            throw new IllegalArgumentException("Las cuotas deben ser positivas");
        }
        if (draw != null && draw.signum() <= 0) {
            throw new IllegalArgumentException("La cuota de empate debe ser positiva");
        }
    }
}
