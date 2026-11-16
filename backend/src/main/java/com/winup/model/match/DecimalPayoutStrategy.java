package com.winup.model.match;
import com.winup.model.vo.Dinero;


import java.math.BigDecimal;
import java.math.RoundingMode;

public final class DecimalPayoutStrategy implements PayoutStrategy {
    @Override
    public Dinero calcular(Dinero stake, BigDecimal odds) {
        return new Dinero(stake.amount().multiply(odds).setScale(2, RoundingMode.HALF_EVEN));
    }
}
