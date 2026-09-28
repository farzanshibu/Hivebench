package com.hivebench.app.ui.neo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

// ── From-scratch primitives. Same call surface as the old toolkit,
//    rendered purely on foundation (BasicText / Image / Box). ──

@Composable
fun resolveNeoContentColor(explicit: Color): Color {
    if (explicit != Color.Unspecified) return explicit
    val local = LocalNeoContentColor.current
    if (local != Color.Unspecified) return local
    return rememberNeoColors().ink
}

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = TextStyle.Default,
) {
    var merged = style
    val resolved = resolveNeoContentColor(color)
    if (style.color == Color.Unspecified || color != Color.Unspecified) {
        merged = merged.copy(color = resolved)
    }
    if (fontSize != TextUnit.Unspecified) merged = merged.copy(fontSize = fontSize)
    if (fontStyle != null) merged = merged.copy(fontStyle = fontStyle)
    if (fontWeight != null) merged = merged.copy(fontWeight = fontWeight)
    if (fontFamily != null) merged = merged.copy(fontFamily = fontFamily)
    if (letterSpacing != TextUnit.Unspecified) merged = merged.copy(letterSpacing = letterSpacing)
    if (textDecoration != null) merged = merged.copy(textDecoration = textDecoration)
    if (textAlign != null) merged = merged.copy(textAlign = textAlign)
    if (lineHeight != TextUnit.Unspecified) merged = merged.copy(lineHeight = lineHeight)
    BasicText(
        text = text,
        modifier = modifier,
        style = merged,
        onTextLayout = onTextLayout,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
    )
}

@Composable
fun Text(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = TextStyle.Default,
) {
    var merged = style
    val resolved = resolveNeoContentColor(color)
    if (style.color == Color.Unspecified || color != Color.Unspecified) {
        merged = merged.copy(color = resolved)
    }
    if (fontSize != TextUnit.Unspecified) merged = merged.copy(fontSize = fontSize)
    if (fontStyle != null) merged = merged.copy(fontStyle = fontStyle)
    if (fontWeight != null) merged = merged.copy(fontWeight = fontWeight)
    if (fontFamily != null) merged = merged.copy(fontFamily = fontFamily)
    if (letterSpacing != TextUnit.Unspecified) merged = merged.copy(letterSpacing = letterSpacing)
    if (textDecoration != null) merged = merged.copy(textDecoration = textDecoration)
    if (textAlign != null) merged = merged.copy(textAlign = textAlign)
    if (lineHeight != TextUnit.Unspecified) merged = merged.copy(lineHeight = lineHeight)
    BasicText(
        text = text,
        modifier = modifier,
        style = merged,
        onTextLayout = onTextLayout,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
    )
}

@Composable
fun Icon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    val resolved = resolveNeoContentColor(tint)
    Image(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier,
        colorFilter = ColorFilter.tint(resolved),
    )
}

@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val neo = rememberNeoColors()
    Box(
        modifier = modifier
            .neoBounce(enabled = enabled, onClick = if (enabled) onClick else null)
            .size(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        NeoButtonContentColor(if (enabled) neo.ink else neo.muted, content)
    }
}

@Composable
fun Surface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(0.dp),
    color: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    border: BorderStroke? = null,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    val neo = rememberNeoColors()
    val bg = if (color == Color.Unspecified) neo.surface else color
    val fg = if (contentColor == Color.Unspecified) neo.ink else contentColor
    var decorated = modifier
        .clip(shape)
        .background(bg, shape)
    if (border != null) {
        decorated = decorated.border(border, shape)
    }
    Box(modifier = decorated) {
        NeoButtonContentColor(fg, content)
    }
}

@Composable
fun HorizontalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
    color: Color = Color.Unspecified,
) {
    val neo = rememberNeoColors()
    Box(
        modifier
            .fillMaxWidth()
            .height(thickness)
            .background(if (color == Color.Unspecified) neo.borderSoft else color),
    )
}
