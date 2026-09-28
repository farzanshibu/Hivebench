package com.hivebench.app.hive

import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile

/**
 * Reads real token usage from Claude Code transcripts
 * (`/root/.claude/projects/<cwd>/<session-id>.jsonl`) incrementally, the way
 * munder's transcript.ts does. Streaming writes the same assistant message
 * once per content block with identical usage, so usage is counted once per
 * `message.id`. Cost is an estimate from public list prices.
 */
class TranscriptMeter {
    private class Cursor(var file: File? = null, var offset: Long = 0, val seen: HashSet<String> = HashSet(), var spend: AgentSpend = AgentSpend())

    private val cursors = HashMap<String, Cursor>()

    fun read(agentId: String, transcript: File?): AgentSpend {
        val cursor = cursors.getOrPut(agentId) { Cursor() }
        if (transcript == null || !transcript.isFile) return cursor.spend
        if (cursor.file != transcript || transcript.length() < cursor.offset) {
            cursors[agentId] = Cursor(file = transcript)
            return read(agentId, transcript)
        }
        if (transcript.length() == cursor.offset) return cursor.spend
        RandomAccessFile(transcript, "r").use { raf ->
            raf.seek(cursor.offset)
            val remaining = (raf.length() - cursor.offset).coerceAtMost(8_000_000L).toInt()
            val bytes = ByteArray(remaining)
            raf.readFully(bytes)
            // Only consume whole lines; a partial last line is re-read next time.
            val text = String(bytes)
            val lastNewline = text.lastIndexOf('\n')
            if (lastNewline < 0) return cursor.spend
            cursor.offset += text.substring(0, lastNewline + 1).toByteArray().size
            var spend = cursor.spend
            text.substring(0, lastNewline).lineSequence().forEach { line ->
                if (!line.contains("\"usage\"")) return@forEach
                val o = runCatching { JSONObject(line) }.getOrNull() ?: return@forEach
                if (o.optString("type") != "assistant") return@forEach
                val message = o.optJSONObject("message") ?: return@forEach
                val usage = message.optJSONObject("usage") ?: return@forEach
                val id = message.optString("id").ifBlank { o.optString("uuid") }
                if (!cursor.seen.add(id)) return@forEach
                val model = message.optString("model").ifBlank { spend.model ?: "" }
                val input = usage.optLong("input_tokens")
                val output = usage.optLong("output_tokens")
                val cacheRead = usage.optLong("cache_read_input_tokens")
                val cacheWrite = usage.optLong("cache_creation_input_tokens")
                spend = spend.copy(
                    inputTokens = spend.inputTokens + input,
                    outputTokens = spend.outputTokens + output,
                    cacheReadTokens = spend.cacheReadTokens + cacheRead,
                    cacheWriteTokens = spend.cacheWriteTokens + cacheWrite,
                    costUsd = spend.costUsd + Pricing.cost(model, input, output, cacheRead, cacheWrite),
                    contextTokens = input + cacheRead + cacheWrite,
                    model = model.takeIf { it.isNotBlank() && it != "<synthetic>" } ?: spend.model,
                )
            }
            cursor.spend = spend
        }
        return cursor.spend
    }

    fun forget(agentId: String) {
        cursors.remove(agentId)
    }
}

/** List prices in USD per million tokens (input, output). Estimates only. */
object Pricing {
    private fun rates(model: String): Pair<Double, Double> {
        val m = model.lowercase()
        return when {
            "opus" in m && Regex("opus-4-[01]|opus-4-20|claude-3-opus|opus-4$").containsMatchIn(m) -> 15.0 to 75.0
            "opus" in m -> 5.0 to 25.0
            "haiku-3" in m || "3-haiku" in m -> 0.25 to 1.25
            "3-5-haiku" in m || "haiku-3-5" in m -> 0.8 to 4.0
            "haiku" in m -> 1.0 to 5.0
            else -> 3.0 to 15.0 // sonnet and unknown models
        }
    }

    fun cost(model: String, input: Long, output: Long, cacheRead: Long, cacheWrite: Long): Double {
        val (inRate, outRate) = rates(model)
        return (input * inRate + output * outRate + cacheRead * inRate * 0.1 + cacheWrite * inRate * 1.25) / 1_000_000.0
    }

    fun contextLimit(model: String?): Long = if (model?.contains("[1m]") == true || model?.contains("-1m") == true) 1_000_000L else 200_000L
}
