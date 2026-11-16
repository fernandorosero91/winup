package com.winup.model.match;

import java.math.BigDecimal;

public interface PayoutStrategy {
    Dinero calcular(Dinero stake, BigDecimal odds);
}
