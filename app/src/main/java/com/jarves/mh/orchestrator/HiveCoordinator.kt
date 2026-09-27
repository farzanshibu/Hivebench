package com.jarves.mh.orchestrator

import org.json.JSONArray
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/** Filesystem transaction boundary for HIVE task records and per-agent mailboxes. */
class HiveCoordinator(private val projectRoot: File) {
    private val lock = ReentrantLock()
    private val hiveRoot get() = File(projectRoot, ".hive")

    init { ensureLayout() }

    fun createTask(task: HiveTask): HiveTask = lock.withLock {
        require(task.title.isNotBlank()) { "A task title is required" }
        require(!taskFile(task.id).exists()) { "Task already exists: ${task.id}" }
        atomicWrite(taskFile(task.id), task.toJson().toString(2))
        task
    }

    fun task(taskId: String): HiveTask? = lock.withLock {
        taskFile(taskId).takeIf(File::isFile)?.let { runCatching { org.json.JSONObject(it.readText()).toHiveTask() }.getOrNull() }
    }

    /** Assigns exactly once and atomically appends the matching mailbox item. */
    fun assign(taskId: String, agentId: String, assignedBy: String = "user"): Result<HiveTask> = lock.withLock {
        val current = task(taskId) ?: return Result.failure(IllegalArgumentException("Task not found: $taskId"))
        if (current.assignedTo != null && current.assignedTo != agentId) {
            return Result.failure(IllegalStateException("Task $taskId is already claimed by ${current.assignedTo}"))
        }
        if (current.dependencies.any { task(it)?.status != HiveTaskStatus.DONE }) {
            return Result.failure(IllegalStateException("Task $taskId has unfinished dependencies"))
        }
        val updated = current.copy(assignedTo = agentId, status = HiveTaskStatus.IN_PROGRESS, updatedAt = System.currentTimeMillis())
        atomicWrite(taskFile(taskId), updated.toJson().toString(2))
        val queue = inbox(agentId).filterNot { it.taskId == taskId } + HiveInboxItem(
            taskId, updated.title, updated.priority, assignedBy, instructions = updated.description,
        )
        writeInbox(agentId, queue)
        Result.success(updated)
    }

    fun inbox(agentId: String): List<HiveInboxItem> = lock.withLock { readArray(inboxFile(agentId)) { it.toInboxItem() } }

    /** Agent completion reports are durable first; their task transition is then applied atomically under this coordinator. */
    fun submitResult(result: HiveOutboxItem): Result<HiveTask> = lock.withLock {
        val current = task(result.taskId) ?: return Result.failure(IllegalArgumentException("Task not found: ${result.taskId}"))
        if (current.assignedTo != result.agentId) return Result.failure(IllegalStateException("Only the assigned agent may report this task"))
        writeOutbox(result.agentId, outbox(result.agentId) + result)
        val status = if (result.reviewTargetAgentId != null) HiveTaskStatus.REVIEW else result.status
        val updated = current.copy(status = status, artifacts = result.filesChanged, resultSummary = result.summary,
            reviewRequestedFrom = result.reviewTargetAgentId, updatedAt = System.currentTimeMillis())
        atomicWrite(taskFile(current.id), updated.toJson().toString(2))
        if (result.reviewTargetAgentId != null) createReviewTask(updated, result)
        Result.success(updated)
    }

    fun outbox(agentId: String): List<HiveOutboxItem> = lock.withLock { readArray(outboxFile(agentId)) { it.toOutboxItem() } }

    private fun createReviewTask(parent: HiveTask, result: HiveOutboxItem) {
        val target = checkNotNull(result.reviewTargetAgentId)
        val review = HiveTask(title = "Review: ${parent.title}", description = result.reviewFocus ?: result.summary,
            creator = result.agentId, priority = parent.priority, assignedTo = target, dependencies = listOf(parent.id))
        atomicWrite(taskFile(review.id), review.toJson().toString(2))
        // Review can begin while its parent is in REVIEW, so this deliberately bypasses dependency checking.
        writeInbox(target, inbox(target) + HiveInboxItem(review.id, review.title, review.priority, result.agentId, instructions = review.description))
    }

    private fun ensureLayout() { listOf(hiveRoot, File(hiveRoot, "tasks"), File(hiveRoot, "inbox"), File(hiveRoot, "outbox")).forEach(File::mkdirs) }
    private fun taskFile(id: String) = hiveRoot.hiveFile("tasks", "$id.json")
    private fun inboxFile(id: String) = hiveRoot.hiveFile("inbox", "$id.json")
    private fun outboxFile(id: String) = hiveRoot.hiveFile("outbox", "$id.json")
    private fun writeInbox(id: String, items: List<HiveInboxItem>) = writeArray(inboxFile(id), items.map(HiveInboxItem::toJson))
    private fun writeOutbox(id: String, items: List<HiveOutboxItem>) = writeArray(outboxFile(id), items.map(HiveOutboxItem::toJson))
    private fun <T> readArray(file: File, mapper: (org.json.JSONObject) -> T): List<T> = runCatching {
        val array = if (file.isFile) JSONArray(file.readText()) else JSONArray()
        List(array.length()) { mapper(array.getJSONObject(it)) }
    }.getOrDefault(emptyList())
    private fun writeArray(file: File, items: List<org.json.JSONObject>) = atomicWrite(file, JSONArray(items).toString(2))
    private fun atomicWrite(file: File, content: String) {
        file.parentFile?.mkdirs()
        val pending = File(file.parentFile, ".${file.name}.tmp")
        pending.writeText(content)
        if (!pending.renameTo(file)) { pending.delete(); throw IllegalStateException("Could not atomically write ${file.name}") }
    }
}
