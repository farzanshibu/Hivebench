package com.hivebench.app.hive

import com.hivebench.app.model.AgentKind
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.util.UUID

/**
 * On-disk hive of one project, at `<project>/.hive/` (guest: `<guestRoot>/.hive`).
 *
 * Single-writer-per-file: agents only write inside their own `agents/<id>/`
 * (outbox, memory, status) and the ledger request folder; this process is the
 * only writer of registry, inboxes, tasks.json and the logs. Every write goes
 * through temp-file + rename so a reader never sees half a file.
 */
class HiveStore(val projectRoot: File, val guestRoot: String) {
    val dir = File(projectRoot, ".hive")
    val guestDir = "$guestRoot/.hive"
    private val agentsDir = File(dir, "agents")
    private val registryFile = File(dir, "registry.json")
    private val tasksFile = File(dir, "tasks.json")
    private val boardFile = File(dir, "board.md")
    private val logFile = File(dir, "log.jsonl")
    private val messagesFile = File(dir, "messages.jsonl")
    private val tasksArchiveFile = File(dir, "tasks-archive.json")
    private val externalFile = File(dir, "external.json")
    private val automationsFile = File(dir, "automations.json")
    private val queueFile = File(dir, "queue.json")
    private val stateFile = File(dir, "state.json")

    fun agentDir(id: String) = File(agentsDir, id)
    fun inboxDir(id: String) = File(agentDir(id), "inbox")
    fun outboxDir(id: String) = File(agentDir(id), "outbox")
    fun guestAgentDir(id: String) = "$guestDir/agents/$id"

    // ---- layout ----------------------------------------------------------------

    fun ensureLayout() {
        listOf(dir, agentsDir, File(dir, "bin")).forEach { it.mkdirs() }
        atomicWrite(File(dir, "PROTOCOL.md"), HiveTemplates.protocol(guestDir))
        atomicWrite(File(dir, "bin/hive-hook.js"), HiveTemplates.hookScript())
        if (!boardFile.exists()) atomicWrite(boardFile, HiveTemplates.initialBoard())
        if (!tasksFile.exists()) atomicWrite(tasksFile, JSONObject().put("tasks", JSONArray()).toString(2))
        writeSkills()
        if (!logFile.exists()) logFile.createNewFile()
        if (!messagesFile.exists()) messagesFile.createNewFile()
        excludeFromGit()
    }

    /** Keeps `.hive/` out of the user's repo without touching tracked files. */
    private fun excludeFromGit() {
        val info = File(projectRoot, ".git/info")
        if (!File(projectRoot, ".git").isDirectory) return
        info.mkdirs()
        val exclude = File(info, "exclude")
        val text = exclude.takeIf { it.isFile }?.readText().orEmpty()
        if (text.lines().none { it.trim() == ".hive/" }) {
            exclude.writeText(text.trimEnd() + (if (text.isBlank()) "" else "\n") + ".hive/\n")
        }
    }

    fun ensureAgentLayout(agent: HiveAgent, roster: List<HiveAgent>) {
        val base = agentDir(agent.id)
        listOf(base, inboxDir(agent.id), File(inboxDir(agent.id), ".done"), outboxDir(agent.id), File(outboxDir(agent.id), ".sent")).forEach { it.mkdirs() }
        atomicWrite(File(base, "identity.md"), HiveTemplates.identity(agent, roster, guestDir))
        val memory = File(base, "memory.md")
        if (!memory.exists()) atomicWrite(memory, HiveTemplates.initialMemory(agent))
        atomicWrite(File(base, "settings.json"), HiveTemplates.claudeSettings(agent, guestDir))
    }

    // ---- registry ----------------------------------------------------------------

