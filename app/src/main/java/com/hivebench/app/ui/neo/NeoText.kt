package com.hivebench.app.ui.neo

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

/**
 * Foundation-only text. Replaces all material3 Text usages.
 * Uses BasicText + NeoType presets so typography is centralized.
 */
@Composable
fun NeoText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = NeoType.Body,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val neo = rememberNeoColors()
    val resolved = style.copy(
        color = if (color == Color.Unspecified) neo.ink else color,
        textAlign = textAlign ?: style.textAlign,
    )
    BasicText(
        text = text,
        modifier = modifier,
        style = resolved,
        maxLines = maxLines,
        overflow = overflow,
    )
}

@Composable
fun NeoDisplay(text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified, maxLines: Int = Int.MAX_VALUE, overflow: TextOverflow = TextOverflow.Clip) {
    NeoText(text, modifier, NeoType.Display, color, null, maxLines, overflow)
}

@Composable
fun NeoTitle(text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified, maxLines: Int = Int.MAX_VALUE, overflow: TextOverflow = TextOverflow.Clip) {
    NeoText(text, modifier, NeoType.Title, color, null, maxLines, overflow)
}

@Composable
fun NeoSubtitle(text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified, maxLines: Int = Int.MAX_VALUE) {
    NeoText(text, modifier, NeoType.Subtitle, color, null, maxLines)
}

@Composable
fun NeoBody(text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified, maxLines: Int = Int.MAX_VALUE, overflow: TextOverflow = TextOverflow.Clip) {
    NeoText(text, modifier, NeoType.Body, color, null, maxLines, overflow)
}

@Composable
fun NeoCaption(text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified, maxLines: Int = Int.MAX_VALUE) {
    val neo = rememberNeoColors()
    NeoText(text, modifier, NeoType.Caption, if (color == Color.Unspecified) neo.muted else color, null, maxLines)
}

@Composable
fun NeoMono(text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified, maxLines: Int = 1, overflow: TextOverflow = TextOverflow.Ellipsis) {
    val neo = rememberNeoColors()
    NeoText(text, modifier, NeoType.Mono, if (color == Color.Unspecified) neo.muted else color, null, maxLines, overflow)
}
