package com.winup.ui

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.winup.data.Match
import com.winup.data.RealtimeClient
import com.winup.data.WinUpData
import com.winup.data.WinUpSession
import com.winup.notifications.NotificationAnnouncer
import com.winup.money.formatCop
import com.winup.money.formatCopWhole
import com.winup.ui.auth.AuthScreen
import com.winup.ui.bets.BetsScreen
import com.winup.ui.components.WinUpLoader
import com.winup.ui.components.WinUpLogo
import com.winup.ui.home.HomeScreen
import com.winup.ui.live.LiveScreen
import com.winup.ui.notifications.NotificationsSheet
import com.winup.ui.profile.ProfileScreen
import com.winup.ui.profile.ThemeMode
import com.winup.ui.theme.LocalWinUpColors
import com.winup.ui.theme.WinUpTheme
import com.winup.ui.wallet.WalletScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WinUpRoot() {
    val context = LocalContext.current
    var themeMode by remember { mutableStateOf(readTheme(context)) }
    val setTheme: (ThemeMode) -> Unit = { mode ->
        themeMode = mode
        saveTheme(context, mode)
    }
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    WinUpTheme(darkTheme = darkTheme) {
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as Activity).window
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }

        var tab by rememberSaveable { mutableIntStateOf(0) }
        var selectedMatch by remember { mutableStateOf<Match?>(null) }
        var selectedSelection by remember { mutableStateOf("LOCAL") }
        var showNotifications by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val token = WinUpSession.token.value
        var bootstrapping by remember { mutableStateOf(token != null) }

        LaunchedEffect(token) {
            if (token == null) {
                RealtimeClient.stop()
                WinUpData.clear()
                bootstrapping = false
            } else {
                bootstrapping = true
                val startedAt = System.currentTimeMillis()
                WinUpData.refresh(WinUpSession.api)
                NotificationAnnouncer.announce(context, WinUpData.notifications)
                val elapsed = System.currentTimeMillis() - startedAt
                if (elapsed < 900L) {
                    delay(900L - elapsed)
                }
                bootstrapping = false
                RealtimeClient.onNotificationsUpdated = {
                    scope.launch {
                        val before = WinUpData.notifications.firstOrNull()?.id
                        repeat(4) { attempt ->
                            WinUpData.refreshNotifications(WinUpSession.api)
                            val latest = WinUpData.notifications.firstOrNull()?.id
                            if (latest != null && latest != before) {
                                NotificationAnnouncer.announce(context, WinUpData.notifications)
                                return@launch
                            }
                            if (attempt < 3) {
                                delay(400L * (attempt + 1))
                            }
                        }
                    }
                }
                RealtimeClient.start()
                while (true) {
                    delay(10_000)
                    WinUpData.refresh(WinUpSession.api)
                    NotificationAnnouncer.announce(context, WinUpData.notifications)
                }
            }
        }
        DisposableEffect(Unit) {
            onDispose {
                RealtimeClient.onNotificationsUpdated = null
                RealtimeClient.stop()
            }
        }

        if (bootstrapping) {
            WinUpLoader()
        } else if (token == null) {
            AuthScreen(
                darkTheme = darkTheme,
                onToggleTheme = {
                    setTheme(if (darkTheme) ThemeMode.LIGHT else ThemeMode.DARK)
                },
                onAuthenticated = {},
            )
        } else {
            Scaffold(
                topBar = {
                    WinUpTopBar(
                        darkTheme = darkTheme,
                        unreadNotifications = WinUpData.unreadNotifications(),
                        onToggleTheme = {
                            setTheme(if (darkTheme) ThemeMode.LIGHT else ThemeMode.DARK)
                        },
                        onNotifications = { showNotifications = true },
                    )
                },
                bottomBar = {
                    WinUpBottomBar(selected = tab, onSelect = { tab = it })
                },
                containerColor = MaterialTheme.colorScheme.background,
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    when (tab) {
                        0 -> HomeScreen(
                            onSelect = { match, selection ->
                                selectedMatch = match
                                selectedSelection = selection
                            },
                            onSeeAll = { tab = 1 },
                        )
                        1 -> LiveScreen { match, selection ->
                            selectedMatch = match
                            selectedSelection = selection
                        }
                        2 -> BetsScreen()
                        3 -> WalletScreen()
                        else -> ProfileScreen(
                            themeMode = themeMode,
                            onThemeChange = setTheme,
                            userName = WinUpSession.user.value?.displayName ?: "Usuario",
                            userEmail = WinUpSession.user.value?.email ?: "",
                            initials = initialsOf(WinUpSession.user.value?.displayName),
                            onLogout = { WinUpSession.clear() },
                        )
                    }
                }
            }

            if (showNotifications) {
                ModalBottomSheet(
                    onDismissRequest = { showNotifications = false },
                    sheetState = sheetState,
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    NotificationsSheet(
                        onClose = { showNotifications = false },
                        onRead = { id ->
                            scope.launch { WinUpData.markNotificationRead(WinUpSession.api, id) }
                        },
                        onReadAll = {
                            scope.launch { WinUpData.markAllNotificationsRead(WinUpSession.api) }
                        },
                    )
                }
            }

            selectedMatch?.let { match ->
                ModalBottomSheet(
                    onDismissRequest = { selectedMatch = null },
                    sheetState = sheetState,
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    BetSlipSheet(
                        match = match,
                        selection = selectedSelection,
                        onDismiss = { selectedMatch = null },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WinUpTopBar(
    darkTheme: Boolean,
    unreadNotifications: Int,
    onToggleTheme: () -> Unit,
    onNotifications: () -> Unit,
) {
    TopAppBar(
        title = { WinUpLogo() },
        actions = {
            IconButton(onClick = onToggleTheme) {
                Icon(
                    imageVector = if (darkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                    contentDescription = if (darkTheme) "Cambiar a modo claro" else "Cambiar a modo oscuro",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            BadgedBox(
                badge = {
                    if (unreadNotifications > 0) {
                        Badge { Text(unreadNotifications.toString()) }
                    }
                },
            ) {
                IconButton(onClick = onNotifications) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Notificaciones",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}

@Composable
private fun WinUpBottomBar(
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    val extras = LocalWinUpColors.current
    val items = listOf(
        "Inicio" to Icons.Outlined.Home,
        "En Vivo" to Icons.Outlined.LiveTv,
        "Apuestas" to Icons.Outlined.ReceiptLong,
        "Cartera" to Icons.Outlined.AccountBalanceWallet,
        "Perfil" to Icons.Outlined.Person,
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        items.forEachIndexed { index, (label, icon) ->
            NavigationBarItem(
                selected = selected == index,
                onClick = { onSelect(index) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(20.dp),
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = extras.muted,
                    unselectedTextColor = extras.muted,
                ),
            )
        }
    }
}

@Composable
private fun BetSlipSheet(
    match: Match,
    selection: String,
    onDismiss: () -> Unit,
) {
    val extras = LocalWinUpColors.current
    val scope = rememberCoroutineScope()
    var stake by remember { mutableStateOf(BigDecimal("50000.00")) }
    var placing by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val odds = when (selection) {
        "LOCAL" -> match.localOdds
        "EMPATE" -> match.drawOdds
        else -> match.awayOdds
    }
    val label = when (selection) {
        "LOCAL" -> match.home
        "EMPATE" -> "Empate"
        else -> match.away
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "CUPÓN",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Cerrar cupón",
                    tint = extras.muted,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = "Resultado final",
                color = extras.muted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${match.home} vs ${match.away}",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                color = extras.success,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                BigDecimal("10000.00"),
                BigDecimal("25000.00"),
                BigDecimal("50000.00"),
                BigDecimal("100000.00"),
            ).forEach { value ->
                Button(
                    onClick = { stake = value },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (stake.compareTo(value) == 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        contentColor = if (stake.compareTo(value) == 0) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
                ) {
                    Text(text = formatCopWhole(value), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Retorno estimado",
                color = extras.muted,
                fontSize = 11.sp,
            )
            Text(
                text = formatCop(
                    stake.multiply(odds.toBigDecimal()).setScale(2, RoundingMode.HALF_EVEN),
                ),
                color = extras.success,
                fontFamily = FontFamily.Monospace,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        if (message != null) {
            Text(
                text = message ?: "",
                color = extras.success,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (error != null) {
            Text(
                text = error ?: "",
                color = MaterialTheme.colorScheme.error,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Button(
            onClick = {
                if (placing) {
                    return@Button
                }
                placing = true
                message = null
                error = null
                scope.launch {
                    val failure = WinUpData.placeBet(
                        api = WinUpSession.api,
                        matchId = match.id,
                        selection = selection,
                        stake = stake,
                    )
                    if (failure == null) {
                        message = "Apuesta registrada. Revisa Mis apuestas."
                    } else {
                        error = failure
                    }
                    placing = false
                }
            },
            enabled = !placing,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text(
                text = if (placing) "Enviando..." else "Apostar ahora",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

private fun initialsOf(name: String?): String {
    if (name.isNullOrBlank()) {
        return "U"
    }
    return name.trim()
        .split(Regex("\\s+"))
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .joinToString("")
}

private const val PREFS = "winup"
private const val THEME_KEY = "theme"

private fun readTheme(context: Context): ThemeMode {
    val value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(THEME_KEY, null)
    return when (value) {
        "light" -> ThemeMode.LIGHT
        "dark" -> ThemeMode.DARK
        else -> ThemeMode.SYSTEM
    }
}

private fun saveTheme(context: Context, mode: ThemeMode) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(THEME_KEY, mode.name.lowercase())
        .apply()
}
