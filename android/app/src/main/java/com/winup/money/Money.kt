package com.winup.money

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

fun formatCop(value: BigDecimal): String {
    val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    format.currency = Currency.getInstance("COP")
    format.minimumFractionDigits = 2
    format.maximumFractionDigits = 2
    return format.format(value)
}

fun formatCopWhole(value: BigDecimal): String {
    val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    format.currency = Currency.getInstance("COP")
    format.minimumFractionDigits = 0
    format.maximumFractionDigits = 0
    return format.format(value)
}
