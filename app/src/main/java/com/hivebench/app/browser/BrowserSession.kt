package com.hivebench.app.browser

import android.content.Context
import android.content.MutableContextWrapper
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal enum class BrowserTool { NONE, INSPECT, DRAW }

private const val MAX_ENTRIES = 500

/**
 * Everything the browser tab shows for one project. It outlives the tab's
 * composition, so switching workspace tabs keeps the page, logs, selection and
 * drawing instead of reloading.
 */
class BrowserSession internal constructor(val projectKey: String) {
    val consoleLogs = mutableStateListOf<ConsoleLogEntry>()
    val networkRequests = mutableStateListOf<NetworkRequestEntry>()
    val strokes = mutableStateListOf<DrawStroke>()
    var selectedElement by mutableStateOf<InspectedElement?>(null)
    var address by mutableStateOf("")
    var activeUrl by mutableStateOf<String?>(null)
    var isLoading by mutableStateOf(false)
    var canGoBack by mutableStateOf(false)
    internal var tool by mutableStateOf(BrowserTool.NONE)
    var currentTab by mutableStateOf(BrowserInspectorTab.PREVIEW)

    /** Last server URL loaded automatically, so a new detection loads once without fighting manual navigation. */
    internal var autoLoadedUrl: String? = null

    private var contextWrapper: MutableContextWrapper? = null
    internal var webView: WebView? = null
        private set

    fun addConsole(entry: ConsoleLogEntry) {
        consoleLogs.add(0, entry)
        if (consoleLogs.size > MAX_ENTRIES) consoleLogs.removeAt(consoleLogs.lastIndex)
    }

    fun addNetwork(entry: NetworkRequestEntry) {
        networkRequests.add(0, entry)
        if (networkRequests.size > MAX_ENTRIES) networkRequests.removeAt(networkRequests.lastIndex)
    }

    fun clearPageData() {
        consoleLogs.clear()
        networkRequests.clear()
        selectedElement = null
    }

    /**
     * Returns this session's WebView, creating it with [create] the first time.
     * The view is detached from any previous parent and rebound to [context] so
     * dialogs and IME use the current Activity.
     */
    internal fun obtainWebView(context: Context, create: (Context) -> WebView): WebView {
        val wrapper = contextWrapper ?: MutableContextWrapper(context).also { contextWrapper = it }
        wrapper.baseContext = context
        val view = webView ?: create(wrapper).also { webView = it }
        (view.parent as? ViewGroup)?.removeView(view)
        return view
    }

    internal fun destroy() {
        webView?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
            view.stopLoading()
            view.destroy()
        }
        webView = null
        contextWrapper = null
    }
}

/** Keeps the browser session of the open project; opening another project replaces it. */
object BrowserSessions {
    private var current: BrowserSession? = null

    fun obtain(projectKey: String): BrowserSession {
        current?.let { if (it.projectKey == projectKey) return it else it.destroy() }
        return BrowserSession(projectKey).also { current = it }
    }
}
