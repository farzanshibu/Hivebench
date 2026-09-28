package com.hivebench.app.hive

import android.content.Context
import android.os.SystemClock
import com.hivebench.app.model.AgentKind
import com.hivebench.app.runtime.RuntimeInstaller
import com.hivebench.app.terminal.TerminalSessions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.util.UUID

/** What the engine needs from the app to launch agents and read their transcripts. */
interface HiveHost {
    suspend fun launch(agent: HiveAgent, extraArgs: List<String>, extraEnv: Map<String, String>): RuntimeInstaller.GuestLaunch
    fun transcriptFile(sessionId: String): File?
    fun connectionEnv(connections: List<String>): Map<String, String>
    fun autoApproveTools(): Boolean
    fun isInstalled(kind: AgentKind): Boolean
    fun toast(message: String)
}

/**
 * The office of one project: the harness side of munder-difflin's hive.
 *
 * Each agent is a real CLI in its own PTY ([TerminalSessions] key
 * `hive:<project>:<agent>`), so any number run in parallel. This engine is the
 * *mechanism* (routing, delivery, breaker, telemetry); the god agent is the
 * *intelligence* that decomposes, delegates and signs off.
 *
 * Loops, all on one coroutine:
 * - router, every 1.5 s — outboxes → inboxes (hop cap 12, broadcast, human→god, bounces);
 * - queue drain — types queued text into an agent only when it is idle (quiet PTY,
 *   past boot grace, not waiting on a permission prompt, 4.5 s cooldown);
 * - inbox nudges, every 4 s — one pending nudge per agent with new mail;
 * - telemetry, every 3 s — hook status, transcript spend, fleet.json every 8 s;
 * - breaker, every 30 s — steer → constrain → stop, and the god's spend cap;
 * - automations, every 30 s — missions, context rules, webhooks; done cards leave the board.
 */
