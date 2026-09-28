package com.hivebench.app.ui.theme

import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hivebench.app.ui.neo.LocalNeoContentColor

// ── Hivebench palette: warm black canvas, cream outlines/shadows, six loud accents ──
// `NeoLime` keeps its name for source compatibility but is now the primary accent (yellow).
val NeoLime = Color(0xFFFFD000)        // Primary accent: signal yellow
val NeoLimeDark = Color(0xFFE0B400)    // Deeper yellow for pressed/borders
val NeoLimeLight = Color(0xFFFFE680)   // Pale yellow highlight
val NeoBlack = Color(0xFF111111)       // Ink
val NeoWhite = Color(0xFFFFFFFF)       // Paper
val NeoMint = Color(0xFF00DD8F)        // Positive / active / running
val NeoCyan = Color(0xFF4B8BFF)        // Blue accent
val NeoPink = Color(0xFFFF6BB5)        // Pink accent
val NeoYellow = Color(0xFFFFD000)      // Yellow accent
val NeoPurple = Color(0xFFA98BFF)      // Purple accent
val NeoOrange = Color(0xFFFF9A2E)      // Orange accent
val NeoCream = Color(0xFFF1EDE3)       // Dark-mode outlines and hard shadows
val NeoDarkSurface = Color(0xFF242424) // Card surface on the dark canvas
val NeoDarkBorder = NeoCream           // Outline in dark mode

/**
 * Draws a solid, non-blurred offset drop shadow behind the composable.
 */
fun Modifier.neoShadow(
    offsetX: Dp = 5.dp,
    offsetY: Dp = 5.dp,
    shadowColor: Color? = null,
    cornerRadius: Dp = 12.dp,
): Modifier = composed {
    val isDark = isSystemInDarkTheme()
    val finalShadowColor = shadowColor ?: if (isDark) NeoCream.copy(alpha = 0.92f) else NeoBlack
    drawBehind {
        val r = cornerRadius.toPx()
        drawHardShadow(finalShadowColor, offsetX.toPx(), offsetY.toPx(), r)
    }
}

/**
 * Draws a circular solid drop shadow.
 */
fun Modifier.neoCircleShadow(
    offsetX: Dp = 3.dp,
    offsetY: Dp = 3.dp,
    shadowColor: Color? = null,
): Modifier = composed {
    val isDark = isSystemInDarkTheme()
    val finalShadowColor = shadowColor ?: if (isDark) NeoCream.copy(alpha = 0.92f) else NeoBlack
    drawBehind {
        drawCircle(
            color = finalShadowColor,
            radius = size.minDimension / 2f,
            center = Offset(size.width / 2f + offsetX.toPx(), size.height / 2f + offsetY.toPx()),
        )
    }
}

/**
 * Adds high-contrast neobrutalist border with specified corner radius.
 */
fun Modifier.neoBorder(
    width: Dp = 2.dp,
    color: Color? = null,
    cornerRadius: Dp = 12.dp,
): Modifier = composed {
    val isDark = isSystemInDarkTheme()
    val borderColor = color ?: if (isDark) NeoDarkBorder else NeoBlack
    border(
        width = width,
        color = borderColor,
        shape = RoundedCornerShape(cornerRadius),
    )
}

/**
 * Mechanical tactile press animation for Neobrutalist buttons, cards, and chips.
 * When pressed:
 * - Translates down-right by 2.dp into the shadow
 * - Compresses slightly (scale 0.98f)
 * - Shadow offset collapses from 4.dp to 1.5.dp
 * - On release: Snaps back with lively spring physics
 */
fun Modifier.neoTactile(
    enabled: Boolean = true,
    shadowOffset: Dp = 3.5.dp,
    pressedOffset: Dp = 1.dp,
    shadowColor: Color? = null,
    cornerRadius: Dp = 12.dp,
    onClick: (() -> Unit)? = null,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val currentOffset by animateDpAsState(
        targetValue = if (isPressed && enabled) pressedOffset else shadowOffset,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "neoShadowOffset",
    )
    val translationDistance by animateDpAsState(
        targetValue = if (isPressed && enabled) (shadowOffset - pressedOffset) else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "neoTranslation",
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "neoScale",
    )

    val isDark = isSystemInDarkTheme()
    val finalShadowColor = shadowColor ?: if (isDark) NeoCream.copy(alpha = 0.92f) else NeoBlack

    this
        .graphicsLayer {
            this.scaleX = scale
            this.scaleY = scale
            this.translationX = translationDistance.toPx()
            this.translationY = translationDistance.toPx()
        }
        .drawBehind {
            val r = cornerRadius.toPx()
            drawHardShadow(finalShadowColor, currentOffset.toPx(), currentOffset.toPx(), r)
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null, // Tactile mechanical motion replaces ripple
                    enabled = enabled,
                    onClick = onClick,
                )
            } else Modifier
        )
}

/**
 * Bouncy spring press scale effect for icon buttons and compact items.
 */
