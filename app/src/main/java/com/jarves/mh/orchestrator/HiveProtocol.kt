package com.jarves.mh.orchestrator

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Local-first, per-project coordination protocol for autonomous agents.
 *
 * HIVE files deliberately live under the project rather than application storage so a
 * workspace remains inspectable and recoverable when the application is restarted.
 */
enum class HiveTaskStatus { BACKLOG, IN_PROGRESS, REVIEW, DONE, BLOCKED, FAILED }

enum class HivePriority { LOW, MEDIUM, HIGH, URGENT }

data class HiveTask(
    val id: String = "task-${UUID.randomUUID()}",
    val title: String,
    val description: String,
    val status: HiveTaskStatus = HiveTaskStatus.BACKLOG,
    val assignedTo: String? = null,
    val creator: String = "user",
    val priority: HivePriority = HivePriority.MEDIUM,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val branchName: String? = null,
    val worktreePath: String? = null,
    val dependencies: List<String> = emptyList(),
    val artifacts: List<String> = emptyList(),
    val reviewRequestedFrom: String? = null,
    val resultSummary: String? = null,
)

data class HiveInboxItem(
    val taskId: String,
    val title: String,
    val priority: HivePriority,
    val assignedBy: String,
    val timestamp: Long = System.currentTimeMillis(),
    val instructions: String,
)

data class HiveOutboxItem(
    val taskId: String,
    val agentId: String,
    val status: HiveTaskStatus,
    val summary: String,
    val filesChanged: List<String> = emptyList(),
    val reviewTargetAgentId: String? = null,
    val reviewFocus: String? = null,
    val completedAt: Long = System.currentTimeMillis(),
)

internal fun HiveTask.toJson() = JSONObject().apply {
    put("id", id); put("title", title); put("description", description); put("status", status.name)
    put("assignedTo", assignedTo); put("creator", creator); put("priority", priority.name)
    put("createdAt", createdAt); put("updatedAt", updatedAt); put("branchName", branchName)
    put("worktreePath", worktreePath); put("dependencies", JSONArray(dependencies)); put("artifacts", JSONArray(artifacts))
    put("reviewRequestedFrom", reviewRequestedFrom); put("resultSummary", resultSummary)
}

internal fun JSONObject.toHiveTask() = HiveTask(
    id = getString("id"), title = getString("title"), description = optString("description"),
    status = enumValue(optString("status"), HiveTaskStatus.BACKLOG), assignedTo = optString("assignedTo").ifBlank { null },
    creator = optString("creator", "user"), priority = enumValue(optString("priority"), HivePriority.MEDIUM),
    createdAt = optLong("createdAt"), updatedAt = optLong("updatedAt"), branchName = optString("branchName").ifBlank { null },
    worktreePath = optString("worktreePath").ifBlank { null }, dependencies = optStringList("dependencies"),
    artifacts = optStringList("artifacts"), reviewRequestedFrom = optString("reviewRequestedFrom").ifBlank { null },
    resultSummary = optString("resultSummary").ifBlank { null },
)

internal inline fun <reified T : Enum<T>> enumValue(value: String, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: fallback

internal fun JSONObject.optStringList(key: String): List<String> = optJSONArray(key)?.let { array ->
    List(array.length()) { array.optString(it) }
}.orEmpty()

internal fun HiveInboxItem.toJson() = JSONObject().apply {
    put("taskId", taskId); put("title", title); put("priority", priority.name); put("assignedBy", assignedBy)
    put("timestamp", timestamp); put("instructions", instructions)
}

internal fun JSONObject.toInboxItem() = HiveInboxItem(
    taskId = getString("taskId"), title = getString("title"), priority = enumValue(optString("priority"), HivePriority.MEDIUM),
    assignedBy = optString("assignedBy", "system"), timestamp = optLong("timestamp"), instructions = optString("instructions"),
)

internal fun HiveOutboxItem.toJson() = JSONObject().apply {
    put("taskId", taskId); put("agentId", agentId); put("status", status.name); put("summary", summary)
    put("filesChanged", JSONArray(filesChanged)); put("completedAt", completedAt)
    reviewTargetAgentId?.let { target -> put("reviewRequest", JSONObject().put("targetAgent", target).put("focus", reviewFocus)) }
}

internal fun JSONObject.toOutboxItem(): HiveOutboxItem {
    val review = optJSONObject("reviewRequest")
    return HiveOutboxItem(
        taskId = getString("taskId"), agentId = getString("agentId"), status = enumValue(optString("status"), HiveTaskStatus.REVIEW),
        summary = optString("summary"), filesChanged = optStringList("filesChanged"),
        reviewTargetAgentId = review?.optString("targetAgent")?.ifBlank { null }, reviewFocus = review?.optString("focus")?.ifBlank { null },
        completedAt = optLong("completedAt"),
    )
}

internal fun File.hiveFile(vararg parts: String): File = parts.fold(this) { parent, part -> File(parent, part) }
