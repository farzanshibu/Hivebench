package com.hivebench.app.ui.neo

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically

// ── Central motion system: springy tactile neubrutalism ──
object NeoMotion {
    // Press / tactile
    val PressSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium,
    )
    val PressDpSpring = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    // Drawer morph open: lively but not bouncy (no overshoot past screen edge)
    val DrawerOpen = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
    val DrawerOpenDp = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    // Drawer close: fast + decisive
    fun closeTween(duration: Int = 220) = tween<Float>(
        durationMillis = duration,
        easing = CubicBezierEasing(0.32f, 0.72f, 0.0f, 1.0f),
    )

    // Scrim fade
    fun scrimIn(duration: Int = 180) = tween<Float>(durationMillis = duration)
    fun scrimOut(duration: Int = 160) = tween<Float>(durationMillis = duration)

    // Staggered list entrance
    fun staggerDelayMs(index: Int, stepMs: Int = 25, maxItems: Int = 8): Int {
        val clamped = index.coerceIn(0, maxItems - 1)
        return clamped * stepMs
    }

    val EaseOut = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)
    val EaseInOut = CubicBezierEasing(0.65f, 0.0f, 0.35f, 1.0f)

    fun entranceTween(delayMs: Int = 0, duration: Int = 220) = tween<Float>(
        durationMillis = duration,
        delayMillis = delayMs,
        easing = EaseOut,
    )
}

// ── Drawer enter/exit transitions (slide + fade + scale morph) ──
fun neoDrawerEnter() =
    slideInVertically(
        initialOffsetY = { it / 3 },
        animationSpec = tween(280, easing = NeoMotion.EaseOut),
    ) + fadeIn(tween(180)) + scaleIn(
        initialScale = 0.96f,
        animationSpec = tween(280, easing = NeoMotion.EaseOut),
    )

fun neoDrawerExit() =
    fadeOut(tween(160)) + scaleOut(
        targetScale = 0.97f,
        animationSpec = tween(160),
    )
