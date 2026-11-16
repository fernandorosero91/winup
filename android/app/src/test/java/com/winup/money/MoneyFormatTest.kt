package com.winup.money

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class MoneyFormatTest {
    @Test
    fun formats_amount_in_colombian_pesos() {
        assertEquals("\$ 100.000,00", formatCop(BigDecimal("100000.00")).replace('\u00A0', ' '))
    }

    @Test
    fun formats_zero_with_two_decimals() {
        assertEquals("\$ 0,00", formatCop(BigDecimal.ZERO).replace('\u00A0', ' '))
    }

    @Test
    fun formats_decimal_amount_with_colombian_separators() {
        assertEquals("\$ 1.234.567,50", formatCop(BigDecimal("1234567.50")).replace('\u00A0', ' '))
    }
}