    fun loadAgents(): List<HiveAgent> {
        val json = readJson(registryFile) ?: return emptyList()
        val array = json.optJSONArray("agents") ?: return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            val o = array.optJSONObject(i) ?: return@mapNotNull null
            val caps = o.optJSONObject("capabilities") ?: JSONObject()
            HiveAgent(
                id = o.optString("id").ifBlank { return@mapNotNull null },
                name = o.optString("name"),
                role = o.optString("role"),
                cli = AgentKind.entries.firstOrNull { it.stableId == o.optString("cli") } ?: AgentKind.CLAUDE_CODE,
                model = o.optString("model"),
                isGod = o.optBoolean("isGod"),
                capabilities = HiveCapabilities(
                    bundle = caps.optString("bundle").ifBlank { null },
                    skills = caps.optJSONArray("skills").strings(),
                    connections = caps.optJSONArray("connections").strings(),
                    mcpServers = caps.optJSONArray("mcpServers").strings(),
                ),
                sessionId = o.optString("sessionId").ifBlank { UUID.randomUUID().toString() },
                launched = o.optBoolean("launched"),
                paused = o.optBoolean("paused"),
                onHold = o.optBoolean("onHold"),
                autoDeliveryPaused = o.optBoolean("autoDeliveryPaused"),
                goal = o.optString("goal"),
                tokenCap = o.optLong("tokenCap"),
                hiredAt = o.optLong("hiredAt", System.currentTimeMillis()),
            )
        }
    }

    fun saveAgents(agents: List<HiveAgent>) {
        val array = JSONArray()
        agents.forEach { a ->
            array.put(
                JSONObject()
                    .put("id", a.id)
                    .put("name", a.name)
                    .put("role", a.role)
                    .put("cli", a.cli.stableId)
                    .put("model", a.model)
                    .put("isGod", a.isGod)
                    .put(
                        "capabilities",
                        JSONObject()
                            .put("bundle", a.capabilities.bundle ?: "")
                            .put("skills", JSONArray(a.capabilities.skills))
                            .put("connections", JSONArray(a.capabilities.connections))
                            .put("mcpServers", JSONArray(a.capabilities.mcpServers)),
                    )
                    .put("sessionId", a.sessionId)
                    .put("launched", a.launched)
                    .put("paused", a.paused)
                    .put("onHold", a.onHold)
                    .put("autoDeliveryPaused", a.autoDeliveryPaused)
                    .put("goal", a.goal)
                    .put("tokenCap", a.tokenCap)
                    .put("hiredAt", a.hiredAt),
            )
        }
        atomicWrite(registryFile, JSONObject().put("agents", array).toString(2))
    }

    // ---- breaker / engine state ------------------------------------------------------

    fun loadBreaker(): BreakerState {
        val o = readJson(stateFile)?.optJSONObject("breaker") ?: return BreakerState()
        val levels = o.optJSONObject("levels")
        val reasons = o.optJSONObject("reasons")
        return BreakerState(
            enabled = o.optBoolean("enabled", true),
            hardStop = o.optBoolean("hardStop"),
            godCapUsd = o.optDouble("godCapUsd", 5.0),
            floorCapUsd = o.optDouble("floorCapUsd", 0.0),
            repeatedToolLimit = o.optInt("repeatedToolLimit", 8),
            errorStormLimit = o.optInt("errorStormLimit", 5),
            tokenVelocityPerMin = o.optLong("tokenVelocityPerMin", 60_000),
            levels = levels?.keys()?.asSequence()?.associateWith { k ->
                runCatching { BreakerLevel.valueOf(levels.optString(k)) }.getOrDefault(BreakerLevel.HEALTHY)
            }.orEmpty(),
            reasons = reasons?.keys()?.asSequence()?.associateWith { reasons.optString(it) }.orEmpty(),
            floorHalted = o.optBoolean("floorHalted"),
        )
    }

    fun saveBreaker(b: BreakerState) {
        val root = readJson(stateFile) ?: JSONObject()
        root.put(
            "breaker",
            JSONObject()
                .put("enabled", b.enabled)
                .put("hardStop", b.hardStop)
                .put("godCapUsd", b.godCapUsd)
                .put("floorCapUsd", b.floorCapUsd)
                .put("repeatedToolLimit", b.repeatedToolLimit)
                .put("errorStormLimit", b.errorStormLimit)
                .put("tokenVelocityPerMin", b.tokenVelocityPerMin)
                .put("levels", JSONObject(b.levels.mapValues { it.value.name }))
                .put("reasons", JSONObject(b.reasons))
                .put("floorHalted", b.floorHalted),
        )
        atomicWrite(stateFile, root.toString(2))
    }

    fun loadDoneSince(): Map<String, Long> {
        val o = readJson(stateFile)?.optJSONObject("doneSince") ?: return emptyMap()
        return o.keys().asSequence().associateWith { o.optLong(it) }
    }

    fun saveDoneSince(map: Map<String, Long>) {
        val root = readJson(stateFile) ?: JSONObject()
        root.put("doneSince", JSONObject(map))
        atomicWrite(stateFile, root.toString(2))
    }

    // ---- mailboxes -------------------------------------------------------------------

    /** Messages an agent has dropped in its outbox, oldest first. Malformed files are quarantined. */
    fun drainOutbox(agentId: String): List<Pair<File, HiveMessage?>> {
        val files = outboxDir(agentId).listFiles { f -> f.isFile && f.name.endsWith(".json") && !f.name.startsWith(".") }
            ?: return emptyList()
        return files.sortedBy { it.name }.map { f ->
            f to runCatching { parseMessage(JSONObject(f.readText()), agentId) }.getOrNull()
        }
    }

    fun markSent(file: File) {
        val sent = File(file.parentFile, ".sent").apply { mkdirs() }
        if (!file.renameTo(File(sent, file.name))) file.delete()
    }

    fun deliver(to: String, message: HiveMessage) {
        val inbox = inboxDir(to).apply { mkdirs() }
        atomicWrite(File(inbox, "${message.id}.json"), messageJson(message).toString(2))
    }

    fun unreadCount(agentId: String): Int =
        inboxDir(agentId).listFiles { f -> f.isFile && f.name.endsWith(".json") }?.size ?: 0

    fun unread(agentId: String): List<HiveMessage> =
        inboxDir(agentId).listFiles { f -> f.isFile && f.name.endsWith(".json") }
            ?.sortedBy { it.name }
            ?.mapNotNull { f -> runCatching { parseMessage(JSONObject(f.readText()), "?") }.getOrNull() }
            .orEmpty()

    fun appendMessage(message: HiveMessage) = appendLine(messagesFile, messageJson(message).put("deliveredAt", message.deliveredAt))

    fun recentMessages(limit: Int = 400): List<HiveMessage> = tailLines(messagesFile, limit).mapNotNull { line ->
        runCatching {
            val o = JSONObject(line)
            parseMessage(o, o.optString("from")).copy(deliveredAt = o.optLong("deliveredAt"))
        }.getOrNull()
    }

    // ---- log -----------------------------------------------------------------------

    fun log(type: String, summary: String, from: String? = null, to: String? = null) {
        appendLine(
            logFile,
            JSONObject().put("at", System.currentTimeMillis()).put("type", type).put("summary", summary)
                .apply { from?.let { put("from", it) }; to?.let { put("to", it) } },
        )
    }

    fun recentLog(limit: Int = 300): List<HiveLogEvent> = tailLines(logFile, limit).mapNotNull { line ->
        runCatching {
            val o = JSONObject(line)
            HiveLogEvent(o.optLong("at"), o.optString("type"), o.optString("from").ifBlank { null }, o.optString("to").ifBlank { null }, o.optString("summary"))
        }.getOrNull()
    }.asReversed()

    // ---- status / board ---------------------------------------------------------------

    fun status(agentId: String): HiveAgentStatus {
        val o = readJson(File(agentDir(agentId), "status.json")) ?: return HiveAgentStatus()
        return HiveAgentStatus(
            state = o.optString("state", "idle"),
            doing = o.optString("doing"),
            tool = o.optString("tool").ifBlank { null },
            at = o.optLong("at"),
        )
    }

    fun board(): String = boardFile.takeIf { it.isFile }?.readText().orEmpty()

    fun memory(agentId: String): String = File(agentDir(agentId), "memory.md").takeIf { it.isFile }?.readText().orEmpty()

    // ---- tasks -----------------------------------------------------------------------
    // Agents (god above all) edit tasks.json directly with their file tools, so
    // every rewrite here is read-modify-write and preserves fields it does not know.

    fun loadTasks(): List<HiveTask> {
        val json = readJson(tasksFile) ?: return emptyList()
        val array = json.optJSONArray("tasks") ?: return emptyList()
        return (0 until array.length()).mapIndexedNotNull { index, i ->
            val o = array.optJSONObject(i) ?: return@mapIndexedNotNull null
            val qa = o.optJSONArray("humanQA")
            HiveTask(
                id = o.optString("id").ifBlank { "t-" + (o.optString("title") + "|" + o.optString("createdAt") + "|" + index).hashCode().toUInt().toString(36) },
                title = o.optString("title").ifBlank { "(untitled)" },
                description = o.optString("description").ifBlank { o.optString("spec") },
                assignee = (o.optString("assignee").ifBlank { o.optString("owner") }).ifBlank { null },
                column = TaskColumn.parse(o.optString("status")),
                dependsOn = o.optJSONArray("dependsOn").strings(),
                priority = o.optInt("priority", 3),
                createdAt = o.optString("createdAt"),
                humanQA = if (qa == null) emptyList() else (0 until qa.length()).mapNotNull { k ->
                    val e = qa.optJSONObject(k) ?: return@mapNotNull null
                    HumanQA(
                        q = e.optString("q"),
                        a = e.optString("a").ifBlank { null },
                        askedAt = e.optString("askedAt").ifBlank { null },
                        answeredAt = e.optString("answeredAt").ifBlank { null },
                        dismissedAt = e.optString("dismissedAt").ifBlank { null },
                    )
                },
                result = o.optString("result").ifBlank { null },
                raw = o,
            )
        }
    }

    /** Applies [edit] to the on-disk ledger (fresh read, so agent edits are never lost). */
    @Synchronized
    fun editTasks(edit: (MutableList<JSONObject>) -> Unit) {
        val root = readJson(tasksFile) ?: JSONObject()
        val array = root.optJSONArray("tasks") ?: JSONArray()
        val list = (0 until array.length()).mapNotNull { array.optJSONObject(it) }.toMutableList()
        edit(list)
        val out = JSONArray()
        list.forEach(out::put)
        root.put("tasks", out)
        atomicWrite(tasksFile, root.toString(2))
    }

    /** Next `bmt-<n>` id: one past the highest number already on the ledger or in the archive. */
    fun nextTaskId(prefix: String = "bmt"): String {
        val re = Regex("^$prefix-(\\d+)$")
        val ids = loadTasks().map { it.id } + (readJson(tasksArchiveFile)?.optJSONArray("tasks")?.let { a ->
            (0 until a.length()).mapNotNull { a.optJSONObject(it)?.optString("id") }
        } ?: emptyList())
        val max = ids.mapNotNull { re.matchEntire(it)?.groupValues?.get(1)?.toIntOrNull() }.maxOrNull() ?: 0
        return "$prefix-${max + 1}"
    }

    /** Moves the given done cards off the board into `tasks-archive.json`. */
    fun archiveTasks(ids: Set<String>) {
        if (ids.isEmpty()) return
        val moved = mutableListOf<JSONObject>()
        editTasks { list ->
            val it = list.iterator()
            while (it.hasNext()) {
                val o = it.next()
                if (o.optString("id") in ids) { moved += o; it.remove() }
            }
        }
        if (moved.isEmpty()) return
        val archive = readJson(tasksArchiveFile) ?: JSONObject()
        val arr = archive.optJSONArray("tasks") ?: JSONArray()
        moved.forEach { arr.put(it.put("archivedAt", Instant.now().toString())) }
        archive.put("tasks", arr)
        atomicWrite(tasksArchiveFile, archive.toString(2))
    }

    fun archivedTasks(limit: Int = 50): List<JSONObject> {
        val arr = readJson(tasksArchiveFile)?.optJSONArray("tasks") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }.takeLast(limit).asReversed()
    }

    // ---- control, steer, goal, fleet ---------------------------------------------------

    /** Per-agent `control.json` the hook reads: pause/halt/gated tools. */
    fun writeControl(agent: HiveAgent, halted: Boolean, gatedTools: List<String> = emptyList()) {
        atomicWrite(
            File(agentDir(agent.id), "control.json"),
            JSONObject().put("paused", agent.paused).put("halted", halted).put("gatedTools", JSONArray(gatedTools)).toString(),
        )
    }

    fun writeFloorControl(halted: Boolean) =
        atomicWrite(File(dir, "control.json"), JSONObject().put("halted", halted).toString())

    /** Queues one operator steer; the hook injects it on the agent's next prompt or tool call. */
    fun addSteer(agentId: String, text: String) {
        val steer = File(agentDir(agentId), "steer").apply { mkdirs() }
        val existing = steer.listFiles { f -> f.name.endsWith(".txt") }?.sortedBy { it.name }.orEmpty()
        existing.dropLast(19).forEach { it.delete() } // at most 20 pending, oldest dropped
        atomicWrite(File(steer, "${System.currentTimeMillis()}.txt"), text.trim().take(10_000))
    }

    fun writeGoal(agentId: String, goal: String) = atomicWrite(File(agentDir(agentId), "goal.md"), goal.trim())

    fun writeFleet(fleet: JSONObject) = atomicWrite(File(dir, "fleet.json"), fleet.toString(2))

    fun writeSkills() {
        HiveSkills.all.forEach { skill ->
            atomicWrite(
                File(dir, "skills/${skill.id}/SKILL.md"),
                "---\nname: ${skill.id}\ndescription: ${skill.description}\n---\n\n# ${skill.title}\n\n${skill.body}\n",
            )
        }
    }

    fun writeMcpConfig(agent: HiveAgent, cwd: String): String? {
        val text = HiveTemplates.mcpConfig(agent, cwd) ?: return null
        atomicWrite(File(agentDir(agent.id), "mcp.json"), text)
        return "${guestAgentDir(agent.id)}/mcp.json"
    }

    /** Breaker signals appended by the hook since [offset]; returns new offset. */
    fun readEvents(agentId: String, offset: Long): Pair<Long, List<JSONObject>> {
        val f = File(agentDir(agentId), "events.jsonl")
        if (!f.isFile) return 0L to emptyList()
        if (f.length() < offset) return 0L to emptyList()
        if (f.length() > 1_000_000L) { f.writeText(""); return 0L to emptyList() }
        java.io.RandomAccessFile(f, "r").use { raf ->
            raf.seek(offset)
            val bytes = ByteArray((raf.length() - offset).toInt())
            raf.readFully(bytes)
            val lines = String(bytes).lines().filter { it.isNotBlank() }.mapNotNull { runCatching { JSONObject(it) }.getOrNull() }
            return raf.length() to lines
        }
    }

    // ---- ask-me, external, automations, queue ------------------------------------------

    fun loadExternal(): List<ExternalItem> = readArray(externalFile).map { o ->
        ExternalItem(
            o.optString("id"), o.optString("source"), o.optString("title"), o.optString("body"), o.optLong("createdAt"),
            o.optString("routedTo").ifBlank { null },
            runCatching { ExternalDecision.valueOf(o.optString("decision")) }.getOrDefault(ExternalDecision.PENDING),
        )
    }

    fun saveExternal(items: List<ExternalItem>) = writeArray(externalFile, items.takeLast(200).map { e ->
        JSONObject().put("id", e.id).put("source", e.source).put("title", e.title).put("body", e.body).put("createdAt", e.createdAt)
            .put("routedTo", e.routedTo ?: "").put("decision", e.decision.name)
    })

    fun loadAutomations(): List<HiveAutomation> = readArray(automationsFile).mapNotNull { o ->
        HiveAutomation(
            id = o.optString("id"),
            kind = runCatching { AutomationKind.valueOf(o.optString("kind")) }.getOrDefault(AutomationKind.MISSION),
            title = o.optString("title"),
            body = o.optString("body"),
            target = o.optString("target", "god"),
            intervalMinutes = o.optInt("intervalMinutes", 60),
            dailyMinute = o.optInt("dailyMinute", -1),
            minContextPct = o.optInt("minContextPct", 60),
            token = o.optString("token"),
            autoAllow = o.optBoolean("autoAllow"),
            enabled = o.optBoolean("enabled", true),
            lastRunAt = o.optLong("lastRunAt"),
        )
    }

    fun saveAutomations(items: List<HiveAutomation>) = writeArray(automationsFile, items.map { a ->
        JSONObject().put("id", a.id).put("kind", a.kind.name).put("title", a.title).put("body", a.body).put("target", a.target)
            .put("intervalMinutes", a.intervalMinutes).put("dailyMinute", a.dailyMinute).put("minContextPct", a.minContextPct)
            .put("token", a.token).put("autoAllow", a.autoAllow).put("enabled", a.enabled).put("lastRunAt", a.lastRunAt)
    })

    fun loadQueue(): List<QueuedSend> = readArray(queueFile).map { o ->
        QueuedSend(
            o.optString("id"), o.optString("agentId"), o.optString("text"), o.optLong("createdAt"),
            manual = o.optBoolean("manual"), precondition = o.optString("precondition").ifBlank { null }, origin = o.optString("origin", "human"),
        )
    }

    fun saveQueue(items: List<QueuedSend>) = writeArray(queueFile, items.map { q ->
        JSONObject().put("id", q.id).put("agentId", q.agentId).put("text", q.text).put("createdAt", q.createdAt)
            .put("manual", q.manual).put("precondition", q.precondition ?: "").put("origin", q.origin)
    })

    /** Webhook drop folder: anything written to `.hive/webhooks/<token>/` (json or text) is an inbound event. */
    fun drainWebhooks(token: String): List<String> {
        val folder = File(dir, "webhooks/$token").apply { mkdirs() }
        return folder.listFiles { f -> f.isFile && !f.name.startsWith(".") }?.sortedBy { it.name }?.mapNotNull { f ->
            runCatching { f.readText() }.getOrNull().also { f.delete() }
        }.orEmpty()
    }

    fun forgetAgent(id: String) {
        agentDir(id).deleteRecursively()
    }

    // ---- helpers -------------------------------------------------------------------

    fun newMessageId(): String =
        Instant.now().toString().replace(":", "-").replace(".", "-") + "-" + UUID.randomUUID().toString().take(4)

    fun messageJson(m: HiveMessage): JSONObject = JSONObject()
        .put("id", m.id)
        .put("conversation", m.conversation)
        .put("in_reply_to", m.inReplyTo ?: JSONObject.NULL)
        .put("from", m.from)
        .put("to", m.to)
        .put("act", m.act.wire)
        .put("subject", m.subject)
        .put("body", m.body)
        .put("hops", m.hops)
        .put("requires_reply", m.requiresReply)
        .put("needs_human", m.needsHuman)
        .put("created_at", m.createdAt)

    private fun parseMessage(o: JSONObject, fallbackFrom: String): HiveMessage {
        val act = HiveAct.parse(o.optString("act"))
        return HiveMessage(
            id = o.optString("id").ifBlank { newMessageId() },
            conversation = o.optString("conversation").ifBlank { "conv-" + UUID.randomUUID().toString().take(6) },
            inReplyTo = o.optString("in_reply_to").takeUnless { it.isBlank() || it == "null" },
            from = o.optString("from").ifBlank { fallbackFrom },
            to = o.optString("to").trim(),
            act = act,
            subject = o.optString("subject").take(200),
            body = o.optString("body"),
            hops = o.optInt("hops", 0),
            requiresReply = o.optBoolean("requires_reply", act.obligatesReply),
            needsHuman = o.optBoolean("needs_human", false),
            createdAt = o.optString("created_at").ifBlank { Instant.now().toString() },
        )
    }

    private fun readJson(f: File): JSONObject? = runCatching { if (f.isFile) JSONObject(f.readText()) else null }.getOrNull()

    private fun readArray(f: File): List<JSONObject> = runCatching {
        if (!f.isFile) return emptyList()
        val a = JSONArray(f.readText())
        (0 until a.length()).mapNotNull { a.optJSONObject(it) }
    }.getOrDefault(emptyList())

    private fun writeArray(f: File, items: List<JSONObject>) {
        val a = JSONArray()
        items.forEach(a::put)
        atomicWrite(f, a.toString(2))
    }

    private fun appendLine(f: File, o: JSONObject) {
        f.appendText(o.toString() + "\n")
    }

    private fun tailLines(f: File, limit: Int): List<String> {
        if (!f.isFile) return emptyList()
        // Rotate so the files never grow without bound on a phone.
        if (f.length() > 2_000_000L) {
            val keep = f.readLines().takeLast(limit)
            atomicWrite(f, keep.joinToString("\n", postfix = "\n"))
            return keep
        }
        return f.readLines().filter { it.isNotBlank() }.takeLast(limit)
    }

    private fun JSONArray?.strings(): List<String> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optString(it).ifBlank { null } }

    companion object {
        fun atomicWrite(target: File, text: String) {
            target.parentFile?.mkdirs()
            val tmp = File(target.parentFile, ".${target.name}.${System.nanoTime()}.tmp")
            tmp.writeText(text)
            if (!tmp.renameTo(target)) {
                target.writeText(text)
                tmp.delete()
            }
        }
    }
}
