package com.hivebench.app.hive

import com.hivebench.app.model.AgentKind

/**
 * Data model of the per-project multi-agent office ("hive"), ported from
 * munder-difflin's HIVE.md: a god agent (the orchestrator) plus hired workers,
 * each a real agent CLI in its own PTY, coordinating through file mailboxes,
 * a shared board and a task ledger that live under `<project>/.hive/`.
 */

/** Speech acts (FIPA-lite). Only [obligatesReply] acts expect an answer. */
enum class HiveAct(val wire: String) {
    REQUEST("request"),
    INFORM("inform"),
    PROPOSE("propose"),
    QUERY("query"),
    AGREE("agree"),
    REFUSE("refuse"),
    DONE("done");

    val obligatesReply: Boolean get() = this == REQUEST || this == QUERY || this == PROPOSE

    companion object {
        fun parse(value: String?): HiveAct = entries.firstOrNull { it.wire == value?.trim()?.lowercase() } ?: INFORM
    }
}

data class HiveMessage(
    val id: String,
    val conversation: String,
    val inReplyTo: String?,
    val from: String,
    val to: String,
    val act: HiveAct,
    val subject: String,
    val body: String,
    val hops: Int,
    val requiresReply: Boolean,
    val needsHuman: Boolean,
    val createdAt: String,
    /** Delivery bookkeeping, not part of the wire format. */
    val deliveredAt: Long = 0L,
)

/** What an agent seat is for. The god seat is unique per project. */
data class HiveRole(
    val id: String,
    val title: String,
    val blurb: String,
    val prompt: String,
)

/** One of the role bundles: grants skills, connections and MCP servers in a single write. */
data class RoleBundle(
    val id: String,
    val title: String,
    val role: String,
    val skills: List<String>,
    val connections: List<String>,
    val mcpServers: List<String>,
)

data class HiveCapabilities(
    val bundle: String? = null,
    val skills: List<String> = emptyList(),
    val connections: List<String> = emptyList(),
    val mcpServers: List<String> = emptyList(),
)

/** A seat in `registry.json`. */
data class HiveAgent(
    val id: String,
    val name: String,
    val role: String,
    val cli: AgentKind,
    val model: String = "",
    val isGod: Boolean = false,
    val capabilities: HiveCapabilities = HiveCapabilities(),
    /** Claude Code `--session-id`, reused with `--resume` across restarts. */
    val sessionId: String,
    val launched: Boolean = false,
    val paused: Boolean = false,
    /** 1:1 with the human: the orchestrator must not dispatch to it. */
    val onHold: Boolean = false,
    /** Queue auto-delivery paused; only "send now" types into its terminal. */
    val autoDeliveryPaused: Boolean = false,
    /** Standing goal injected into its context on every turn. */
    val goal: String = "",
    /** Per-agent token cap; 0 = no cap. */
    val tokenCap: Long = 0L,
    val hiredAt: Long = System.currentTimeMillis(),
) {
    fun terminalKey(projectId: String) = "hive:$projectId:$id"
}

/** Live status an agent reports through its hooks (`agents/<id>/status.json`). */
data class HiveAgentStatus(
    val state: String = "idle",
    val doing: String = "",
    val tool: String? = null,
    val at: Long = 0L,
)

enum class TaskColumn(val wire: String, val title: String) {
    TODO("todo", "To do"),
    DOING("doing", "Doing"),
    BLOCKED("blocked", "Blocked"),
    DONE("done", "Done");

    companion object {
        fun parse(value: String?): TaskColumn {
            val v = value?.trim()?.lowercase().orEmpty()
            return entries.firstOrNull { it.wire == v } ?: when (v) {
                "in_progress", "in-progress", "wip", "active", "review" -> DOING
                "complete", "completed", "closed" -> DONE
                else -> TODO
            }
        }
    }
}

/** One entry of a card's human Q&A thread (the ASK ME trail). Never reordered. */
data class HumanQA(
    val q: String,
    val a: String? = null,
    val askedAt: String? = null,
    val answeredAt: String? = null,
    val dismissedAt: String? = null,
)

/**
 * A task card in `tasks.json` (munder schema): id like `bmt-12`, an assignee
 * (never cleared, so done cards still say who did the work) and a status column.
 * [raw] keeps every field agents wrote so rewrites never drop data.
 */
data class HiveTask(
    val id: String,
    val title: String,
    val description: String = "",
    val assignee: String? = null,
    val column: TaskColumn = TaskColumn.TODO,
    val dependsOn: List<String> = emptyList(),
    val priority: Int = 3,
    val createdAt: String = "",
    val humanQA: List<HumanQA> = emptyList(),
    val result: String? = null,
    val raw: org.json.JSONObject = org.json.JSONObject(),
) {
    /** The last unanswered, undismissed question on the card. */
    val openQuestion: HumanQA? get() = humanQA.lastOrNull { it.a == null && it.dismissedAt == null && it.q.isNotBlank() }
    val waitsOnHuman: Boolean get() = column == TaskColumn.BLOCKED && openQuestion != null
}

/** Decision on an inbound item: strict webhooks wait for the human before reaching the floor. */
enum class ExternalDecision { AUTO_ALLOWED, PENDING, APPROVED, REJECTED }