class HiveEngine(
    private val context: Context,
    private val scope: CoroutineScope,
    private val host: HiveHost,
) {
    private val _snapshot = MutableStateFlow(HiveSnapshot())
    val snapshot: StateFlow<HiveSnapshot> = _snapshot.asStateFlow()

    private val lock = Mutex()
    private var loop: Job? = null
    private var store: HiveStore? = null
    private var projectId: String? = null
    private var guestRoot: String = ""

    private var agents: List<HiveAgent> = emptyList()
    private var queue: List<QueuedSend> = emptyList()
    private var automations: List<HiveAutomation> = emptyList()
    private var external: List<ExternalItem> = emptyList()
    private var breaker = BreakerState()
    private var doneSince: Map<String, Long> = emptyMap()

    private val meter = TranscriptMeter()
    private val spawnedAt = HashMap<String, Long>()
    private val spawnedWall = HashMap<String, Long>()
    private val lastDelivery = HashMap<String, Long>()
    private val announced = HashMap<String, Set<String>>()
    private val eventOffsets = HashMap<String, Long>()
    private val repeat = HashMap<String, Pair<String, Int>>()
    private val errors = HashMap<String, Int>()
    private val lastOutputTokens = HashMap<String, Pair<Long, Long>>()
    private val routedTimes = ArrayDeque<Long>()
    private var webhook: HiveWebhookServer? = null

    val webhookPort: Int get() = HiveWebhookServer.DEFAULT_PORT

    // ---- lifecycle ---------------------------------------------------------------

    /** Opens (or re-opens) the office of [projectId]; agents of another project keep running untouched. */
    fun open(projectId: String, projectRoot: File, guestRoot: String) {
        if (this.projectId == projectId && loop?.isActive == true) return
        loop?.cancel()
        webhook?.stop()
        webhook = null
        this.projectId = projectId
        this.guestRoot = guestRoot
        val s = HiveStore(projectRoot, guestRoot)
        store = s
        resetCaches()
        _snapshot.value = HiveSnapshot()
        loop = scope.launch(Dispatchers.IO) {
            val opened = runCatching { lock.withLock {
                s.ensureLayout()
                agents = s.loadAgents()
                if (agents.none { it.isGod }) {
                    agents = listOf(newGod()) + agents
                    s.log("spawn", "Michael took the corner office", from = HiveTemplates.GOD_ID)
                }
                agents.forEach { s.ensureAgentLayout(it, agents); s.writeGoal(it.id, it.goal) }
                s.saveAgents(agents)
                queue = s.loadQueue()
                automations = s.loadAutomations().ifEmpty { defaultAutomations().also(s::saveAutomations) }
                external = s.loadExternal()
                breaker = s.loadBreaker()
                doneSince = s.loadDoneSince()
                s.writeFloorControl(breaker.floorHalted)
                agents.forEach { s.writeControl(it, halted = false) }
            } }
            opened.onFailure {
                android.util.Log.w("HiveEngine", "open failed", it)
                host.toast("Could not open the office: ${it.message ?: it.javaClass.simpleName}")
                return@launch
            }
            var tick = 0L
            while (isActive) {
                runCatching { tick(tick) }.onFailure { android.util.Log.w("HiveEngine", "tick failed", it) }
                tick++
                delay(TICK_MS)
            }
        }
    }

    fun close() {
        loop?.cancel()
        loop = null
        webhook?.stop()
        webhook = null
        projectId = null
        store = null
        _snapshot.value = HiveSnapshot()
    }

    private fun resetCaches() {
        spawnedAt.clear(); spawnedWall.clear(); lastDelivery.clear(); announced.clear(); eventOffsets.clear()
        repeat.clear(); errors.clear(); lastOutputTokens.clear(); routedTimes.clear()
    }

    private fun newGod() = HiveAgent(
        id = HiveTemplates.GOD_ID,
        name = HiveTemplates.DEFAULT_GOD_NAME,
        role = "orchestrator (god) — runs the floor, triages requests, escalates only critical calls to you",
        cli = AgentKind.CLAUDE_CODE,
        isGod = true,
        capabilities = RoleBundles.byId("orchestrator")!!.let { HiveCapabilities(it.id, it.skills, it.connections, it.mcpServers) },
        sessionId = UUID.randomUUID().toString(),
    )

    private fun defaultAutomations() = listOf(
        HiveAutomation(UUID.randomUUID().toString(), AutomationKind.MISSION, "Ops standup", HiveTemplates.STANDUP, target = HiveTemplates.GOD_ID, intervalMinutes = 60),
        HiveAutomation(UUID.randomUUID().toString(), AutomationKind.CONTEXT_RULE, "Compact heavy contexts", HiveTemplates.COMPACT_RULE, target = "all", intervalMinutes = 120, minContextPct = 60),
    )

    private fun key(agentId: String) = "hive:$projectId:$agentId"

    // ---- tick --------------------------------------------------------------------

    private suspend fun tick(n: Long) = lock.withLock {
        val s = store ?: return@withLock
        route(s)
        if (n % 3 == 0L) nudge(s)
        drainQueue(s)
        if (n % 2 == 0L) publish(s, withFleet = n % 5 == 0L)
        if (n % 20 == 0L) {
            breakerBeat(s)
            automationsBeat(s)
            cleanupDoneCards(s)
        }
    }

    // ---- router ------------------------------------------------------------------

    private fun route(s: HiveStore) {
        val now = System.currentTimeMillis()
        agents.forEach { sender ->
            s.drainOutbox(sender.id).forEach { (file, parsed) ->
                if (parsed == null) {
                    s.log("drop", "malformed-json ${file.name}", from = sender.id)
                    val sent = File(file.parentFile, ".sent").apply { mkdirs() }
                    file.renameTo(File(sent, "bad-${file.name}"))
                    return@forEach
                }
                // The harness owns id and from: agent-supplied ids could collide across outboxes.
                val msg = parsed.copy(id = s.newMessageId(), from = sender.id)
                routeMessage(s, msg)
                s.markSent(file)
            }
        }
        while (routedTimes.isNotEmpty() && now - routedTimes.first() > 60_000) routedTimes.removeFirst()
    }

    private fun resolve(to: String): String = when (to.lowercase()) {
        "human", "god", "michael", "orchestrator" -> agents.firstOrNull { it.isGod }?.id ?: HiveTemplates.GOD_ID
        else -> agents.firstOrNull { it.id.equals(to, true) || it.name.equals(to, true) }?.id ?: to
    }

    /** Delivers [msg] per munder's routeMessage rules and records it. */
    private fun routeMessage(s: HiveStore, msg: HiveMessage) {
        if (msg.hops > HOP_CAP) {
            s.log("drop", "hop-cap: ${msg.subject}", from = msg.from, to = msg.to)
            return
        }
        val godId = agents.firstOrNull { it.isGod }?.id ?: HiveTemplates.GOD_ID
        val targets = if (msg.to.equals("broadcast", true)) {
            agents.filter { it.id != msg.from }.map { it.id }
        } else listOf(resolve(msg.to)).filter { it != msg.from }
        val delivered = mutableListOf<String>()
        val stamped = msg.copy(
            needsHuman = msg.needsHuman || msg.to.equals("human", true),
            deliveredAt = System.currentTimeMillis(),
        )
        targets.forEach { t ->
            if (agents.none { it.id == t }) {
                s.log("drop", "no-inbox: \"$t\"", from = msg.from, to = t)
                if (t != godId && msg.from != godId) {
                    val bounce = stamped.copy(id = s.newMessageId(), to = godId, subject = "[undeliverable — no agent \"$t\" on this floor; check the id against the roster] ${msg.subject}")
                    s.deliver(godId, bounce)
                    delivered += godId
                }
                return@forEach
            }
            s.deliver(t, stamped)
            delivered += t
        }
        s.appendMessage(stamped)
        s.log("message", "${msg.act.wire}: ${msg.subject}", from = msg.from, to = if (msg.to.equals("broadcast", true)) "broadcast" else delivered.joinToString(",").ifBlank { msg.to })
        routedTimes.addLast(System.currentTimeMillis())
    }

    /** Injects a message from outside the agents (human, scheduler, breaker, webhook). */
    private fun sendFrom(s: HiveStore, from: String, to: String, act: HiveAct, subject: String, body: String, needsHuman: Boolean = false) {
        routeMessage(
            s,
            HiveMessage(
                id = s.newMessageId(),
                conversation = "conv-" + UUID.randomUUID().toString().take(6),
                inReplyTo = null,
                from = from,
                to = to,
                act = act,
                subject = subject,
                body = body,
                hops = 0,
                requiresReply = act.obligatesReply,
                needsHuman = needsHuman,
                createdAt = Instant.now().toString(),
            ),
        )
    }

    // ---- delivery queue (single writer into every PTY) ----------------------------------

    private fun nudge(s: HiveStore) {
        agents.forEach { a ->
            if (!TerminalSessions.isRunning(key(a.id))) return@forEach
            val ids = s.unread(a.id).map { it.id }
            val fresh = ids.filterNot { it in (announced[a.id] ?: emptySet()) }
            announced[a.id] = ids.toSet()
            if (fresh.isEmpty()) return@forEach
            if (queue.any { it.agentId == a.id && it.origin == "nudge" }) return@forEach
            val text = if (a.cli == AgentKind.CLAUDE_CODE || a.cli == AgentKind.CODEX || a.cli == AgentKind.OPENCODE) {
                HiveTemplates.inboxNudge(ids)
            } else {
                // CLIs without a system-prompt hook get the work order itself.
                s.unread(a.id).firstOrNull()?.let(HiveTemplates::workOrder) ?: HiveTemplates.inboxNudge(ids)
            }
            enqueueLocked(s, QueuedSend(UUID.randomUUID().toString(), a.id, text, System.currentTimeMillis(), precondition = "inbox-nonempty", origin = "nudge"))
        }
    }

    private fun idle(agent: HiveAgent, quietMs: Long): Boolean {
        val k = key(agent.id)
        if (!TerminalSessions.isQuiet(k, quietMs)) return false
        val status = store?.status(agent.id)
        // Never type into a pending permission prompt: that would answer it.
        if (status?.state == "waiting" || status?.state == "halted") return false
        // CLIs without the hook (and Claude as a backstop): look at the screen itself.
        if (looksLikePrompt(TerminalSessions.screenText(k))) return false
        return true
    }

    private val promptPatterns = listOf(
        Regex("""(?i)\bdo you want to\b"""),
        Regex("""(?i)\ballow\b.{0,40}\?"""),
        Regex("""(?i)\bapprove\b.{0,40}\?"""),
        Regex("""(?i)\[y/n]|\(y/n\)"""),
        Regex("""(?i)yes, and don't ask again"""),
        Regex("""(?i)press enter to (continue|confirm)"""),
        Regex("""(?i)\b(sign|log) ?in\b.{0,30}(to continue|required)"""),
    )

    /** True when the bottom of the screen shows a question the queue must not answer. */
    private fun looksLikePrompt(screen: String?): Boolean {
        if (screen.isNullOrBlank()) return false
        val tail = screen.lines().filter { it.isNotBlank() }.takeLast(12).joinToString("\n")
        return promptPatterns.any { it.containsMatchIn(tail) }
    }

    private fun drainQueue(s: HiveStore) {
        if (queue.isEmpty()) return
        val now = SystemClock.uptimeMillis()
        var changed = false
        val byAgent = queue.groupBy { it.agentId }
        byAgent.forEach { (agentId, items) ->
            val agent = agents.firstOrNull { it.id == agentId }
            if (agent == null) {
                queue = queue.filterNot { it.agentId == agentId }; changed = true; return@forEach
            }
            val head = items.firstOrNull { it.manual } ?: items.first()
            val k = key(agentId)
            if (!TerminalSessions.isRunning(k)) return@forEach
            if (agent.autoDeliveryPaused && !head.manual) return@forEach
            val boot = spawnedAt[agentId]
            val booting = boot != null && now - boot < BOOT_GRACE_MS
            // Never type into a TUI that is still starting, signing in or onboarding:
            // Claude agents must have fired SessionStart for this spawn; others must
            // have drawn something and settled.
            if (!sessionReady(s, agent, k, now)) return@forEach
            if (head.origin == "boot") {
                if (!TerminalSessions.isQuiet(k, 2_500)) return@forEach
            } else {
                if (booting && !head.manual) return@forEach
                if (!idle(agent, if (head.manual) 2_500 else IDLE_MS)) return@forEach
            }
            if (now - (lastDelivery[agentId] ?: 0L) < FLUSH_COOLDOWN_MS) return@forEach
            if (head.precondition == "inbox-nonempty" && s.unreadCount(agentId) == 0) {
                queue = queue - head; changed = true; return@forEach
            }
            submit(k, head.text)
            lastDelivery[agentId] = now
            queue = queue - head
            changed = true
            if (head.origin == "human") s.log("typed", head.text.lineSequence().first().take(80), from = "human", to = agentId)
        }
        if (changed) s.saveQueue(queue)
    }

    private fun sessionReady(s: HiveStore, agent: HiveAgent, key: String, now: Long): Boolean {
        if (TerminalSessions.lastOutputAt(key) == null) return false
        return if (agent.cli == AgentKind.CLAUDE_CODE) {
            s.status(agent.id).at >= (spawnedWall[agent.id] ?: 1L) // adopted sessions: any hook ever fired
        } else {
            now - (spawnedAt[agent.id] ?: now) >= 8_000
        }
    }

    /** Types [text] then Enter, bracketed-pasting multi-line text so the TUI takes it as one prompt. */
    private fun submit(key: String, text: String) {
        scope.launch(Dispatchers.Main) {
            val payload = if (text.contains('\n')) "\u001b[200~$text\u001b[201~" else text
            TerminalSessions.write(key, payload)
            delay(140)
            TerminalSessions.write(key, "\r")
        }
    }

    private fun enqueueLocked(s: HiveStore, item: QueuedSend) {
        val text = item.text.trim()
        if (text.isEmpty()) return
        if (text.startsWith("/compact") && queue.any { it.agentId == item.agentId && it.text.startsWith("/compact") }) return
        queue = queue + item.copy(text = text)
        s.saveQueue(queue)
    }

    // ---- telemetry -----------------------------------------------------------------

    private fun publish(s: HiveStore, withFleet: Boolean) {
        val status = agents.associate { it.id to s.status(it.id) }.mapValues { (id, st) ->
            // Quiescence fallback: a "working" agent whose PTY went silent is idle.
            if (st.state == "working" && TerminalSessions.isQuiet(key(id), 30_000)) st.copy(state = "idle") else st
        }
        val spend = agents.associate { a ->
            a.id to if (a.cli == AgentKind.CLAUDE_CODE) meter.read(a.id, host.transcriptFile(a.sessionId)) else AgentSpend()
        }
        val unread = agents.associate { it.id to s.unreadCount(it.id) }
        val tasks = s.loadTasks()
        val snapshot = HiveSnapshot(
            ready = true,
            projectId = projectId,
            hiveGuestPath = s.guestDir,
            agents = agents,
            status = status,
            spend = spend,
            unread = unread,
            tasks = tasks,
            messages = s.recentMessages(),
            external = external,
            automations = automations,
            breaker = breaker,
            log = s.recentLog(),
            queue = queue,
            board = s.board(),
            doneSince = doneSince,
        )
        _snapshot.value = snapshot
        if (withFleet) s.writeFleet(fleetJson(snapshot))
    }

    private fun fleetJson(snap: HiveSnapshot): JSONObject {
        val arr = JSONArray()
        snap.agents.forEach { a ->
            val sp = snap.spend[a.id] ?: AgentSpend()
            val st = snap.status[a.id] ?: HiveAgentStatus()
            val running = TerminalSessions.isRunning(key(a.id))
            arr.put(
                JSONObject()
                    .put("id", a.id).put("name", a.name).put("role", a.role).put("cli", a.cli.stableId)
                    .put("state", if (!running) "offline" else st.state).put("doing", st.doing).put("lastTool", st.tool ?: "")
                    .put("tokens", sp.totalTokens).put("cost", sp.costUsd)
                    .put("ctx", if (sp.contextTokens > 0) (sp.contextTokens * 100 / Pricing.contextLimit(sp.model)).toInt() else 0)
                    .put("inbox", snap.unread[a.id] ?: 0)
                    .put("breaker", (snap.breaker.levels[a.id] ?: BreakerLevel.HEALTHY).name.lowercase())
                    .put("onHold", a.onHold).put("paused", a.paused)
                    .put("task", snap.tasks.firstOrNull { it.assignee == a.id && it.column == TaskColumn.DOING }?.id ?: ""),
            )
        }
        return JSONObject().put("at", System.currentTimeMillis()).put("agents", arr)
    }

    // ---- breaker -------------------------------------------------------------------

    private fun breakerBeat(s: HiveStore) {
        val snap = _snapshot.value
        val god = agents.firstOrNull { it.isGod }
        // The orchestrator's own cap: the breaker that stops him.
        if (god != null && breaker.godCapUsd > 0) {
            val godSpend = snap.spend[god.id]?.costUsd ?: 0.0
            val level = breaker.levels[god.id] ?: BreakerLevel.HEALTHY
            if (godSpend >= breaker.godCapUsd && level != BreakerLevel.STOPPED) {
                setLevel(god.id, BreakerLevel.STOPPED, "spend \$${"%.2f".format(godSpend)} reached his cap \$${"%.2f".format(breaker.godCapUsd)}")
                s.writeControl(god, halted = true)
                s.log("breaker", "Michael stopped at his spend cap", to = god.id)
                host.toast("Breaker stopped ${god.name}: spend cap reached")
            }
        }
        if (!breaker.enabled) return
        if (routedTimes.size > MESSAGES_PER_MINUTE_CAP) {
            s.log("breaker", "message storm: ${routedTimes.size} routed in the last minute")
        }
        val floorSpend = snap.floorSpendUsd
        val topSpender = snap.spend.filterKeys { id -> agents.any { it.id == id && !it.isGod } }.maxByOrNull { it.value.costUsd }?.key
        agents.filterNot { it.isGod }.forEach { a ->
            if (!TerminalSessions.isRunning(key(a.id))) return@forEach
            val (offset, events) = s.readEvents(a.id, eventOffsets[a.id] ?: 0L)
            eventOffsets[a.id] = offset
            events.forEach { e ->
                val k = e.optString("key")
                val (lastKey, count) = repeat[a.id] ?: ("" to 0)
                repeat[a.id] = if (k == lastKey) k to count + 1 else k to 1
                errors[a.id] = if (e.optBoolean("err")) (errors[a.id] ?: 0) + 1 else 0
            }
            val sp = snap.spend[a.id] ?: AgentSpend()
            val workTokens = sp.inputTokens + sp.outputTokens + sp.cacheWriteTokens
            val (prevOut, prevAt) = lastOutputTokens[a.id] ?: (sp.outputTokens to System.currentTimeMillis())
            val minutes = ((System.currentTimeMillis() - prevAt) / 60_000.0).coerceAtLeast(0.25)
            val velocity = ((sp.outputTokens - prevOut) / minutes).toLong()
            lastOutputTokens[a.id] = sp.outputTokens to System.currentTimeMillis()
            val reason = when {
                (repeat[a.id]?.second ?: 0) >= breaker.repeatedToolLimit -> "repeated the same tool call ${repeat[a.id]?.second} times"
                (errors[a.id] ?: 0) >= breaker.errorStormLimit -> "${errors[a.id]} tool errors in a row"
                a.tokenCap > 0 && workTokens >= a.tokenCap -> "token cap reached (${workTokens / 1000}k / ${a.tokenCap / 1000}k)"
                breaker.floorCapUsd > 0 && floorSpend >= breaker.floorCapUsd && a.id == topSpender -> "floor spend \$${"%.2f".format(floorSpend)} over cap; top spender"
                velocity > breaker.tokenVelocityPerMin -> "token velocity $velocity/min"
                else -> null
            }
            val level = breaker.levels[a.id] ?: BreakerLevel.HEALTHY
            if (reason != null) {
                val cap = if (breaker.hardStop) BreakerLevel.STOPPED else BreakerLevel.CONSTRAINED
                val next = BreakerLevel.entries[(level.ordinal + 1).coerceAtMost(cap.ordinal)]
                if (next != level) {
                    setLevel(a.id, next, reason)
                    when (next) {
                        BreakerLevel.STEERING -> sendFrom(s, "breaker", a.id, HiveAct.REQUEST, "Circuit breaker: steer", HiveTemplates.steerBody(reason))
                        BreakerLevel.CONSTRAINED -> {
                            sendFrom(s, "breaker", a.id, HiveAct.REQUEST, "Circuit breaker: constrain", HiveTemplates.constrainBody(reason))
                            host.toast("Breaker constrained ${a.name}: $reason")
                        }
                        BreakerLevel.STOPPED -> {
                            scope.launch(Dispatchers.Main) { TerminalSessions.close(key(a.id)) }
                            host.toast("Breaker stopped ${a.name}: $reason")
                        }
                        BreakerLevel.HEALTHY -> Unit
                    }
                    s.log("breaker", "${a.name}: ${next.name.lowercase()} — $reason", to = a.id)
                }
                repeat.remove(a.id)
                errors.remove(a.id)
            } else if (level != BreakerLevel.HEALTHY && level != BreakerLevel.STOPPED) {
                setLevel(a.id, BreakerLevel.entries[level.ordinal - 1], "recovering — signals cleared")
            }
        }
        s.saveBreaker(breaker)
    }

    private fun setLevel(agentId: String, level: BreakerLevel, reason: String) {
        breaker = breaker.copy(levels = breaker.levels + (agentId to level), reasons = breaker.reasons + (agentId to reason))
    }

    // ---- automations -------------------------------------------------------------------

    private fun automationsBeat(s: HiveStore) {
        val now = System.currentTimeMillis()
        var changed = false
        automations = automations.map { a ->
            if (!a.enabled) return@map a
            when (a.kind) {
                AutomationKind.MISSION -> {
                    // The first beat after creation only arms the timer.
                    if (a.lastRunAt == 0L) { changed = true; return@map a.copy(lastRunAt = now) }
                    if (!due(a, now)) return@map a
                    sendFrom(s, "scheduler", a.target, HiveAct.REQUEST, a.title, a.body)
                    changed = true
                    a.copy(lastRunAt = now)
                }
                AutomationKind.CONTEXT_RULE -> {
                    if (now - a.lastRunAt < a.intervalMinutes * 60_000L) return@map a
                    val snap = _snapshot.value
                    agents.filter { a.target == "all" || it.id == a.target }.filter { it.cli == AgentKind.CLAUDE_CODE }.forEach { agent ->
                        val sp = snap.spend[agent.id] ?: return@forEach
                        val pct = sp.contextTokens * 100 / Pricing.contextLimit(sp.model)
                        if (pct >= a.minContextPct && TerminalSessions.isRunning(key(agent.id))) {
                            enqueueLocked(s, QueuedSend(UUID.randomUUID().toString(), agent.id, "/compact ${a.body}", now, origin = "compact"))
                        }
                    }
                    changed = true
                    a.copy(lastRunAt = now)
                }
                AutomationKind.WEBHOOK -> a
            }
        }
        if (changed) s.saveAutomations(automations)
        val hooks = automations.filter { it.kind == AutomationKind.WEBHOOK && it.enabled && it.token.isNotBlank() }
        if (hooks.isNotEmpty() && webhook?.running != true) {
            webhook = HiveWebhookServer(tokens = { automations.filter { it.kind == AutomationKind.WEBHOOK && it.enabled }.map { it.token }.toSet() }) { token, title, body, from ->
                scope.launch(Dispatchers.IO) { onWebhook(token, title, body, from) }
            }.also { it.start() }
        } else if (hooks.isEmpty()) {
            webhook?.stop()
            webhook = null
        }
    }

    private fun due(a: HiveAutomation, now: Long): Boolean {
        if (a.dailyMinute >= 0) {
            val cal = java.util.Calendar.getInstance()
            val minuteOfDay = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
            // Once a day at the chosen minute; catches up within 6 h if the phone was asleep.
            return minuteOfDay >= a.dailyMinute && minuteOfDay - a.dailyMinute < 360 && now - a.lastRunAt > 20 * 60 * 60 * 1000L
        }
        return now - a.lastRunAt >= a.intervalMinutes * 60_000L
    }

    private suspend fun onWebhook(token: String, title: String, body: String, from: String?) = lock.withLock {
        val s = store ?: return@withLock
        val hook = automations.firstOrNull { it.kind == AutomationKind.WEBHOOK && it.token == token } ?: return@withLock
        val item = ExternalItem(
            id = UUID.randomUUID().toString(),
            source = "webhook · ${hook.title}${from?.let { " · $it" }.orEmpty()}",
            title = title,
            body = body,
            createdAt = System.currentTimeMillis(),
            routedTo = hook.target,
            decision = if (hook.autoAllow) ExternalDecision.AUTO_ALLOWED else ExternalDecision.PENDING,
        )
        external = external + item
        s.saveExternal(external)
        if (hook.autoAllow) sendFrom(s, "webhook", hook.target, HiveAct.REQUEST, "[webhook] $title", body)
        s.log("webhook", "${hook.title}: $title (${item.decision.name.lowercase()})", to = hook.target)
        if (!hook.autoAllow) host.toast("Webhook request waiting in Inbox › Outside")
    }

    // ---- done cards leave the board -------------------------------------------------------

    private fun cleanupDoneCards(s: HiveStore) {
        val now = System.currentTimeMillis()
        val tasks = s.loadTasks()
        val doneIds = tasks.filter { it.column == TaskColumn.DONE }.map { it.id }.toSet()
        val next = doneIds.associateWith { doneSince[it] ?: now }
        val expired = next.filterValues { now - it >= DONE_CARD_TTL_MS }.keys
        if (expired.isNotEmpty()) {
            s.archiveTasks(expired)
            s.log("tasks", "${expired.size} done card(s) left the board: ${expired.joinToString(", ")}")
        }
        val kept = next - expired
        if (kept != doneSince) {
            doneSince = kept
            s.saveDoneSince(kept)
        }
    }

    // ---- public actions -------------------------------------------------------------------

    private fun act(block: suspend (HiveStore) -> Unit) {
        scope.launch(Dispatchers.IO) {
            // A failed launch (runtime missing, bad guest path, IO) must surface, never crash the app.
            runCatching { lock.withLock { store?.let { block(it) } } }.onFailure {
                if (it is kotlinx.coroutines.CancellationException) throw it
                android.util.Log.w("HiveEngine", "action failed", it)
                host.toast(it.message ?: it.javaClass.simpleName)
            }
            runCatching { store?.let { s -> lock.withLock { publish(s, withFleet = true) } } }
        }
    }

    fun hire(name: String, role: String, cli: AgentKind, model: String, bundleId: String?, goal: String, tokenCap: Long, start: Boolean) = act { s ->
        val bundle = RoleBundles.byId(bundleId)
        val base = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "agent" }.take(24)
        var id = base
        var i = 2
        while (agents.any { it.id == id } || id == HiveTemplates.GOD_ID) id = "$base-${i++}"
        val agent = HiveAgent(
            id = id,
            name = name.trim().ifBlank { id },
            role = role.trim().ifBlank { bundle?.role ?: "agent" },
            cli = cli,
            model = model.trim(),
            capabilities = bundle?.let { HiveCapabilities(it.id, it.skills, it.connections, it.mcpServers) } ?: HiveCapabilities(),
            sessionId = UUID.randomUUID().toString(),
            goal = goal.trim(),
            tokenCap = tokenCap,
        )
        agents = agents + agent
        s.saveAgents(agents)
        agents.forEach { s.ensureAgentLayout(it, agents) }
        s.writeGoal(agent.id, agent.goal)
        s.writeControl(agent, halted = false)
        s.log("spawn", "${agent.name} hired as ${agent.role}", to = agent.id)
        sendFrom(s, "system", HiveTemplates.GOD_ID, HiveAct.INFORM, "New hire: ${agent.name} (${agent.id})", "${agent.name} joined the floor as ${agent.role} running ${cli.title}. Route matching work to `${agent.id}`.")
        if (start) startLocked(s, agent)
    }

    fun update(agentId: String, transform: (HiveAgent) -> HiveAgent) = act { s ->
        val before = agents.firstOrNull { it.id == agentId } ?: return@act
        val after = transform(before).copy(id = before.id, isGod = before.isGod, sessionId = before.sessionId)
        agents = agents.map { if (it.id == agentId) after else it }
        s.saveAgents(agents)
        agents.forEach { s.ensureAgentLayout(it, agents) }
        if (after.goal != before.goal) s.writeGoal(agentId, after.goal)
        if (after.paused != before.paused) {
            s.writeControl(after, halted = false)
            s.log("control", "${after.name} ${if (after.paused) "paused" else "resumed"}", from = "human", to = agentId)
        }
        if (after.onHold != before.onHold) s.log("agent-hold", "${after.name} ${if (after.onHold) "on hold (1:1 with you)" else "back on the floor"}", to = agentId)
        if (after.capabilities != before.capabilities) s.log("capabilities", "${after.name}: ${after.capabilities.bundle ?: "custom"} — restart to apply", to = agentId)
    }

    /** Grants a role bundle in a single write (role, skills, connections, MCP servers). */
    fun grantBundle(agentId: String, bundleId: String) {
        val bundle = RoleBundles.byId(bundleId) ?: return
        update(agentId) { it.copy(role = if (it.isGod) it.role else bundle.role, capabilities = HiveCapabilities(bundle.id, bundle.skills, bundle.connections, bundle.mcpServers)) }
    }

    fun fire(agentId: String) = act { s ->
        val agent = agents.firstOrNull { it.id == agentId } ?: return@act
        if (agent.isGod) return@act
        withContext(Dispatchers.Main) { TerminalSessions.close(key(agentId)) }
        agents = agents - agent
        queue = queue.filterNot { it.agentId == agentId }
        s.saveQueue(queue)
        s.saveAgents(agents)
        meter.forget(agentId)
        s.log("archive", "${agent.name} left the floor (memory kept)", to = agentId)
    }

    fun start(agentId: String, restart: Boolean = false) = act { s ->
        val agent = agents.firstOrNull { it.id == agentId } ?: return@act
        if (!restart && TerminalSessions.isRunning(key(agentId))) return@act
        startLocked(s, agent)
    }

    fun startAll() = act { s ->
        agents.filterNot { it.paused || TerminalSessions.isRunning(key(it.id)) }.forEach { a ->
            runCatching { startLocked(s, a) }.onFailure { host.toast("${a.name}: ${it.message ?: "failed to start"}") }
        }
    }

    private suspend fun startLocked(s: HiveStore, agent: HiveAgent) {
        if (!host.isInstalled(agent.cli)) {
            s.log("spawn", "${agent.name} can't start: ${agent.cli.title} is not installed", to = agent.id)
            host.toast("${agent.name} needs ${agent.cli.title} — install it from the Agents tab")
            return
        }
        s.ensureAgentLayout(agent, agents)
        s.writeControl(agent, halted = false)
        if ((breaker.levels[agent.id] ?: BreakerLevel.HEALTHY) == BreakerLevel.STOPPED) {
            breaker = breaker.copy(levels = breaker.levels - agent.id, reasons = breaker.reasons - agent.id)
            s.saveBreaker(breaker)
        }
        val root = s.guestDir
        val env = mapOf(
            "AGENT_ID" to agent.id,
            "AGENT_NAME" to agent.name,
            "HIVE_ROOT" to root,
            "AGENT_DIR" to s.guestAgentDir(agent.id),
        ) + host.connectionEnv(agent.capabilities.connections)
        val resume = agent.launched && host.transcriptFile(agent.sessionId) != null
        val args = if (agent.cli == AgentKind.CLAUDE_CODE) buildList {
            if (resume) { add("--resume"); add(agent.sessionId) } else { add("--session-id"); add(agent.sessionId) }
            add("--settings"); add("${s.guestAgentDir(agent.id)}/settings.json")
            add("--append-system-prompt"); add(HiveTemplates.injectedPrompt(agent, root, godName()))
            if (agent.model.isNotBlank()) { add("--model"); add(agent.model) }
            s.writeMcpConfig(agent, guestRoot)?.let { add("--mcp-config"); add(it) }
            if (host.autoApproveTools()) { add("--permission-mode"); add("acceptEdits") }
        } else emptyList()
        val launch = host.launch(agent, args, env)
        spawnedWall[agent.id] = System.currentTimeMillis()
        // TerminalSessions sizes new sessions itself, so the process spawns even with no room open.
        withContext(Dispatchers.Main) { TerminalSessions.restart(context, key(agent.id), agent.name, launch) }
        spawnedAt[agent.id] = SystemClock.uptimeMillis()
        announced.remove(agent.id)
        if (!agent.launched) {
            agents = agents.map { if (it.id == agent.id) it.copy(launched = true) else it }
            s.saveAgents(agents)
        }
        when {
            agent.isGod && !resume -> enqueueLocked(s, QueuedSend(UUID.randomUUID().toString(), agent.id, HiveTemplates.godOrientation(godName()), System.currentTimeMillis(), origin = "boot"))
            agent.cli != AgentKind.CLAUDE_CODE && !agent.launched ->
                enqueueLocked(s, QueuedSend(UUID.randomUUID().toString(), agent.id, HiveTemplates.seedPrompt(agent, root, godName()), System.currentTimeMillis(), origin = "boot"))
        }
        s.log("spawn", "${agent.name} ${if (resume) "resumed" else "started"} (${agent.cli.title})", to = agent.id)
    }

    private fun godName() = agents.firstOrNull { it.isGod }?.name ?: HiveTemplates.DEFAULT_GOD_NAME

    /** Kills the agent's process. Claude sessions resume their conversation on next start. */
    fun stop(agentId: String) = act { s ->
        withContext(Dispatchers.Main) { TerminalSessions.close(key(agentId)) }
        s.log("control", "${agents.firstOrNull { it.id == agentId }?.name ?: agentId} stopped", from = "human", to = agentId)
    }

    /** Clean halt at the agent's next hook (session kept for --resume). */
    fun halt(agentId: String) = act { s ->
        val agent = agents.firstOrNull { it.id == agentId } ?: return@act
        s.writeControl(agent, halted = true)
        s.log("control", "${agent.name} halted", from = "human", to = agentId)
    }

    fun setFloorHalt(halted: Boolean) = act { s ->
        breaker = breaker.copy(floorHalted = halted)
        s.saveBreaker(breaker)
        s.writeFloorControl(halted)
        s.log("control", if (halted) "Floor halted" else "Floor resumed", from = "human")
    }

    fun updateBreaker(transform: (BreakerState) -> BreakerState) = act { s ->
        breaker = transform(breaker)
        s.saveBreaker(breaker)
    }

    /** Clears an agent's breaker level (and the god's cap stop) so it can run again. */
    fun resetBreaker(agentId: String) = act { s ->
        breaker = breaker.copy(levels = breaker.levels - agentId, reasons = breaker.reasons - agentId)
        s.saveBreaker(breaker)
        agents.firstOrNull { it.id == agentId }?.let { s.writeControl(it, halted = false) }
        s.log("breaker", "reset by the human", from = "human", to = agentId)
    }

    // ---- messaging ----------------------------------------------------------------------

    /** Queues [text] to be typed into the agent's terminal when it is idle. */
    fun enqueue(agentId: String, text: String) = act { s ->
        enqueueLocked(s, QueuedSend(UUID.randomUUID().toString(), agentId, text, System.currentTimeMillis()))
    }

    fun removeQueued(id: String) = act { s -> queue = queue.filterNot { it.id == id }; s.saveQueue(queue) }

    fun clearQueue(agentId: String) = act { s -> queue = queue.filterNot { it.agentId == agentId }; s.saveQueue(queue) }

    /** "Send now": moves the item to the front and lets it bypass the auto-delivery pause. */
    fun sendNow(id: String) = act { s ->
        val item = queue.firstOrNull { it.id == id } ?: return@act
        val others = queue.filterNot { it.id == id }
        val firstOfAgent = others.indexOfFirst { it.agentId == item.agentId }.let { if (it < 0) others.size else it }
        queue = others.toMutableList().apply { add(firstOfAgent, item.copy(manual = true)) }
        s.saveQueue(queue)
    }

    /** Reorders one agent's queue by moving [id] up (-1) or down (+1). */
    fun moveQueued(id: String, delta: Int) = act { s ->
        val item = queue.firstOrNull { it.id == id } ?: return@act
        val mine = queue.filter { it.agentId == item.agentId }.toMutableList()
        val idx = mine.indexOf(item)
        val target = (idx + delta).coerceIn(0, mine.lastIndex)
        if (target == idx) return@act
        mine.removeAt(idx)
        mine.add(target, item)
        val it = mine.iterator()
        queue = queue.map { q -> if (q.agentId == item.agentId) it.next() else q }
        s.saveQueue(queue)
    }

    /** Steers without interrupting: injected into the agent's context at its next prompt or tool call. */
    fun steer(agentId: String, text: String) = act { s ->
        s.addSteer(agentId, text)
        s.log("steer", text.take(80), from = "human", to = agentId)
    }

    /** A hive message from the human, delivered to the agent's inbox. */
    fun mail(to: String, subject: String, body: String) = act { s ->
        sendFrom(s, "human", to, HiveAct.REQUEST, subject.ifBlank { body.lineSequence().first().take(60) }, body)
    }

    /** Answers an ASK ME card: records it on the card and mails god, like munder's AskMeTab. */
    fun answer(taskId: String, answer: String) = act { s ->
        val task = s.loadTasks().firstOrNull { it.id == taskId } ?: return@act
        val open = task.openQuestion ?: return@act
        val now = Instant.now().toString()
        s.editTasks { list ->
            list.firstOrNull { it.optString("id") == taskId }?.let { o ->
                val qa = o.optJSONArray("humanQA") ?: return@let
                for (i in qa.length() - 1 downTo 0) {
                    val e = qa.optJSONObject(i) ?: continue
                    if (e.optString("a").isBlank() && e.optString("dismissedAt").isBlank() && e.optString("q").isNotBlank()) {
                        e.put("a", answer.trim()).put("answeredAt", now)
                        break
                    }
                }
            }
        }
        sendFrom(s, "human", HiveTemplates.GOD_ID, HiveAct.INFORM, "HUMAN ANSWER on task \"${task.title}\"", HiveTemplates.humanAnswer(task, open.q, answer.trim()))
    }

    fun dismissAsk(taskId: String) = act { s ->
        val now = Instant.now().toString()
        s.editTasks { list ->
            list.firstOrNull { it.optString("id") == taskId }?.optJSONArray("humanQA")?.let { qa ->
                for (i in qa.length() - 1 downTo 0) {
                    val e = qa.optJSONObject(i) ?: continue
                    if (e.optString("a").isBlank() && e.optString("dismissedAt").isBlank()) { e.put("dismissedAt", now); break }
                }
            }
        }
    }

    // ---- tasks ---------------------------------------------------------------------------

    /**
     * Adds a card from an integration (Linear, GitHub, …) to `.hive/tasks.json`.
     * It lands in To do, unassigned and undispatched; [source] is kept as the card's
     * `origin` and [externalId] as `externalId` so re-imports can be detected.
     */
    fun importTask(title: String, description: String, source: String, externalId: String? = null) = act { s ->
        if (externalId != null && s.loadTasks().any { it.raw.optString("externalId") == externalId }) return@act
        createLocked(s, title, description, assignee = null, dispatch = false, origin = source, externalId = externalId)
    }

    fun createTask(title: String, description: String, assignee: String?, dispatch: Boolean) = act { s ->
        createLocked(s, title, description, assignee, dispatch, origin = "human", externalId = null)
    }

    private fun createLocked(s: HiveStore, title: String, description: String, assignee: String?, dispatch: Boolean, origin: String, externalId: String?) {
        val id = s.nextTaskId()
        s.editTasks { list ->
            list += JSONObject()
                .put("id", id).put("title", title.trim()).put("description", description.trim())
                .put("assignee", assignee ?: JSONObject.NULL).put("status", "todo")
                .put("dependsOn", JSONArray()).put("priority", 3).put("createdAt", Instant.now().toString())
                .put("humanQA", JSONArray()).put("origin", origin)
                .apply { externalId?.let { put("externalId", it) } }
        }
        s.log("tasks", "$id created: $title", from = origin, to = assignee)
        if (dispatch) {
            val to = assignee ?: HiveTemplates.GOD_ID
            val body = if (assignee == null) {
                "New card $id from the human: \"$title\".\n\n$description\n\nDecompose if needed, pick owners from the live roster, set assignee on the card and dispatch with a 4-part contract."
            } else "You own card $id: \"$title\".\n\n$description\n\nSet the card to doing in tasks.json, do the work, then set it done and send god a done message."
            sendFrom(s, "human", to, HiveAct.REQUEST, "$id: $title", body)
        }
    }

    fun moveTask(taskId: String, column: TaskColumn) = act { s ->
        s.editTasks { list -> list.firstOrNull { it.optString("id") == taskId }?.put("status", column.wire) }
        s.log("tasks", "$taskId → ${column.title}", from = "human")
    }

    fun assignTask(taskId: String, assignee: String?) = act { s ->
        s.editTasks { list -> list.firstOrNull { it.optString("id") == taskId }?.put("assignee", assignee ?: JSONObject.NULL) }
        s.log("tasks", "$taskId assigned to ${assignee ?: "nobody"}", from = "human", to = assignee)
    }

    fun deleteTask(taskId: String) = act { s ->
        s.editTasks { list -> list.removeAll { it.optString("id") == taskId } }
        s.log("tasks", "$taskId dismissed", from = "human")
    }

    // ---- automations & inbound ------------------------------------------------------------

    fun saveAutomation(item: HiveAutomation) = act { s ->
        automations = if (automations.any { it.id == item.id }) automations.map { if (it.id == item.id) item else it } else automations + item
        s.saveAutomations(automations)
    }

    fun deleteAutomation(id: String) = act { s ->
        automations = automations.filterNot { it.id == id }
        s.saveAutomations(automations)
    }

    fun runAutomationNow(id: String) = act { s ->
        val a = automations.firstOrNull { it.id == id } ?: return@act
        if (a.kind == AutomationKind.MISSION) sendFrom(s, "scheduler", a.target, HiveAct.REQUEST, a.title, a.body)
        automations = automations.map { if (it.id == id) it.copy(lastRunAt = System.currentTimeMillis()) else it }
        s.saveAutomations(automations)
    }

    fun decideExternal(id: String, approve: Boolean) = act { s ->
        val item = external.firstOrNull { it.id == id } ?: return@act
        external = external.map { if (it.id == id) it.copy(decision = if (approve) ExternalDecision.APPROVED else ExternalDecision.REJECTED) else it }
        s.saveExternal(external)
        if (approve) sendFrom(s, "webhook", item.routedTo ?: HiveTemplates.GOD_ID, HiveAct.REQUEST, "[webhook] ${item.title}", item.body)
    }

    // ---- reads for the UI ---------------------------------------------------------------------

    suspend fun searchMemory(query: String): HiveMemory.Results = withContext(Dispatchers.IO) {
        val s = store ?: return@withContext HiveMemory.Results(emptyList(), emptyList(), emptyList())
        val snap = _snapshot.value
        val memories = snap.agents.associate { it.id to s.memory(it.id) }
        HiveMemory.search(query, HiveMemory.documents(snap, memories, s.archivedTasks(200)))
    }

    suspend fun memoryOf(agentId: String): String = withContext(Dispatchers.IO) { store?.memory(agentId).orEmpty() }

    fun terminalKey(agentId: String): String = key(agentId)

    companion object {
        const val TICK_MS = 1_500L
        const val HOP_CAP = 12
        const val IDLE_MS = 10_000L
        const val BOOT_GRACE_MS = 35_000L
        const val FLUSH_COOLDOWN_MS = 4_500L
        const val MESSAGES_PER_MINUTE_CAP = 40
    }
}
