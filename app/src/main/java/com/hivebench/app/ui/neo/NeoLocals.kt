package com.hivebench.app.ui.neo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalNeoContentColor = compositionLocalOf { Color.Unspecified }

@Composable
fun NeoButtonContentColor(color: Color, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalNeoContentColor provides color, content = content)
}

@Composable
fun neoContentColor(): Color {
    val neo = rememberNeoColors()
    val local = LocalNeoContentColor.current
    return if (local == Color.Unspecified) neo.ink else local
}