fun Modifier.neoBounce(
    enabled: Boolean = true,
    pressedScale: Float = 0.92f,
    onClick: (() -> Unit)? = null,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "neoBounceScale",
    )

    this
        .graphicsLayer {
            this.scaleX = scale
            this.scaleY = scale
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick,
                )
            } else Modifier
        )
}

/**
 * Animated breathing status dot with outer pulse wave.
 */
@Composable
fun PulsingDot(
    modifier: Modifier = Modifier,
    color: Color = NeoLime,
    size: Dp = 8.dp,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseRingScale",
    )

    Box(modifier = modifier.size(size * 1.5f), contentAlignment = Alignment.Center) {
        // Outer pulsing ring
        Box(
            modifier = Modifier
                .size(size * ringScale)
                .background(color.copy(alpha = alpha * 0.35f), CircleShape)
        )
        // Solid core dot
        Box(
            modifier = Modifier
                .size(size)
                .background(color, CircleShape)
        )
    }
}

val LocalNeoContentColorLegacy = compositionLocalOf { Color.Unspecified }

/**
 * Retro Neobrutalist tag / badge with thick border and optional pulsing indicator.
 * Foundation-only (no Material3).
 */
@Composable
fun NeoBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = NeoLime,
    color: Color = containerColor,
    contentColor: Color = NeoBlack,
    textColor: Color = contentColor,
    borderColor: Color? = null,
    isPulsing: Boolean = false,
) {
    val finalBg = if (color != containerColor) color else containerColor
    val finalTxt = if (textColor != contentColor) textColor else contentColor
    val isDark = isSystemInDarkTheme()
    val finalBorderColor = borderColor ?: if (isDark) NeoDarkBorder else NeoBlack
    Box(
        modifier = modifier
            .neoShadow(offsetX = 2.dp, offsetY = 2.dp, cornerRadius = 50.dp)
            .background(finalBg, RoundedCornerShape(50))
            .border(BorderStroke(1.5.dp, finalBorderColor), RoundedCornerShape(50)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isPulsing) {
                PulsingDot(color = finalTxt, size = 6.dp)
                Spacer(Modifier.width(5.dp))
            }
            BasicText(
                text = text.uppercase(),
                style = TextStyle(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = finalTxt,
                    letterSpacing = 0.8.sp,
                ),
            )
        }
    }
}

/**
 * Tactile Neobrutalist Button with solid offset shadow and bold styling.
 * Foundation-only (no Material3).
 */
@Composable
fun NeoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = NeoLime,
    buttonColor: Color = containerColor,
    contentColor: Color = NeoBlack,
    textColor: Color = contentColor,
    borderColor: Color? = null,
    cornerRadius: Dp = 12.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val finalBg = if (buttonColor != containerColor) buttonColor else containerColor
    val finalTxt = if (textColor != contentColor) textColor else contentColor
    val isDark = isSystemInDarkTheme()
    val finalBorderColor = borderColor ?: if (isDark) NeoDarkBorder else NeoBlack

    Box(
        modifier = modifier
            .neoTactile(
                enabled = enabled,
                shadowOffset = 3.5.dp,
                pressedOffset = 1.dp,
                cornerRadius = cornerRadius,
                onClick = onClick,
            )
            .background(
                color = if (enabled) finalBg else finalBg.copy(alpha = 0.5f),
                shape = RoundedCornerShape(cornerRadius),
            )
            .border(
                width = 2.dp,
                color = finalBorderColor,
                shape = RoundedCornerShape(cornerRadius),
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(
            LocalNeoContentColorLegacy provides finalTxt,
            LocalNeoContentColor provides finalTxt,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

/**
 * Reusable Neobrutalist Card with thick border, hard drop shadow, and optional click motion.
 * Foundation-only (no Material3). backgroundColor defaults to theme surface.
 */
@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    borderWidth: Dp = 2.dp,
    borderColor: Color? = null,
    backgroundColor: Color? = null,
    shadowOffset: Dp = 5.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val finalBorderColor = borderColor ?: if (isDark) NeoDarkBorder else NeoBlack
    // Translucent fills are composited onto the surface so the hard shadow behind the card
    // (cream in dark mode) never shows through.
    val finalBg = (backgroundColor ?: if (isDark) NeoDarkSurface else NeoWhite).let { bg ->
        if (bg.alpha < 1f) bg.compositeOver(if (isDark) NeoDarkSurface else NeoWhite) else bg
    }

    val cardModifier = if (onClick != null) {
        modifier
            .neoTactile(
                shadowOffset = shadowOffset,
                pressedOffset = 1.dp,
                cornerRadius = cornerRadius,
                onClick = onClick,
            )
            .background(finalBg, RoundedCornerShape(cornerRadius))
            .border(borderWidth, finalBorderColor, RoundedCornerShape(cornerRadius))
    } else {
        modifier
            .neoShadow(offsetX = shadowOffset, offsetY = shadowOffset, cornerRadius = cornerRadius)
            .background(finalBg, RoundedCornerShape(cornerRadius))
            .border(borderWidth, finalBorderColor, RoundedCornerShape(cornerRadius))
    }

    Column(
        modifier = cardModifier,
        content = content,
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
