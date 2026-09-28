package com.hivebench.app.usage

import com.hivebench.app.model.AgentKind
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Instant
import java.time.LocalDate

class AgentUsageTrackerTest {
    @get:Rule val temp = TemporaryFolder()

    private fun tracker() = AgentUsageTracker { path -> File(temp.root, path.removePrefix("/")) }

    private fun guest(path: String) = File(temp.root, path.removePrefix("/")).apply { parentFile.mkdirs() }

    @Test
    fun claudeCountsEachMessageOnceAndReadsAppendedLines() {
        val transcript = guest("/root/.claude/projects/-workspace-demo/s1.jsonl")
        val now = Instant.now().toString()
        val line = """{"type":"assistant","timestamp":"$now","message":{"id":"m1","model":"claude-sonnet-5","usage":{"input_tokens":10,"output_tokens":5,"cache_read_input_tokens":100,"cache_creation_input_tokens":20}}}"""
        transcript.writeText("$line\n$line\n")
        val tracker = tracker()
        val first = tracker.sessionUsage(AgentKind.CLAUDE_CODE, "s1", "/workspace/demo", 0)!!
        assertEquals(10, first.inputTokens)
        assertEquals(5, first.outputTokens)
        assertEquals(100, first.cachedTokens)
        assertEquals(20, first.cacheWriteTokens)
        assertEquals(130, first.contextTokens)

        transcript.appendText(line.replace("\"m1\"", "\"m2\"") + "\n")
        assertEquals(20, tracker.sessionUsage(AgentKind.CLAUDE_CODE, "s1", "/workspace/demo", 0)!!.inputTokens)
        assertEquals(20, tracker.todayUsage(AgentKind.CLAUDE_CODE)!!.inputTokens)
    }

    @Test
    fun claudePlanLimitsComeFromTheStatusFile() {
        guest("/root/.claude/projects/-workspace-demo/s1.jsonl").writeText(
            """{"type":"assistant","message":{"id":"m1","usage":{"input_tokens":1,"output_tokens":1}}}""" + "\n",
        )
        guest("/root/.cache/hivebench/claude-status.json").writeText(
            """{"rate_limits":{"five_hour":{"used_percentage":42.5,"resets_at":2000000000},"seven_day":{"used_percentage":7}}}""",
        )
        val limits = tracker().sessionUsage(AgentKind.CLAUDE_CODE, "s1", "/workspace/demo", 0)!!.rateLimits
        assertEquals(listOf("5h", "week"), limits.map { it.label })
        assertEquals(42.5, limits[0].usedPercent, 0.0)
        assertEquals(2_000_000_000_000L, limits[0].resetsAtMillis)
    }

    @Test
    fun codexUsesRunningTotalsAndRateLimits() {
        val day = LocalDate.now()
        val rollout = guest("/root/.codex/sessions/%04d/%02d/%02d/rollout-x.jsonl".format(day.year, day.monthValue, day.dayOfMonth))
        val now = Instant.now().toString()
        fun tokens(input: Int, cached: Int, output: Int) =
            """{"timestamp":"$now","type":"event_msg","payload":{"type":"token_count","info":{"total_token_usage":{"input_tokens":$input,"cached_input_tokens":$cached,"output_tokens":$output},"last_token_usage":{"input_tokens":900},"model_context_window":10000},"rate_limits":{"primary":{"used_percent":12.0,"window_minutes":300,"resets_at":2000000000},"secondary":{"used_percent":3.0,"window_minutes":10080}}}}"""
        rollout.writeText(
            listOf(
                """{"timestamp":"$now","type":"session_meta","payload":{"id":"x","cwd":"/workspace/demo"}}""",
                """{"timestamp":"$now","type":"turn_context","payload":{"model":"gpt-6-codex","cwd":"/workspace/demo"}}""",
                tokens(100, 40, 10),
                tokens(300, 200, 30),
            ).joinToString("\n", postfix = "\n"),
        )
        val usage = tracker().sessionUsage(AgentKind.CODEX, "ignored", "/workspace/demo", 0)!!
        assertEquals(100, usage.inputTokens)
        assertEquals(200, usage.cachedTokens)
        assertEquals(30, usage.outputTokens)
        assertEquals("gpt-6-codex", usage.model)
        assertEquals(900, usage.contextTokens)
        assertEquals(10_000L, usage.contextWindow)
        assertEquals(listOf("5h", "week"), usage.rateLimits.map { it.label })
        assertEquals(null, tracker().sessionUsage(AgentKind.CODEX, "ignored", "/workspace/other", 0))
    }

    @Test
    fun antigravityTrajectoryUsageIsDecoded() {
        fun varint(out: ByteArrayOutputStream, value: Long) {
            var v = value
            while (v >= 0x80) { out.write(((v and 0x7f) or 0x80).toInt()); v = v ushr 7 }
            out.write(v.toInt())
        }
        fun message(vararg fields: Pair<Int, Any>): ByteArray = ByteArrayOutputStream().also { out ->
            fields.forEach { (number, value) ->
                when (value) {
                    is Long -> { varint(out, (number shl 3).toLong()); varint(out, value) }
                    is ByteArray -> { varint(out, ((number shl 3) or 2).toLong()); varint(out, value.size.toLong()); out.write(value) }
                    is String -> { val b = value.toByteArray(); varint(out, ((number shl 3) or 2).toLong()); varint(out, b.size.toLong()); out.write(b) }
                }
            }
        }.toByteArray()
        fun step(id: String, input: Long, output: Long) =
            message(1 to message(4 to message(2 to input, 3 to output, 5 to 7L, 11 to id), 21 to "Gemini 3 Pro"))
        val trajectory = message(1 to "conversation", 3 to step("r1", 50, 5), 3 to step("r2", 60, 6), 3 to step("r2", 60, 6))
        guest("/root/.gemini/antigravity-cli/conversations/c1.pb").writeBytes(trajectory)

        val usage = tracker().sessionUsage(AgentKind.ANTIGRAVITY, "ignored", "/workspace/demo", 0)!!
        assertEquals(110, usage.inputTokens)
        assertEquals(11, usage.outputTokens)
        assertEquals(14, usage.cachedTokens)
        assertEquals("Gemini 3 Pro", usage.model)
    }
}
