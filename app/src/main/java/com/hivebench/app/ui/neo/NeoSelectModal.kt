package com.hivebench.app.ui.neo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    shape: Shape = RoundedCornerShape(NeoShapes.Lg),
    containerColor: Color = Color.Unspecified,
    titleContentColor: Color = Color.Unspecified,
    textContentColor: Color = Color.Unspecified,
) {
    val neo = rememberNeoColors()
    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = modifier.fillMaxWidth().neoShadow(5.dp, 5.dp, cornerRadius = NeoShapes.Lg)
                .background(if (containerColor == Color.Unspecified) neo.surface else containerColor, shape)
                .border(NeoBorders.Regular, neo.border, shape).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (icon != null) Box(Modifier.align(Alignment.CenterHorizontally)) { icon() }
            if (title != null) NeoButtonContentColor(if (titleContentColor == Color.Unspecified) neo.ink else titleContentColor) { title() }
            if (text != null) NeoButtonContentColor(if (textContentColor == Color.Unspecified) neo.ink else textContentColor) { text() }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (dismissButton != null) dismissButton()
                Spacer(Modifier.size(8.dp))
                confirmButton()
            }
        }
    }
}

/**
 * Popup menu. Use [offset] (not Modifier.offset) to position it: the popup window is sized from
 * the content's layout bounds, so a modifier offset shifts content outside the window and clips it.
 * Content scrolls inside the bordered card once it exceeds [maxHeight].
 */
@Composable
fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset.Zero,
    maxHeight: Dp = 360.dp,
    content: @Composable () -> Unit,
) {
    if (!expanded) return
    val density = LocalDensity.current
    val popupOffset = with(density) { IntOffset(offset.x.roundToPx(), offset.y.roundToPx()) }
    val neo = rememberNeoColors()
    val shape = RoundedCornerShape(NeoShapes.Md)
    Popup(offset = popupOffset, onDismissRequest = onDismissRequest, properties = PopupProperties(focusable = true)) {
        Column(
            modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .neoShadow(3.dp, 3.dp, cornerRadius = NeoShapes.Md)
                .background(neo.surface, shape)
                .border(NeoBorders.Regular, neo.border, shape)
                .clip(shape)
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
        ) { content() }
    }
}

@Composable
fun DropdownMenuItem(text: @Composable () -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier, leadingIcon: (@Composable () -> Unit)? = null, trailingIcon: (@Composable () -> Unit)? = null, enabled: Boolean = true) {
    val neo = rememberNeoColors()
    Row(modifier.fillMaxWidth().neoBounce(enabled = enabled, onClick = if (enabled) onClick else null).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (leadingIcon != null) leadingIcon()
        Box(Modifier.weight(1f).padding(horizontal = 8.dp)) { NeoButtonContentColor(neo.ink) { text() } }
        if (trailingIcon != null) trailingIcon()
    }
}

@Composable
fun AssistChip(onClick: () -> Unit, label: @Composable () -> Unit, modifier: Modifier = Modifier, leadingIcon: (@Composable () -> Unit)? = null, enabled: Boolean = true) {
    val neo = rememberNeoColors()
    Row(modifier = modifier.neoBounce(enabled = enabled, onClick = if (enabled) onClick else null).background(neo.surface, RoundedCornerShape(NeoShapes.Pill)).border(NeoBorders.Thin, neo.border, RoundedCornerShape(NeoShapes.Pill)).padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        if (leadingIcon != null) leadingIcon()
        NeoButtonContentColor(neo.ink) { label() }
    }
}

data class NeoCheckboxColors(val checkedColor: Color = Color.Unspecified, val checkmarkColor: Color = Color.Unspecified)
object CheckboxDefaults {
    fun colors(checkedColor: Color = Color.Unspecified, checkmarkColor: Color = Color.Unspecified) = NeoCheckboxColors(checkedColor, checkmarkColor)
}
@Composable
fun Checkbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, colors: NeoCheckboxColors = NeoCheckboxColors()) = NeoCheckbox(checked, onCheckedChange, modifier, enabled)

