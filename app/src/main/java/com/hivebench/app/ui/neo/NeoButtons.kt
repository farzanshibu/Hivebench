package com.hivebench.app.ui.neo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

// ── From-scratch button family. Same call surface as the old toolkit. ──

data class NeoButtonColors(
    val containerColor: Color,
    val contentColor: Color,
    val disabledContainerColor: Color,
    val disabledContentColor: Color,
)

object ButtonDefaults {
    fun buttonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
    ): NeoButtonColors {
        return NeoButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
        )
    }
}

@Composable
private fun NeoToolkitButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    colors: NeoButtonColors? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    border: BorderStroke? = null,
    outlined: Boolean = false,
    textOnly: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val neo = rememberNeoColors()
    val palette = colors ?: ButtonDefaults.buttonColors()
    val bg = when {
        !enabled && palette.disabledContainerColor != Color.Unspecified -> palette.disabledContainerColor
        !enabled -> neo.surfaceVariant.copy(alpha = 0.6f)
        outlined || textOnly -> Color.Transparent
        palette.containerColor != Color.Unspecified -> palette.containerColor
        else -> neo.accent
    }
    val fg = when {
        !enabled && palette.disabledContentColor != Color.Unspecified -> palette.disabledContentColor
        !enabled -> neo.muted
        palette.contentColor != Color.Unspecified -> palette.contentColor
        textOnly -> neo.accent
        outlined -> neo.ink
        else -> neo.onAccent
    }
    val borderStroke = border ?: if (outlined && !textOnly) {
        BorderStroke(NeoBorders.Thin, if (enabled) neo.border else neo.muted)
    } else {
        null
    }
    // Resolve corner for the hard shadow.
    val corner = NeoShapes.Md
    var decorated = modifier.then(
        if (textOnly) {
            Modifier.neoBounce(enabled = enabled, onClick = if (enabled) onClick else null)
        } else {
            Modifier.neoTactile(
                enabled = enabled,
                shadowOffset = NeoShadows.Small,
                pressedOffset = NeoShadows.Pressed,
                cornerRadius = corner,
                onClick = if (enabled) onClick else null,
            )
        }
    )
        .background(bg, shape)
    if (borderStroke != null) {
        decorated = decorated.border(borderStroke, shape)
    }
    Box(
        modifier = decorated.padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        NeoButtonContentColor(fg) {
            Row(verticalAlignment = Alignment.CenterVertically, content = content)
        }
    }
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    colors: NeoButtonColors? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    border: BorderStroke? = null,
    content: @Composable RowScope.() -> Unit,
) {
    NeoToolkitButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        contentPadding = contentPadding,
        border = border,
        content = content,
    )
}

@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    colors: NeoButtonColors? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    border: BorderStroke? = null,
    content: @Composable RowScope.() -> Unit,
) {
    NeoToolkitButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        contentPadding = contentPadding,
        border = border,
        outlined = true,
        content = content,
    )
}

@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    colors: NeoButtonColors? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    content: @Composable RowScope.() -> Unit,
) {
    NeoToolkitButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        contentPadding = contentPadding,
        textOnly = true,
        content = content,
    )
}

@Composable
fun ExtendedFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit = {},
    text: @Composable () -> Unit = {},
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    shape: Shape = RoundedCornerShape(14.dp),
) {
    val neo = rememberNeoColors()
    val bg = if (containerColor == Color.Unspecified) neo.accent else containerColor
    val fg = if (contentColor == Color.Unspecified) neo.onAccent else contentColor
    Box(
        modifier = modifier
            .neoTactile(
                shadowOffset = NeoShadows.Regular,
                pressedOffset = NeoShadows.Pressed,
                cornerRadius = NeoShapes.Lg,
                onClick = onClick,
            )
            .background(bg, shape)
            .border(NeoBorders.Regular, neo.border, shape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        NeoButtonContentColor(fg) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon()
                Spacer(Modifier.width(8.dp))
                text()
            }
        }
    }
}
