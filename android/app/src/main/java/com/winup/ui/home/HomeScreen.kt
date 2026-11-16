package com.winup.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Sports
import androidx.compose.material.icons.outlined.SportsBaseball
import androidx.compose.material.icons.outlined.SportsBasketball
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.SportsFootball
import androidx.compose.material.icons.outlined.SportsHandball
import androidx.compose.material.icons.outlined.SportsHockey
import androidx.compose.material.icons.outlined.SportsMma
import androidx.compose.material.icons.outlined.SportsMotorsports
import androidx.compose.material.icons.outlined.SportsRugby
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.SportsTennis
import androidx.compose.material.icons.outlined.SportsVolleyball
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Sports
import androidx.compose.material.icons.outlined.SportsBaseball
import androidx.compose.material.icons.outlined.SportsBasketball
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.SportsFootball
import androidx.compose.material.icons.outlined.SportsHandball
import androidx.compose.material.icons.outlined.SportsHockey
import androidx.compose.material.icons.outlined.SportsMma
import androidx.compose.material.icons.outlined.SportsMotorsports
import androidx.compose.material.icons.outlined.SportsRugby
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.SportsTennis
import androidx.compose.material.icons.outlined.SportsVolleyball
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winup.R
import com.winup.data.Match
import com.winup.data.WinUpData
import com.winup.data.WinUpSession
import com.winup.ui.components.MatchCard
import com.winup.ui.components.WinUpLogo
import com.winup.ui.theme.LocalWinUpColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class SportFilter(
    val name: String,
    val label: String,
    val icon: ImageVector,
)

private val sportFilters = listOf(
    SportFilter("TODOS", "Todos", Icons.Outlined.GridView),
    SportFilter("Fútbol", "Fútbol", Icons.Outlined.SportsSoccer),
    SportFilter("AFL", "AFL", Icons.Outlined.Sports),
    SportFilter("Béisbol", "Béisbol", Icons.Outlined.SportsBaseball),
    SportFilter("Baloncesto", "Baloncesto", Icons.Outlined.SportsBasketball),
    SportFilter("Fórmula 1", "Fórmula 1", Icons.Outlined.SportsMotorsports),
    SportFilter("Balonmano", "Balonmano", Icons.Outlined.SportsHandball),
    SportFilter("Hockey", "Hockey", Icons.Outlined.SportsHockey),
    SportFilter("MMA", "MMA", Icons.Outlined.SportsMma),
    SportFilter("NBA", "NBA", Icons.Outlined.SportsBasketball),
    SportFilter("NFL & NCAA", "NFL & NCAA", Icons.Outlined.SportsFootball),
    SportFilter("Rugby", "Rugby", Icons.Outlined.SportsRugby),
    SportFilter("Voleibol", "Voleibol", Icons.Outlined.SportsVolleyball),
    SportFilter("Tenis", "Tenis", Icons.Outlined.SportsTennis),
    SportFilter("eSports", "eSports", Icons.Outlined.SportsEsports),
)

@Composable
fun HomeScreen(
    onSelect: (Match, String) -> Unit,
    onSeeAll: () -> Unit,
) {
    val extras = LocalWinUpColors.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val selectedLabel = sportFilters.firstOrNull { it.name == WinUpData.sportFilter }?.label ?: "Todos"
    val slides = remember {
        listOf(
            R.drawable.banner_estadio,
            R.drawable.banner_futbol_festejo,
            R.drawable.banner_futbol_balon,
            R.drawable.banner_futbol_accion,
            R.drawable.banner_baloncesto_mate,
            R.drawable.banner_baloncesto_balon,
            R.drawable.banner_tenis,
            R.drawable.banner_formula1,
            R.drawable.banner_winup_estadio,
            R.drawable.banner_multideporte,
        )
    }
    var slide by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(6000)
            slide = (slide + 1) % slides.size
        }
    }
    val liveMatches = WinUpData.matches.filter { it.isLive }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
            ) {
                Crossfade(
                    targetState = slide,
                    modifier = Modifier.matchParentSize(),
                    animationSpec = tween(durationMillis = 900),
                    label = "banner",
                ) { current ->
                    Image(
                        painter = painterResource(slides[current]),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xF2101B2B),
                                    Color(0xC7101B2B),
                                    Color(0x33101B2B),
                                ),
                            ),
                        ),
                )
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    WinUpLogo(large = true, onDark = true)
                    Text(
                        text = "CADA SEGUNDO",
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 30.sp,
                    )
                    Text(
                        text = "IMPORTA",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 30.sp,
                    )
                    Text(
                        text = "MÁS DEPORTES. MÁS EMOCIÓN. MÁS GANANCIAS.",
                        color = Color(0xFFB8C2D9),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                listState.animateScrollToItem(2)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            text = "Apuesta ahora",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    slides.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .size(width = if (index == slide) 18.dp else 7.dp, height = 7.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (index == slide) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        Color.White.copy(alpha = 0.4f)
                                    },
                                ),
                        )
                    }
                }
            }
        }

        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(sportFilters, key = { it.name }) { sport ->
                    SportItem(
                        label = sport.label,
                        icon = sport.icon,
                        selected = WinUpData.sportFilter == sport.name,
                        modifier = Modifier.width(92.dp),
                        onClick = {
                            scope.launch {
                                WinUpData.selectSport(WinUpSession.api, sport.name)
                            }
                        },
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Partidos en vivo",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = onSeeAll) {
                    Text(
                        text = "Ver todos",
                        color = extras.muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
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

        if (liveMatches.isEmpty()) {
            item {
                Text(
                    text = "No hay partidos en vivo de $selectedLabel.",
                    color = LocalWinUpColors.current.muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SportItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
