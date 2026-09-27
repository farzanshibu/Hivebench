package com.jarves.mh.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// ── Re-export Neobrutal tokens for seamless application-wide adoption ──
val PocketLime = NeoLime
val PocketOrange = NeoLime // Replaced brand orange with Neobrutal Lime Green accent!
val PocketBlue = NeoCyan
val PocketGreen = NeoLime
val PocketBackground = Color(0xFF0D0F12)
val PocketSurface = Color(0xFF15181E)
val PocketSurfaceVariant = Color(0xFF1F242D)
val PocketOutline = Color(0xFF384152)

private val DarkColors = darkColorScheme(
    primary = NeoLime,
    onPrimary = NeoBlack,
    primaryContainer = Color(0xFF243B06),
    onPrimaryContainer = Color(0xFFD9FF66),
    secondary = NeoCyan,
    onSecondary = NeoBlack,
    secondaryContainer = Color(0xFF0F2D3D),
    onSecondaryContainer = Color(0xFFBAE6FD),
    tertiary = NeoPink,
    onTertiary = NeoBlack,
    tertiaryContainer = Color(0xFF3D141D),
    onTertiaryContainer = Color(0xFFFECDD3),
    background = Color(0xFF0D0F12),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF15181E),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1F242D),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = NeoDarkBorder,
    outlineVariant = Color(0xFF252C37),
    error = Color(0xFFF87171),
    onError = NeoBlack,
    errorContainer = Color(0xFF3B1214),
    onErrorContainer = Color(0xFFFECACA),
)

private val LightColors = lightColorScheme(
    primary = NeoLime,
    onPrimary = NeoBlack,
    primaryContainer = Color(0xFFE5FAB3),
    onPrimaryContainer = Color(0xFF142404),
    secondary = NeoCyan,
    onSecondary = NeoBlack,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),
    tertiary = NeoPink,
    onTertiary = NeoBlack,
    tertiaryContainer = Color(0xFFFFE4E6),
    onTertiaryContainer = Color(0xFFBE123C),
    background = Color(0xFFF5F6F0),
    onBackground = NeoBlack,
    surface = Color(0xFFFFFFFF),
    onSurface = NeoBlack,
    surfaceVariant = Color(0xFFE8ECE2),
    onSurfaceVariant = Color(0xFF475569),
    outline = NeoBlack,
    outlineVariant = Color(0xFF2D333B),
    error = Color(0xFFEF4444),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
)

private val NeoShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

enum class AppThemeMode { SYSTEM, DARK, LIGHT }

@Composable
fun PocketTheme(themeMode: AppThemeMode = AppThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val isDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = if (isDark) DarkColors else LightColors,
        shapes = NeoShapes,
        content = content,
    )
}
