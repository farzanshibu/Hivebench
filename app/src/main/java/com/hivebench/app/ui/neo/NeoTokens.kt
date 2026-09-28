package com.hivebench.app.ui.neo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Neo color tokens (single source of truth, no Material3) ──
object NeoColors {
    // "Lime" is the primary accent slot (now yellow); kept for source compatibility.
    val Lime = Color(0xFFFFD000)
    val LimeDark = Color(0xFFE0B400)
    val LimeLight = Color(0xFFFFE680)
    val Ink = Color(0xFF111111)
    val Paper = Color(0xFFFFFFFF)
    val Cyan = Color(0xFF4B8BFF)
    val Pink = Color(0xFFFF6BB5)
    val Yellow = Color(0xFFFFD000)
    val Purple = Color(0xFFA98BFF)
    val Orange = Color(0xFFFF9A2E)
    val Green = Color(0xFF00DD8F)
    val Mint = Color(0xFF00DD8F)
    val Cream = Color(0xFFF1EDE3)
    val DarkBg = Color(0xFF141414)
    val DarkSurface = Color(0xFF242424)
    val DarkSurfaceVariant = Color(0xFF2E2E2E)
    val DarkBorder = Color(0xFFF1EDE3)
    val DarkBorderSoft = Color(0xFF3A3A3A)
    val LightBg = Color(0xFFF4F1E8)
    val LightSurfaceVariant = Color(0xFFEAE5D8)
    val LightMuted = Color(0xFF55524B)
    val DarkMuted = Color(0xFFB3AEA3)
    val Error = Color(0xFFF87171)
    val ErrorStrong = Color(0xFFEF4444)
}

// ── Resolved theme (dark/light aware, no MaterialTheme) ──
data class NeoThemeColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val border: Color,
    val borderSoft: Color,
    val ink: Color,
    val muted: Color,
    val accent: Color,
    val onAccent: Color,
    val error: Color,
    val shadow: Color,
    val isDark: Boolean,
    /** Positive/active state (toggles, checks, progress, running). */
    val positive: Color = NeoColors.Mint,
)

@Composable
@ReadOnlyComposable
fun rememberNeoColors(): NeoThemeColors {
    val scheme = LocalNeoColorScheme.current
    val dark = scheme.background.luminance() < 0.5f
    return NeoThemeColors(
        background = scheme.background,
        surface = scheme.surface,
        surfaceVariant = scheme.surfaceVariant,
        border = scheme.outline,
        borderSoft = scheme.outlineVariant,
        ink = scheme.onSurface,
        muted = scheme.onSurfaceVariant,
        accent = scheme.primary,
        onAccent = scheme.onPrimary,
        error = scheme.error,
        shadow = if (dark) NeoColors.Cream.copy(alpha = 0.92f) else NeoColors.Ink,
        isDark = dark,
        positive = scheme.secondary,
    )
}

// ── Shape / border / shadow tokens ──
object NeoShapes {
    val Xs: Dp = 4.dp
    val Sm: Dp = 8.dp
    val Md: Dp = 14.dp
    val Lg: Dp = 16.dp
    val Xl: Dp = 20.dp
    val Pill: Dp = 50.dp
}

object NeoBorders {
    val Regular: Dp = 2.dp
    val Thin: Dp = 1.5.dp
    val Hairline: Dp = 1.dp
}

object NeoShadows {
    val Regular: Dp = 5.dp
    val Small: Dp = 3.dp
    val Pressed: Dp = 1.dp
}

// ── Typography (replaces M3 Typography, rendered via BasicText) ──
object NeoType {
    val Display = TextStyle(
        fontSize = 26.sp, fontWeight = FontWeight.Black,
        letterSpacing = (-0.3).sp, fontFamily = FontFamily.Default,
    )
    val Title = TextStyle(
        fontSize = 20.sp, fontWeight = FontWeight.Bold,
        letterSpacing = (-0.2).sp,
    )
    val Subtitle = TextStyle(
        fontSize = 15.sp, fontWeight = FontWeight.Bold,
    )
    val Body = TextStyle(
        fontSize = 14.sp, fontWeight = FontWeight.Normal,
    )
    val BodyBold = TextStyle(
        fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
    )
    val Caption = TextStyle(
        fontSize = 12.sp, fontWeight = FontWeight.Medium,
    )
    val Micro = TextStyle(
        fontSize = 11.sp, fontWeight = FontWeight.Bold,
    )
    val Badge = TextStyle(
        fontSize = 10.sp, fontWeight = FontWeight.Black,
        letterSpacing = 0.8.sp,
    )
    val Mono = TextStyle(
        fontSize = 12.sp, fontWeight = FontWeight.Normal,
        fontFamily = FontFamily.Monospace,
    )
    val MonoSmall = TextStyle(
        fontSize = 11.sp, fontWeight = FontWeight.Normal,
        fontFamily = FontFamily.Monospace,
    )
}
