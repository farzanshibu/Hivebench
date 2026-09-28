package com.hivebench.app.ui.neo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

// ── From-scratch outlined field. Same call surface as the old toolkit. ──

data class NeoTextFieldColors(
    val focusedBorderColor: Color,
    val unfocusedBorderColor: Color,
    val errorBorderColor: Color,
    val cursorColor: Color,
)

object OutlinedTextFieldDefaults {
    @Composable
    fun colors(
        focusedBorderColor: Color = Color.Unspecified,
        unfocusedBorderColor: Color = Color.Unspecified,
        errorBorderColor: Color = Color.Unspecified,
        cursorColor: Color = Color.Unspecified,
    ): NeoTextFieldColors {
        val neo = rememberNeoColors()
        return NeoTextFieldColors(
            focusedBorderColor = if (focusedBorderColor == Color.Unspecified) neo.accent else focusedBorderColor,
            unfocusedBorderColor = if (unfocusedBorderColor == Color.Unspecified) neo.border else unfocusedBorderColor,
            errorBorderColor = if (errorBorderColor == Color.Unspecified) neo.error else errorBorderColor,
            cursorColor = if (cursorColor == Color.Unspecified) neo.accent else cursorColor,
        )
    }
}

@Composable
fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    shape: Shape = RoundedCornerShape(12.dp),
    colors: NeoTextFieldColors? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val neo = rememberNeoColors()
    val palette = colors ?: OutlinedTextFieldDefaults.colors()
    val ownInteraction = remember { MutableInteractionSource() }
    val interaction = interactionSource ?: ownInteraction
    val focused by interaction.collectIsFocusedAsState()
    val borderColor = when {
        !enabled -> neo.border.copy(alpha = 0.5f)
        isError -> palette.errorBorderColor
        focused -> palette.focusedBorderColor
        else -> palette.unfocusedBorderColor
    }
    val contentColor = if (enabled) neo.ink else neo.muted
    Column(modifier = modifier) {
        if (label != null) {
            NeoButtonContentColor(if (isError) neo.error else neo.muted) {
                label()
            }
            Spacer(Modifier.height(6.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .neoShadow(offsetX = 2.dp, offsetY = 2.dp, cornerRadius = NeoShapes.Md)
                .background(neo.surface, shape)
                .border(NeoBorders.Regular, borderColor, shape)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingIcon != null) {
                    NeoButtonContentColor(contentColor) { leadingIcon() }
                    Spacer(Modifier.width(8.dp))
                }
                Box(Modifier.weight(1f)) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabled,
                        readOnly = readOnly,
                        textStyle = textStyle.copy(color = contentColor),
                        cursorBrush = SolidColor(palette.cursorColor),
                        visualTransformation = visualTransformation,
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        singleLine = singleLine,
                        maxLines = maxLines,
                        minLines = minLines,
                        interactionSource = interaction,
                        decorationBox = { inner ->
                            if (value.isEmpty() && placeholder != null) {
                                NeoButtonContentColor(neo.muted) { placeholder() }
                            }
                            inner()
                        },
                    )
                }
                if (trailingIcon != null) {
                    Spacer(Modifier.width(8.dp))
                    NeoButtonContentColor(contentColor) { trailingIcon() }
                }
            }
        }
        if (supportingText != null) {
            Spacer(Modifier.height(4.dp))
            NeoButtonContentColor(if (isError) neo.error else neo.muted) {
                supportingText()
            }
        }
    }
}
