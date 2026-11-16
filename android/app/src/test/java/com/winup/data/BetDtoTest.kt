package com.winup.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BetDtoTest {
    @Test
    fun maps_the_final_outcome() {
        val bet = dto().toUi()

        assertTrue(bet.outcome?.finished == true)
        assertEquals("Equipo B", bet.outcome?.winnerName)
        assertEquals(1, bet.outcome?.homeScore)
        assertEquals(3, bet.outcome?.awayScore)
    }

    @Test
    fun keeps_a_pending_bet_without_outcome() {
        val bet = dto(
            status = "PENDIENTE",
            outcome = BetOutcomeDto(finished = false),
        ).toUi()

        assertFalse(bet.outcome?.finished == true)
        assertEquals(null, bet.outcome?.winnerName)
    }

    private fun dto(
        status: String = "GANADA",
        outcome: BetOutcomeDto = BetOutcomeDto(
            winner = "VISITANTE",
            winnerName = "Equipo B",
            homeScore = 1,
            awayScore = 3,
            finished = true,
        ),
    ) = BetDto(
        id = "b-1",
        match = "Equipo A vs Equipo B",
        market = "Resultado final",
        selection = "Equipo B",
        stake = "100.00",
        odds = 3.0,
        potential = "300.00",
        status = status,
        placedAt = "10:00",
        outcome = outcome,
    )
}
