package com.hivebench.app.ui.neo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class NeoButtonVariant { Primary, Secondary, Dark, Danger, Ghost }

@Composable
private fun variantColors(variant: NeoButtonVariant, neo: NeoThemeColors): Pair<Color, Color> {
    return when (variant) {
        NeoButtonVariant.Primary -> NeoColors.Lime to NeoColors.Ink
        NeoButtonVariant.Secondary -> neo.surface to neo.ink
        NeoButtonVariant.Dark -> NeoColors.Ink to NeoColors.Lime
        NeoButtonVariant.Danger -> NeoColors.Pink to NeoColors.Ink
        NeoButtonVariant.Ghost -> Color.Transparent to neo.ink
    }
}

/**
 * Foundation-only neubrutalist button. No Material3.
 * Tactile morph: shadow 3.5→1dp, translate, scale 0.98 on press.
 */
@Composable
fun NeoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: NeoButtonVariant = NeoButtonVariant.Primary,
    enabled: Boolean = true,
    cornerRadius: Dp = NeoShapes.Md,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val neo = rememberNeoColors()
    val (bg, fg) = variantColors(variant, neo)
    val finalBg = if (enabled) bg else bg.copy(alpha = 0.45f)
    val finalFg = if (enabled) fg else fg.copy(alpha = 0.5f)
    val borderColor = if (variant == NeoButtonVariant.Ghost) neo.border.copy(alpha = 0.7f) else neo.border

    Box(
        modifier = modifier
            .neoTactile(
                enabled = enabled,
                shadowOffset = NeoShadows.Regular,
                pressedOffset = NeoShadows.Pressed,
                cornerRadius = cornerRadius,
                onClick = if (enabled) onClick else null,
            )
            .background(finalBg, RoundedCornerShape(cornerRadius))
            .border(NeoBorders.Regular, borderColor, RoundedCornerShape(cornerRadius))
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        NeoButtonContentColor(color = finalFg) {
            Row(verticalAlignment = Alignment.CenterVertically, content = content)
        }
    }
}

@Composable
fun NeoButtonLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    NeoText(text = text, modifier = modifier, style = NeoType.BodyBold, color = color)
}
