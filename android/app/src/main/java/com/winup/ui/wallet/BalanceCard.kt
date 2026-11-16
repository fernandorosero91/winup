package com.winup.ui.wallet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winup.data.WinUpData
import com.winup.money.formatCop
import com.winup.ui.theme.Neon

@Composable
fun BalanceCard(
    onDeposit: () -> Unit,
    onWithdraw: () -> Unit,
    notice: String?,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val content = if (dark) Color(0xFFF8FAFC) else MaterialTheme.colorScheme.onSurface
    val muted = if (dark) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (dark) Neon.copy(alpha = 0.18f) else MaterialTheme.colorScheme.outlineVariant
    val background = if (dark) {
        Brush.linearGradient(listOf(Color(0xFF101B2B), Color(0xFF14213A), Color(0xFF0E1828)))
    } else {
        Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF8FAFC)))
    }
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(1.dp, borderColor, shape),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    val center = Offset(size.width * 0.92f, size.height * 0.02f)
                    val radius = size.minDimension * 0.62f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Neon.copy(alpha = if (dark) 0.16f else 0.14f),
                                Color.Transparent,
                            ),
                            center = center,
                            radius = radius,
                        ),
                        radius = radius,
                        center = center,
                    )
                },
        )

        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "SALDO DISPONIBLE",
                        color = muted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.4.sp,
                    )
                    Text(
                        text = "Listo para apostar en vivo",
                        color = muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (dark) Color.White.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            1.dp,
                            if (dark) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(50),
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "COP",
                        color = muted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                }
            }

            Text(
                text = formatCop(WinUpData.balance),
                color = content,
                fontFamily = FontFamily.Monospace,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onDeposit,
                    modifier = Modifier.height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    Text("Depositar", fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
                OutlinedButton(
                    onClick = onWithdraw,
                    modifier = Modifier.height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (dark) Color.White.copy(alpha = 0.24f) else MaterialTheme.colorScheme.outline,
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = content),
                ) {
                    Text("Retirar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (notice != null) {
                Text(
                    text = notice,
                    color = muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
