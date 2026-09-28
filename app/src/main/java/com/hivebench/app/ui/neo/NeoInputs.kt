package com.hivebench.app.ui.neo

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun NeoSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val neo = rememberNeoColors()
    val knob by animateFloatAsState(
        if (checked) 1f else 0f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "switch",
    )
    Box(
        modifier = modifier
            .neoShadow(2.dp, 2.dp, cornerRadius = 50.dp)
            .background(if (checked) neo.positive else neo.surfaceVariant, RoundedCornerShape(50))
            .border(NeoBorders.Regular, neo.border, RoundedCornerShape(50))
            .neoBounce(enabled = enabled, onClick = { if (enabled) onCheckedChange(!checked) })
            .padding(4.dp)
            .width(52.dp),
    ) {
        Box(
            Modifier
                .size(22.dp)
                .graphicsLayer { translationX = knob * 28.dp.toPx() }
                .background(if (checked) neo.onAccent else neo.muted, CircleShape)
                .border(1.5.dp, neo.border, CircleShape),
        )
    }
}

@Composable
fun NeoCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val neo = rememberNeoColors()
    Box(
        modifier = modifier
            .neoShadow(2.dp, 2.dp, cornerRadius = 8.dp)
            .background(if (checked) neo.positive else neo.surface, RoundedCornerShape(8.dp))
            .border(NeoBorders.Regular, neo.border, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .neoBounce(enabled = enabled, onClick = { if (enabled) onCheckedChange(!checked) })
            .size(26.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) NeoText("✓", style = NeoType.BodyBold, color = neo.onAccent)
    }
}

@Composable
fun NeoDrawerRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badge: String? = null,
    badgeColor: androidx.compose.ui.graphics.Color? = null,
    selected: Boolean = false,
    cornerRadius: Dp = NeoShapes.Lg,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    val neo = rememberNeoColors()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (selected) Modifier.neoShadow(offsetX = 3.dp, offsetY = 3.dp, cornerRadius = cornerRadius) else Modifier.neoShadow(offsetX = 2.dp, offsetY = 2.dp, cornerRadius = cornerRadius))
            .background(
                if (selected) neo.accent.copy(alpha = 0.16f) else neo.surface,
                RoundedCornerShape(cornerRadius),
            )
            .border(
                if (selected) NeoBorders.Regular else NeoBorders.Thin,
                if (selected) neo.accent else neo.border.copy(alpha = 0.6f),
                RoundedCornerShape(cornerRadius),
            )
            .neoTactile(cornerRadius = cornerRadius, shadowOffset = 2.dp, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NeoText(
                        text = title,
                        style = NeoType.BodyBold,
                        color = if (selected) neo.accent else neo.ink,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                    if (badge != null) {
                        Spacer(Modifier.width(8.dp))
                        NeoBadge(
                            text = badge,
                            containerColor = if (selected) neo.accent else neo.surfaceVariant,
                            contentColor = if (selected) neo.onAccent else neo.muted,
                        )
                    }
                }
                if (subtitle != null) {
                    NeoMono(text = subtitle)
                }
            }
            if (trailing != null) trailing() else NeoDot(selected = selected)
        }
    }
}

@Composable
fun NeoSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    action: (@Composable RowScope.() -> Unit)? = null,
) {
    val neo = rememberNeoColors()
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        NeoText(text = title.uppercase(), style = NeoType.Badge, color = neo.muted)
        if (count != null) {
            Spacer(Modifier.width(8.dp))
            NeoBadge(text = count.toString())
        }
        Spacer(Modifier.weight(1f))
        if (action != null) Row(verticalAlignment = Alignment.CenterVertically, content = action)
    }
}
