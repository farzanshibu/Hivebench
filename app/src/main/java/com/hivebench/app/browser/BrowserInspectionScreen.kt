package com.hivebench.app.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.compose.BackHandler
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.hivebench.app.ui.neo.CircularProgressIndicator
import com.hivebench.app.ui.neo.HorizontalDivider
import com.hivebench.app.ui.neo.Icon
import com.hivebench.app.ui.neo.IconButton
import com.hivebench.app.ui.neo.MaterialTheme
import com.hivebench.app.ui.neo.OutlinedTextField
import com.hivebench.app.ui.neo.OutlinedTextFieldDefaults
import com.hivebench.app.ui.neo.ScrollableTabRow
import com.hivebench.app.ui.neo.Surface
import com.hivebench.app.ui.neo.Tab
import com.hivebench.app.ui.neo.TabRowDefaults
import com.hivebench.app.ui.neo.Text
import com.hivebench.app.ui.neo.tabIndicatorOffset
import com.hivebench.app.ui.theme.NeoBlack
import com.hivebench.app.ui.theme.NeoDarkBorder
import com.hivebench.app.ui.theme.NeoLime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class BrowserInspectorTab(val label: String, val icon: ImageVector) {
    PREVIEW("Preview", Icons.Default.Language),
    CONSOLE("Console", Icons.Default.Terminal),
    NETWORK("Network", Icons.Default.Wifi),
    ELEMENTS("Elements", Icons.Default.Code),
}

private val ErrorRed = Color(0xFFFF5252)
private val WarnYellow = Color(0xFFFFD600)
private val InfoCyan = Color(0xFF00E5FF)

