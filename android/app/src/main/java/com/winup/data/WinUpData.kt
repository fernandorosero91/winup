package com.winup.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

data class BetOutcome(
    val winner: String?,
    val winnerName: String?,
    val homeScore: Int?,
    val awayScore: Int?,
    val finished: Boolean,
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
    val outcome: BetOutcome?,
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
    var incomeTotal by mutableStateOf(BigDecimal.ZERO)
    var expensesTotal by mutableStateOf(BigDecimal.ZERO)
    var movementPage by mutableIntStateOf(0)
    var movementTotal by mutableIntStateOf(0)
    var movementHasNext by mutableStateOf(false)
    var movementsLoading by mutableStateOf(false)
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var sportFilter by mutableStateOf("TODOS")
    private var cacheRestored = false

    suspend fun selectSport(api: WinUpApiService, sport: String) {
        if (sportFilter == sport) {
            return
        }
        sportFilter = sport
        refresh(api)
    }

    suspend fun refresh(api: WinUpApiService) = coroutineScope {
        restoreCache()
        loading = true
        try {
            val matchesTask = async {
                api.matches(status = "EN VIVO", sport = sportFilter, page = 0, size = 24)
            }
            val betsTask = async { api.bets() }
            val notificationsTask = async { api.notifications() }
            val readTask = async { api.readNotifications() }
            val walletTask = async { api.wallet() }
            val movementsTask = async { api.movements(page = 0, size = 20) }

            val page = matchesTask.await()
            matches.clear()
            matches.addAll(page.items.map { it.toUi() })

            val betsPage = betsTask.await()
            bets.clear()
            bets.addAll(betsPage.map { it.toUi() })

            val items = notificationsTask.await()
            notifications.clear()
            notifications.addAll(items.map {
                NotificationItem(it.id, it.kind, it.title, it.detail, it.date)
            })

            val read = readTask.await()
            readNotifications.clear()
            readNotifications.addAll(read)

            val wallet = walletTask.await()
            balance = wallet.balance.toBigDecimal()
            heldBalance = wallet.heldBalance.toBigDecimal()
            incomeTotal = wallet.income.toBigDecimal()
            expensesTotal = wallet.expenses.toBigDecimal()

            val movementsPage = movementsTask.await()
            applyMovementPage(movementsPage, replace = true)
            saveCache(page, betsPage, wallet, movementsPage)
            error = null
        } catch (exception: Exception) {
            error = exception.message ?: "No se pudieron cargar los datos"
        } finally {
            loading = false
        }
    }

    suspend fun loadMoreMovements(api: WinUpApiService) {
        if (movementsLoading || !movementHasNext) {
            return
        }
        movementsLoading = true
        try {
            loadMovements(api, movementPage + 1)
        } finally {
            movementsLoading = false
        }
    }

    private fun restoreCache() {
        if (cacheRestored) {
            return
        }
        cacheRestored = true
        val user = WinUpSession.user.value?.id ?: return
        val matchesPage = WinUpCache.read("matches.$user", MatchPageDto.serializer())
        if (matchesPage != null) {
            matches.clear()
            matches.addAll(matchesPage.items.map { it.toUi() })
        }
        val betsPage = WinUpCache.read("bets.$user", kotlinx.serialization.builtins.ListSerializer(BetDto.serializer()))
        if (betsPage != null) {
            bets.clear()
            bets.addAll(betsPage.map { it.toUi() })
        }
        val wallet = WinUpCache.read("wallet.$user", WalletDto.serializer())
        if (wallet != null) {
            balance = wallet.balance.toBigDecimal()
            heldBalance = wallet.heldBalance.toBigDecimal()
            incomeTotal = wallet.income.toBigDecimal()
            expensesTotal = wallet.expenses.toBigDecimal()
        }
        val movementsPage = WinUpCache.read("movements.$user", MovementPageDto.serializer())
        if (movementsPage != null) {
            applyMovementPage(movementsPage, replace = true)
        }
    }

    private fun saveCache(
        matchesPage: MatchPageDto,
        betsPage: List<BetDto>,
        wallet: WalletDto,
        movementsPage: MovementPageDto,
    ) {
        val user = WinUpSession.user.value?.id ?: return
        WinUpCache.write("matches.$user", Json.encodeToString(MatchPageDto.serializer(), matchesPage))
        WinUpCache.write("bets.$user", Json.encodeToString(kotlinx.serialization.builtins.ListSerializer(BetDto.serializer()), betsPage))
        WinUpCache.write("wallet.$user", Json.encodeToString(WalletDto.serializer(), wallet))
        WinUpCache.write("movements.$user", Json.encodeToString(MovementPageDto.serializer(), movementsPage))
    }

    private suspend fun loadMovements(api: WinUpApiService, page: Int) {
        applyMovementPage(api.movements(page = page, size = 20), replace = page == 0)
    }

    private fun applyMovementPage(result: MovementPageDto, replace: Boolean) {
        if (replace) {
            movements.clear()
        }
        movements.addAll(result.items.map { it.toUi() })
        movementPage = result.page
        movementTotal = result.totalElements
        movementHasNext = result.hasNext
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

    suspend fun clearNotifications(api: WinUpApiService) {
        try {
            api.clearNotifications()
            notifications.clear()
            readNotifications.clear()
        } catch (exception: Exception) {
            error = errorMessage(exception, "No se pudieron limpiar las notificaciones")
        }
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
            isLive = event.status == "OPEN" && event.minute > 0,
            status = when {
                event.status == "CLOSED" -> "FINALIZADO"
                event.status == "VOID" -> "ANULADO"
                event.status == "OPEN" && event.minute > 0 -> "EN VIVO"
                event.status == "OPEN" -> "PRÓXIMO"
                else -> current.status
            },
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
