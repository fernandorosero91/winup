package com.winup.ui.bets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winup.data.Bet
import com.winup.data.WinUpData
import com.winup.money.formatCop
import com.winup.ui.theme.LocalWinUpColors

@Composable
fun BetsScreen() {
    val extras = LocalWinUpColors.current

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Mis apuestas",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "Sigue el estado de tus apuestas en tiempo real.",
                color = extras.muted,
                fontSize = 12.sp,
            )
        }

        items(WinUpData.bets, key = { it.id }) { bet ->
            BetCard(bet)
        }
    }
}

@Composable
private fun BetCard(bet: Bet) {
    val extras = LocalWinUpColors.current
    val statusColor = when (bet.status) {
        "GANADA" -> extras.success
        "PERDIDA" -> MaterialTheme.colorScheme.error
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
                        text = when (bet.status) {
                            "GANADA" -> "Ganancia"
                            "ANULADA" -> "Reembolso"
                            else -> "Potencial"
                        },
                        color = extras.muted,
                        fontSize = 10.sp,
                    )
                    Text(
                        text = formatCop(bet.potential),
                        color = when (bet.status) {
                            "GANADA" -> extras.success
                            "ANULADA" -> extras.muted
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
