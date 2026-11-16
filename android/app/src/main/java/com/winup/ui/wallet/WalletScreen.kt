package com.winup.ui.wallet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.winup.data.DepositFlow
import com.winup.data.WinUpData
import com.winup.data.WinUpSession
import com.winup.ui.theme.LocalWinUpColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val failedDepositStatuses = setOf("FAILED", "REJECTED", "CANCELLED", "CANCELED", "EXPIRED")

@Composable
fun WalletScreen() {
    val extras = LocalWinUpColors.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showDeposit by remember { mutableStateOf(false) }
    var showWithdraw by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var resumeCount by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                resumeCount++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(resumeCount) {
        val pending = DepositFlow.pendingIntentId
        if (pending == null) {
            if (resumeCount > 0) {
                WinUpData.refresh(WinUpSession.api)
            }
            return@LaunchedEffect
        }
        repeat(6) { attempt ->
            val status = WinUpData.depositStatus(WinUpSession.api, pending)
            if (status == "PAID") {
                DepositFlow.clear()
                WinUpData.refresh(WinUpSession.api)
                notice = "Depósito acreditado. Tu saldo ya está actualizado."
                return@LaunchedEffect
            }
            if (status != null && status in failedDepositStatuses) {
                DepositFlow.clear()
                notice = "El pago no se completó. Puedes intentarlo de nuevo."
                return@LaunchedEffect
            }
            if (attempt < 5) {
                delay(2500)
            }
        }
        WinUpData.refresh(WinUpSession.api)
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Cartera",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "Consulta tu saldo y todos tus movimientos.",
                color = extras.muted,
                fontSize = 12.sp,
            )
        }

        item {
            BalanceCard(
                onDeposit = { showDeposit = true },
                onWithdraw = { showWithdraw = true },
                notice = notice,
            )
        }

        item {
            WalletStats()
        }

        item {
            Text(
                text = "Movimientos",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        items(WinUpData.movements, key = { it.id }) { movement ->
            MovementRow(movement)
        }

        if (WinUpData.movementHasNext) {
            item {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            WinUpData.loadMoreMovements(WinUpSession.api)
                        }
                    },
                    enabled = !WinUpData.movementsLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = if (WinUpData.movementsLoading) {
                            "Cargando..."
                        } else {
                            "Cargar más movimientos"
                        },
                    )
                }
            }
        }
    }

    if (showDeposit) {
        DepositSheet(
            onDismiss = { showDeposit = false },
            onMessage = { message ->
                notice = message
                showDeposit = false
            },
        )
    }

    if (showWithdraw) {
        WithdrawSheet(
            onDismiss = { showWithdraw = false },
            onMessage = { message ->
                notice = message
                showWithdraw = false
            },
        )
    }
}

