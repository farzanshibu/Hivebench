package com.hivebench.app.ui.theme

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.hivebench.app.ui.neo.LocalNeoColorScheme
import com.hivebench.app.ui.neo.LocalNeoShapes
import com.hivebench.app.ui.neo.LocalNeoTypography
import com.hivebench.app.ui.neo.NeoColorScheme
import com.hivebench.app.ui.neo.NeoColors
import com.hivebench.app.ui.neo.NeoShapeScale
import com.hivebench.app.ui.neo.NeoType
import com.hivebench.app.ui.neo.NeoTypography

// ── Re-export Neobrutal tokens for seamless application-wide adoption ──
val PocketLime = NeoLime
val PocketOrange = NeoOrange
val PocketBlue = NeoCyan
val PocketGreen = NeoMint
val PocketBackground = Color(0xFF141414)
val PocketSurface = Color(0xFF242424)
val PocketSurfaceVariant = Color(0xFF2E2E2E)
val PocketOutline = NeoCream

private val DarkColors = NeoColorScheme(
    background = Color(0xFF141414),
    onBackground = Color(0xFFF7F4EC),
    surface = Color(0xFF242424),
    onSurface = Color(0xFFF7F4EC),
    surfaceVariant = Color(0xFF2E2E2E),
    onSurfaceVariant = Color(0xFFB3AEA3),
    surfaceContainerHigh = Color(0xFF333333),
    primary = NeoLime,
    onPrimary = NeoBlack,
    primaryContainer = Color(0xFF3D3300),
    onPrimaryContainer = Color(0xFFFFE680),
    secondary = NeoMint,
    onSecondary = NeoBlack,
    tertiary = NeoPink,
    onTertiary = NeoBlack,
    outline = NeoCream,
    outlineVariant = Color(0xFF3A3A3A),
    error = Color(0xFFF87171),
    onError = NeoBlack,
    errorContainer = Color(0xFF3B1214),
    onErrorContainer = Color(0xFFFECACA),
)

private val LightColors = NeoColorScheme(
    background = Color(0xFFF4F1E8),
    onBackground = NeoBlack,
    surface = Color(0xFFFFFFFF),
    onSurface = NeoBlack,
    surfaceVariant = Color(0xFFEAE5D8),
    onSurfaceVariant = Color(0xFF55524B),
    surfaceContainerHigh = Color(0xFFE0DACB),
    primary = NeoLime,
    onPrimary = NeoBlack,
    primaryContainer = Color(0xFFFFF1B3),
    onPrimaryContainer = Color(0xFF2B2300),
    secondary = NeoMint,
    onSecondary = NeoBlack,
    tertiary = NeoPink,
    onTertiary = NeoBlack,
    outline = NeoBlack,
    outlineVariant = Color(0xFF2D333B),
    error = Color(0xFFEF4444),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
)

private val NeoTypeScale = NeoTypography(
    displayLarge = NeoType.Display,
    headlineLarge = NeoType.Display,
    headlineMedium = NeoType.Title,
    headlineSmall = NeoType.Title,
    titleLarge = NeoType.Title,
    titleMedium = NeoType.Subtitle,
    titleSmall = NeoType.BodyBold,
    bodyLarge = NeoType.Body,
    bodyMedium = NeoType.Body,
    bodySmall = NeoType.Caption,
    labelLarge = NeoType.BodyBold,
    labelMedium = NeoType.Caption,
    labelSmall = NeoType.Badge,
)

private val NeoShapeValues = NeoShapeScale(
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
    val systemConfiguration = LocalConfiguration.current
    val themedConfiguration = remember(systemConfiguration, isDark) {
        Configuration(systemConfiguration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (isDark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        }
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

    CompositionLocalProvider(
        LocalConfiguration provides themedConfiguration,
        LocalNeoColorScheme provides if (isDark) DarkColors else LightColors,
        LocalNeoTypography provides NeoTypeScale,
        LocalNeoShapes provides NeoShapeValues,
        content = content,
    )
}
