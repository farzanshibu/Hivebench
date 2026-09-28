package com.hivebench.app.ui.neo

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class NeoTabPosition(val left: Dp, val width: Dp)

@Composable
fun ScrollableTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    edgePadding: Dp = 0.dp,
    indicator: @Composable (List<NeoTabPosition>) -> Unit = {},
    divider: @Composable () -> Unit = {},
    tabs: @Composable RowScope.() -> Unit,
) {
    val neo = rememberNeoColors()
    Row(modifier.fillMaxWidth().background(if (containerColor == Color.Unspecified) neo.background else containerColor).horizontalScroll(rememberScrollState()).padding(horizontal = edgePadding), content = tabs)
    divider()
}

@Composable
fun Tab(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    selectedContentColor: Color = Color.Unspecified,
    unselectedContentColor: Color = Color.Unspecified,
) {
    val neo = rememberNeoColors()
    Box(modifier.neoBounce(enabled = enabled, onClick = if (enabled) onClick else null).background(if (selected) neo.accent.copy(alpha = 0.16f) else Color.Transparent, RoundedCornerShape(NeoShapes.Md)).padding(horizontal = 12.dp, vertical = 9.dp), contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) NeoButtonContentColor(if (selected) neo.accent else neo.muted) { icon() }
            if (text != null) NeoButtonContentColor(if (selected) neo.ink else neo.muted) { text() }
        }
    }
}

object TabRowDefaults {
    @Composable fun SecondaryIndicator(modifier: Modifier = Modifier, height: Dp = 2.dp, color: Color = Color.Unspecified) {
        val neo = rememberNeoColors()
        Box(modifier.fillMaxWidth().padding(top = 2.dp).background(if (color == Color.Unspecified) neo.accent else color).padding(vertical = height / 2))
    }
}

fun Modifier.tabIndicatorOffset(currentTabPosition: NeoTabPosition): Modifier = this
