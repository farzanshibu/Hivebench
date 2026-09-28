package com.hivebench.app.terminal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import com.hivebench.app.runtime.RuntimeInstaller
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Observable state of one terminal session, keyed by [TerminalSessions] key. */
data class TerminalSessionInfo(
    val key: String,
    val title: String,
    val running: Boolean,
    val exitStatus: Int? = null,
)

/**
 * Owns long-lived PTY sessions (agent TUIs, login flows) so they survive tab
 * switches and recomposition. Sessions must be created on the main thread
 * because Termux's session posts output through a main-looper Handler.
 */
object TerminalSessions {
    private const val TRANSCRIPT_ROWS = 5_000
    private const val DEFAULT_COLUMNS = 120
    private const val DEFAULT_ROWS = 40
    private const val DEFAULT_CELL_WIDTH = 12
    private const val DEFAULT_CELL_HEIGHT = 24

    /** [command] is the guest argv (after the PRoot launcher), used to tell which agent a session runs. */
    private class Entry(val session: TerminalSession, val host: Host, val command: List<String>)

    // Read from IO threads (hive idle checks) while the main thread starts/closes sessions.
    private val entries = java.util.concurrent.ConcurrentHashMap<String, Entry>()
    private val mutableInfo = MutableStateFlow<Map<String, TerminalSessionInfo>>(emptyMap())
    val info: StateFlow<Map<String, TerminalSessionInfo>> = mutableInfo.asStateFlow()

    // Sign-in flows print an OAuth URL; the terminal renderer has no tappable links.
    private val urlPattern = Regex("""https?://[^\s"'<>]+""")
    private val mutableLinks = MutableStateFlow<Map<String, String>>(emptyMap())
    /** Most recent URL visible on each session's screen. */
    val links: StateFlow<Map<String, String>> = mutableLinks.asStateFlow()

    fun get(key: String): TerminalSession? = entries[key]?.session

    /** Uptime-millis of the last screen change per session; drives idle detection. */
    private val lastOutput = java.util.concurrent.ConcurrentHashMap<String, Long>()

    fun lastOutputAt(key: String): Long? = lastOutput[key]

    /** True when [key] is running and its screen has not changed for [quietMillis]. */
    fun isQuiet(key: String, quietMillis: Long): Boolean {
        if (!isRunning(key)) return false
        val last = lastOutput[key] ?: return true
        return android.os.SystemClock.uptimeMillis() - last >= quietMillis
    }

    /** Keys of every session whose process is still alive. */
    fun runningKeys(): Set<String> = entries.filterValues { it.session.isRunning }.keys.toSet()

    /** Visible screen text of [key] (soft-wrapped lines re-joined), or null. */
    fun screenText(key: String): String? {
        val emulator = entries[key]?.session?.emulator ?: return null
        return runCatching { emulator.screen.getSelectedText(0, 0, emulator.mColumns, emulator.mRows - 1) }.getOrNull()
    }

    fun isRunning(key: String): Boolean = entries[key]?.session?.isRunning == true

    /** True while any live session is running [binary] (a guest path such as /usr/local/bin/codex). */
    fun isBinaryRunning(binary: String): Boolean =
        entries.values.any { it.session.isRunning && it.command.any { arg -> arg == binary || arg.endsWith(" $binary") || arg.startsWith("$binary ") } }

    /** Returns the live session for [key], starting [launch] when none is running. */
    fun obtain(context: Context, key: String, title: String, launch: () -> RuntimeInstaller.GuestLaunch): TerminalSession {
        entries[key]?.let { if (it.session.isRunning) return it.session }
        return start(context, key, title, launch())
    }

    /** Kills any existing session for [key] and starts a new one. */
    fun restart(context: Context, key: String, title: String, launch: RuntimeInstaller.GuestLaunch): TerminalSession {
        close(key)
        return start(context, key, title, launch)
    }

    fun close(key: String) {
        lastOutput.remove(key)
        entries.remove(key)?.session?.let { session ->
            // Termux's pid is 0 until a view first lays the session out (the PTY is created
            // lazily), and finishIfRunning() would then call kill(0, SIGKILL) — killing this
            // app's whole process group. Only signal a real child.
            if (session.pid > 0) session.finishIfRunning()
        }
        mutableInfo.update { it - key }
        mutableLinks.update { it - key }
    }