/**
 * Live preview with a DevTools-style inspector. Everything captured here
 * (elements, drawings, console, network) can be copied or pushed into an
 * agent session chosen from [agentTargets].
 *
 * @param projectKey the page, logs and selection persist per project across tab switches.
 * @param url the detected dev-server URL; loaded automatically when it changes.
 * @param onSaveCapture stores a screenshot in the project and returns the path
 *   the agent sees (inside the Linux runtime), or null when there is no project.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserInspectionScreen(
    projectKey: String,
    ready: Boolean,
    url: String?,
    agentTargets: List<BrowserAgentTarget> = emptyList(),
    onPushToAgent: (targetId: String, text: String) -> Unit = { _, _ -> },
    onSaveCapture: (Bitmap) -> String? = { null },
) {
    val context = LocalContext.current
    val session = remember(projectKey) { BrowserSessions.obtain(projectKey) }
    var pushPayload by remember { mutableStateOf<BrowserPushPayload?>(null) }
    var drawColor by remember { mutableStateOf(DrawColors.first()) }
    val strokePx = with(LocalDensity.current) { 4.dp.toPx() }
    var strokeWidth by remember { mutableFloatStateOf(strokePx) }

    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) NeoDarkBorder else NeoBlack
    val pageUrl = session.activeUrl ?: session.address

    val bridge = remember(session) {
        BrowserInspectorBridge(
            onConsoleMessage = session::addConsole,
            onNetworkEvent = session::addNetwork,
            onElementSelected = { element -> session.selectedElement = element },
        )
    }

    fun load(target: String) {
        session.address = target
        session.activeUrl = target
        session.clearPageData()
        session.webView?.loadUrl(target)
    }

    // A dev server found (or restarted on a new port) while the tab is open loads once;
    // manual navigation afterwards is left alone.
    LaunchedEffect(url, ready) {
        if (ready && url != null && url != session.autoLoadedUrl) {
            session.autoLoadedUrl = url
            load(url)
        }
    }

    LaunchedEffect(session.tool) {
        session.webView?.evaluateJavascript(BrowserInspectorBridge.setInspectModeScript(session.tool == BrowserTool.INSPECT), null)
    }

    // Back walks the page's own history before leaving the workspace.
    BackHandler(enabled = session.canGoBack && session.currentTab == BrowserInspectorTab.PREVIEW) {
        session.webView?.goBack()
    }

    val navigate = {
        val normalized = normalizeUrl(session.address)
        if (normalized.isNotBlank()) load(normalized)
    }

    fun pageSnapshot(): BrowserPushPayload {
        val errors = session.consoleLogs.filter { it.level == ConsoleLogLevel.ERROR }
        val failed = session.networkRequests.filter { it.failed }
        return BrowserPushPayload(
            title = "Page snapshot",
            context = buildString {
                appendLine("### Browser page: $pageUrl")
                appendLine("- Console: ${errors.size} errors, ${session.consoleLogs.count { it.level == ConsoleLogLevel.WARN }} warnings, ${session.consoleLogs.size} total")
                appendLine("- Network: ${failed.size} failed of ${session.networkRequests.size} requests")
                if (errors.isNotEmpty()) {
                    appendLine("\nConsole errors:")
                    errors.take(10).forEach { appendLine("- ${it.format()}") }
                }
                if (failed.isNotEmpty()) {
                    appendLine("\nFailed requests:")
                    failed.take(10).forEach { appendLine("- ${it.summary()}") }
                }
                session.selectedElement?.let { appendLine(); append(it.toPromptContext()) }
            },
        )
    }

    fun sendDrawing() {
        val webView = session.webView ?: return
        val bitmap = captureWithDrawing(webView, session.strokes.toList()) ?: return
        val path = onSaveCapture(bitmap)
        if (path == null) {
            Toast.makeText(context, "Open a project to attach screenshots", Toast.LENGTH_SHORT).show()
            return
        }
        pushPayload = BrowserPushPayload(
            title = "Annotated screenshot",
            context = buildString {
                appendLine("### Annotated screenshot of $pageUrl")
                appendLine("I drew on the page to mark what I mean. Look at the image: $path")
                appendLine("- Viewport: ${bitmap.width}×${bitmap.height}px (device pixels)")
                session.selectedElement?.let { appendLine(); append(it.toPromptContext()) }
            },
        )
    }

    Column(Modifier.fillMaxSize()) {
        // ---- Address bar + tools ----
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().border(1.dp, borderColor)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = session.address,
                        onValueChange = { session.address = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("localhost:3000 or URL", fontSize = 12.sp) },
                        shape = RoundedCornerShape(8.dp),
                        leadingIcon = {
                            if (session.isLoading) CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = NeoLime)
                            else Icon(Icons.Default.Language, null, Modifier.size(16.dp), tint = NeoLime)
                        },
                        trailingIcon = {
                            IconButton(onClick = navigate) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Go", tint = NeoLime) }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { navigate() }),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeoLime, unfocusedBorderColor = borderColor),
                    )
                    Spacer(Modifier.width(6.dp))
                    IconButton(onClick = { session.webView?.reload() }, modifier = Modifier.size(40.dp).border(1.5.dp, borderColor, RoundedCornerShape(8.dp))) {
                        Icon(Icons.Default.Refresh, "Reload", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ToolChip("Inspect", Icons.Default.NearMe, active = session.tool == BrowserTool.INSPECT) {
                        session.tool = if (session.tool == BrowserTool.INSPECT) BrowserTool.NONE else BrowserTool.INSPECT
                        session.currentTab = BrowserInspectorTab.PREVIEW
                    }
                    ToolChip("Draw", Icons.Default.Brush, active = session.tool == BrowserTool.DRAW) {
                        session.tool = if (session.tool == BrowserTool.DRAW) BrowserTool.NONE else BrowserTool.DRAW
                        session.currentTab = BrowserInspectorTab.PREVIEW
                    }
                    ToolChip("Send page", Icons.Default.Send, active = false) { pushPayload = pageSnapshot() }
                    ToolChip("Copy page", Icons.Default.ContentCopy, active = false) {
                        copyToClipboard(context, "Page context", pageSnapshot().context)
                    }
                }
            }
        }

        // ---- Tabs ----
        val errorCount = session.consoleLogs.count { it.level == ConsoleLogLevel.ERROR }
        val failedCount = session.networkRequests.count { it.failed }
        ScrollableTabRow(
            selectedTabIndex = session.currentTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            edgePadding = 8.dp,
            indicator = { positions ->
                TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(positions[session.currentTab.ordinal]), height = 3.dp, color = NeoLime)
            },
            modifier = Modifier.border(BorderStroke(1.dp, borderColor)),
        ) {
            BrowserInspectorTab.entries.forEach { tab ->
                val selected = session.currentTab == tab
                Tab(selected = selected, onClick = { session.currentTab = tab }, text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(tab.icon, null, Modifier.size(15.dp), tint = if (selected) NeoLime else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(tab.label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontSize = 12.sp)
                        val badge = when (tab) {
                            BrowserInspectorTab.CONSOLE -> errorCount
                            BrowserInspectorTab.NETWORK -> failedCount
                            BrowserInspectorTab.ELEMENTS -> if (session.selectedElement != null) 1 else 0
                            else -> 0
                        }
                        if (badge > 0) CountBadge(if (tab == BrowserInspectorTab.ELEMENTS) "●" else "$badge", if (tab == BrowserInspectorTab.ELEMENTS) NeoLime else ErrorRed)
                    }
                })
            }
        }

        // ---- Body: the WebView stays attached; other tabs cover it ----
        Box(Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx -> session.obtainWebView(ctx) { wrapped -> createInspectorWebView(wrapped, session, bridge) } },
            )

            if (session.activeUrl == null) {
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.Language, null, Modifier.size(40.dp), tint = NeoLime)
                        Spacer(Modifier.height(12.dp))
                        Text("No dev server running", fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Start one in the Terminal or ask an agent (e.g. npm run dev). It opens here automatically, or type a URL above.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else if (session.currentTab == BrowserInspectorTab.PREVIEW) {
                if (session.tool == BrowserTool.DRAW) {
                    DrawOverlay(session.strokes, drawColor, strokeWidth)
                    DrawToolbar(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        color = drawColor,
                        onColor = { drawColor = it },
                        thick = strokeWidth > strokePx,
                        onToggleThick = { strokeWidth = if (strokeWidth > strokePx) strokePx else strokePx * 2.5f },
                        canUndo = session.strokes.isNotEmpty(),
                        onUndo = { if (session.strokes.isNotEmpty()) session.strokes.removeAt(session.strokes.lastIndex) },
                        onClear = { session.strokes.clear() },
                        onSend = { sendDrawing() },
                    )
                } else {
                    session.selectedElement?.let { element ->
                        ElementQuickBar(
                            element = element,
                            modifier = Modifier.align(Alignment.BottomCenter),
                            onParent = { session.webView?.evaluateJavascript(BrowserInspectorBridge.SELECT_PARENT_SCRIPT, null) },
                            onChild = { session.webView?.evaluateJavascript(BrowserInspectorBridge.SELECT_CHILD_SCRIPT, null) },
                            onDetails = { session.currentTab = BrowserInspectorTab.ELEMENTS },
                            onCopy = { copyToClipboard(context, "Element context", element.toClipboardText()) },
                            onAnnotate = { pushPayload = BrowserPushPayload("Element ${element.displaySelector}", element.toPromptContext()) },
                            onClose = {
                                session.selectedElement = null
                                session.webView?.evaluateJavascript(BrowserInspectorBridge.CLEAR_SELECTION_SCRIPT, null)
                            },
                        )
                    }
                }
            } else {
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                    when (session.currentTab) {
                        BrowserInspectorTab.CONSOLE -> ConsoleTabContent(
                            logs = session.consoleLogs,
                            onClear = { session.consoleLogs.clear() },
                            onCopyEntry = { copyToClipboard(context, "Log", it.format()) },
                            onSend = { logs ->
                                pushPayload = BrowserPushPayload(
                                    title = "${logs.size} console entries",
                                    context = buildString {
                                        appendLine("### Browser console on $pageUrl")
                                        logs.take(30).forEach { appendLine("- ${it.format()}") }
                                    },
                                )
                            },
                        )
                        BrowserInspectorTab.NETWORK -> NetworkTabContent(
                            requests = session.networkRequests,
                            onClear = { session.networkRequests.clear() },
                            onCopy = { copyToClipboard(context, "Request", it.toPromptContext()) },
                            onSendOne = { pushPayload = BrowserPushPayload("${it.method} ${it.url.take(60)}", it.toPromptContext()) },
                            onSendMany = { list ->
                                pushPayload = BrowserPushPayload(
                                    title = "${list.size} requests",
                                    context = buildString {
                                        appendLine("### Network requests on $pageUrl")
                                        list.take(20).forEach { appendLine("- ${it.summary()}") }
                                        list.filter { it.failed }.take(3).forEach { appendLine(); append(it.toPromptContext()) }
                                    },
                                )
                            },
                        )
                        BrowserInspectorTab.ELEMENTS -> ElementsTabContent(
                            element = session.selectedElement,
                            onStartInspect = { session.tool = BrowserTool.INSPECT; session.currentTab = BrowserInspectorTab.PREVIEW },
                            onParent = { session.webView?.evaluateJavascript(BrowserInspectorBridge.SELECT_PARENT_SCRIPT, null) },
                            onChild = { session.webView?.evaluateJavascript(BrowserInspectorBridge.SELECT_CHILD_SCRIPT, null) },
                            onCopy = { text, label -> copyToClipboard(context, label, text) },
                            onSend = { el -> pushPayload = BrowserPushPayload("Element ${el.displaySelector}", el.toPromptContext()) },
                        )
                        BrowserInspectorTab.PREVIEW -> Unit
                    }
                }
            }
        }
    }

    pushPayload?.let { payload ->
        AgentPushSheet(
            payload = payload,
            targets = agentTargets,
            onDismiss = { pushPayload = null },
            onSend = { targetId, text ->
                pushPayload = null
                if (session.tool == BrowserTool.DRAW) {
                    session.strokes.clear()
                    session.tool = BrowserTool.NONE
                }
                onPushToAgent(targetId, text)
            },
        )
    }
}

/** Builds the inspector WebView once per [session]; callbacks write into the session's state. */
@SuppressLint("SetJavaScriptEnabled")
private fun createInspectorWebView(context: android.content.Context, session: BrowserSession, bridge: BrowserInspectorBridge): WebView =
    WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        addJavascriptInterface(bridge, BrowserInspectorBridge.INTERFACE_NAME)
        // Document-start injection sees requests made before the page finishes loading.
        val documentStart = WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
        if (documentStart) {
            WebViewCompat.addDocumentStartJavaScript(this, BrowserInspectorBridge.INJECTION_SCRIPT, setOf("*"))
        }
        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                session.isLoading = newProgress < 100
            }

            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                val level = when (message.messageLevel()) {
                    ConsoleMessage.MessageLevel.ERROR -> ConsoleLogLevel.ERROR
                    ConsoleMessage.MessageLevel.WARNING -> ConsoleLogLevel.WARN
                    ConsoleMessage.MessageLevel.DEBUG -> ConsoleLogLevel.DEBUG
                    ConsoleMessage.MessageLevel.TIP -> ConsoleLogLevel.INFO
                    else -> ConsoleLogLevel.LOG
                }
                session.addConsole(
                    ConsoleLogEntry(
                        level = level,
                        message = message.message(),
                        source = message.sourceId()?.takeIf { it.isNotBlank() },
                        lineNumber = message.lineNumber().takeIf { it > 0 },
                    ),
                )
                return true
            }
        }
        webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                session.isLoading = true
                url?.let { session.address = it }
            }

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                session.canGoBack = view?.canGoBack() == true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                session.isLoading = false
                session.canGoBack = view?.canGoBack() == true
                if (!documentStart) view?.evaluateJavascript(BrowserInspectorBridge.INJECTION_SCRIPT, null)
                view?.evaluateJavascript(BrowserInspectorBridge.setInspectModeScript(session.tool == BrowserTool.INSPECT), null)
            }

            override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                if (request?.isForMainFrame == true) {
                    session.addNetwork(
                        NetworkRequestEntry(
                            method = request.method ?: "GET",
                            url = request.url.toString(),
                            status = 0,
                            type = "document",
                            error = error?.description?.toString().orEmpty(),
                        ),
                    )
                }
            }
        }
        session.activeUrl?.let { loadUrl(it) }
    }

