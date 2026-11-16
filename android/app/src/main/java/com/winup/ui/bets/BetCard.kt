package com.winup.ui.bets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winup.data.Bet
import com.winup.money.formatCop
import com.winup.ui.theme.LocalWinUpColors
import java.math.BigDecimal

@Composable
internal fun BetCard(bet: Bet) {
    val extras = LocalWinUpColors.current
    val statusColor = when (bet.status) {
        "GANADA" -> extras.success
        "PERDIDA" -> MaterialTheme.colorScheme.error
        else -> extras.muted
    }
    val net = netValue(bet)
    val netColor = when {
        bet.status == "PENDIENTE" -> MaterialTheme.colorScheme.onSurface
        net.signum() > 0 -> extras.success
        net.signum() < 0 -> MaterialTheme.colorScheme.error
        else -> extras.muted
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = bet.market,
                    color = extras.muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = bet.status,
                    color = statusColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            Text(
                text = bet.match,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )

            bet.outcome?.takeIf { it.finished }?.let { outcome ->
                BetOutcomeRow(outcome)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Selección",
                        color = extras.muted,
                        fontSize = 10.sp,
                    )
                    Text(
                        text = bet.selection,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Importe",
                        color = extras.muted,
                        fontSize = 10.sp,
                    )
                    Text(
                        text = formatCop(bet.stake),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = returnLabel(bet),
                        color = extras.muted,
                        fontSize = 10.sp,
                    )
                    Text(
                        text = formatCop(displayReturn(bet)),
                        color = if (bet.status == "GANADA") extras.success else MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = netLabel(bet),
                    color = extras.muted,
                    fontSize = 10.sp,
                )
                Text(
                    text = formatCop(net),
                    color = netColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

internal fun displayReturn(bet: Bet): BigDecimal = when (bet.status) {
    "PERDIDA" -> BigDecimal.ZERO.setScale(2)
    "ANULADA" -> bet.stake
    else -> bet.potential
}

private fun returnLabel(bet: Bet): String = when (bet.status) {
    "ANULADA" -> "Reembolso"
    "PENDIENTE" -> "Potencial"
    else -> "Retorno"
}

private fun netLabel(bet: Bet): String = when (bet.status) {
    "GANADA" -> "Ganancia neta"
    "PERDIDA" -> "Pérdida"
    "ANULADA" -> "Neto"
    else -> "Ganancia potencial"
}

private fun netValue(bet: Bet): BigDecimal = when (bet.status) {
    "GANADA" -> bet.potential - bet.stake
    "PERDIDA" -> bet.stake.negate()
    "ANULADA" -> BigDecimal.ZERO.setScale(2)
    else -> bet.potential - bet.stake
}
