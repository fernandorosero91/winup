package com.winup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winup.data.Match
import com.winup.ui.theme.LocalWinUpColors

@Composable
internal fun MatchSourceBadge(
    match: Match,
    modifier: Modifier = Modifier,
) {
    Text(
        text = sourceLabel(match),
        color = LocalWinUpColors.current.muted,
        fontSize = 8.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.6.sp,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

private fun sourceLabel(match: Match): String = when {
    match.isSimulated -> "SIMULADO"
    match.source == "API_FOOTBALL" || match.source == "FOOTBALLDATA" -> "API"
    else -> match.source
}
