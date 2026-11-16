package com.winup.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winup.R
import com.winup.ui.theme.DarkBackground
import com.winup.ui.theme.DarkMuted
import com.winup.ui.theme.DarkSidebar
import com.winup.ui.theme.Neon

@Composable
fun WinUpLoader(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "winup-loader")

    val scale by transition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )
    val glow by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )
    val taglineAlpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "tagline",
    )

    val tileShape = RoundedCornerShape(26.dp)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkBackground, DarkSidebar)))
            .semantics { contentDescription = "Cargando WINUP" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension * 0.48f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Neon.copy(alpha = 0.13f * glow), Color.Transparent),
                    center = Offset(center.x, center.y - 48.dp.toPx()),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(center.x, center.y - 48.dp.toPx()),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_winup),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(104.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .shadow(
                        elevation = 24.dp,
                        shape = tileShape,
                        ambientColor = Neon.copy(alpha = 0.3f),
                        spotColor = Neon.copy(alpha = 0.3f),
                    )
                    .clip(tileShape)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), tileShape),
            )

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.White)) { append("WIN") }
                    withStyle(SpanStyle(color = Neon)) { append("UP") }
                },
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "CADA SEGUNDO IMPORTA",
                color = DarkMuted.copy(alpha = taglineAlpha),
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 4.sp,
            )

            Spacer(modifier = Modifier.height(30.dp))

            LinearProgressIndicator(
                modifier = Modifier
                    .width(112.dp)
                    .height(2.5.dp),
                color = Neon,
                trackColor = Neon.copy(alpha = 0.14f),
                strokeCap = StrokeCap.Round,
            )
        }
    }
}
