package com.hivebench.app.ui.neo

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Foundation-only scaffold + top bar + bottom nav. No Scaffold/TopAppBar/NavigationBar.
 */

@Composable
fun NeoScaffold(
    modifier: Modifier = Modifier,
    containerColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    topBar: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    floatingActionButton: (@Composable () -> Unit)? = null,
    applySystemInsets: Boolean = true,
    content: @Composable (PaddingValues) -> Unit,
) {
    val neo = rememberNeoColors()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (containerColor == androidx.compose.ui.graphics.Color.Unspecified) neo.background else containerColor)
            .then(if (applySystemInsets) Modifier.statusBarsPadding().navigationBarsPadding() else Modifier),
    ) {
        if (topBar != null) {
            topBar()
        }
        Box(Modifier.weight(1f).fillMaxWidth()) { content(PaddingValues(0.dp)) }
        if (floatingActionButton != null) floatingActionButton()
        if (bottomBar != null) {
            bottomBar()
        }
    }
}

@Composable
fun Scaffold(
    modifier: Modifier = Modifier,
    containerColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    contentWindowInsets: androidx.compose.foundation.layout.WindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    applySystemInsets: Boolean = true,
    content: @Composable (PaddingValues) -> Unit,
) = NeoScaffold(
    modifier = modifier,
    containerColor = containerColor,
    topBar = topBar,
    bottomBar = bottomBar,
    floatingActionButton = floatingActionButton,
    applySystemInsets = applySystemInsets,
    content = content,
)

@Composable
fun TopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: NeoTopBarColors = NeoTopBarColors(),
) {
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        navigationIcon()
        Box(Modifier.weight(1f)) { title() }
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

data class NeoTopBarColors(val containerColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified)
object TopAppBarDefaults {
    @Composable fun topAppBarColors(containerColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified) = NeoTopBarColors(containerColor)
}

@Composable
fun NeoTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigation: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (navigation != null) {
            navigation()
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            NeoText(text = title, style = NeoType.Title)
            if (subtitle != null) NeoCaption(text = subtitle)
        }
        if (actions != null) {
            Row(verticalAlignment = Alignment.CenterVertically, content = actions)
        }
    }
}

@Composable
fun NeoDivider(modifier: Modifier = Modifier) {
    val neo = rememberNeoColors()
    Box(modifier.fillMaxWidth().height(2.dp).background(neo.border))
}

@Composable
fun NeoNavBar(
    modifier: Modifier = Modifier,
    items: List<NeoNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val neo = rememberNeoColors()
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            val scale by animateFloatAsState(
                if (selected) 1.04f else 1f,
                spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                label = "navPop",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .then(
                        if (selected) Modifier.neoShadow(2.dp, 2.dp, cornerRadius = NeoShapes.Md)
                        else Modifier
                    )
                    .background(
                        if (selected) neo.accent else neo.surface,
                        RoundedCornerShape(NeoShapes.Md),
                    )
                    .border(NeoBorders.Regular, neo.border, RoundedCornerShape(NeoShapes.Md))
                    .neoBounce(onClick = { onSelect(index) })
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    item.icon()
                    NeoText(
                        text = item.label.uppercase(),
                        style = NeoType.Badge,
                        color = if (selected) neo.onAccent else neo.ink,
                    )
                }
            }
        }
    }
}

data class NeoNavItem(val label: String, val icon: @Composable () -> Unit)

@Composable
fun NeoTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val neo = rememberNeoColors()
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .then(if (selected) Modifier.neoShadow(offsetX = 2.dp, offsetY = 2.dp, cornerRadius = NeoShapes.Pill) else Modifier)
                    .background(
                        if (selected) neo.accent else neo.surface,
                        RoundedCornerShape(NeoShapes.Pill),
                    )
                    .border(NeoBorders.Thin, neo.border, RoundedCornerShape(NeoShapes.Pill))
                    .clip(RoundedCornerShape(NeoShapes.Pill))
                    .neoBounce(onClick = { onSelect(index) })
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                NeoText(
                    text = tab.uppercase(),
                    style = NeoType.Badge,
                    color = if (selected) neo.onAccent else neo.muted,
                )
            }
        }
    }
}

@Composable
fun NeoChip(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    val neo = rememberNeoColors()
    Box(
        modifier = modifier
            .then(if (selected) Modifier.neoShadow(offsetX = 2.dp, offsetY = 2.dp, cornerRadius = NeoShapes.Pill) else Modifier)
            .background(
                if (selected) neo.accent else neo.surface,
                RoundedCornerShape(NeoShapes.Pill),
            )
            .border(NeoBorders.Thin, neo.border, RoundedCornerShape(NeoShapes.Pill))
            .then(if (onClick != null) Modifier.neoBounce(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(6.dp))
            }
            NeoText(
                text = text.uppercase(),
                style = NeoType.Badge,
                color = if (selected) neo.onAccent else neo.ink,
            )
        }
    }
}
