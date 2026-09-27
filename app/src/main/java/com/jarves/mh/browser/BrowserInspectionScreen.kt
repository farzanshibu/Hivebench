package com.jarves.mh.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.jarves.mh.ui.theme.NeoBlack
import com.jarves.mh.ui.theme.NeoCard
import com.jarves.mh.ui.theme.NeoDarkBorder
import com.jarves.mh.ui.theme.NeoLime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class BrowserInspectorTab(val label: String, val icon: ImageVector) {
    PREVIEW("Preview", Icons.Default.Language),
    CONSOLE("Console", Icons.Default.Terminal),
    NETWORK("Network", Icons.Default.Wifi),
    ELEMENTS("Elements", Icons.Default.Code),
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserInspectionScreen(
    ready: Boolean,
    url: String?,
    onSendContextToAgent: (String) -> Unit = {},
) {
    var address by rememberSaveable(url) { mutableStateOf(if (ready) url.orEmpty() else "") }
    var activeUrl by rememberSaveable(url) { mutableStateOf(if (ready) url else null) }
    var currentTab by rememberSaveable { mutableStateOf(BrowserInspectorTab.PREVIEW) }
    var designModeActive by rememberSaveable { mutableStateOf(false) }

    val consoleLogs = remember { mutableStateListOf<ConsoleLogEntry>() }
    val networkRequests = remember { mutableStateListOf<NetworkRequestEntry>() }
    var selectedElement by remember { mutableStateOf<InspectedElement?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isLoadingPage by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) NeoDarkBorder else NeoBlack

    val bridge = remember {
        BrowserInspectorBridge(
            onConsoleMessage = { entry -> consoleLogs.add(0, entry) },
            onNetworkEvent = { entry -> networkRequests.add(0, entry) },
            onElementSelected = { element ->
                selectedElement = element
                currentTab = BrowserInspectorTab.ELEMENTS
            },
        )
    }

    LaunchedEffect(designModeActive) {
        webViewRef?.evaluateJavascript("window.__mhSetDesignMode && window.__mhSetDesignMode($designModeActive);", null)
    }

    val navigate = {
        val normalized = normalizeUrl(address)
        if (normalized.isNotBlank()) {
            address = normalized
            activeUrl = normalized
            webViewRef?.loadUrl(normalized)
        }
    }

    Column(Modifier.fillMaxSize()) {
        // 1. Retro Neobrutalist Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, borderColor),
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
                // Window Buttons & Design Mode Switch
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(10.dp).background(Color(0xFFFF5252), CircleShape).border(1.dp, NeoBlack, CircleShape))
                        Box(Modifier.size(10.dp).background(Color(0xFFFFD600), CircleShape).border(1.dp, NeoBlack, CircleShape))
                        Box(Modifier.size(10.dp).background(NeoLime, CircleShape).border(1.dp, NeoBlack, CircleShape))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "BROWSER CONSOLE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.weight(1f))

                    // Design Mode Pill Toggle
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (designModeActive) NeoLime else MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.5.dp, NeoBlack, RoundedCornerShape(8.dp))
                            .clickable { designModeActive = !designModeActive }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = "Design Mode",
                            tint = if (designModeActive) NeoBlack else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = if (designModeActive) "DESIGN MODE ON" else "DESIGN MODE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = if (designModeActive) NeoBlack else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // URL Bar Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("localhost:3000 or URL", fontSize = 12.sp) },
                        shape = RoundedCornerShape(8.dp),
                        leadingIcon = {
                            if (isLoadingPage) {
                                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = NeoLime)
                            } else {
                                Icon(Icons.Default.Language, null, Modifier.size(16.dp), tint = NeoLime)
                            }
                        },
                        trailingIcon = {
                            IconButton(onClick = navigate) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Navigate", tint = NeoLime)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { navigate() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeoLime,
                            unfocusedBorderColor = borderColor,
                        ),
                    )
                    Spacer(Modifier.width(6.dp))
                    IconButton(
                        onClick = { webViewRef?.reload() },
                        modifier = Modifier
                            .size(40.dp)
                            .border(1.5.dp, borderColor, RoundedCornerShape(8.dp)),
                    ) {
                        Icon(Icons.Default.Refresh, "Reload", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            val failedReqs = networkRequests.filter { it.status >= 400 }
                            val errorLogs = consoleLogs.filter { it.level == ConsoleLogLevel.ERROR }
                            val snapshot = buildString {
                                appendLine("[BROWSER PAGE CONTEXT & SNAPSHOT]")
                                appendLine("URL: ${activeUrl ?: address}")
                                appendLine("Console Errors: ${errorLogs.size} | Total Logs: ${consoleLogs.size}")
                                appendLine("Failed Network Calls: ${failedReqs.size} | Total Requests: ${networkRequests.size}")
                                if (errorLogs.isNotEmpty()) {
                                    appendLine("\nTop Console Errors:")
                                    errorLogs.take(5).forEach { err ->
                                        appendLine("- [${err.level.label}] ${err.message}${if (!err.source.isNullOrBlank()) " (${err.source}:${err.lineNumber ?: ""})" else ""}")
                                    }
                                }
                                if (failedReqs.isNotEmpty()) {
                                    appendLine("\nFailed Network Calls:")
                                    failedReqs.take(5).forEach { req ->
                                        appendLine("- [HTTP ${req.status}] ${req.method} ${req.url} (${req.durationMs}ms)")
                                    }
                                }
                                appendLine("\nPlease inspect this browser session context and help diagnose and fix frontend issues.")
                            }
                            onSendContextToAgent(snapshot)
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(NeoLime, RoundedCornerShape(8.dp))
                            .border(1.5.dp, NeoBlack, RoundedCornerShape(8.dp)),
                    ) {
                        Icon(Icons.Default.AutoAwesome, "Ingest page snapshot into agent chat", Modifier.size(18.dp), tint = NeoBlack)
                    }
                }
            }
        }

        // 2. Inspection Tab Selector
        val errorCount = consoleLogs.count { it.level == ConsoleLogLevel.ERROR }
        ScrollableTabRow(
            selectedTabIndex = currentTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            edgePadding = 8.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[currentTab.ordinal]),
                    height = 3.dp,
                    color = NeoLime,
                )
            },
            modifier = Modifier.border(BorderStroke(1.dp, borderColor)),
        ) {
            BrowserInspectorTab.entries.forEach { tab ->
                val selected = currentTab == tab
                Tab(
                    selected = selected,
                    onClick = { currentTab = tab },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(tab.icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = if (selected) NeoLime else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                tab.label,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (tab == BrowserInspectorTab.CONSOLE && errorCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF5252))
                                        .padding(horizontal = 5.dp, vertical = 1.dp),
                                ) {
                                    Text("$errorCount", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    },
                )
            }
        }

        // 3. Tab Body
        Box(Modifier.fillMaxSize()) {
            // Keep WebView attached in PREVIEW tab
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true

                            addJavascriptInterface(bridge, BrowserInspectorBridge.INTERFACE_NAME)

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    isLoadingPage = newProgress < 100
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    isLoadingPage = true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoadingPage = false
                                    view?.evaluateJavascript(BrowserInspectorBridge.INJECTION_SCRIPT, null)
                                    if (designModeActive) {
                                        view?.evaluateJavascript("window.__mhSetDesignMode && window.__mhSetDesignMode(true);", null)
                                    }
                                }
                            }

                            activeUrl?.let { loadUrl(it) }
                            webViewRef = this
                        }
                    },
                    update = { view ->
                        webViewRef = view
                    },
                )

                // If not in PREVIEW tab, render the overlay console/network/elements view
                if (currentTab != BrowserInspectorTab.PREVIEW) {
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        when (currentTab) {
                            BrowserInspectorTab.CONSOLE -> ConsoleTabContent(
                                logs = consoleLogs,
                                onClear = { consoleLogs.clear() },
                                onSendErrorsToAgent = {
                                    val errorLogs = consoleLogs.filter { it.level == ConsoleLogLevel.ERROR }
                                    val logsToReport = if (errorLogs.isNotEmpty()) errorLogs else consoleLogs
                                    val report = buildString {
                                        appendLine("[BROWSER CONSOLE ERROR REPORT]")
                                        appendLine("URL: ${activeUrl ?: address}")
                                        appendLine("Reported at: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
                                        appendLine("Total errors: ${errorLogs.size} | Total logs: ${consoleLogs.size}")
                                        appendLine("\nConsole Output:")
                                        logsToReport.take(15).forEach { log ->
                                            appendLine("[${log.level.label}] ${log.message}${if (!log.source.isNullOrBlank()) " (${log.source}:${log.lineNumber ?: ""})" else ""}")
                                        }
                                        appendLine("\nPlease analyze the stack traces and runtime errors above and resolve them in the code.")
                                    }
                                    onSendContextToAgent(report)
                                },
                            )
                            BrowserInspectorTab.NETWORK -> NetworkTabContent(
                                requests = networkRequests,
                                onClear = { networkRequests.clear() },
                                onSendFailedCallsToAgent = {
                                    val failed = networkRequests.filter { it.status >= 400 }
                                    val report = buildString {
                                        appendLine("[BROWSER FAILED NETWORK REQUESTS]")
                                        appendLine("URL: ${activeUrl ?: address}")
                                        appendLine("Failed requests count: ${failed.size} | Total: ${networkRequests.size}")
                                        appendLine("\nFailed Calls:")
                                        failed.take(15).forEach { req ->
                                            appendLine("- [HTTP ${req.status}] ${req.method} ${req.url} (${req.durationMs}ms)")
                                        }
                                        appendLine("\nPlease inspect these failed API/network requests, ensure backend routes or network handling is corrected.")
                                    }
                                    onSendContextToAgent(report)
                                },
                            )
                            BrowserInspectorTab.ELEMENTS -> ElementsTabContent(
                                element = selectedElement,
                                onSendToAgent = {
                                    selectedElement?.let { el ->
                                        onSendContextToAgent(el.toPromptContext())
                                    }
                                },
                            )
                            BrowserInspectorTab.PREVIEW -> Unit
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsoleTabContent(
    logs: List<ConsoleLogEntry>,
    onClear: () -> Unit,
    onSendErrorsToAgent: () -> Unit = {},
) {
    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) NeoDarkBorder else NeoBlack
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("${logs.size} log entries", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val errorCount = logs.count { it.level == ConsoleLogLevel.ERROR }
                if (errorCount > 0) {
                    Button(
                        onClick = onSendErrorsToAgent,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252), contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.5.dp, NeoBlack),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp),
                    ) {
                        Icon(Icons.Default.Warning, null, Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("INGEST $errorCount ERRORS", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.DeleteSweep, "Clear console", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        HorizontalDivider(color = borderColor)

        if (logs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No console logs captured yet", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(logs, key = { it.id }) { log ->
                    val badgeColor = when (log.level) {
                        ConsoleLogLevel.ERROR -> Color(0xFFFF5252)
                        ConsoleLogLevel.WARN -> Color(0xFFFFD600)
                        ConsoleLogLevel.INFO -> Color(0xFF00E5FF)
                        ConsoleLogLevel.LOG -> NeoLime
                    }
                    val textColor = when (log.level) {
                        ConsoleLogLevel.ERROR -> Color(0xFFFF8A80)
                        ConsoleLogLevel.WARN -> Color(0xFFFFE57F)
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(badgeColor)
                                .border(1.dp, NeoBlack, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                        ) {
                            Text(log.level.label, fontSize = 9.sp, fontWeight = FontWeight.Black, color = NeoBlack)
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = log.message,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = textColor,
                            )
                            if (log.source != null && log.source.isNotBlank()) {
                                Text(
                                    text = "${log.source}${if (log.lineNumber != null) ":${log.lineNumber}" else ""}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                        }
                        Text(
                            timeFormat.format(Date(log.timestamp)),
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace,
                        )
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
    onSendFailedCallsToAgent: () -> Unit = {},
) {
    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) NeoDarkBorder else NeoBlack

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("${requests.size} network requests", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val failedCount = requests.count { it.status >= 400 }
                if (failedCount > 0) {
                    Button(
                        onClick = onSendFailedCallsToAgent,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600), contentColor = NeoBlack),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.5.dp, NeoBlack),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp),
                    ) {
                        Icon(Icons.Default.Wifi, null, Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("INGEST $failedCount FAILED", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.DeleteSweep, "Clear network", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        HorizontalDivider(color = borderColor)

        if (requests.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No network activity captured yet", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(requests, key = { it.id }) { req ->
                    val statusColor = when {
                        req.status in 200..299 -> NeoLime
                        req.status in 300..399 -> Color(0xFF00E5FF)
                        req.status in 400..499 -> Color(0xFFFFD600)
                        else -> Color(0xFFFF5252)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(statusColor)
                                .border(1.dp, NeoBlack, RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp),
                        ) {
                            Text("${req.status}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeoBlack)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(req.method, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            req.url,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("${req.durationMs}ms", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ElementsTabContent(
    element: InspectedElement?,
    onSendToAgent: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) NeoDarkBorder else NeoBlack

    if (element == null) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Default.NearMe, null, modifier = Modifier.size(48.dp), tint = NeoLime)
            Spacer(Modifier.height(16.dp))
            Text("No Element Selected", fontWeight = FontWeight.Black, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Turn on DESIGN MODE in the top bar, then touch or click any element in the web page preview to inspect its CSS, DOM context, and send to the AI agent.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
        }
    } else {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Action Banner
            Button(
                onClick = onSendToAgent,
                colors = ButtonDefaults.buttonColors(containerColor = NeoLime, contentColor = NeoBlack),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(2.dp, NeoBlack),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Icon(Icons.Default.Send, null, Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("SEND ELEMENT TO AGENT PROMPT", fontWeight = FontWeight.Black, fontSize = 12.sp)
            }

            // Selector Info Card
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(2.dp, borderColor),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("SELECTOR", fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeoLime)
                    Text(element.displaySelector, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

                    if (element.boundingBox.isNotBlank()) {
                        Text("Bounds: ${element.boundingBox}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (element.textContent.isNotBlank()) {
                        Text("Text Content: \"${element.textContent}\"", fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            // Computed CSS Styles Card
            if (element.computedStyles.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(2.dp, borderColor),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("COMPUTED CSS STYLES", fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeoLime)
                        element.computedStyles.forEach { (prop, value) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(prop, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(value, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // HTML Snippet Card
            if (element.outerHtml.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(2.dp, borderColor),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("OUTER HTML SNIPPET", fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeoLime)
                        Text(
                            text = element.outerHtml,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                                .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                                .padding(8.dp),
                        )
                    }
                }
            }
        }
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
