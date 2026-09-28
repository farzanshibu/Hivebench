package com.hivebench.app.ui.neo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp

/**
 * Foundation-only card. Static or tactile-clickable.
 */
@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = NeoShapes.Lg,
    borderWidth: Dp = NeoBorders.Regular,
    borderColor: Color? = null,
    backgroundColor: Color? = null,
    shadowOffset: Dp = NeoShadows.Regular,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val neo = rememberNeoColors()
    val bc = borderColor ?: neo.border
    // Translucent fills sit on the surface so the hard shadow never shows through.
    val bg = (backgroundColor ?: neo.surface).let { if (it.alpha < 1f) it.compositeOver(neo.surface) else it }

    val decorated = if (onClick != null) {
        modifier
            .neoTactile(
                shadowOffset = shadowOffset,
                pressedOffset = NeoShadows.Pressed,
                cornerRadius = cornerRadius,
                onClick = onClick,
            )
            .background(bg, RoundedCornerShape(cornerRadius))
            .border(borderWidth, bc, RoundedCornerShape(cornerRadius))
    } else {
        modifier
            .neoShadow(offsetX = shadowOffset, offsetY = shadowOffset, cornerRadius = cornerRadius)
            .background(bg, RoundedCornerShape(cornerRadius))
            .border(borderWidth, bc, RoundedCornerShape(cornerRadius))
    }
    Column(modifier = decorated, content = content)
}
