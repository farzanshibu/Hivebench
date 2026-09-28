package com.hivebench.app.terminal

import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.TextStyle
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient

/** A shortcut shown above the keyboard that types [send] into the session. */
data class TerminalQuickAction(val label: String, val send: String)

/**
 * Where attached files go: [hostDir] on the device, visible to the agent as [guestDir].
 * Picked files are copied there and their guest paths typed into the prompt.
 */
data class TerminalAttachmentTarget(val hostDir: java.io.File, val guestDir: String)

private val TerminalBackground = Color(0xFF0B0D10)
private val KeyBackground = Color(0xFF1A1F26)
private val KeyBorder = Color(0xFF2C333D)
private val KeyActive = Color(0xFFB8F200)

/**
 * Full-screen PTY renderer for the session registered under [sessionKey] in
 * [TerminalSessions], plus a row of keys phone keyboards lack (Esc, Tab, Ctrl,
 * arrows) and agent-specific [quickActions].
 */
@Composable
fun AgentTerminalView(
    sessionKey: String,
    modifier: Modifier = Modifier,
    quickActions: List<TerminalQuickAction> = emptyList(),
    attachments: TerminalAttachmentTarget? = null,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // Voice dictation types the recognised text into the prompt (not submitted).
    val speechLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val spoken = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) TerminalSessions.get(sessionKey)?.emulator?.paste(spoken)
    }
    val attachLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        val target = attachments ?: return@rememberLauncherForActivityResult
        val paths = uris.mapNotNull { uri -> copyAttachment(context, uri, target) }
        if (paths.isNotEmpty()) TerminalSessions.get(sessionKey)?.emulator?.paste(paths.joinToString(" ") + " ")
    }
    var ctrlDown by remember { mutableStateOf(false) }
    var altDown by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    var fontPx by remember { mutableStateOf(with(density) { 12.sp.toPx() }) }
    var terminalView by remember { mutableStateOf<TerminalView?>(null) }

    Column(modifier.background(TerminalBackground)) {
        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            factory = { context ->
                TerminalView(context, null).apply {
                    isFocusable = true
                    isFocusableInTouchMode = true
                    // setTextSize creates the renderer (monospace by default); setTypeface
                    // before it would dereference a null renderer.
                    setTextSize(fontPx.toInt())
                    setTerminalViewClient(
                        ViewClient(
                            context = context,
                            view = { terminalView },
                            readCtrl = { ctrlDown.also { if (it) ctrlDown = false } },
                            readAlt = { altDown.also { if (it) altDown = false } },
                            onScaleFont = { scale ->
                                fontPx = (fontPx * scale).coerceIn(with(density) { 8.sp.toPx() }, with(density) { 24.sp.toPx() })
                                setTextSize(fontPx.toInt())
                            },
                        ),
                    )
                    terminalView = this
                    TerminalSessions.attach(sessionKey, this)
                }
            },
            update = { view ->
                if (view.currentSession !== TerminalSessions.get(sessionKey)) {
                    TerminalSessions.attach(sessionKey, view)
                }
            },
            onRelease = { view -> TerminalSessions.detach(sessionKey, view) },
        )
        TerminalKeyRow(
            ctrlDown = ctrlDown,
            altDown = altDown,
            onToggleCtrl = { ctrlDown = !ctrlDown },
            onToggleAlt = { altDown = !altDown },
            onKey = { keyCode, meta -> terminalView?.handleKeyCode(keyCode, meta) },
            onText = { text -> TerminalSessions.write(sessionKey, text) },
            quickActions = quickActions,
            onVoice = {
                val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                runCatching { speechLauncher.launch(intent) }
                    .onFailure { android.widget.Toast.makeText(context, "No voice input available", android.widget.Toast.LENGTH_SHORT).show() }
            },
            onAttach = attachments?.let { { attachLauncher.launch(arrayOf("*/*")) } },
        )
    }
}

