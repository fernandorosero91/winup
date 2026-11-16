package com.winup.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.math.RoundingMode

data class Match(
    val id: String,
    val sport: String,
    val league: String,
    val home: String,
    val away: String,
    val homeShort: String,
    val awayShort: String,
    val homeScore: Int,
    val awayScore: Int,
    val minute: Int,
    val isLive: Boolean,
    val status: String,
    val startsAt: String,
    val localOdds: Double,
    val drawOdds: Double,
    val awayOdds: Double,
    val drawMarket: Boolean,
    val source: String = "RANDOM",
    val isSimulated: Boolean = true,
)

data class Bet(
    val id: String,
    val match: String,
    val market: String,
    val selection: String,
    val stake: BigDecimal,
    val odds: Double,
    val potential: BigDecimal,
    val status: String,
)

data class NotificationItem(
    val id: String,
    val kind: String,
    val title: String,
    val detail: String,
    val date: String,
)

data class Movement(
    val id: String,
    val concept: String,
    val date: String,
    val amount: BigDecimal,
    val isIncome: Boolean,
)

object WinUpData {
    val matches = mutableStateListOf<Match>()
    val bets = mutableStateListOf<Bet>()
    val movements = mutableStateListOf<Movement>()
    val notifications = mutableStateListOf<NotificationItem>()
    val readNotifications = mutableStateListOf<String>()
    var balance by mutableStateOf(BigDecimal.ZERO)
    var heldBalance by mutableStateOf(BigDecimal.ZERO)
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var sportFilter by mutableStateOf("TODOS")

    suspend fun selectSport(api: WinUpApiService, sport: String) {
        if (sportFilter == sport) {
            return
        }
        sportFilter = sport
        refresh(api)
    }

    suspend fun refresh(api: WinUpApiService) {
        loading = true
        try {
            val page = api.matches(status = "EN VIVO", sport = sportFilter, page = 0, size = 50)
            matches.clear()
            matches.addAll(page.items.map { it.toUi() })

            val betsPage = api.bets()
            bets.clear()
            bets.addAll(betsPage.map { it.toUi() })

            refreshNotifications(api)

            val wallet = api.wallet()
            balance = wallet.balance.toBigDecimal()
            heldBalance = wallet.heldBalance.toBigDecimal()
            movements.clear()
            movements.addAll(wallet.movements.map { it.toUi() })
            error = null
        } catch (exception: Exception) {
            error = exception.message ?: "No se pudieron cargar los datos"
        } finally {
            loading = false
        }
    }

    suspend fun refreshNotifications(api: WinUpApiService) {
        val items = api.notifications()
        notifications.clear()
        notifications.addAll(items.map {
            NotificationItem(it.id, it.kind, it.title, it.detail, it.date)
        })
        val read = api.readNotifications()
        readNotifications.clear()
        readNotifications.addAll(read)
    }

    suspend fun markNotificationRead(api: WinUpApiService, id: String) {
        if (id in readNotifications) {
            return
        }
        readNotifications.add(id)
        api.markNotificationsRead(NotificationReadRequest(listOf(id)))
    }

    suspend fun markAllNotificationsRead(api: WinUpApiService) {
        val pending = notifications.map { it.id }.filter { it !in readNotifications }
        if (pending.isEmpty()) {
            return
        }
        readNotifications.addAll(pending)
        api.markNotificationsRead(NotificationReadRequest(pending))
    }

    fun unreadNotifications(): Int = notifications.count { it.id !in readNotifications }

    suspend fun placeBet(
        api: WinUpApiService,
        matchId: String,
        selection: String,
        stake: BigDecimal,
    ): String? {
        return try {
            api.placeBet(
                idempotencyKey = java.util.UUID.randomUUID().toString(),
                request = PlaceBetRequest(
                    matchId = matchId,
                    selection = selection,
                    stake = stake.setScale(2, RoundingMode.HALF_EVEN).toPlainString(),
                ),
            )
            refresh(api)
            null
        } catch (exception: Exception) {
            errorMessage(exception)
        }
    }

    suspend fun deposit(api: WinUpApiService, amount: BigDecimal): DepositResult {
        return try {
            val deposit = api.deposit(
                idempotencyKey = java.util.UUID.randomUUID().toString(),
                request = DepositRequest(
                    amount = amount.setScale(2, RoundingMode.HALF_EVEN).toPlainString(),
                    returnUrl = DepositFlow.returnUrl(),
                ),
            )
            val checkout = deposit.checkoutUrl
            if (!checkout.isNullOrBlank()) {
                DepositFlow.start(deposit.id)
                DepositResult.Checkout(deposit.id, checkout)
            } else {
                api.simulateDeposit(deposit.id, "APPROVED")
                refresh(api)
                DepositResult.Completed
            }
        } catch (exception: Exception) {
            DepositResult.Failed(errorMessage(exception, "No se pudo crear el depósito"))
        }
    }

    suspend fun withdraw(api: WinUpApiService, amount: BigDecimal): WithdrawalResult {
        return try {
            val withdrawal = api.withdraw(
                idempotencyKey = java.util.UUID.randomUUID().toString(),
                request = WithdrawalRequest(amount.setScale(2, RoundingMode.HALF_EVEN).toPlainString()),
            )
            refresh(api)
            withdrawal.toResult()
        } catch (exception: Exception) {
            WithdrawalResult.Failed(errorMessage(exception, "No se pudo crear el retiro"))
        }
    }

    suspend fun depositStatus(api: WinUpApiService, intentId: String): String? {
        return try {
            api.depositIntent(intentId).status
        } catch (exception: Exception) {
            null
        }
    }

    fun applyRealtime(event: RealtimeEvent) {
        val index = matches.indexOfFirst { it.id == event.matchId }
        if (index < 0) {
            return
        }
        val current = matches[index]
        matches[index] = current.copy(
            homeScore = event.homeScore,
            awayScore = event.awayScore,
            minute = event.minute,
            isLive = event.status == "OPEN",
            status = if (event.status == "CLOSED") "FINALIZADO" else current.status,
        )
    }

    fun clear() {
        matches.clear()
        bets.clear()
        movements.clear()
        notifications.clear()
        readNotifications.clear()
        balance = BigDecimal.ZERO
        heldBalance = BigDecimal.ZERO
        error = null
        sportFilter = "TODOS"
    }

    private fun errorMessage(
        exception: Exception,
        fallback: String = "No se pudo registrar la apuesta",
    ): String {
        val http = exception as? retrofit2.HttpException
        if (http != null) {
            val body = http.response()?.errorBody()?.string()
            if (!body.isNullOrBlank()) {
                return try {
                    Json { ignoreUnknownKeys = true }
                        .decodeFromString(ErrorDto.serializer(), body)
                        .message
                } catch (ignored: Exception) {
                    fallback
                }
            }
        }
        return exception.message ?: fallback
    }
}
