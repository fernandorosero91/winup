package com.winup.ui.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winup.data.Match
import com.winup.data.WinUpData
import com.winup.ui.components.MatchCard
import com.winup.ui.theme.LocalWinUpColors

@Composable
fun LiveScreen(
    onSelect: (Match, String) -> Unit,
) {
    val extras = LocalWinUpColors.current
    val liveMatches = WinUpData.matches.filter { it.isLive }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(extras.accent),
                    )
                    Text(
                        text = "EN DIRECTO",
                        color = extras.accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                    )
                }
                Text(
                    text = "En vivo",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "${liveMatches.size} partidos en juego ahora mismo",
                    color = extras.muted,
                    fontSize = 12.sp,
                )
            }
        }

        if (liveMatches.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Sin partidos en vivo",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Cuando empiece un partido aparecerá aquí.",
                        color = extras.muted,
                        fontSize = 11.sp,
                    )
                }
            }
        }

        items(liveMatches, key = { it.id }) { match ->
            MatchCard(
                match = match,
                onSelect = { selection -> onSelect(match, selection) },
            )
        }
    }
}
