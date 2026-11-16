package com.winup.ui.bets

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winup.data.WinUpData
import com.winup.money.formatCop
import com.winup.ui.theme.LocalWinUpColors
import java.math.BigDecimal

@Composable
fun BetsScreen() {
    val extras = LocalWinUpColors.current
    val bets = WinUpData.bets
    val settled = bets.filter { it.status != "PENDIENTE" }
    val totalStake = bets.fold(BigDecimal.ZERO) { total, bet -> total + bet.stake }
    val totalReturned = settled.fold(BigDecimal.ZERO) { total, bet -> total + displayReturn(bet) }
    val netResult = totalReturned - totalStake

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

        item {
            BetSummary(
                totalStake = totalStake,
                totalReturned = totalReturned,
                netResult = netResult,
            )
        }

        items(bets, key = { it.id }) { bet ->
            BetCard(bet)
        }
    }
}

@Composable
private fun BetSummary(totalStake: BigDecimal, totalReturned: BigDecimal, netResult: BigDecimal) {
    val extras = LocalWinUpColors.current
    val netColor = if (netResult.signum() >= 0) extras.success else MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SummaryStat("Apostado", totalStake, MaterialTheme.colorScheme.onSurface)
            SummaryStat("Retornado", totalReturned, extras.success)
            SummaryStat("Neto", netResult, netColor)
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: BigDecimal, tone: Color) {
    val extras = LocalWinUpColors.current
    Column {
        Text(
            text = label,
            color = extras.muted,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = formatCop(value),
            color = tone,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