data class NeoSwitchColors(val checkedThumbColor: Color = Color.Unspecified, val checkedTrackColor: Color = Color.Unspecified)
object SwitchDefaults {
    fun colors(checkedThumbColor: Color = Color.Unspecified, checkedTrackColor: Color = Color.Unspecified) = NeoSwitchColors(checkedThumbColor, checkedTrackColor)
}
@Composable
fun Switch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true, colors: NeoSwitchColors = NeoSwitchColors()) = NeoSwitch(checked, onCheckedChange ?: {}, modifier, enabled && onCheckedChange != null)

@Composable
fun Card(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(NeoShapes.Md), colors: NeoCardColors = NeoCardColors(), content: @Composable () -> Unit) {
    Surface(modifier = modifier, shape = shape, color = colors.containerColor, contentColor = colors.contentColor, border = androidx.compose.foundation.BorderStroke(NeoBorders.Thin, colors.borderColor), content = content)
}

data class NeoCardColors(val containerColor: Color = Color.Unspecified, val contentColor: Color = Color.Unspecified, val borderColor: Color = Color.Unspecified)
object CardDefaults {
    @Composable fun cardColors(containerColor: Color = Color.Unspecified, contentColor: Color = Color.Unspecified) = NeoCardColors(containerColor, contentColor)
}

@Composable
fun Badge(modifier: Modifier = Modifier, content: @Composable (() -> Unit)? = null) {
    Box(modifier.background(rememberNeoColors().accent, RoundedCornerShape(NeoShapes.Pill)).padding(horizontal = 6.dp, vertical = 2.dp)) { if (content != null) content() }
}

@Composable
fun BadgedBox(badge: @Composable () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier) { content(); Box(Modifier.align(Alignment.TopEnd)) { badge() } }
}

@Composable
fun NavigationBar(modifier: Modifier = Modifier, containerColor: Color = Color.Unspecified, content: @Composable RowScope.() -> Unit) {
    val neo = rememberNeoColors()
    Row(
        modifier.fillMaxWidth()
            .background(if (containerColor == Color.Unspecified) neo.surface else containerColor, RoundedCornerShape(NeoShapes.Lg))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

data class NeoNavigationColors(val selectedIconColor: Color = Color.Unspecified, val selectedTextColor: Color = Color.Unspecified, val indicatorColor: Color = Color.Unspecified, val unselectedIconColor: Color = Color.Unspecified, val unselectedTextColor: Color = Color.Unspecified)
object NavigationBarItemDefaults {
    @Composable fun colors(selectedIconColor: Color = Color.Unspecified, selectedTextColor: Color = Color.Unspecified, indicatorColor: Color = Color.Unspecified, unselectedIconColor: Color = Color.Unspecified, unselectedTextColor: Color = Color.Unspecified) = NeoNavigationColors(selectedIconColor, selectedTextColor, indicatorColor, unselectedIconColor, unselectedTextColor)
}

@Composable
fun RowScope.NavigationBarItem(selected: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, label: (@Composable () -> Unit)? = null, alwaysShowLabel: Boolean = true, colors: NeoNavigationColors = NeoNavigationColors()) {
    val neo = rememberNeoColors()
    Column(
        modifier.weight(1f)
            .neoBounce(enabled = enabled, onClick = if (enabled) onClick else null)
            .background(if (selected) neo.accent else Color.Transparent, RoundedCornerShape(NeoShapes.Md))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        NeoButtonContentColor(if (selected) (colors.selectedIconColor.takeIf { it != Color.Unspecified } ?: neo.onAccent) else (colors.unselectedIconColor.takeIf { it != Color.Unspecified } ?: neo.muted)) { icon() }
        if (label != null && (alwaysShowLabel || selected)) NeoButtonContentColor(if (selected) neo.onAccent else neo.muted) { label() }
    }
}
