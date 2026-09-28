package com.hivebench.app.browser

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hivebench.app.ui.neo.DrawerDetent
import com.hivebench.app.ui.neo.MaterialTheme
import com.hivebench.app.ui.neo.NeoBottomDrawer
import com.hivebench.app.ui.neo.NeoDrawerRow
import com.hivebench.app.ui.neo.OutlinedTextField
import com.hivebench.app.ui.neo.Text
import com.hivebench.app.ui.theme.NeoBlack
import com.hivebench.app.ui.theme.NeoLime

/** An agent session the inspector can push context into. */
data class BrowserAgentTarget(
    /** Existing session id, or `new:<agent stableId>` to start a new session for that agent. */
    val id: String,
    val title: String,
    val subtitle: String,
    val running: Boolean,
)

/** Something ready to hand to an agent: shown as a preview, sent with the user's note on top. */
data class BrowserPushPayload(val title: String, val context: String)

/** One freehand stroke drawn over the page, in WebView pixel coordinates. */
data class DrawStroke(val points: List<Offset>, val color: Color, val width: Float)

val DrawColors = listOf(Color(0xFFFF3B30), NeoLime, Color(0xFFFFD60A), Color(0xFF0A84FF), Color.White)

internal fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
}

/** Freehand drawing layer laid exactly over the WebView. */
@Composable
fun DrawOverlay(
    strokes: SnapshotStateList<DrawStroke>,
    color: Color,
    strokeWidthPx: Float,
    modifier: Modifier = Modifier,
) {
    var current by remember { mutableStateOf<List<Offset>>(emptyList()) }
    Canvas(
        modifier
            .fillMaxSize()
            .pointerInput(color, strokeWidthPx) {
                detectDragGestures(
                    onDragStart = { start -> current = listOf(start) },
                    onDrag = { change, _ ->
                        change.consume()
                        current = current + change.position
                    },
                    onDragEnd = {
                        if (current.size > 1) strokes.add(DrawStroke(current, color, strokeWidthPx))
                        current = emptyList()
                    },
                    onDragCancel = { current = emptyList() },
                )
            },
    ) {
        (strokes + listOfNotNull(current.takeIf { it.size > 1 }?.let { DrawStroke(it, color, strokeWidthPx) }))
            .forEach { stroke ->
                val path = Path().apply {
                    moveTo(stroke.points.first().x, stroke.points.first().y)
                    stroke.points.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(path, stroke.color, style = Stroke(stroke.width, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
    }
}

/** Renders the visible page plus the user's strokes into one bitmap. */
fun captureWithDrawing(webView: WebView, strokes: List<DrawStroke>): Bitmap? {
    if (webView.width <= 0 || webView.height <= 0) return null
    val bitmap = Bitmap.createBitmap(webView.width, webView.height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    webView.draw(canvas)
    val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }
    strokes.forEach { stroke ->
        paint.color = stroke.color.toArgb()
        paint.strokeWidth = stroke.width
        val path = android.graphics.Path().apply {
            moveTo(stroke.points.first().x, stroke.points.first().y)
            stroke.points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        canvas.drawPath(path, paint)
    }
    return bitmap
}

/**
 * Lets the user add an instruction and choose which agent session receives
 * [payload]. The text lands in that agent's prompt (not submitted), so the user
 * can review it in the agent's own TUI before pressing Enter.
 */
@Composable
fun AgentPushSheet(
    payload: BrowserPushPayload,
    targets: List<BrowserAgentTarget>,
    onDismiss: () -> Unit,
    onSend: (targetId: String, text: String) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var note by rememberSaveable(payload.context) { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf(targets.firstOrNull { it.running }?.id ?: targets.firstOrNull()?.id) }
    fun finalText() = if (note.isBlank()) payload.context else "${note.trim()}\n\n${payload.context}"

    NeoBottomDrawer(
        visible = true,
        onDismiss = onDismiss,
        initialDetent = DrawerDetent.Full,
        allowedDetents = listOf(DrawerDetent.Half, DrawerDetent.Full),
        title = "Send to agent",
        subtitle = payload.title,
    ) { _, _ ->
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                label = { Text("What should the agent do? (optional)") },
                placeholder = { Text("e.g. Make this button match the header style") },
            )
            Text("AGENT SESSION", fontWeight = FontWeight.Black, fontSize = 11.sp, color = NeoLime, letterSpacing = 1.sp)
            if (targets.isEmpty()) {
                Text(
                    "No agent is installed yet. Install one from the Agents tab.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            targets.forEach { target ->
                NeoDrawerRow(
                    title = target.title,
                    subtitle = target.subtitle,
                    badge = if (target.running) "RUNNING" else null,
                    selected = selected == target.id,
                    onClick = { selected = target.id },
                )
            }
            Text("CONTEXT", fontWeight = FontWeight.Black, fontSize = 11.sp, color = NeoLime, letterSpacing = 1.sp)
            Text(
                payload.context.take(1_200) + if (payload.context.length > 1_200) "\n…" else "",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(10.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PushButton("Send to agent", primary = true, enabled = selected != null) {
                    selected?.let { onSend(it, finalText()) }
                }
                PushButton("Copy", primary = false) { copyToClipboard(context, "Context", finalText()) }
            }
        }
    }
}

@Composable
internal fun PushButton(label: String, primary: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier
            .clip(shape)
            .background(if (primary && enabled) NeoLime else MaterialTheme.colorScheme.surface)
            .border(1.5.dp, NeoBlack, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(
            label,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = if (primary && enabled) NeoBlack else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Colour swatch used in the drawing toolbar. */
@Composable
internal fun ColorDot(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(if (selected) 26.dp else 22.dp)
            .clip(CircleShape)
            .background(color)
            .border(if (selected) 3.dp else 1.dp, if (selected) Color.White else NeoBlack, CircleShape)
            .clickable(onClick = onClick),
    )
}
