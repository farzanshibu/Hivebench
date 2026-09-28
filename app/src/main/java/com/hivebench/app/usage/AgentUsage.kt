package com.hivebench.app.usage

import com.hivebench.app.model.AgentKind
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One plan rate-limit window as the agent last reported it (e.g. Codex's 5 h and weekly limits). */
data class RateLimitWindow(val label: String, val usedPercent: Double, val resetsAtMillis: Long?)

/** Token usage read from an agent's own session logs. */
data class AgentUsage(
    val inputTokens: Long = 0,
    val outputTokens: Long = 0,
    /** Prompt tokens served from the provider's cache. */
    val cachedTokens: Long = 0,
    /** Tokens written to the provider's cache (Claude only). */
    val cacheWriteTokens: Long = 0,
    val reasoningTokens: Long = 0,
    val model: String? = null,
    /** Tokens in the most recent request's context. */
    val contextTokens: Long = 0,
    val contextWindow: Long? = null,
    val rateLimits: List<RateLimitWindow> = emptyList(),
    val updatedAt: Long = 0,
) {
    val totalTokens: Long get() = inputTokens + outputTokens + cachedTokens + cacheWriteTokens + reasoningTokens

    operator fun plus(other: AgentUsage) = AgentUsage(
        inputTokens = inputTokens + other.inputTokens,
        outputTokens = outputTokens + other.outputTokens,
        cachedTokens = cachedTokens + other.cachedTokens,
        cacheWriteTokens = cacheWriteTokens + other.cacheWriteTokens,
        reasoningTokens = reasoningTokens + other.reasoningTokens,
        model = if (other.updatedAt >= updatedAt) other.model ?: model else model ?: other.model,
        contextTokens = if (other.updatedAt >= updatedAt) other.contextTokens else contextTokens,
        contextWindow = if (other.updatedAt >= updatedAt) other.contextWindow ?: contextWindow else contextWindow,
        rateLimits = if (other.updatedAt >= updatedAt && other.rateLimits.isNotEmpty()) other.rateLimits else rateLimits,
        updatedAt = maxOf(updatedAt, other.updatedAt),
    )
}

/**
 * Parses one agent's log format. [consume] sees each new complete line once and
 * returns the usage that line adds; per-file state (dedupe ids, cumulative
 * counters) lives in [FileState].
 */
private interface UsageFormat {
    fun consume(line: String, state: FileState): AgentUsage?
}

private class FileState {
    val seen = HashSet<String>()
    var model: String? = null
    var contextWindow: Long? = null
    /** Last cumulative total reported by formats that log running totals. */
    var lastCumulative: AgentUsage = AgentUsage()
}

/**
 * Reads only the bytes appended since the previous call, so polling several
 * large transcripts every few seconds stays cheap.
 */
private class LogCursor(val file: File, val format: UsageFormat) {
    private var offset = 0L
    private val state = FileState()
    /** Usage per local day, so "today" survives a session that spans midnight. */
    val byDay = HashMap<LocalDate, AgentUsage>()
    var total = AgentUsage()

    fun poll() {
        val length = file.length()
        if (length < offset) {
            offset = 0L
            byDay.clear()
            total = AgentUsage()
        }
        if (length == offset) return
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(offset)
            val bytes = ByteArray((length - offset).coerceAtMost(8_000_000L).toInt())
            raf.readFully(bytes)
            val lastNewline = bytes.lastIndexOf('\n'.code.toByte())
            if (lastNewline < 0) return
            offset += lastNewline + 1
            String(bytes, 0, lastNewline, Charsets.UTF_8).lineSequence().forEach { line ->
                if (line.isBlank()) return@forEach
                val usage = runCatching { format.consume(line, state) }.getOrNull() ?: return@forEach
                total += usage
                val day = Instant.ofEpochMilli(usage.updatedAt.takeIf { it > 0 } ?: file.lastModified())
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                byDay[day] = (byDay[day] ?: AgentUsage()) + usage
            }
        }
    }
}

private fun timestamp(value: String?): Long =
    value?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() } ?: System.currentTimeMillis()

