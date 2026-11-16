package com.winup.model.match;
import com.winup.model.vo.Dinero;


import java.math.BigDecimal;

public interface PayoutStrategy {
    Dinero calcular(Dinero stake, BigDecimal odds);
}
