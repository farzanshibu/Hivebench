package com.jarves.mh.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarves.mh.ui.theme.AppThemeMode
import com.jarves.mh.ui.theme.NeoLime
import com.jarves.mh.ui.theme.NeoBlack
import com.jarves.mh.ui.theme.NeoDarkBorder
import com.jarves.mh.ui.theme.neoShadow
import com.jarves.mh.ui.theme.neoTactile
import com.jarves.mh.ui.theme.neoBounce
import com.jarves.mh.ui.theme.neoBorder
import com.jarves.mh.ui.theme.PocketGreen
import com.jarves.mh.ui.theme.PocketOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    lines: List<TerminalOutputLine>,
    isRunning: Boolean,
    onRun: (String) -> Unit,
    onInput: (String) -> Unit = {},
    onInterrupt: (() -> Unit)? = null,
    onClear: () -> Unit,
    onToggleTheme: () -> Unit,
    themeMode: AppThemeMode,
    title: String = "Linux Terminal",
    subtitle: String = "Ubuntu 24.04 · PRoot Sandbox",
    liveOutput: String = "",
    currentCommand: String? = null,
    commandDraft: String? = null,
    onCommandDraftConsumed: () -> Unit = {},
    promptPath: String = "/workspace",
    onStop: (() -> Unit)? = null,
    showThemeAction: Boolean = false,
    showQuickCommands: Boolean = true,
    compactHeader: Boolean = false,
) {
    var commandInput by remember { mutableStateOf(TextFieldValue()) }
    var commandHistory by remember { mutableStateOf(emptyList<String>()) }
    var historyIndex by remember { mutableStateOf(-1) }
    var altActive by rememberSaveable { mutableStateOf(false) }
    var ctrlActive by rememberSaveable { mutableStateOf(false) }
    val terminalScrollState = rememberScrollState()
    val inputFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val scope = rememberCoroutineScope()
    val terminalPromptPath = if (promptPath == "/workspace") "~" else "~${promptPath.removePrefix("/workspace")}" 
    val openTerminalKeyboard = {
        inputFocusRequester.requestFocus()
        scope.launch {
            delay(80)
            keyboardController?.show()
        }
    }
    val submitCommand = {
        if (commandInput.text.isNotBlank()) {
            val submitted = commandInput.text
            if (isRunning) {
                onInput(submitted)
            } else {
                onRun(submitted)
                commandHistory = (commandHistory + submitted).takeLast(50)
                historyIndex = -1
            }
            commandInput = TextFieldValue()
        }
    }

    LaunchedEffect(commandDraft) {
        if (!commandDraft.isNullOrBlank()) {
            commandInput = TextFieldValue(commandDraft, TextRange(commandDraft.length))
            historyIndex = -1
            onCommandDraftConsumed()
        }
    }

    // Auto-scroll to bottom whenever scrollable content grows
    LaunchedEffect(Unit) {
        snapshotFlow { terminalScrollState.maxValue }
            .collect { maxValue ->
                terminalScrollState.scrollTo(maxValue)
            }
    }
    // Also trigger scroll when key state changes (e.g. isRunning toggling)
    LaunchedEffect(lines.size, isRunning, commandInput.text.length) {
        delay(100)
        terminalScrollState.scrollTo(terminalScrollState.maxValue)
    }

    val quickCommands = listOf(
        "uname -a",
        "ls -la",
        "pwd",
        "node -v",
        "python3 --version",
        "df -h",
        "free -m",
        "claude --version",
    )

    Scaffold(
        modifier = if (compactHeader) Modifier else Modifier.statusBarsPadding(),
        topBar = {
            if (compactHeader) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(NeoLime, RoundedCornerShape(9.dp))
                                .border(2.dp, if (themeMode == AppThemeMode.DARK) NeoDarkBorder else NeoBlack, RoundedCornerShape(9.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Terminal, contentDescription = null, tint = NeoBlack, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(subtitle, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = onClear, modifier = Modifier.size(38.dp)) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Clear output", modifier = Modifier.size(20.dp))
                        }
                        if (showThemeAction) {
                            IconButton(onClick = onToggleTheme) {
                                Icon(
                                    if (themeMode == AppThemeMode.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Toggle theme",
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f))
                }
            } else {
                TopAppBar(
                    modifier = Modifier.padding(top = 8.dp),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        color = NeoLime,
                                        shape = RoundedCornerShape(9.dp),
                                    )
                                    .border(
                                        width = 2.dp,
                                        color = if (themeMode == AppThemeMode.DARK) NeoDarkBorder else NeoBlack,
                                        shape = RoundedCornerShape(9.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Terminal,
                                    contentDescription = null,
                                    tint = NeoBlack,
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = onClear) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Clear output")
                        }
                        if (showThemeAction) {
                            IconButton(onClick = onToggleTheme) {
                                Icon(
                                    if (themeMode == AppThemeMode.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Toggle theme",
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .then(if (compactHeader) Modifier else Modifier.imePadding()),
        ) {
            if (showQuickCommands && !keyboardVisible) {
                // Quick command chips are useful in the standalone terminal, but
                // project terminal space is reserved for the actual project session.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    quickCommands.forEach { cmd ->
                        AssistChip(
                            onClick = { onRun(cmd) },
                            label = {
                                Text(
                                    cmd,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                )
                            },
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            }

            val isDark = when (themeMode) {
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            val terminalBg = if (isDark) Color(0xFF090D14) else MaterialTheme.colorScheme.surface
            val promptGreen = NeoLime
            val commandTextColor = if (isDark) Color(0xFFF0F6FC) else MaterialTheme.colorScheme.onSurface
            val outputTextColor = if (isDark) Color(0xFFC9D1D9) else MaterialTheme.colorScheme.onSurface
            val emptyStateColor = if (isDark) Color(0xFF6E7681) else MaterialTheme.colorScheme.onSurfaceVariant

            // Console output area with Neobrutal hard shadow and 2.dp border
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .neoShadow(offsetX = 3.dp, offsetY = 3.dp, cornerRadius = 12.dp)
                    .border(2.dp, if (isDark) NeoDarkBorder else NeoBlack, RoundedCornerShape(12.dp)),
                color = terminalBg,
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(terminalScrollState)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SelectionContainer {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (lines.isEmpty()) {
                                Text(
                                    "Mobile Harness Terminal ready.\nType a bash command below or tap a quick command chip above.",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = emptyStateColor,
                                )
                            }

                            lines.forEach { item ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TerminalCommandPrompt(promptPath = terminalPromptPath, command = item.command, isDark = isDark)
                                    if (item.output.isNotEmpty()) {
                                        Text(
                                            text = sanitizeTerminalOutput(item.output),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            lineHeight = 18.sp,
                                            color = if (item.exitCode != 0) MaterialTheme.colorScheme.error else outputTextColor,
                                            modifier = Modifier.padding(start = 8.dp),
                                        )
                                    }
                                }
                            }

                            if (isRunning && currentCommand != null) {
                                TerminalCommandPrompt(promptPath = terminalPromptPath, command = currentCommand, isDark = isDark)
                            }

                            if (liveOutput.isNotBlank()) {
                                Text(
                                    sanitizeTerminalOutput(liveOutput),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    color = outputTextColor,
                                )
                            }

                        }
                    }
                    run {
                        // While a process is running, keep the input surface visually
                        // empty until the user types. A permanent prompt/cursor here
                        // changed the console height and made auto-scroll look like a
                        // full terminal refresh on every blink.
                        val prefix = if (isRunning) "" else "root@pocket:$terminalPromptPath# "
                        val prefixVisualTransformation = remember(prefix, isDark) {
                            VisualTransformation { text ->
                                val transformed = buildAnnotatedString {
                                    withStyle(SpanStyle(color = promptGreen, fontWeight = FontWeight.Bold)) {
                                        append(prefix)
                                    }
                                    withStyle(SpanStyle(color = commandTextColor, fontWeight = FontWeight.SemiBold)) {
                                        append(text.text)
                                    }
                                }
                                val offsetMapping = object : OffsetMapping {
                                    override fun originalToTransformed(offset: Int): Int =
                                        (offset + prefix.length).coerceIn(0, prefix.length + text.length)

                                    override fun transformedToOriginal(offset: Int): Int =
                                        (offset - prefix.length).coerceIn(0, text.length)
                                }
                                TransformedText(transformed, offsetMapping)
                            }
                        }

                        BasicTextField(
                            value = commandInput,
                            onValueChange = { next ->
                                if (ctrlActive && next.text.length > commandInput.text.length) {
                                    val inserted = next.text.substring(
                                        commandInput.selection.start.coerceAtMost(next.text.length),
                                        next.selection.end.coerceAtMost(next.text.length),
                                    )
                                    if (inserted.any { it.equals('c', ignoreCase = true) }) {
                                        if (isRunning) {
                                            onInterrupt?.invoke()
                                        } else {
                                            // A shell with no foreground process uses
                                            // Ctrl+C to cancel the current command line.
                                            commandInput = TextFieldValue()
                                        }
                                        ctrlActive = false
                                    } else {
                                        commandInput = next
                                        ctrlActive = false
                                    }
                                } else {
                                    commandInput = next
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(inputFocusRequester),
                            singleLine = false,
                            visualTransformation = prefixVisualTransformation,
                            cursorBrush = SolidColor(promptGreen),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = commandTextColor,
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = { submitCommand() },
                                onDone = { submitCommand() },
                                onGo = { submitCommand() },
                            ),
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }

            // Keyboard helper row. These operate on the command draft, so they are
            // useful even when the phone keyboard does not expose terminal keys.
            if (!keyboardVisible) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp)
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    TerminalKeyButton("↑", "Previous command") {
                        commandHistory.getOrNull(if (historyIndex < 0) commandHistory.lastIndex else (historyIndex - 1).coerceAtLeast(0))?.let {
                            historyIndex = if (historyIndex < 0) commandHistory.lastIndex else (historyIndex - 1).coerceAtLeast(0)
                            commandInput = TextFieldValue(it, TextRange(it.length))
                        }
                    }
                    TerminalKeyButton("↓", "Next command") {
                        if (historyIndex >= 0) {
                            historyIndex = (historyIndex + 1).takeIf { it < commandHistory.size } ?: -1
                            commandInput = TextFieldValue(commandHistory.getOrNull(historyIndex) ?: "", TextRange((commandHistory.getOrNull(historyIndex) ?: "").length))
                        }
                    }
                    TerminalIconKeyButton(Icons.Default.ArrowBack, "Move cursor left") {
                        commandInput = commandInput.copy(selection = TextRange((commandInput.selection.start - 1).coerceAtLeast(0)))
                    }
                    TerminalIconKeyButton(Icons.Default.ArrowForward, "Move cursor right") {
                        commandInput = commandInput.copy(selection = TextRange((commandInput.selection.end + 1).coerceAtMost(commandInput.text.length)))
                    }
                    TerminalKeyButton("ALT", "Alt modifier", active = altActive, fixedWidth = true) { altActive = !altActive }
                    TerminalKeyButton("ESC", "Escape") { commandInput = TextFieldValue() }
                    TerminalKeyButton("CTRL", "Control modifier; press C to interrupt", active = ctrlActive, fixedWidth = true) {
                        ctrlActive = !ctrlActive
                        if (ctrlActive) openTerminalKeyboard()
                    }
                }
            }
        }
    }
}

@Composable
private fun TerminalKeyButton(
    label: String,
    description: String,
    active: Boolean = false,
    fixedWidth: Boolean = false,
    onClick: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val borderColor = if (active) NeoLime else if (isDark) NeoDarkBorder else NeoBlack
    val bgColor = if (active) NeoLime else if (isDark) Color(0xFF1B2028) else Color(0xFFFFFFFF)
    val textColor = if (active) NeoBlack else if (isDark) Color(0xFFF8FAFC) else NeoBlack

    Box(
        modifier = Modifier
            .height(34.dp)
            .then(if (fixedWidth) Modifier.width(78.dp) else Modifier)
            .neoTactile(
                shadowOffset = 2.dp,
                pressedOffset = 0.5.dp,
                cornerRadius = 8.dp,
                onClick = onClick,
            )
            .background(bgColor, RoundedCornerShape(8.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (active) "$label ✓" else label,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            maxLines = 1,
        )
    }
}

@Composable
private fun TerminalIconKeyButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .neoBounce(pressedScale = 0.88f, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun TerminalCommandPrompt(promptPath: String, command: String, isDark: Boolean = true) {
    val promptGreen = NeoLime
    val commandColor = if (isDark) Color(0xFFF0F6FC) else MaterialTheme.colorScheme.onSurface
    val promptText = remember(promptPath, command, isDark) {
        buildAnnotatedString {
            withStyle(SpanStyle(color = promptGreen, fontWeight = FontWeight.Bold)) {
                append("root@pocket:$promptPath# ")
            }
            withStyle(SpanStyle(color = commandColor, fontWeight = FontWeight.SemiBold)) {
                append(command)
            }
        }
    }
    Text(
        text = promptText,
        modifier = Modifier.fillMaxWidth(),
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        softWrap = true,
    )
}
