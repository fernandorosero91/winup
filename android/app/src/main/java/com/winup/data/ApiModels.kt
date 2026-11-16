package com.winup.data

import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String,
)

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val displayName: String,
    val role: String = "USER",
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: UserDto,
)

@Serializable
data class OddsDto(
    val local: Double,
    val draw: Double,
    val away: Double,
)

@Serializable
data class MatchDto(
    val id: String,
    val sport: String = "Fútbol",
    val league: String,
    val home: String,
    val away: String,
    val homeShort: String,
    val awayShort: String,
    val homeScore: Int,
    val awayScore: Int,
    val minute: Int,
    val status: String,
    val startsAt: String,
    val odds: OddsDto,
    val drawMarket: Boolean = true,
    val source: String = "RANDOM",
    val isSimulated: Boolean = true,
)

@Serializable
data class MatchPageDto(
    val items: List<MatchDto>,
    val page: Int = 0,
    val size: Int = 0,
    val total: Long = 0,
    val hasMore: Boolean = false,
)

@Serializable
data class BetOutcomeDto(
    val winner: String? = null,
    val winnerName: String? = null,
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    val finished: Boolean = false,
)

@Serializable
data class BetDto(
    val id: String,
    val match: String,
    val market: String,
    val selection: String,
    val stake: String,
    val odds: Double,
    val potential: String,
    val status: String,
    val placedAt: String,
    val outcome: BetOutcomeDto? = null,
)

@Serializable
data class MovementDto(
    val id: String,
    val concept: String,
    val date: String,
    val amount: String,
    val type: String,
)

@Serializable
data class WalletDto(
    val balance: String,
    val heldBalance: String = "0.00",
    val currency: String = "COP",
    val movements: List<MovementDto>,
)

@Serializable
data class PlaceBetRequest(
    val matchId: String,
    val selection: String,
    val stake: String,
)

@Serializable
data class DepositRequest(val amount: String, val returnUrl: String? = null)

@Serializable
data class WithdrawalRequest(val amount: String)

@Serializable
data class WithdrawalDto(
    val id: String,
    val status: String,
    val amountCop: String,
    val provider: String,
    val message: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class DepositDto(
    val id: String,
    val status: String,
    val amountCop: String,
    val checkoutUrl: String? = null,
    val provider: String,
)

@Serializable
data class NotificationDto(
    val id: String,
    val kind: String,
    val title: String,
    val detail: String,
    val amount: String? = null,
    val date: String,
)

@Serializable
data class NotificationReadRequest(
    val ids: List<String>,
)

@Serializable
data class RealtimeEvent(
    val matchId: String,
    val status: String,
    val home: String = "",
    val away: String = "",
    val homeScore: Int = 0,
    val awayScore: Int = 0,
    val minute: Int = 0,
    val winner: String = "",
)

@Serializable
data class ErrorDto(
    val code: String = "ERROR",
    val message: String = "Error",
)

fun MatchDto.toUi(): Match = Match(
    id = id,
    sport = sport,
    league = league,
    home = home,
    away = away,
    homeShort = homeShort,
    awayShort = awayShort,
    homeScore = homeScore,
    awayScore = awayScore,
    minute = minute,
    isLive = status == "EN VIVO",
    status = status,
    startsAt = startsAt,
    localOdds = odds.local,
    drawOdds = odds.draw,
    awayOdds = odds.away,
    drawMarket = drawMarket,
    source = source,
    isSimulated = isSimulated,
)

fun BetDto.toUi(): Bet = Bet(
    id = id,
    match = match,
    market = market,
    selection = selection,
    stake = stake.toBigDecimal(),
    odds = odds,
    potential = potential.toBigDecimal(),
    status = status,
    outcome = outcome?.toUi(),
)

fun BetOutcomeDto.toUi(): BetOutcome = BetOutcome(
    winner = winner,
    winnerName = winnerName,
    homeScore = homeScore,
    awayScore = awayScore,
    finished = finished,
)

fun MovementDto.toUi(): Movement = Movement(
    id = id,
    concept = concept,
    date = date,
    amount = amount.toBigDecimal(),
    isIncome = amount.toBigDecimal().signum() >= 0,
)
