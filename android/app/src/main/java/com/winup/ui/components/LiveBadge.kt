package com.winup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winup.ui.theme.LocalWinUpColors

@Composable
fun LiveBadge(minute: Int) {
    val extras = LocalWinUpColors.current

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(extras.accent)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(extras.onAccent),
        )
        Text(
            text = "EN VIVO",
            color = extras.onAccent,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = "$minute'",
            color = extras.onAccent,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
        )
    }
}
