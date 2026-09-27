package com.jarves.mh.orchestrator

/** Lifecycle used to gate terminal injection: nothing is written to a busy terminal. */
enum class WorkerState { ASLEEP, WAKING, AWAITING_PROMPT, IDLE, BUSY, REVIEW, BLOCKED }

class PromptQuiescenceDetector(private val quiescenceMillis: Long = 500L) {
    private var trailingText = ""
    private var lastOutputAt = 0L

    fun onOutput(raw: String, nowMillis: Long): Boolean {
        trailingText = (trailingText + stripAnsi(raw)).takeLast(512)
        lastOutputAt = nowMillis
        return false
    }

    fun isReady(nowMillis: Long): Boolean = nowMillis - lastOutputAt >= quiescenceMillis && promptVisible(trailingText)

    internal fun promptVisible(text: String): Boolean = text.trimEnd().let { clean ->
        clean.endsWith('$') || clean.endsWith('#') || clean.endsWith('>') || clean.endsWith('❯') || clean.endsWith("claude>")
    }

    private fun stripAnsi(text: String): String = text.replace(ANSI, "")
    private companion object { val ANSI = Regex("\\u001B(?:\\][^\\u0007]*(?:\\u0007|\\u001B\\\\)|\\[[0-?]*[ -/]*[@-~])") }
}

class WorkerLifecycle(private val agentId: String, private val detector: PromptQuiescenceDetector = PromptQuiescenceDetector()) {
    var state: WorkerState = WorkerState.ASLEEP
        private set
    fun wake() { if (state == WorkerState.ASLEEP) state = WorkerState.WAKING }
    fun ptySpawned() { if (state == WorkerState.WAKING) state = WorkerState.AWAITING_PROMPT }
    fun onTerminalOutput(output: String, nowMillis: Long) { detector.onOutput(output, nowMillis); if (state == WorkerState.IDLE) state = WorkerState.BUSY }
    fun tick(nowMillis: Long): Boolean {
        if (state == WorkerState.AWAITING_PROMPT || state == WorkerState.BUSY) {
            if (detector.isReady(nowMillis)) state = WorkerState.IDLE
        }
        return state == WorkerState.IDLE
    }
    fun deliverIfReady(hasQueuedWork: Boolean, write: (String) -> Unit): Boolean {
        if (!hasQueuedWork || state != WorkerState.IDLE) return false
        write("Read .hive/inbox/$agentId.json, claim the first assigned task, and proceed with implementation.\n")
        state = WorkerState.BUSY
        return true
    }
    fun block() { state = WorkerState.BLOCKED }
    fun unblock() { if (state == WorkerState.BLOCKED) state = WorkerState.AWAITING_PROMPT }
    fun sleep() { state = WorkerState.ASLEEP }
}