/** Claude Code transcripts: one assistant line per content block, deduped by message id. */
private object ClaudeFormat : UsageFormat {
    override fun consume(line: String, state: FileState): AgentUsage? {
        if (!line.contains("\"usage\"")) return null
        val o = JSONObject(line)
        if (o.optString("type") != "assistant") return null
        val message = o.optJSONObject("message") ?: return null
        val usage = message.optJSONObject("usage") ?: return null
        val id = message.optString("id").ifBlank { o.optString("uuid") }
        if (!state.seen.add(id)) return null
        val model = message.optString("model").takeIf { it.isNotBlank() && it != "<synthetic>" }
        val input = usage.optLong("input_tokens")
        val cacheRead = usage.optLong("cache_read_input_tokens")
        val cacheWrite = usage.optLong("cache_creation_input_tokens")
        return AgentUsage(
            inputTokens = input,
            outputTokens = usage.optLong("output_tokens"),
            cachedTokens = cacheRead,
            cacheWriteTokens = cacheWrite,
            model = model,
            contextTokens = input + cacheRead + cacheWrite,
            contextWindow = if (model?.contains("[1m]") == true) 1_000_000L else 200_000L,
            updatedAt = timestamp(o.optString("timestamp").ifBlank { null }),
        )
    }
}

private fun windowLabel(minutes: Long): String = when {
    minutes <= 0 -> "limit"
    minutes % 10_080 == 0L -> if (minutes == 10_080L) "week" else "${minutes / 10_080}w"
    minutes % 1_440 == 0L -> "${minutes / 1_440}d"
    minutes % 60 == 0L -> "${minutes / 60}h"
    else -> "${minutes}m"
}

/**
 * Codex rollouts log running totals (`token_count` events), so each event adds
 * only its difference from the previous one. The model is in `turn_context`.
 */
private object CodexFormat : UsageFormat {
    override fun consume(line: String, state: FileState): AgentUsage? {
        if (!line.contains("\"turn_context\"") && !line.contains("\"token_count\"")) return null
        val o = JSONObject(line)
        val payload = o.optJSONObject("payload") ?: return null
        if (o.optString("type") == "turn_context") {
            state.model = payload.optString("model").ifBlank { null } ?: state.model
            return null
        }
        if (o.optString("type") != "event_msg" || payload.optString("type") != "token_count") return null
        val at = timestamp(o.optString("timestamp").ifBlank { null })
        val limits = payload.optJSONObject("rate_limits")?.let { limits ->
            listOf("primary", "secondary").mapNotNull { key ->
                val window = limits.optJSONObject(key) ?: return@mapNotNull null
                val resetsAt = window.optLong("resets_at").takeIf { it > 0 }?.times(1_000)
                    ?: window.optLong("resets_in_seconds").takeIf { it > 0 }?.let { at + it * 1_000 }
                RateLimitWindow(windowLabel(window.optLong("window_minutes")), window.optDouble("used_percent", 0.0), resetsAt)
            }
        }.orEmpty()
        val info = payload.optJSONObject("info")
        val total = info?.optJSONObject("total_token_usage")
        var delta = AgentUsage()
        if (total != null) {
            // Codex counts cached prompt tokens inside input_tokens.
            val cached = total.optLong("cached_input_tokens")
            val cumulative = AgentUsage(
                inputTokens = (total.optLong("input_tokens") - cached).coerceAtLeast(0),
                outputTokens = total.optLong("output_tokens"),
                cachedTokens = cached,
                cacheWriteTokens = total.optLong("cache_write_input_tokens"),
            )
            val previous = state.lastCumulative
            delta = if (cumulative.totalTokens < previous.totalTokens) cumulative else AgentUsage(
                inputTokens = cumulative.inputTokens - previous.inputTokens,
                outputTokens = cumulative.outputTokens - previous.outputTokens,
                cachedTokens = cumulative.cachedTokens - previous.cachedTokens,
                cacheWriteTokens = cumulative.cacheWriteTokens - previous.cacheWriteTokens,
            )
            state.lastCumulative = cumulative
            info.optLong("model_context_window").takeIf { it > 0 }?.let { state.contextWindow = it }
        }
        val last = info?.optJSONObject("last_token_usage")
        return delta.copy(
            model = state.model,
            contextTokens = last?.optLong("input_tokens") ?: 0,
            contextWindow = state.contextWindow,
            rateLimits = limits,
            updatedAt = at,
        )
    }
}

/**
 * Minimal protobuf reader for Antigravity's conversation trajectories
 * (`conversations/<id>.pb`). Token counts live at Trajectory.generator_metadata(3)
 * → chat_model(1) → usage(4) as ModelUsageStats: input 2, output 3, cache write 4,
 * cache read 5, response id 11. Anything that does not decode is skipped.
 */
private object AntigravityTrajectory {
    private class Reader(val bytes: ByteArray, var pos: Int, val end: Int) {
        fun varint(): Long? {
            var result = 0L
            var shift = 0
            while (pos < end && shift < 64) {
                val b = bytes[pos++].toInt()
                result = result or ((b and 0x7f).toLong() shl shift)
                if (b and 0x80 == 0) return result
                shift += 7
            }
            return null
        }
    }