/** Something that came in from outside the floor (webhook, integration). */
data class ExternalItem(
    val id: String,
    val source: String,
    val title: String,
    val body: String,
    val createdAt: Long,
    val routedTo: String? = null,
    val decision: ExternalDecision = ExternalDecision.PENDING,
)

enum class AutomationKind(val title: String) { MISSION("Mission"), CONTEXT_RULE("Context rule"), WEBHOOK("Webhook") }

/**
 * Missions on a schedule, context rules and webhooks in one list (munder triggers.ts):
 * - MISSION: every [intervalMinutes] (optionally only at [dailyMinute] local time),
 *   sends [body] to [target] as a `request` from `scheduler`.
 * - CONTEXT_RULE: every [intervalMinutes], agents whose context window is at least
 *   [minContextPct]% full get `/compact <body>` queued (one pending per agent).
 * - WEBHOOK: accepts POSTs carrying header `x-md-webhook-secret: <token>` on the
 *   phone's LAN; [autoAllow] lets them straight through, otherwise they wait in Inbox › Outside.
 */
data class HiveAutomation(
    val id: String,
    val kind: AutomationKind,
    val title: String,
    val body: String,
    val target: String = "god",
    val intervalMinutes: Int = 60,
    val dailyMinute: Int = -1,
    val minContextPct: Int = 60,
    val token: String = "",
    val autoAllow: Boolean = false,
    val enabled: Boolean = true,
    val lastRunAt: Long = 0L,
)

enum class BreakerLevel { HEALTHY, STEERING, CONSTRAINED, STOPPED }

/**
 * Cost/runaway breaker (munder breaker.ts): watches every agent for looping on
 * one tool, error storms, token velocity and spend caps, escalating
 * steer → constrain → (stop when [hardStop]). [godCapUsd] is the orchestrator's cap.
 */
data class BreakerState(
    val enabled: Boolean = true,
    val hardStop: Boolean = false,
    val godCapUsd: Double = 5.0,
    val floorCapUsd: Double = 0.0,
    val repeatedToolLimit: Int = 8,
    val errorStormLimit: Int = 5,
    val tokenVelocityPerMin: Long = 60_000,
    val levels: Map<String, BreakerLevel> = emptyMap(),
    val reasons: Map<String, String> = emptyMap(),
    /** Floor-wide halt thrown by the operator: every agent stops at its next hook. */
    val floorHalted: Boolean = false,
)

/** Token/cost usage read from an agent's transcript. */
data class AgentSpend(
    val inputTokens: Long = 0,
    val outputTokens: Long = 0,
    val cacheReadTokens: Long = 0,
    val cacheWriteTokens: Long = 0,
    val costUsd: Double = 0.0,
    /** Tokens of the last turn's context window (input + cache). */
    val contextTokens: Long = 0,
    val model: String? = null,
) {
    val totalTokens: Long get() = inputTokens + outputTokens + cacheReadTokens + cacheWriteTokens
}

/** An entry of the routing log (`log.jsonl`). */
data class HiveLogEvent(
    val at: Long,
    val type: String,
    val from: String? = null,
    val to: String? = null,
    val summary: String,
)

/** A message the human queued for an agent, typed into its terminal when it is idle. */
data class QueuedSend(
    val id: String,
    val agentId: String,
    val text: String,
    val createdAt: Long,
    /** "Send now": bypasses the auto-delivery pause and sits at the front. */
    val manual: Boolean = false,
    /** Dropped at delivery time if the precondition no longer holds ("inbox-nonempty"). */
    val precondition: String? = null,
    /** Who queued it: human, nudge, compact, mission. */
    val origin: String = "human",
)

/** A memory note (from `memory.md`, the board or a task) for the memory graph. */
data class MemoryHit(
    val kind: String, // "ticket" | "agent" | "note"
    val ref: String,
    val title: String,
    val snippet: String,
    val score: Double,
)

/** Everything the UI renders for the office, rebuilt on every engine tick. */
data class HiveSnapshot(
    val ready: Boolean = false,
    val projectId: String? = null,
    val hiveGuestPath: String = "",
    val agents: List<HiveAgent> = emptyList(),
    val status: Map<String, HiveAgentStatus> = emptyMap(),
    val spend: Map<String, AgentSpend> = emptyMap(),
    val unread: Map<String, Int> = emptyMap(),
    val tasks: List<HiveTask> = emptyList(),
    val messages: List<HiveMessage> = emptyList(),
    /** When each done card was first seen done; cards leave the board [DONE_CARD_TTL_MS] later. */
    val doneSince: Map<String, Long> = emptyMap(),
    val external: List<ExternalItem> = emptyList(),
    val automations: List<HiveAutomation> = emptyList(),
    val breaker: BreakerState = BreakerState(),
    val log: List<HiveLogEvent> = emptyList(),
    val queue: List<QueuedSend> = emptyList(),
    val board: String = "",
) {
    val god: HiveAgent? get() = agents.firstOrNull { it.isGod }
    val workers: List<HiveAgent> get() = agents.filterNot { it.isGod }
    val floorSpendUsd: Double get() = spend.values.sumOf { it.costUsd }
    val askMe: List<HiveTask> get() = tasks.filter { it.waitsOnHuman }.sortedByDescending { it.openQuestion?.askedAt.orEmpty() }
}

/** Done cards leave the board on their own this long after they turn done. */
const val DONE_CARD_TTL_MS = 30 * 60 * 1000L