    fun write(key: String, text: String) {
        val session = entries[key]?.session ?: return
        if (!session.isRunning) return
        val bytes = text.toByteArray()
        session.write(bytes, 0, bytes.size)
    }

    /** Binds [view] as the renderer for [key]; output then redraws that view. */
    fun attach(key: String, view: TerminalView) {
        val entry = entries[key] ?: return
        // A reused view must stop receiving redraws from the session it showed before.
        entries.values.forEach { other -> if (other !== entry && other.host.view === view) other.host.view = null }
        entry.host.view = view
        view.attachSession(entry.session)
    }

    fun detach(key: String, view: TerminalView) {
        val entry = entries[key] ?: return
        if (entry.host.view === view) entry.host.view = null
    }

    private fun start(context: Context, key: String, title: String, launch: RuntimeInstaller.GuestLaunch): TerminalSession {
        val host = Host(context.applicationContext, key)
        val session = TerminalSession(
            launch.argv.first(),
            launch.cwd,
            launch.argv.toTypedArray(),
            launch.environment.map { "${it.key}=${it.value}" }.toTypedArray(),
            TRANSCRIPT_ROWS,
            host,
        )
        session.mSessionName = title
        // Termux only forks the process on the first size update, which normally comes
        // from an attached view. Size it now so background sessions (hive agents, pushes to
        // a session nobody is viewing) really run; a view resizes it once attached.
        session.updateSize(DEFAULT_COLUMNS, DEFAULT_ROWS, DEFAULT_CELL_WIDTH, DEFAULT_CELL_HEIGHT)
        entries[key] = Entry(session, host, guestCommand(launch.argv))
        mutableInfo.update { it + (key to TerminalSessionInfo(key, title, running = true)) }
        return session
    }

    /** The guest command inside a PRoot argv: everything after the `-w <dir>` working-directory option. */
    private fun guestCommand(argv: List<String>): List<String> {
        val workdir = argv.indexOf("-w")
        return if (workdir >= 0 && workdir + 2 <= argv.size) argv.drop(workdir + 2) else argv
    }

    private class Host(private val context: Context, private val key: String) : TerminalSessionClient {
        var view: TerminalView? = null

        override fun onTextChanged(changedSession: TerminalSession) {
            lastOutput[key] = android.os.SystemClock.uptimeMillis()
            view?.onScreenUpdated()
            val emulator = changedSession.emulator ?: return
            // Visible screen only; getSelectedText re-joins soft-wrapped lines so long URLs survive.
            val screen = emulator.screen.getSelectedText(0, 0, emulator.mColumns, emulator.mRows - 1)
            val url = urlPattern.findAll(screen).lastOrNull()?.value?.trimEnd('.', ',', ')', ']')
            if (mutableLinks.value[key] != url) {
                mutableLinks.update { if (url == null) it - key else it + (key to url) }
            }
        }

        override fun onTitleChanged(changedSession: TerminalSession) = Unit

        override fun onSessionFinished(finishedSession: TerminalSession) {
            view?.onScreenUpdated()
            mutableInfo.update { current ->
                val existing = current[key] ?: return@update current
                current + (key to existing.copy(running = false, exitStatus = finishedSession.exitStatus))
            }
        }

        override fun onCopyTextToClipboard(session: TerminalSession, text: String) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Terminal", text))
        }

        override fun onPasteTextFromClipboard(session: TerminalSession?) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString() ?: return
            session?.emulator?.paste(text)
        }

        override fun onBell(session: TerminalSession) = Unit
        override fun onColorsChanged(session: TerminalSession) { view?.invalidate() }
        override fun onTerminalCursorStateChange(state: Boolean) = Unit
        override fun getTerminalCursorStyle(): Int? = null
        override fun logError(tag: String?, message: String?) { Log.e(tag ?: "Terminal", message.orEmpty()) }
        override fun logWarn(tag: String?, message: String?) { Log.w(tag ?: "Terminal", message.orEmpty()) }
        override fun logInfo(tag: String?, message: String?) = Unit
        override fun logDebug(tag: String?, message: String?) = Unit
        override fun logVerbose(tag: String?, message: String?) = Unit
        override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) {
            Log.e(tag ?: "Terminal", message.orEmpty(), e)
        }
        override fun logStackTrace(tag: String?, e: Exception?) { Log.e(tag ?: "Terminal", "", e) }
    }
}
