package com.winup.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val Neon = Color(0xFF39FF14)
val NeonDark = Color(0xFF2CE00C)
val NeonSoft = Color(0xFFEEFDE8)
val Orange = Color(0xFFFF8A00)
val DarkBackground = Color(0xFF101B2B)
val DarkOnPrimary = Color(0xFF0B1020)
val DarkSidebar = Color(0xFF080D19)
val DarkSurface = Color(0xFF141B2D)
val DarkSurfaceHigh = Color(0xFF1A2338)
val DarkBorder = Color(0xFF263149)
val LightBackground = Color(0xFFF8F9FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceHigh = Color(0xFFF1F5F9)
val LightBorder = Color(0xFFE2E8F0)
val LightText = Color(0xFF0F172A)
val LightMuted = Color(0xFF475569)
val LightSubtle = Color(0xFF64748B)
val DarkText = Color(0xFFFFFFFF)
val DarkMuted = Color(0xFF94A3B8)
val DangerDark = Color(0xFFFF5C5C)
val DangerLight = Color(0xFFB42318)
val SuccessDark = Color(0xFF39FF14)
val SuccessLight = Color(0xFF15803D)
val DarkContainer = Color(0xFF123B1C)
val DarkOnContainer = Color(0xFFB6FFA8)
val DarkOnDanger = Color(0xFF2A0A0A)

@Immutable
data class WinUpColors(
    val accent: Color,
    val onAccent: Color,
    val muted: Color,
    val subtle: Color,
    val success: Color,
    val warning: Color,
    val info: Color,
    val focus: Color,
    val logoUp: Color,
    val heroStart: Color,
    val heroEnd: Color,
)

val DarkWinUpColors = WinUpColors(
    accent = Orange,
    onAccent = DarkBackground,
    muted = DarkMuted,
    subtle = Color(0xFF64748B),
    success = SuccessDark,
    warning = Orange,
    info = Color(0xFF38BDF8),
    focus = Neon,
    logoUp = Neon,
    heroStart = Color(0xFF101B2B),
    heroEnd = Color(0xFF10251A),
)

val LightWinUpColors = WinUpColors(
    accent = Orange,
    onAccent = LightText,
    muted = LightMuted,
    subtle = LightSubtle,
    success = SuccessLight,
    warning = Color(0xFFB45309),
    info = Color(0xFF0369A1),
    focus = LightText,
    logoUp = NeonDark,
    heroStart = Color(0xFF0F172A),
    heroEnd = Color(0xFF10251A),
)

val LocalWinUpColors = staticCompositionLocalOf { DarkWinUpColors }
