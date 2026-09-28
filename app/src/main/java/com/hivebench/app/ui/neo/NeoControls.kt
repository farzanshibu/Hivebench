package com.hivebench.app.ui.neo

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun NeoIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cornerRadius: Dp = NeoShapes.Md,
    containerColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val neo = rememberNeoColors()
    Box(
        modifier = modifier
            .neoTactile(
                enabled = enabled,
                shadowOffset = 2.5.dp,
                pressedOffset = 1.dp,
                cornerRadius = cornerRadius,
                onClick = if (enabled) onClick else null,
            )
            .background(containerColor ?: neo.surface, RoundedCornerShape(cornerRadius))
            .border(NeoBorders.Thin, neo.border, RoundedCornerShape(cornerRadius))
            .size(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        NeoButtonContentColor(if (enabled) neo.ink else neo.muted, content)
    }
}

@Composable
fun NeoCircleIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val neo = rememberNeoColors()
    Box(
        modifier = modifier
            .neoBounce(enabled = enabled, onClick = if (enabled) onClick else null)
            .size(40.dp)
            .background(neo.surface, CircleShape)
            .border(NeoBorders.Thin, neo.border, CircleShape)
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        NeoButtonContentColor(if (enabled) neo.ink else neo.muted, content)
    }
}

@Composable
fun NeoProgressBar(
    progress: Float?, // null = indeterminate shimmer track
    modifier: Modifier = Modifier,
) {
    val neo = rememberNeoColors()
    val animated by animateFloatAsState(
        targetValue = (progress ?: 0.35f).coerceIn(0f, 1f),
        animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow),
        label = "neoProgress",
    )
    Box(
        modifier = modifier
            .background(neo.surfaceVariant, RoundedCornerShape(50))
            .border(NeoBorders.Hairline, neo.border, RoundedCornerShape(50)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(10.dp)
                .background(neo.accent, RoundedCornerShape(50)),
        )
    }
}
