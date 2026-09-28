package com.hivebench.app.ui.neo

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

enum class DrawerDetent(val fraction: Float) {
    Peek(0.38f),
    Half(0.62f),
    Full(0.92f),
}

private fun nearestDetent(fraction: Float, allowed: List<DrawerDetent>): DrawerDetent {
    return allowed.minByOrNull { kotlin.math.abs(it.fraction - fraction) } ?: DrawerDetent.Half
}

/**
 * Proper neubrutalist bottom drawer. Foundation-only, zero Material3.
 *
 * Morph open/close: scrim fade + panel slide/scale/corner morph with springs.
 * 3-detent: Peek / Half / Full with drag-to-resize + fling-snap + swipe-down dismiss.
 */
@Composable
fun NeoBottomDrawer(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    initialDetent: DrawerDetent = DrawerDetent.Half,
    allowedDetents: List<DrawerDetent> = listOf(DrawerDetent.Peek, DrawerDetent.Half, DrawerDetent.Full),
    title: String = "",
    subtitle: String? = null,
    searchValue: String? = null,
    onSearchChange: ((String) -> Unit)? = null,
    searchPlaceholder: String = "Search",
    headerActions: (@Composable RowScope.() -> Unit)? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.(detent: DrawerDetent, setDetent: (DrawerDetent) -> Unit) -> Unit,
) {
    var detent by remember(visible) { mutableStateOf(initialDetent) }
    var dragPx by remember { mutableFloatStateOf(0f) }

    // Animate height fraction toward detent target (morph, not jump)
    val fraction by animateFloatAsState(
        targetValue = detent.fraction,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "drawerFraction",
    )
    val corner by animateDpAsState(
        targetValue = if (detent == DrawerDetent.Full) 16.dp else 20.dp,
        animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium),
        label = "drawerCorner",
    )
    val scrimAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(180, easing = NeoMotion.EaseOut),
        label = "scrim",
    )

    LaunchedEffect(visible) {
        if (visible) {
            detent = initialDetent
            dragPx = 0f
        }
    }

    if (!visible) return

    // Own window: the drawer must sit above the Scaffold and bottom bar no matter
    // where in the tree the caller composes it.
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false,
        ),
    ) {
        (LocalView.current.parent as? DialogWindowProvider)?.window?.let { window ->
            LaunchedEffect(window) {
                window.setDimAmount(0f)
                window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            }
        }
        DrawerBody(
            visible = visible,
            onDismiss = onDismiss,
            modifier = modifier,
            detent = detent,
            setDetent = { detent = it },
            dragPx = dragPx,
            setDragPx = { dragPx = it },
            fraction = fraction,
            corner = corner,
            scrimAlpha = scrimAlpha,
            allowedDetents = allowedDetents,
            title = title,
            subtitle = subtitle,
            searchValue = searchValue,
            onSearchChange = onSearchChange,
            searchPlaceholder = searchPlaceholder,
            headerActions = headerActions,
            footer = footer,
            content = content,
        )
    }
}

@Composable
private fun DrawerBody(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier,
    detent: DrawerDetent,
    setDetent: (DrawerDetent) -> Unit,
    dragPx: Float,
    setDragPx: (Float) -> Unit,
    fraction: Float,
    corner: androidx.compose.ui.unit.Dp,
    scrimAlpha: Float,
    allowedDetents: List<DrawerDetent>,
    title: String,
    subtitle: String?,
    searchValue: String?,
    onSearchChange: ((String) -> Unit)?,
    searchPlaceholder: String,
    headerActions: (@Composable RowScope.() -> Unit)?,
    footer: (@Composable ColumnScope.() -> Unit)?,
    content: @Composable ColumnScope.(detent: DrawerDetent, setDetent: (DrawerDetent) -> Unit) -> Unit,
) {
    val neo = rememberNeoColors()
    BackHandler(enabled = visible, onBack = onDismiss)

    val dragState = rememberDraggableState { delta ->
        // Only track downward drag for dismiss + detent shrink; upward reduces offset
        setDragPx((dragPx + delta).coerceIn(-220f, 600f))
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Scrim with fade morph
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = scrimAlpha * 0.55f }
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )

        // Panel with slide + scale + corner morph
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(
                initialOffsetY = { it / 2 },
                animationSpec = tween(280, easing = NeoMotion.EaseOut),
            ) + fadeIn(tween(180)),
            exit = fadeOut(tween(160)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction)
                    .graphicsLayer {
                        translationY = dragPx.coerceAtLeast(0f)
                        scaleX = 1f - (dragPx.coerceAtLeast(0f) / 3000f).coerceIn(0f, 0.04f)
                    }
                    .neoShadow(offsetX = 0.dp, offsetY = (-4).dp, cornerRadius = corner)
                    .background(neo.surface, RoundedCornerShape(topStart = corner, topEnd = corner))
                    .border(
                        NeoBorders.Regular,
                        neo.border,
                        RoundedCornerShape(topStart = corner, topEnd = corner),
                    )
                    .clip(RoundedCornerShape(topStart = corner, topEnd = corner)),
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    // Drag handle: drag to resize across detents, fling down to dismiss
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .draggable(
                                state = dragState,
                                orientation = Orientation.Vertical,
                                onDragStopped = { velocity ->
                                    val downFling = velocity > 900f
                                    when {
                                        downFling || dragPx > 140f -> onDismiss()
                                        dragPx > 48f -> {
                                            // shrink one detent
                                            val idx = allowedDetents.indexOf(detent)
                                            if (idx > 0) setDetent(allowedDetents[idx - 1])
                                        }
                                        dragPx < -70f -> {
                                            val idx = allowedDetents.indexOf(detent)
                                            if (idx < allowedDetents.lastIndex) setDetent(allowedDetents[idx + 1])
                                        }
                                    }
                                    setDragPx(0f)
                                },
                            )
                            .padding(vertical = 6.dp),
                    ) {
                        Box(
                            Modifier
                                .width(48.dp)
                                .height(5.dp)
                                .background(neo.border, RoundedCornerShape(50)),
                        )
                    }

                    if (title.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                NeoText(text = title, style = NeoType.Title)
                                if (subtitle != null) NeoCaption(text = subtitle)
                            }
                            if (headerActions != null) {
                                Row(verticalAlignment = Alignment.CenterVertically, content = headerActions)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }

                    if (searchValue != null && onSearchChange != null) {
                        NeoSearchField(
                            value = searchValue,
                            onValueChange = onSearchChange,
                            placeholder = searchPlaceholder,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(10.dp))
                    }

                    content(detent, setDetent)

                    if (footer != null) {
                        Spacer(Modifier.height(10.dp))
                        footer()
                    }
                }
            }
        }
    }
}

/**
 * Staggered entrance wrapper for drawer rows: fade + slide with per-index delay.
 * Gives the smooth morph list feel.
 */
@Composable
fun NeoStagger(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = true,
        modifier = modifier,
        enter = slideInVertically(
            initialOffsetY = { 24 },
            animationSpec = tween(
                durationMillis = 220,
                delayMillis = NeoMotion.staggerDelayMs(index),
                easing = NeoMotion.EaseOut,
            ),
        ) + fadeIn(
            tween(
                durationMillis = 180,
                delayMillis = NeoMotion.staggerDelayMs(index),
            ),
        ),
    ) {
        content()
    }
}