@Composable
private fun ToolChip(label: String, icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        Modifier.clip(shape)
            .background(if (active) NeoLime else MaterialTheme.colorScheme.surfaceVariant)
            .border(1.5.dp, NeoBlack, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, null, Modifier.size(14.dp), tint = if (active) NeoBlack else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (active) NeoBlack else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun CountBadge(text: String, color: Color) {
    Box(Modifier.clip(CircleShape).background(color).padding(horizontal = 5.dp, vertical = 1.dp)) {
        Text(text, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (color == NeoLime) NeoBlack else Color.White)
    }
}

@Composable
private fun SmallAction(label: String, primary: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(7.dp)
    Box(
        Modifier.clip(shape)
            .background(if (primary) NeoLime else MaterialTheme.colorScheme.surface)
            .border(1.dp, NeoBlack, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp),
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (primary) NeoBlack else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun DrawToolbar(
    modifier: Modifier,
    color: Color,
    onColor: (Color) -> Unit,
    thick: Boolean,
    onToggleThick: () -> Unit,
    canUndo: Boolean,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onSend: () -> Unit,
) {
    Row(
        modifier.padding(12.dp).clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.5.dp, NeoBlack, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DrawColors.forEach { swatch -> ColorDot(swatch, swatch == color) { onColor(swatch) } }
        SmallAction(if (thick) "Thick" else "Thin", onClick = onToggleThick)
        if (canUndo) SmallAction("Undo", onClick = onUndo)
        if (canUndo) SmallAction("Clear", onClick = onClear)
        SmallAction("Send", primary = true, onClick = onSend)
    }
}

@Composable
private fun ElementQuickBar(
    element: InspectedElement,
    modifier: Modifier,
    onParent: () -> Unit,
    onChild: () -> Unit,
    onDetails: () -> Unit,
    onCopy: () -> Unit,
    onAnnotate: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier.fillMaxWidth().padding(10.dp).clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.5.dp, NeoLime, RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(element.displaySelector, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(
                        element.boundingBox.takeIf { it.isNotBlank() },
                        element.componentName.takeIf { it.isNotBlank() }?.let { "<$it>" },
                        element.sourceFile.takeIf { it.isNotBlank() }?.substringAfterLast('/'),
                    ).joinToString(" · "),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text("✕", fontSize = 14.sp, modifier = Modifier.clickable(onClick = onClose).padding(6.dp))
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SmallAction("Annotate & send", primary = true, onClick = onAnnotate)
            SmallAction("Copy", onClick = onCopy)
            SmallAction("Parent", onClick = onParent)
            SmallAction("Child", onClick = onChild)
            SmallAction("Details", onClick = onDetails)
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier.clip(shape)
            .background(if (selected) NeoLime else Color.Transparent)
            .border(1.dp, if (selected) NeoLime else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (selected) NeoBlack else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ConsoleTabContent(
    logs: List<ConsoleLogEntry>,
    onClear: () -> Unit,
    onCopyEntry: (ConsoleLogEntry) -> Unit,
    onSend: (List<ConsoleLogEntry>) -> Unit,
) {
    val borderColor = if (isSystemInDarkTheme()) NeoDarkBorder else NeoBlack
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }
    var filter by rememberSaveable { mutableStateOf("all") }
    val shown = when (filter) {
        "errors" -> logs.filter { it.level == ConsoleLogLevel.ERROR }
        "warnings" -> logs.filter { it.level == ConsoleLogLevel.WARN }
        else -> logs
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip("All ${logs.size}", filter == "all") { filter = "all" }
            FilterChip("Errors ${logs.count { it.level == ConsoleLogLevel.ERROR }}", filter == "errors") { filter = "errors" }
            FilterChip("Warnings ${logs.count { it.level == ConsoleLogLevel.WARN }}", filter == "warnings") { filter = "warnings" }
            Spacer(Modifier.weight(1f))
            if (shown.isNotEmpty()) SmallAction("Send", primary = true) { onSend(shown) }
            IconButton(onClick = onClear) { Icon(Icons.Default.DeleteSweep, "Clear console", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        HorizontalDivider(color = borderColor)
        if (shown.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No console output yet", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(shown, key = { it.id }) { log ->
                    val badgeColor = when (log.level) {
                        ConsoleLogLevel.ERROR -> ErrorRed
                        ConsoleLogLevel.WARN -> WarnYellow
                        ConsoleLogLevel.INFO -> InfoCyan
                        ConsoleLogLevel.DEBUG -> Color(0xFFB39DDB)
                        ConsoleLogLevel.LOG -> NeoLime
                    }
                    Row(
                        Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                            .clickable { onCopyEntry(log) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Box(Modifier.clip(RoundedCornerShape(4.dp)).background(badgeColor).padding(horizontal = 4.dp, vertical = 2.dp)) {
                            Text(log.level.label, fontSize = 9.sp, fontWeight = FontWeight.Black, color = NeoBlack)
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                log.message,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = when (log.level) {
                                    ConsoleLogLevel.ERROR -> Color(0xFFFF8A80)
                                    ConsoleLogLevel.WARN -> Color(0xFFFFE57F)
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 12,
                                overflow = TextOverflow.Ellipsis,
                            )
                            log.source?.let {
                                Text("${it.substringAfterLast('/')}${log.lineNumber?.let { n -> ":$n" } ?: ""}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace)
                            }
                        }
                        Text(timeFormat.format(Date(log.timestamp)), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkTabContent(
    requests: List<NetworkRequestEntry>,
    onClear: () -> Unit,
    onCopy: (NetworkRequestEntry) -> Unit,
    onSendOne: (NetworkRequestEntry) -> Unit,
    onSendMany: (List<NetworkRequestEntry>) -> Unit,
) {
    val borderColor = if (isSystemInDarkTheme()) NeoDarkBorder else NeoBlack
    var filter by rememberSaveable { mutableStateOf("api") }
    var expanded by remember { mutableStateOf<String?>(null) }
    val shown = when (filter) {
        "api" -> requests.filter { it.type == "fetch" || it.type == "xhr" }
        "failed" -> requests.filter { it.failed }
        else -> requests
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip("Fetch/XHR", filter == "api") { filter = "api" }
            FilterChip("Failed ${requests.count { it.failed }}", filter == "failed") { filter = "failed" }
            FilterChip("All ${requests.size}", filter == "all") { filter = "all" }
            Spacer(Modifier.weight(1f))
            if (shown.isNotEmpty()) SmallAction("Send", primary = true) { onSendMany(shown) }
            IconButton(onClick = onClear) { Icon(Icons.Default.DeleteSweep, "Clear network", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        HorizontalDivider(color = borderColor)
        if (shown.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No matching requests yet", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(shown, key = { it.id }) { req ->
                    val statusColor = when {
                        req.failed -> ErrorRed
                        req.status in 300..399 -> InfoCyan
                        req.status in 200..299 -> NeoLime
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val isOpen = expanded == req.id
                    Column(
                        Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                            .border(1.dp, if (isOpen) NeoLime else borderColor, RoundedCornerShape(6.dp))
                            .clickable { expanded = if (isOpen) null else req.id }
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.clip(RoundedCornerShape(4.dp)).background(statusColor).padding(horizontal = 5.dp, vertical = 2.dp)) {
                                Text(if (req.status > 0) "${req.status}" else if (req.failed) "ERR" else "—", fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeoBlack)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(req.method, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                            Spacer(Modifier.width(8.dp))
                            Text(req.url, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = if (isOpen) 4 else 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Spacer(Modifier.width(8.dp))
                            Text("${req.durationMs}ms", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isOpen) {
                            Text(
                                req.toPromptContext(),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp)).padding(8.dp),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                SmallAction("Send to agent", primary = true) { onSendOne(req) }
                                SmallAction("Copy") { onCopy(req) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ElementsTabContent(
    element: InspectedElement?,
    onStartInspect: () -> Unit,
    onParent: () -> Unit,
    onChild: () -> Unit,
    onCopy: (text: String, label: String) -> Unit,
    onSend: (InspectedElement) -> Unit,
) {
    val borderColor = if (isSystemInDarkTheme()) NeoDarkBorder else NeoBlack
    if (element == null) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.NearMe, null, modifier = Modifier.size(44.dp), tint = NeoLime)
            Spacer(Modifier.height(12.dp))
            Text("No element selected", fontWeight = FontWeight.Black, fontSize = 16.sp)
            Spacer(Modifier.height(6.dp))
            Text("Tap Inspect, then tap anything on the page.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Spacer(Modifier.height(14.dp))
            SmallAction("Start inspecting", primary = true, onClick = onStartInspect)
        }
        return
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SmallAction("Annotate & send", primary = true) { onSend(element) }
            SmallAction("Copy context") { onCopy(element.toClipboardText(), "Element context") }
            SmallAction("Copy selector") { onCopy(element.selector.ifBlank { element.displaySelector }, "Selector") }
            SmallAction("Copy HTML") { onCopy(element.outerHtml, "HTML") }
            SmallAction("Parent", onClick = onParent)
            SmallAction("Child", onClick = onChild)
        }
        if (element.ancestors.isNotEmpty()) {
            Text(
                (element.ancestors.map { it.label } + element.displaySelector).joinToString("  ›  "),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        InspectorCard("Element", borderColor) {
            KeyValue("selector", element.selector.ifBlank { element.displaySelector })
            KeyValue("xpath", element.xpath)
            KeyValue("component", element.componentName)
            KeyValue("source", element.sourceFile)
            KeyValue("box", element.boundingBox)
            KeyValue("children", element.childCount.toString())
            if (element.textContent.isNotBlank()) KeyValue("text", element.textContent.take(200))
        }
        if (element.boxModel.isNotEmpty()) {
            InspectorCard("Box model", borderColor) { element.boxModel.forEach { (k, v) -> KeyValue(k, v) } }
        }
        if (element.attributes.isNotEmpty()) {
            InspectorCard("Attributes", borderColor) { element.attributes.forEach { (k, v) -> KeyValue(k, v) } }
        }
        if (element.computedStyles.isNotEmpty()) {
            InspectorCard("Computed styles", borderColor) { element.computedStyles.forEach { (k, v) -> KeyValue(k, v) } }
        }
        if (element.outerHtml.isNotBlank()) {
            InspectorCard("HTML", borderColor) {
                Text(element.outerHtml, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun InspectorCard(title: String, borderColor: Color, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(title.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeoLime, letterSpacing = 1.sp)
        content()
    }
}

@Composable
private fun KeyValue(key: String, value: String) {
    if (value.isBlank()) return
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(key, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(96.dp))
        Text(value, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
    }
}

private fun normalizeUrl(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""
    return when {
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
        trimmed.startsWith("localhost") || trimmed.startsWith("127.0.0.1") || trimmed.startsWith("10.0.2.2") -> "http://$trimmed"
        trimmed.contains(":") -> "http://$trimmed"
        else -> "https://$trimmed"
    }
}
