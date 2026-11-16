package com.winup.model.match;
import com.winup.model.vo.BetId;
import com.winup.model.vo.Dinero;
import com.winup.model.vo.UserId;


import com.winup.model.vo.BetSelection;

import java.math.BigDecimal;
import java.util.Objects;

public record Bet(
        BetId id,
        UserId userId,
        BetSelection selection,
        Dinero stake,
        BigDecimal odds
) {
    public Bet {
        Objects.requireNonNull(id, "La apuesta necesita id");
        Objects.requireNonNull(userId, "La apuesta necesita usuario");
        Objects.requireNonNull(selection, "La apuesta necesita selección");
        Objects.requireNonNull(stake, "La apuesta necesita importe");
        Objects.requireNonNull(odds, "La apuesta necesita cuota");
        if (odds.compareTo(BigDecimal.ONE) < 0) {
            throw new IllegalArgumentException("La cuota debe ser mayor o igual a 1");
        }
    }
}
