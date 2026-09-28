package com.hivebench.app.ui.neo

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ── From-scratch progress indicators. Same call surface as the old toolkit. ──

@Composable
fun LinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    trackColor: Color = Color.Unspecified,
    strokeCap: StrokeCap = StrokeCap.Round,
) {
    LinearProgressIndicator(
        progress = progress(),
        modifier = modifier,
        color = color,
        trackColor = trackColor,
        strokeCap = strokeCap,
    )
}

@Composable
fun LinearProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    trackColor: Color = Color.Unspecified,
    strokeCap: StrokeCap = StrokeCap.Round,
) {
    val neo = rememberNeoColors()
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow),
        label = "neoLinearProgress",
    )
    val bar = if (color == Color.Unspecified) neo.accent else color
    val track = if (trackColor == Color.Unspecified) neo.surfaceVariant else trackColor
    val cap = if (strokeCap == StrokeCap.Round) 50.dp else NeoShapes.Sm
    Box(
        modifier
            .clip(RoundedCornerShape(cap))
            .background(track, RoundedCornerShape(cap))
            .border(NeoBorders.Hairline, neo.border, RoundedCornerShape(cap)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(7.dp)
                .background(bar, RoundedCornerShape(cap)),
        )
    }
}

@Composable
fun LinearProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    trackColor: Color = Color.Unspecified,
    strokeCap: StrokeCap = StrokeCap.Round,
) {
    // Indeterminate: full-width accent bar (morphs via parent AnimatedVisibility).
    LinearProgressIndicator(
        progress = 1f,
        modifier = modifier,
        color = color,
        trackColor = trackColor,
        strokeCap = strokeCap,
    )
}

@Composable
fun CircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    strokeWidth: Dp = 3.dp,
    trackColor: Color = Color.Unspecified,
    strokeCap: StrokeCap = StrokeCap.Round,
) {
    val neo = rememberNeoColors()
    val dot = if (color == Color.Unspecified) neo.accent else color
    Box(
        modifier = modifier.size(24.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        PulsingDot(color = dot, size = 12.dp)
    }
}
