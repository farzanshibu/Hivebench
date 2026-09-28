package com.hivebench.app.browser

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import org.json.JSONObject

/**
 * JavaScript bridge between the inspected page and the inspector UI. Page
 * callbacks arrive on a WebView binder thread; they are re-posted to the main
 * thread before touching Compose state.
 */
class BrowserInspectorBridge(
    private val onConsoleMessage: (ConsoleLogEntry) -> Unit,
    private val onNetworkEvent: (NetworkRequestEntry) -> Unit,
    private val onElementSelected: (InspectedElement) -> Unit,
) {
    private val main = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun postConsole(level: String, message: String, source: String?, line: Int) {
        val entry = ConsoleLogEntry(
            level = runCatching { ConsoleLogLevel.valueOf(level.uppercase()) }.getOrDefault(ConsoleLogLevel.LOG),
            message = message,
            source = source?.takeIf { it.isNotBlank() },
            lineNumber = line.takeIf { it > 0 },
        )
        main.post { onConsoleMessage(entry) }
    }

    @JavascriptInterface
    fun postNetwork(json: String) {
        val entry = runCatching { NetworkRequestEntry.fromJson(JSONObject(json)) }.getOrNull() ?: return
        main.post { onNetworkEvent(entry) }
    }

    @JavascriptInterface
    fun postElement(json: String) {
        val element = runCatching { InspectedElement.fromJson(JSONObject(json)) }.getOrNull() ?: return
        main.post { onElementSelected(element) }
    }

    companion object {
        const val INTERFACE_NAME = "AndroidInspector"

        fun setInspectModeScript(enabled: Boolean) = "window.__mhSetInspect && window.__mhSetInspect($enabled);"
        const val SELECT_PARENT_SCRIPT = "window.__mhSelectRelative && window.__mhSelectRelative('parent');"
        const val SELECT_CHILD_SCRIPT = "window.__mhSelectRelative && window.__mhSelectRelative('child');"
        const val CLEAR_SELECTION_SCRIPT = "window.__mhClearSelection && window.__mhClearSelection();"

        /**
         * Injected at document start when the WebView supports it (so early
         * requests are seen), otherwise on page finish. Console output is taken
         * from WebChromeClient.onConsoleMessage instead, which also sees logs
         * printed before this script runs.
         */
        val INJECTION_SCRIPT = """
            (function() {
              if (window.__mhInspectorInjected) return;
              window.__mhInspectorInjected = true;
              var bridge = function() { return window.AndroidInspector; };
              var MAX_BODY = 4096;
              function post(kind, payload) {
                try { var b = bridge(); if (b) b[kind](JSON.stringify(payload)); } catch (e) {}
              }
              // Streaming (SSE) and binary bodies are never read: cloning them would hold the
              // request open and buffer the whole stream just to log it.
              function isTextual(type) {
                if (!type || /event-stream|octet-stream|multipart/i.test(type)) return false;
                return /json|text|xml|javascript|graphql|form-urlencoded/i.test(type);
              }
              function clip(s) { s = s == null ? '' : String(s); return s.length > MAX_BODY ? s.substring(0, MAX_BODY) + '…' : s; }
              function bodyToText(body) {
                if (body == null) return '';
                if (typeof body === 'string') return clip(body);
                if (body instanceof URLSearchParams) return clip(body.toString());
                if (typeof FormData !== 'undefined' && body instanceof FormData) {
                  var parts = []; body.forEach(function(v, k) { parts.push(k + '=' + (typeof v === 'string' ? v : '[file]')); });
                  return clip(parts.join('&'));
                }
                return '[' + (body.constructor ? body.constructor.name : 'binary') + ' body]';
              }
              function headersToObject(h) {
                var out = {};
                try {
                  if (!h) return out;
                  if (typeof h.forEach === 'function') { h.forEach(function(v, k) { out[k] = v; }); return out; }
                  if (Array.isArray(h)) { h.forEach(function(p) { out[p[0]] = p[1]; }); return out; }
                  Object.keys(h).forEach(function(k) { out[k] = h[k]; });
                } catch (e) {}
                return out;
              }

              // ---- Errors not printed through console ----
              window.addEventListener('unhandledrejection', function(e) {
                try {
                  var r = e.reason; var msg = r && (r.stack || r.message) ? (r.stack || r.message) : String(r);
                  var b = bridge(); if (b) b.postConsole('ERROR', 'Unhandled promise rejection: ' + msg, '', 0);
                } catch (err) {}
              });

              // ---- fetch ----
              var origFetch = window.fetch;
              if (origFetch) {
                window.fetch = function(input, init) {
                  var start = performance.now();
                  var url = typeof input === 'string' ? input : (input && input.url) || String(input);
                  var method = ((init && init.method) || (input && input.method) || 'GET').toUpperCase();
                  var reqHeaders = headersToObject((init && init.headers) || (input && input.headers));
                  var reqBody = bodyToText(init && init.body);
                  var reqBodyPromise = (!reqBody && input && typeof input.clone === 'function' && method !== 'GET' && method !== 'HEAD')
                    ? input.clone().text().then(clip, function() { return ''; }) : Promise.resolve(reqBody);
                  return origFetch.apply(this, arguments).then(function(res) {
                    var entry = { method: method, url: res.url || url, status: res.status, statusText: res.statusText,
                      type: 'fetch', duration: Math.round(performance.now() - start), requestHeaders: reqHeaders,
                      requestBody: reqBody, responseHeaders: headersToObject(res.headers) };
                    var len = res.headers.get('content-length'); entry.size = len ? parseInt(len, 10) : -1;
                    var send = function() { reqBodyPromise.then(function(b) { entry.requestBody = b; post('postNetwork', entry); }); };
                    if (isTextual(res.headers.get('content-type'))) {
                      res.clone().text().then(function(t) { entry.responseBody = clip(t); if (entry.size < 0) entry.size = t.length; send(); }, send);
                    } else { send(); }
                    return res;
                  }, function(err) {
                    post('postNetwork', { method: method, url: url, status: 0, type: 'fetch', duration: Math.round(performance.now() - start),
                      requestHeaders: reqHeaders, requestBody: reqBody, error: String(err && err.message || err) });
                    throw err;
                  });
                };
              }

              // ---- XMLHttpRequest ----
              var XHR = window.XMLHttpRequest;
              if (XHR) {
                var open = XHR.prototype.open, send = XHR.prototype.send, setHeader = XHR.prototype.setRequestHeader;
                XHR.prototype.open = function(method, url) { this.__mh = { method: String(method || 'GET').toUpperCase(), url: String(url), headers: {} }; return open.apply(this, arguments); };
                XHR.prototype.setRequestHeader = function(k, v) { if (this.__mh) this.__mh.headers[k] = v; return setHeader.apply(this, arguments); };
                XHR.prototype.send = function(body) {
                  var xhr = this, meta = xhr.__mh || { method: 'GET', url: '', headers: {} }, start = performance.now();
                  xhr.addEventListener('loadend', function() {
                    var resHeaders = {};
                    (xhr.getAllResponseHeaders() || '').trim().split(/[\r\n]+/).forEach(function(line) {
                      var i = line.indexOf(':'); if (i > 0) resHeaders[line.substring(0, i).trim()] = line.substring(i + 1).trim();
                    });
                    var text = ''; try { if (xhr.responseType === '' || xhr.responseType === 'text') text = clip(xhr.responseText); else if (xhr.responseType === 'json') text = clip(JSON.stringify(xhr.response)); } catch (e) {}
                    post('postNetwork', { method: meta.method, url: xhr.responseURL || meta.url, status: xhr.status, statusText: xhr.statusText,
                      type: 'xhr', duration: Math.round(performance.now() - start), requestHeaders: meta.headers, requestBody: bodyToText(body),
                      responseHeaders: resHeaders, responseBody: text, size: text.length || -1, error: xhr.status === 0 ? 'network error or CORS' : '' });
                  });
                  return send.apply(this, arguments);
                };
              }

              // ---- Everything else the page loads (scripts, images, css, fonts) ----
              try {
                var seen = {};
                new PerformanceObserver(function(list) {
                  list.getEntries().forEach(function(e) {
                    if (e.initiatorType === 'fetch' || e.initiatorType === 'xmlhttprequest') return;
                    var key = e.name + '@' + Math.round(e.startTime); if (seen[key]) return; seen[key] = 1;
                    post('postNetwork', { method: 'GET', url: e.name, status: e.responseStatus || -1, type: e.initiatorType || 'resource',
                      duration: Math.round(e.duration), size: e.transferSize || e.encodedBodySize || -1 });
                  });
                }).observe({ type: 'resource', buffered: true });
              } catch (e) {}
              // The page document itself (status + timing) — not visible to fetch/XHR hooks.
              try {
                new PerformanceObserver(function(list) {
                  list.getEntries().forEach(function(e) {
                    post('postNetwork', { method: 'GET', url: e.name, status: e.responseStatus || -1, type: 'document',
                      duration: Math.round(e.duration), size: e.transferSize || e.encodedBodySize || -1 });
                  });
                }).observe({ type: 'navigation', buffered: true });
              } catch (e) {}

              // ---- Element inspector ----
              var inspect = false, selected = null, box = null, label = null;
              function ensureOverlay() {
                if (box) return;
                box = document.createElement('div'); box.id = '__mh_inspect_box';
                box.style.cssText = 'position:fixed;pointer-events:none;z-index:2147483646;border:2px solid #B8F200;background:rgba(184,242,0,.18);display:none;box-sizing:border-box;';
                label = document.createElement('div'); label.id = '__mh_inspect_label';
                label.style.cssText = 'position:fixed;pointer-events:none;z-index:2147483647;background:#B8F200;color:#000;font:600 11px/1.3 monospace;padding:2px 6px;border-radius:4px;display:none;max-width:90vw;overflow:hidden;white-space:nowrap;text-overflow:ellipsis;';
                (document.body || document.documentElement).appendChild(box);
                (document.body || document.documentElement).appendChild(label);
              }
              function short(el) {
                var s = el.tagName.toLowerCase();
                if (el.id) s += '#' + el.id;
                var cls = (typeof el.className === 'string' ? el.className : '').trim().split(/\s+/).filter(Boolean).slice(0, 2);
                if (cls.length) s += '.' + cls.join('.');
                return s;
              }
              function cssPath(el) {
                if (el.id && document.querySelectorAll('#' + CSS.escape(el.id)).length === 1) return '#' + CSS.escape(el.id);
                var parts = [];
                while (el && el.nodeType === 1 && el !== document.documentElement) {
                  var part = el.tagName.toLowerCase();
                  if (el.id && document.querySelectorAll('#' + CSS.escape(el.id)).length === 1) { parts.unshift('#' + CSS.escape(el.id)); break; }
                  var testAttr = el.hasAttribute('data-testid') ? 'data-testid' : (el.hasAttribute('data-test') ? 'data-test' : '');
                  var testId = testAttr ? el.getAttribute(testAttr) : '';
                  if (testId) { part += '[' + testAttr + '="' + testId.replace(/\\/g, '\\\\').replace(/"/g, '\\"') + '"]'; }
                  else {
                    var parent = el.parentElement;
                    if (parent) {
                      var same = Array.prototype.filter.call(parent.children, function(c) { return c.tagName === el.tagName; });
                      if (same.length > 1) part += ':nth-of-type(' + (same.indexOf(el) + 1) + ')';
                    }
                  }
                  parts.unshift(part); el = el.parentElement;
                }
                return parts.join(' > ');
              }
              function xpath(el) {
                var parts = [];
                for (; el && el.nodeType === 1; el = el.parentNode) {
                  var i = 1; for (var s = el.previousElementSibling; s; s = s.previousElementSibling) if (s.tagName === el.tagName) i++;
                  parts.unshift(el.tagName.toLowerCase() + '[' + i + ']');
                }
                return '/' + parts.join('/');
              }
              function componentInfo(el) {
                var info = { name: '', source: '' };
                try {
                  var key = Object.keys(el).find(function(k) { return k.indexOf('__reactFiber$') === 0 || k.indexOf('__reactInternalInstance$') === 0; });
                  if (key) {
                    for (var f = el[key]; f; f = f.return) {
                      var t = f.type;
                      if (t && typeof t !== 'string') {
                        if (!info.name) info.name = t.displayName || t.name || '';
                        var src = f._debugSource;
                        if (src && !info.source) info.source = src.fileName + ':' + src.lineNumber;
                        if (info.name && info.source) break;
                      }
                    }
                    return info;
                  }
                  for (var n = el; n; n = n.parentElement) {
                    var v3 = n.__vueParentComponent;
                    if (v3 && v3.type) { info.name = v3.type.name || v3.type.__name || ''; info.source = v3.type.__file || ''; return info; }
                    if (n.__vue__) { var o = n.__vue__.${'$'}options || {}; info.name = o.name || ''; info.source = o.__file || ''; return info; }
                    if (n.__svelte_meta && n.__svelte_meta.loc) { info.source = n.__svelte_meta.loc.file + ':' + n.__svelte_meta.loc.line; return info; }
                  }
                } catch (e) {}
                return info;
              }
              var STYLE_KEYS = ['display','position','top','left','width','height','margin','padding','border','border-radius',
                'color','background-color','background-image','font-family','font-size','font-weight','line-height','text-align',
                'flex-direction','justify-content','align-items','gap','grid-template-columns','overflow','opacity','z-index','box-shadow','transform'];
              function describe(el) {
                var r = el.getBoundingClientRect(), cs = getComputedStyle(el), styles = {}, attrs = {}, anc = [];
                STYLE_KEYS.forEach(function(k) { var v = cs.getPropertyValue(k); if (v && v !== 'none' && v !== 'normal' && v !== 'auto' && v !== '0px' && v !== 'rgba(0, 0, 0, 0)') styles[k] = v; });
                Array.prototype.forEach.call(el.attributes, function(a) { attrs[a.name] = a.value.length > 200 ? a.value.substring(0, 200) + '…' : a.value; });
                for (var p = el.parentElement; p && anc.length < 6; p = p.parentElement) anc.unshift(short(p));
                var comp = componentInfo(el);
                return {
                  pageUrl: location.href, tagName: el.tagName.toLowerCase(), id: el.id || '',
                  className: typeof el.className === 'string' ? el.className : '', selector: cssPath(el), xpath: xpath(el),
                  text: (el.innerText || el.textContent || '').trim().replace(/\s+/g, ' ').substring(0, 300),
                  attributes: attrs, styles: styles,
                  rect: [Math.round(r.left), Math.round(r.top), Math.round(r.width), Math.round(r.height)],
                  box: { margin: cs.margin, padding: cs.padding, border: cs.borderWidth },
                  outerHtml: (el.outerHTML || '').substring(0, 3000), ancestors: anc, childCount: el.children.length,
                  component: comp.name, source: comp.source, viewport: innerWidth + 'x' + innerHeight
                };
              }
              function highlight(el) {
                ensureOverlay();
                var r = el.getBoundingClientRect();
                box.style.left = r.left + 'px'; box.style.top = r.top + 'px'; box.style.width = r.width + 'px'; box.style.height = r.height + 'px';
                box.style.display = 'block';
                label.textContent = short(el) + '  ' + Math.round(r.width) + '×' + Math.round(r.height);
                label.style.left = Math.max(0, r.left) + 'px';
                label.style.top = (r.top > 22 ? r.top - 20 : r.bottom + 2) + 'px';
                label.style.display = 'block';
              }
              function select(el) {
                if (!el || el === box || el === label) return;
                selected = el; highlight(el); post('postElement', describe(el));
              }
              window.__mhSetInspect = function(on) {
                inspect = !!on;
                if (!inspect && box) { box.style.display = 'none'; label.style.display = 'none'; }
                else if (inspect && selected) highlight(selected);
              };
              window.__mhClearSelection = function() { selected = null; if (box) { box.style.display = 'none'; label.style.display = 'none'; } };
              window.__mhSelectRelative = function(which) {
                if (!selected) return;
                var next = which === 'parent' ? selected.parentElement : selected.firstElementChild;
                while (next && (next === box || next === label)) next = next.nextElementSibling;
                if (next && next !== document.documentElement) select(next);
              };
              window.addEventListener('scroll', function() { if (inspect && selected) highlight(selected); }, true);
              window.addEventListener('resize', function() { if (inspect && selected) highlight(selected); });
              // Only the click is cancelled: preventing touchstart/pointerdown would stop the
              // browser from synthesising the click (and from scrolling). Earlier events just
              // don't reach the page's own handlers while inspecting.
              ['pointerdown', 'mousedown', 'touchstart'].forEach(function(type) {
                document.addEventListener(type, function(e) { if (inspect) e.stopPropagation(); }, { capture: true, passive: true });
              });
              document.addEventListener('click', function(e) {
                if (!inspect) return;
                e.preventDefault(); e.stopPropagation();
                select(document.elementFromPoint(e.clientX, e.clientY) || e.target);
              }, true);
            })();
        """.trimIndent()
    }
}
