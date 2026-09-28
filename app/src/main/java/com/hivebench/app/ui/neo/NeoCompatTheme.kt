package com.hivebench.app.ui.neo

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

// ── From-scratch color scheme. No Material3. ──
data class NeoColorScheme(
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceContainerHigh: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val outline: Color,
    val outlineVariant: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
)

private val NeoDarkScheme = NeoColorScheme(
    background = Color(0xFF0D0F12),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF15181E),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1F242D),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainerHigh = Color(0xFF252C37),
    primary = NeoColors.Lime,
    onPrimary = NeoColors.Ink,
    primaryContainer = Color(0xFF243B06),
    onPrimaryContainer = Color(0xFFD9FF66),
    secondary = NeoColors.Cyan,
    onSecondary = NeoColors.Ink,
    tertiary = NeoColors.Pink,
    onTertiary = NeoColors.Ink,
    outline = NeoColors.DarkBorder,
    outlineVariant = Color(0xFF252C37),
    error = Color(0xFFF87171),
    onError = NeoColors.Ink,
    errorContainer = Color(0xFF3B1214),
    onErrorContainer = Color(0xFFFECACA),
)

private val NeoLightScheme = NeoColorScheme(
    background = Color(0xFFF5F6F0),
    onBackground = NeoColors.Ink,
    surface = Color(0xFFFFFFFF),
    onSurface = NeoColors.Ink,
    surfaceVariant = Color(0xFFE8ECE2),
    onSurfaceVariant = Color(0xFF475569),
    surfaceContainerHigh = Color(0xFFDDE3D5),
    primary = NeoColors.Lime,
    onPrimary = NeoColors.Ink,
    primaryContainer = Color(0xFFE5FAB3),
    onPrimaryContainer = Color(0xFF142404),
    secondary = NeoColors.Cyan,
    onSecondary = NeoColors.Ink,
    tertiary = NeoColors.Pink,
    onTertiary = NeoColors.Ink,
    outline = NeoColors.Ink,
    outlineVariant = Color(0xFF2D333B),
    error = Color(0xFFEF4444),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
)

// ── From-scratch typography. No Material3. ──
data class NeoTypography(
    val displayLarge: TextStyle,
    val headlineLarge: TextStyle,
    val headlineMedium: TextStyle,
    val headlineSmall: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle,
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

data class NeoShapeScale(
    val extraSmall: Shape,
    val small: Shape,
    val medium: Shape,
    val large: Shape,
    val extraLarge: Shape,
)

private val NeoShapeValues = NeoShapeScale(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

val LocalNeoColorScheme = staticCompositionLocalOf { NeoDarkScheme }
val LocalNeoTypography = staticCompositionLocalOf { NeoTypeScale }
val LocalNeoShapes = staticCompositionLocalOf { NeoShapeValues }

@Composable
@ReadOnlyComposable
fun neoColorScheme(): NeoColorScheme {
    return if (isSystemInDarkTheme()) NeoDarkScheme else NeoLightScheme
}

/**
 * Drop-in from-scratch replacement for the Material3 MaterialTheme object.
 * Same member surface (colorScheme / typography / shapes) so call sites
 * keep working after swapping the import — zero Material3 code runs.
 */
object MaterialTheme {
    val colorScheme: NeoColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalNeoColorScheme.current

    val typography: NeoTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalNeoTypography.current

    val shapes: NeoShapeScale
        @Composable
        @ReadOnlyComposable
        get() = LocalNeoShapes.current
}