    private sealed interface Field { val number: Int }
    private data class Varint(override val number: Int, val value: Long) : Field
    private data class Bytes(override val number: Int, val start: Int, val end: Int) : Field

    /** Decodes one message's fields, or null when the bytes are not a well-formed message. */
    private fun fields(bytes: ByteArray, start: Int, end: Int): List<Field>? {
        val reader = Reader(bytes, start, end)
        val out = ArrayList<Field>()
        while (reader.pos < end) {
            val key = reader.varint() ?: return null
            val number = (key ushr 3).toInt()
            if (number <= 0) return null
            when ((key and 7).toInt()) {
                0 -> out += Varint(number, reader.varint() ?: return null)
                1 -> { reader.pos += 8; if (reader.pos > end) return null }
                2 -> {
                    val length = reader.varint() ?: return null
                    if (length < 0 || reader.pos + length > end) return null
                    out += Bytes(number, reader.pos, (reader.pos + length).toInt())
                    reader.pos += length.toInt()
                }
                5 -> { reader.pos += 4; if (reader.pos > end) return null }
                else -> return null
            }
        }
        return out
    }

    fun read(file: File): AgentUsage {
        val bytes = file.readBytes()
        val seen = HashSet<String>()
        var usage = AgentUsage(updatedAt = file.lastModified())
        val root = fields(bytes, 0, bytes.size) ?: return usage
        root.filterIsInstance<Bytes>().filter { it.number == 3 }.forEach { generator ->
            val chat = fields(bytes, generator.start, generator.end)
                ?.filterIsInstance<Bytes>()?.firstOrNull { it.number == 1 } ?: return@forEach
            val chatFields = fields(bytes, chat.start, chat.end) ?: return@forEach
            val stats = chatFields.filterIsInstance<Bytes>().firstOrNull { it.number == 4 } ?: return@forEach
            val statFields = fields(bytes, stats.start, stats.end) ?: return@forEach
            fun count(number: Int) = statFields.filterIsInstance<Varint>().firstOrNull { it.number == number }?.value ?: 0L
            val id = statFields.filterIsInstance<Bytes>().firstOrNull { it.number == 11 || it.number == 7 }
                ?.let { String(bytes, it.start, it.end - it.start, Charsets.UTF_8) }
            if (id != null && !seen.add(id)) return@forEach
            val model = chatFields.filterIsInstance<Bytes>().firstOrNull { it.number == 21 || it.number == 19 }
                ?.let { String(bytes, it.start, it.end - it.start, Charsets.UTF_8) }
                ?.takeIf { text -> text.isNotBlank() && text.all { it.code in 32..126 } }
            val input = count(2)
            val cacheRead = count(5)
            usage = usage.copy(
                inputTokens = usage.inputTokens + input,
                outputTokens = usage.outputTokens + count(3),
                cachedTokens = usage.cachedTokens + cacheRead,
                cacheWriteTokens = usage.cacheWriteTokens + count(4),
                model = model ?: usage.model,
                contextTokens = input + cacheRead,
            )
        }
        return usage
    }
}

/**
 * Tracks live usage for the agents whose logs Hivebench can read. [guestFile]
 * maps a guest path (e.g. /root/.codex) to its host file.
 */
class AgentUsageTracker(private val guestFile: (String) -> File) {
    private val cursors = HashMap<String, LogCursor>()
    private val trajectories = HashMap<String, Pair<Long, AgentUsage>>()
    private val codexCwds = HashMap<String, String?>()

    val trackedAgents: Set<AgentKind> = setOf(AgentKind.CLAUDE_CODE, AgentKind.CODEX, AgentKind.ANTIGRAVITY)

    private fun cursor(file: File, format: UsageFormat): LogCursor =
        cursors.getOrPut(file.absolutePath) { LogCursor(file, format) }.also { it.poll() }

    /** Usage of one session's conversation, or null when its log is not found yet. */
    fun sessionUsage(kind: AgentKind, sessionKey: String, guestCwd: String, startedAt: Long): AgentUsage? {
        val usage = when (kind) {
            AgentKind.ANTIGRAVITY -> antigravityConversations()
                .filter { it.lastModified() >= startedAt }
                .maxByOrNull { it.lastModified() }
                ?.let(::trajectory)
            else -> sessionLog(kind, sessionKey, guestCwd, startedAt)?.let { (file, format) -> cursor(file, format).total }
        } ?: return null
        return withAccountLimits(kind, usage)
    }

