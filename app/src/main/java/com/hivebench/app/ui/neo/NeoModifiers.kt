package com.hivebench.app.ui.neo

import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring

/**
 * Foundation-only neubrutalist modifiers. Zero Material3.
 * Replaces theme/Neobrutal.kt modifiers with detent/morph-aware versions.
 */

fun Modifier.neoShadow(
    offsetX: Dp = NeoShadows.Regular,
    offsetY: Dp = NeoShadows.Regular,
    shadowColor: Color? = null,
    cornerRadius: Dp = NeoShapes.Md,
): Modifier = composed {
    val dark = isSystemInDarkTheme()
    val final = shadowColor ?: if (dark) NeoColors.Cream.copy(alpha = 0.92f) else NeoColors.Ink
    drawBehind {
        val r = cornerRadius.toPx()
        drawHardShadow(final, offsetX.toPx(), offsetY.toPx(), r)
    }
}

fun Modifier.neoBorder(
    width: Dp = NeoBorders.Regular,
    color: Color? = null,
    cornerRadius: Dp = NeoShapes.Md,
): Modifier = composed {
    val dark = isSystemInDarkTheme()
    val c = color ?: if (dark) NeoColors.DarkBorder else NeoColors.Ink
    border(width = width, color = c, shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius))
}

/**
 * Tactile press morph: shadow collapses, card translates into shadow, scale 0.98.
 * Springy tactile per locked motion spec. Replaces ripple.
 */
fun Modifier.neoTactile(
    enabled: Boolean = true,
    shadowOffset: Dp = NeoShadows.Regular,
    pressedOffset: Dp = NeoShadows.Pressed,
    shadowColor: Color? = null,
    cornerRadius: Dp = NeoShapes.Md,
    onClick: (() -> Unit)? = null,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val active = pressed && enabled

    val currentOffset by animateDpAsState(
        targetValue = if (active) pressedOffset else shadowOffset,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "neoShadowOffset",
    )
    val travel by animateDpAsState(
        targetValue = if (active) (shadowOffset - pressedOffset) else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "neoTravel",
    )
    val scale by animateFloatAsState(
        targetValue = if (active) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "neoScale",
    )
    val dark = isSystemInDarkTheme()
    val sc = shadowColor ?: if (dark) NeoColors.Cream.copy(alpha = 0.92f) else NeoColors.Ink

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            translationX = travel.toPx()
            translationY = travel.toPx()
        }
        .drawBehind {
            val r = cornerRadius.toPx()
            drawHardShadow(sc, currentOffset.toPx(), currentOffset.toPx(), r)
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick,
                )
            } else Modifier
        )
}

fun Modifier.neoBounce(
    enabled: Boolean = true,
    pressedScale: Float = 0.92f,
    onClick: (() -> Unit)? = null,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "neoBounce",
    )
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick,
                )
            } else Modifier
        )
}

/**
 * Hard offset shadow that skips the area under the element itself, so a
 * translucent fill on top never shows the shadow through it.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHardShadow(color: androidx.compose.ui.graphics.Color, dx: Float, dy: Float, r: Float) {
    val self = androidx.compose.ui.graphics.Path().apply {
        addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, size.width, size.height, CornerRadius(r, r)))
    }
    clipPath(self, androidx.compose.ui.graphics.ClipOp.Difference) {
        drawRoundRect(color = color, topLeft = Offset(dx, dy), size = size, cornerRadius = CornerRadius(r, r))
    }
}
