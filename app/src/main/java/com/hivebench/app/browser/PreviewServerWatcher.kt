package com.hivebench.app.browser

import com.hivebench.app.terminal.TerminalSessions
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Finds the project's dev server wherever it was started: a URL printed in any
 * terminal or agent session, else a well-known dev-server port. A candidate
 * only counts while something is actually listening on it (PRoot shares the
 * device's network, so guest servers are reachable on 127.0.0.1).
 */
object PreviewServerWatcher {
    private val urlPattern = Regex("""https?://(?:localhost|127\.0\.0\.1|0\.0\.0\.0|\[::1?]):(\d{2,5})(/[^\s"'<>)]*)?""")

    /** Vite, Next/CRA/Express, Angular, Astro, Flask, Django, Rails, Expo web, http.server, … */
    private val commonPorts = listOf(5173, 3000, 5174, 4200, 4321, 8080, 8000, 5000, 3001, 4173, 8081, 8888, 1234, 9000)

    /** URLs seen in terminal output, most recent last; kept so a scrolled-away URL still counts. */
    private val seen = LinkedHashMap<Int, String>()

    fun reset() = synchronized(seen) { seen.clear() }

    /** Blocking; call off the main thread. Returns the preview URL to show, or null when no server is up. */
    fun detect(extraText: String = ""): String? {
        val texts = TerminalSessions.runningKeys().mapNotNull { TerminalSessions.screenText(it) } + extraText
        synchronized(seen) {
            texts.forEach { text ->
                urlPattern.findAll(text).forEach { match ->
                    val port = match.groupValues[1].toIntOrNull()?.takeIf { it in 1..65535 } ?: return@forEach
                    val path = match.groupValues[2].ifBlank { "/" }
                    seen.remove(port)
                    seen[port] = "http://127.0.0.1:$port$path"
                }
            }
            seen.entries.reversed().firstOrNull { isListening(it.key) }?.let { return it.value }
        }
        return commonPorts.firstOrNull(::isListening)?.let { "http://127.0.0.1:$it/" }
    }

    fun isListening(port: Int): Boolean = runCatching {
        Socket().use { it.connect(InetSocketAddress("127.0.0.1", port), 250) }
        true
    }.getOrDefault(false)

    fun portOf(url: String?): Int? = url?.let { Regex(""":(\d{2,5})""").find(it)?.groupValues?.get(1)?.toIntOrNull() }
}