    /** Usage recorded today across all of [kind]'s conversations. */
    fun todayUsage(kind: AgentKind): AgentUsage? {
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val usage = if (kind == AgentKind.ANTIGRAVITY) {
            // Trajectories carry no per-turn times: conversations active today count in full.
            antigravityConversations().filter { it.lastModified() >= startOfDay }
                .fold(AgentUsage()) { sum, file -> sum + trajectory(file) }
        } else {
            val logs = logsModifiedSince(kind, startOfDay) ?: return null
            logs.fold(AgentUsage()) { sum, (file, format) -> sum + (cursor(file, format).byDay[today] ?: AgentUsage()) }
        }
        return withAccountLimits(kind, usage)
    }

    /** Claude's plan limits come from the status line Hivebench attaches, not from transcripts. */
    private fun withAccountLimits(kind: AgentKind, usage: AgentUsage): AgentUsage {
        if (kind != AgentKind.CLAUDE_CODE) return usage
        val status = guestFile(com.hivebench.app.runtime.RuntimeInstaller.CLAUDE_STATUS_FILE)
        val limits = runCatching { JSONObject(status.readText()).optJSONObject("rate_limits") }.getOrNull() ?: return usage
        val windows = listOf("five_hour" to "5h", "seven_day" to "week").mapNotNull { (key, label) ->
            val window = limits.optJSONObject(key) ?: return@mapNotNull null
            RateLimitWindow(
                label,
                window.optDouble("used_percentage", window.optDouble("used_percent", 0.0)),
                window.optLong("resets_at").takeIf { it > 0 }?.times(1_000),
            )
        }
        return if (windows.isEmpty()) usage else usage.copy(rateLimits = windows)
    }

    private fun antigravityConversations(): List<File> =
        guestFile("/root/.gemini/antigravity-cli/conversations").listFiles { f -> f.name.endsWith(".pb") }.orEmpty().toList()

    private fun trajectory(file: File): AgentUsage {
        val modified = file.lastModified()
        trajectories[file.absolutePath]?.takeIf { it.first == modified }?.let { return it.second }
        val usage = runCatching { AntigravityTrajectory.read(file) }.getOrDefault(AgentUsage(updatedAt = modified))
        trajectories[file.absolutePath] = modified to usage
        return usage
    }

    /** Codex rollouts from the last [days] days, newest folders first. */
    private fun codexRollouts(days: Long): List<File> {
        val root = guestFile("/root/.codex/sessions")
        val today = LocalDate.now()
        return (0..days).flatMap { back ->
            val day = today.minusDays(back)
            File(root, "%04d/%02d/%02d".format(day.year, day.monthValue, day.dayOfMonth))
                .listFiles { f -> f.name.startsWith("rollout-") && f.name.endsWith(".jsonl") }.orEmpty().toList()
        }
    }

    private fun codexCwd(file: File): String? = codexCwds.getOrPut(file.absolutePath) {
        runCatching {
            file.bufferedReader().use { it.readLine() }
                ?.let { JSONObject(it).optJSONObject("payload")?.optString("cwd") }
        }.getOrNull()
    }

    private fun sessionLog(kind: AgentKind, sessionKey: String, guestCwd: String, startedAt: Long): Pair<File, UsageFormat>? =
        when (kind) {
            AgentKind.CLAUDE_CODE -> guestFile("/root/.claude/projects").listFiles()
                ?.map { File(it, "$sessionKey.jsonl") }
                ?.firstOrNull { it.isFile }
                ?.let { it to ClaudeFormat }
            // `codex resume --last` appends to an older rollout, so match by folder, newest write wins.
            AgentKind.CODEX -> codexRollouts(days = 30)
                .filter { codexCwd(it) == guestCwd && it.lastModified() >= startedAt - 60_000 }
                .maxByOrNull { it.lastModified() }
                ?.let { it to CodexFormat }
            else -> null
        }

    private fun logsModifiedSince(kind: AgentKind, since: Long): List<Pair<File, UsageFormat>>? =
        when (kind) {
            AgentKind.CLAUDE_CODE -> guestFile("/root/.claude/projects").listFiles().orEmpty()
                .flatMap { dir -> dir.listFiles { f -> f.name.endsWith(".jsonl") && f.lastModified() >= since }.orEmpty().toList() }
                .map { it to ClaudeFormat }
            AgentKind.CODEX -> codexRollouts(days = 30).filter { it.lastModified() >= since }.map { it to CodexFormat }
            else -> null
        }
}