@Composable
private fun TerminalKeyRow(
    ctrlDown: Boolean,
    altDown: Boolean,
    onToggleCtrl: () -> Unit,
    onToggleAlt: () -> Unit,
    onKey: (Int, Int) -> Unit,
    onText: (String) -> Unit,
    quickActions: List<TerminalQuickAction>,
    onVoice: () -> Unit,
    onAttach: (() -> Unit)?,
) {
    Column(Modifier.fillMaxWidth().background(TerminalBackground).padding(horizontal = 6.dp, vertical = 4.dp)) {
        if (quickActions.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                quickActions.forEach { action ->
                    TerminalKey(action.label, accent = true) { onText(action.send) }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TerminalKey("🎤", onClick = onVoice)
            onAttach?.let { TerminalKey("📎", onClick = it) }
            TerminalKey("ESC") { onKey(KeyEvent.KEYCODE_ESCAPE, 0) }
            TerminalKey("TAB") { onKey(KeyEvent.KEYCODE_TAB, 0) }
            TerminalKey("⇧TAB") { onKey(KeyEvent.KEYCODE_TAB, KeyEvent.META_SHIFT_ON) }
            TerminalKey("CTRL", active = ctrlDown, onClick = onToggleCtrl)
            TerminalKey("ALT", active = altDown, onClick = onToggleAlt)
            TerminalKey("↑") { onKey(KeyEvent.KEYCODE_DPAD_UP, 0) }
            TerminalKey("↓") { onKey(KeyEvent.KEYCODE_DPAD_DOWN, 0) }
            TerminalKey("←") { onKey(KeyEvent.KEYCODE_DPAD_LEFT, 0) }
            TerminalKey("→") { onKey(KeyEvent.KEYCODE_DPAD_RIGHT, 0) }
            TerminalKey("/") { onText("/") }
            TerminalKey("^C") { onText("\u0003") }
            TerminalKey("↵") { onKey(KeyEvent.KEYCODE_ENTER, 0) }
        }
    }
}

@Composable
private fun TerminalKey(label: String, active: Boolean = false, accent: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        Modifier
            .clip(shape)
            .background(if (active) KeyActive else KeyBackground)
            .border(1.dp, if (accent) KeyActive.copy(alpha = 0.6f) else KeyBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            label,
            style = TextStyle(
                color = when {
                    active -> Color.Black
                    accent -> KeyActive
                    else -> Color(0xFFE6EDF3)
                },
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
            ),
        )
    }
}

private class ViewClient(
    private val context: Context,
    private val view: () -> TerminalView?,
    private val readCtrl: () -> Boolean,
    private val readAlt: () -> Boolean,
    private val onScaleFont: (Float) -> Unit,
) : TerminalViewClient {
    override fun onScale(scale: Float): Float {
        // Termux reports cumulative scale; apply it in coarse steps to avoid thrashing layout.
        if (scale < 0.9f || scale > 1.1f) {
            onScaleFont(scale)
            return 1f
        }
        return scale
    }

    override fun onSingleTapUp(e: MotionEvent) {
        val terminal = view() ?: return
        terminal.requestFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(terminal, InputMethodManager.SHOW_IMPLICIT)
    }

    override fun shouldBackButtonBeMappedToEscape(): Boolean = false
    override fun shouldEnforceCharBasedInput(): Boolean = true
    override fun shouldUseCtrlSpaceWorkaround(): Boolean = false
    override fun isTerminalViewSelected(): Boolean = true
    override fun copyModeChanged(copyMode: Boolean) = Unit
    override fun onKeyDown(keyCode: Int, e: KeyEvent, session: TerminalSession): Boolean = false
    override fun onKeyUp(keyCode: Int, e: KeyEvent): Boolean = false
    override fun onLongPress(event: MotionEvent): Boolean = false
    override fun readControlKey(): Boolean = readCtrl()
    override fun readAltKey(): Boolean = readAlt()
    override fun readShiftKey(): Boolean = false
    override fun readFnKey(): Boolean = false
    override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession): Boolean = false
    override fun onEmulatorSet() = Unit
    override fun logError(tag: String?, message: String?) = Unit
    override fun logWarn(tag: String?, message: String?) = Unit
    override fun logInfo(tag: String?, message: String?) = Unit
    override fun logDebug(tag: String?, message: String?) = Unit
    override fun logVerbose(tag: String?, message: String?) = Unit
    override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) = Unit
    override fun logStackTrace(tag: String?, e: Exception?) = Unit
}

/** Copies [uri] into the attachment folder and returns the path the agent sees, or null on failure. */
private fun copyAttachment(context: android.content.Context, uri: android.net.Uri, target: TerminalAttachmentTarget): String? = runCatching {
    val display = context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    val safe = (display ?: "attachment").replace(Regex("[^A-Za-z0-9._-]"), "_").take(80).ifBlank { "attachment" }
    target.hostDir.mkdirs()
    var file = java.io.File(target.hostDir, safe)
    var n = 1
    while (file.exists()) file = java.io.File(target.hostDir, "${safe.substringBeforeLast('.')}-${n++}${safe.substringAfterLast('.', "").let { if (it.isEmpty()) "" else ".$it" }}")
    context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } } ?: return null
    "${target.guestDir.trimEnd('/')}/${file.name}"
}.getOrNull()
