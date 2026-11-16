package com.winup.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winup.R
import com.winup.ui.theme.LocalWinUpColors

@Composable
fun WinUpLogo(
    modifier: Modifier = Modifier,
    large: Boolean = false,
    onDark: Boolean = false,
) {
    val extras = LocalWinUpColors.current
    val wordSize: TextUnit = if (large) 28.sp else 18.sp
    val markHeight = if (large) 38.dp else 24.dp
    val darkBackground = onDark || MaterialTheme.colorScheme.background.luminance() < 0.5f
    val mark = if (darkBackground) R.drawable.mark_winup_dark else R.drawable.mark_winup_light
    val winColor = if (darkBackground) Color.White else MaterialTheme.colorScheme.onBackground
    val upColor = if (darkBackground) Color(0xFF39FF14) else extras.logoUp

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Image(
            painter = painterResource(mark),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.height(markHeight),
        )
        Row {
            Text(
                text = "WIN",
                color = winColor,
                fontSize = wordSize,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "UP",
                color = upColor,
                fontSize = wordSize,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Black,
            )
        }
    }
}
