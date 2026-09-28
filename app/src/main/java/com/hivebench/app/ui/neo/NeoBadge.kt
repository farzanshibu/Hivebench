package com.hivebench.app.ui.neo

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PulsingDot(
    modifier: Modifier = Modifier,
    color: Color = NeoColors.Lime,
    size: Dp = 8.dp,
) {
    val t = rememberInfiniteTransition(label = "neoPulse")
    val alpha by t.animateFloat(0.35f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse), label = "a")
    val ring by t.animateFloat(0.9f, 1.35f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "r")
    Box(modifier = modifier.size(size * 1.5f), contentAlignment = Alignment.Center) {
        Box(Modifier.size(size * ring).background(color.copy(alpha = alpha * 0.35f), CircleShape))
        Box(Modifier.size(size).background(color, CircleShape))
    }
}

@Composable
fun NeoBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    contentColor: Color? = null,
    borderColor: Color? = null,
    isPulsing: Boolean = false,
) {
    val neo = rememberNeoColors()
    val bg = containerColor ?: neo.accent
    val fg = contentColor ?: neo.onAccent
    val bc = borderColor ?: neo.border
    Box(
        modifier = modifier
            .neoShadow(offsetX = 2.dp, offsetY = 2.dp, cornerRadius = NeoShapes.Pill)
            .background(bg, RoundedCornerShape(NeoShapes.Pill))
            .border(NeoBorders.Thin, bc, RoundedCornerShape(NeoShapes.Pill)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isPulsing) {
                PulsingDot(color = fg, size = 6.dp)
                Spacer(Modifier.width(5.dp))
            }
            NeoText(text = text.uppercase(), style = NeoType.Badge, color = fg)
        }
    }
}

@Composable
fun NeoDot(
    modifier: Modifier = Modifier,
    color: Color = NeoColors.Lime,
    size: Dp = 8.dp,
    selected: Boolean = false,
) {
    val neo = rememberNeoColors()
    Box(modifier = modifier.size(size * 2f), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(size)
                .background(if (selected) NeoColors.Lime else Color.Transparent, CircleShape)
                .border(1.5.dp, if (selected) NeoColors.Lime else neo.border.copy(alpha = 0.6f), CircleShape)
        )
    }
}
