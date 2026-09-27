package com.jarves.mh.browser

import android.webkit.JavascriptInterface
import org.json.JSONObject

/**
 * JavaScript interface bridge between Android WebView and the Browser Inspection Console.
 */
class BrowserInspectorBridge(
    private val onConsoleMessage: (ConsoleLogEntry) -> Unit,
    private val onNetworkEvent: (NetworkRequestEntry) -> Unit,
    private val onElementSelected: (InspectedElement) -> Unit,
) {

    @JavascriptInterface
    fun postConsole(level: String, message: String, source: String?, line: Int) {
        val lvl = try {
            ConsoleLogLevel.valueOf(level.uppercase())
        } catch (_: Exception) {
            ConsoleLogLevel.LOG
        }
        val entry = ConsoleLogEntry(
            level = lvl,
            message = message,
            source = source,
            lineNumber = if (line > 0) line else null,
            timestamp = System.currentTimeMillis(),
        )
        onConsoleMessage(entry)
    }

    @JavascriptInterface
    fun postNetwork(method: String, url: String, status: Int, type: String, duration: Long) {
        val entry = NetworkRequestEntry(
            method = method.uppercase(),
            url = url,
            status = status,
            type = type,
            durationMs = duration,
            timestamp = System.currentTimeMillis(),
        )
        onNetworkEvent(entry)
    }

    @JavascriptInterface
    fun postElement(
        tagName: String,
        id: String,
        className: String,
        textContent: String,
        xpath: String,
        boundingBox: String,
        outerHtml: String,
        stylesJson: String,
    ) {
        val styles = mutableMapOf<String, String>()
        try {
            val json = JSONObject(stylesJson)
            json.keys().forEach { k -> styles[k] = json.optString(k) }
        } catch (_: Exception) {
            // Ignore parse errors
        }

        val element = InspectedElement(
            tagName = tagName,
            id = id,
            className = className,
            textContent = textContent,
            xpath = xpath,
            computedStyles = styles,
            boundingBox = boundingBox,
            outerHtml = outerHtml,
        )
        onElementSelected(element)
    }

    companion object {
        const val INTERFACE_NAME = "AndroidInspector"

        val INJECTION_SCRIPT = """
            (function() {
                if (window.__mhInspectorInjected) return;
                window.__mhInspectorInjected = true;

                // 1. Console Interception
                const origLog = console.log;
                const origWarn = console.warn;
                const origError = console.error;
                const origInfo = console.info;

                console.log = function(...args) {
                    origLog.apply(console, args);
                    try { window.AndroidInspector && window.AndroidInspector.postConsole('LOG', args.map(a => typeof a === 'object' ? JSON.stringify(a) : String(a)).join(' '), '', 0); } catch(e){}
                };
                console.warn = function(...args) {
                    origWarn.apply(console, args);
                    try { window.AndroidInspector && window.AndroidInspector.postConsole('WARN', args.map(a => typeof a === 'object' ? JSON.stringify(a) : String(a)).join(' '), '', 0); } catch(e){}
                };
                console.error = function(...args) {
                    origError.apply(console, args);
                    try { window.AndroidInspector && window.AndroidInspector.postConsole('ERROR', args.map(a => typeof a === 'object' ? JSON.stringify(a) : String(a)).join(' '), '', 0); } catch(e){}
                };
                console.info = function(...args) {
                    origInfo.apply(console, args);
                    try { window.AndroidInspector && window.AndroidInspector.postConsole('INFO', args.map(a => typeof a === 'object' ? JSON.stringify(a) : String(a)).join(' '), '', 0); } catch(e){}
                };

                window.addEventListener('error', function(e) {
                    try { window.AndroidInspector && window.AndroidInspector.postConsole('ERROR', e.message || 'Script error', e.filename || '', e.lineno || 0); } catch(err){}
                });

                // 2. Network Interception (fetch + xhr)
                const origFetch = window.fetch;
                if (origFetch) {
                    window.fetch = async function(...args) {
                        const start = Date.now();
                        const url = typeof args[0] === 'string' ? args[0] : (args[0] && args[0].url ? args[0].url : 'unknown');
                        const method = (args[1] && args[1].method) || 'GET';
                        try {
                            const res = await origFetch.apply(this, args);
                            const dur = Date.now() - start;
                            try { window.AndroidInspector && window.AndroidInspector.postNetwork(method, url, res.status, 'fetch', dur); } catch(e){}
                            return res;
                        } catch(err) {
                            const dur = Date.now() - start;
                            try { window.AndroidInspector && window.AndroidInspector.postNetwork(method, url, 0, 'fetch', dur); } catch(e){}
                            throw err;
                        }
                    };
                }

                // 3. Design Mode / Element Inspection
                let highlightEl = null;
                function ensureHighlightOverlay() {
                    if (!highlightEl) {
                        highlightEl = document.createElement('div');
                        highlightEl.id = '__mh_design_highlight';
                        highlightEl.style.position = 'absolute';
                        highlightEl.style.border = '2px solid #B4F000';
                        highlightEl.style.backgroundColor = 'rgba(180, 240, 0, 0.25)';
                        highlightEl.style.pointerEvents = 'none';
                        highlightEl.style.zIndex = '999999';
                        highlightEl.style.display = 'none';
                        highlightEl.style.transition = 'all 0.08s ease-out';
                        document.body.appendChild(highlightEl);
                    }
                }

                window.__mhSetDesignMode = function(enabled) {
                    window.__mhDesignModeActive = enabled;
                    ensureHighlightOverlay();
                    if (!enabled && highlightEl) {
                        highlightEl.style.display = 'none';
                    }
                };

                document.addEventListener('click', function(e) {
                    if (!window.__mhDesignModeActive) return;
                    e.preventDefault();
                    e.stopPropagation();

                    const el = e.target;
                    if (!el || el === highlightEl) return;

                    ensureHighlightOverlay();
                    const rect = el.getBoundingClientRect();
                    highlightEl.style.left = (rect.left + window.scrollX) + 'px';
                    highlightEl.style.top = (rect.top + window.scrollY) + 'px';
                    highlightEl.style.width = rect.width + 'px';
                    highlightEl.style.height = rect.height + 'px';
                    highlightEl.style.display = 'block';

                    const cs = window.getComputedStyle(el);
                    const styles = {
                        display: cs.display,
                        position: cs.position,
                        width: cs.width,
                        height: cs.height,
                        color: cs.color,
                        backgroundColor: cs.backgroundColor,
                        fontSize: cs.fontSize,
                        fontFamily: cs.fontFamily,
                        padding: cs.padding,
                        margin: cs.margin,
                        border: cs.border,
                        borderRadius: cs.borderRadius
                    };

                    const bounds = Math.round(rect.width) + 'x' + Math.round(rect.height) + ' at (' + Math.round(rect.left) + ',' + Math.round(rect.top) + ')';
                    const outer = el.outerHTML ? el.outerHTML.substring(0, 1500) : '';

                    try {
                        window.AndroidInspector && window.AndroidInspector.postElement(
                            el.tagName,
                            el.id || '',
                            el.className || '',
                            (el.textContent || '').trim().substring(0, 200),
                            '',
                            bounds,
                            outer,
                            JSON.stringify(styles)
                        );
                    } catch(err){}
                }, true);
            })();
        """.trimIndent()
    }
}
