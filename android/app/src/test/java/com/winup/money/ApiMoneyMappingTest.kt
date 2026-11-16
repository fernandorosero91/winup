package com.winup.money

import com.winup.data.BetDto
import com.winup.data.MovementDto
import com.winup.data.MovementPageDto
import com.winup.data.WalletDto
import com.winup.data.toUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.math.BigDecimal

class ApiMoneyMappingTest {
    @Test
    fun maps_bet_money_to_big_decimal() {
        val bet = BetDto(
            id = "b-1",
            match = "Liverpool vs Arsenal",
            market = "Resultado final",
            selection = "Liverpool",
            stake = "50000.00",
            odds = 2.1,
            potential = "105000.00",
            status = "PENDIENTE",
            placedAt = "10:00",
        ).toUi()

        assertEquals(BigDecimal("50000.00"), bet.stake)
        assertEquals(BigDecimal("105000.00"), bet.potential)
    }

    @Test
    fun maps_wallet_balance_and_movement_to_big_decimal() {
        val wallet = WalletDto(
            balance = "100000.00",
            currency = "COP",
            income = "150000.00",
            expenses = "50000.00",
            movementCount = 2,
        )
        val page = MovementPageDto(
            items = listOf(
                MovementDto(
                    id = "m-1",
                    concept = "Apuesta",
                    date = "10:00",
                    amount = "-5000.00",
                    type = "GASTO",
                ),
            ),
            page = 0,
            size = 20,
            totalElements = 1,
            totalPages = 1,
            hasNext = false,
        )

        assertEquals(BigDecimal("100000.00"), wallet.balance.toBigDecimal())
        val movement = page.items.first().toUi()
        assertEquals(BigDecimal("-5000.00"), movement.amount)
        assertFalse(movement.isIncome)
    }
}
