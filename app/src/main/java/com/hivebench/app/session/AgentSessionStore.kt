package com.hivebench.app.session

import com.hivebench.app.model.AgentKind
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * One interactive agent TUI session inside a project. Several can run in
 * parallel; each owns its own PTY in [com.hivebench.app.terminal.TerminalSessions]
 * under [terminalKey].
 */
data class AgentSessionEntry(
    val id: String = UUID.randomUUID().toString().take(8),
    val projectId: String,
    val kind: AgentKind,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    /** Claude Code `--session-id`, reused with `--resume` so a restart keeps the conversation. */
    val resumeId: String = UUID.randomUUID().toString(),
    /** True once the TUI has been launched at least once (so later launches resume). */
    val launched: Boolean = false,
) {
    val terminalKey: String get() = "agent:$projectId:$id"
}

/** Persists the per-project list of agent sessions under `filesDir/agent-sessions/`. */
class AgentSessionStore(filesDir: File) {
    private val dir = File(filesDir, "agent-sessions").apply { mkdirs() }

    private fun file(projectId: String) = File(dir, "$projectId.json")

    fun load(projectId: String): List<AgentSessionEntry> {
        val f = file(projectId)
        if (!f.isFile) return emptyList()
        return runCatching {
            val array = JSONArray(f.readText())
            (0 until array.length()).mapNotNull { i ->
                val o = array.getJSONObject(i)
                val kind = AgentKind.entries.firstOrNull { it.stableId == o.optString("kind") } ?: return@mapNotNull null
                AgentSessionEntry(
                    id = o.getString("id"),
                    projectId = projectId,
                    kind = kind,
                    title = o.optString("title", kind.title),
                    createdAt = o.optLong("createdAt"),
                    resumeId = o.optString("resumeId").ifBlank { UUID.randomUUID().toString() },
                    launched = o.optBoolean("launched"),
                )
            }
        }.getOrDefault(emptyList())
    }

    fun save(projectId: String, sessions: List<AgentSessionEntry>) {
        val array = JSONArray()
        sessions.forEach { s ->
            array.put(
                JSONObject()
                    .put("id", s.id)
                    .put("kind", s.kind.stableId)
                    .put("title", s.title)
                    .put("createdAt", s.createdAt)
                    .put("resumeId", s.resumeId)
                    .put("launched", s.launched),
            )
        }
        val target = file(projectId)
        val tmp = File(dir, "$projectId.json.tmp")
        tmp.writeText(array.toString(2))
        if (!tmp.renameTo(target)) {
            target.writeText(array.toString(2))
            tmp.delete()
        }
    }

    fun delete(projectId: String) {
        file(projectId).delete()
    }
}
