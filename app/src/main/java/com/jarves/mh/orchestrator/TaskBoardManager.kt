package com.jarves.mh.orchestrator

import com.jarves.mh.model.AgentTask
import com.jarves.mh.model.TaskActivityEntry
import com.jarves.mh.model.TaskPriority
import com.jarves.mh.model.TaskStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Manages the Jira-like task board: tasks, dependencies, column transitions, and activity logs.
 */
class TaskBoardManager {

    private val tasks = mutableListOf<AgentTask>()

    fun getAllTasks(): List<AgentTask> = tasks.toList()

    fun getTasksByStatus(status: TaskStatus): List<AgentTask> =
        tasks.filter { it.status == status }

    fun getTask(taskId: String): AgentTask? = tasks.firstOrNull { it.id == taskId }

    fun getTasksForAgent(agentId: String): List<AgentTask> =
        tasks.filter { it.assignedAgentId == agentId }

    fun createTask(
        title: String,
        description: String = "",
        priority: TaskPriority = TaskPriority.MEDIUM,
        status: TaskStatus = TaskStatus.TODO,
        assignedAgentId: String? = null,
        dependencyTaskIds: List<String> = emptyList(),
        worktreeBranch: String? = null,
        author: String = "GOD Orchestrator",
    ): AgentTask {
        val initialActivity = TaskActivityEntry(
            author = author,
            action = "Created task",
            detail = "Status: ${status.label}, Priority: ${priority.label}",
        )
        val task = AgentTask(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            priority = priority,
            status = status,
            assignedAgentId = assignedAgentId,
            createdTimestamp = System.currentTimeMillis(),
            updatedTimestamp = System.currentTimeMillis(),
            dependencyTaskIds = dependencyTaskIds,
            worktreeBranch = worktreeBranch,
            activityLog = listOf(initialActivity),
        )
        tasks.add(task)
        return task
    }

    fun updateTaskStatus(
        taskId: String,
        newStatus: TaskStatus,
        author: String = "GOD Orchestrator",
    ): Result<AgentTask> {
        val index = tasks.indexOfFirst { it.id == taskId }
        if (index == -1) return Result.failure(IllegalArgumentException("Task not found: $taskId"))

        val current = tasks[index]
        if (current.status == newStatus) return Result.success(current)

        // Check dependencies when moving to IN_PROGRESS
        if (newStatus == TaskStatus.IN_PROGRESS) {
            val unmet = current.dependencyTaskIds.mapNotNull { depId ->
                tasks.firstOrNull { it.id == depId && it.status != TaskStatus.DONE }
            }
            if (unmet.isNotEmpty()) {
                val depTitles = unmet.joinToString(", ") { it.title }
                return Result.failure(IllegalStateException("Cannot start task: waiting on dependencies: $depTitles"))
            }
        }

        val activity = TaskActivityEntry(
            author = author,
            action = "Moved status",
            detail = "${current.status.label} -> ${newStatus.label}",
        )
        val updated = current.copy(
            status = newStatus,
            updatedTimestamp = System.currentTimeMillis(),
            activityLog = current.activityLog + activity,
        )
        tasks[index] = updated
        return Result.success(updated)
    }

    fun assignAgent(
        taskId: String,
        agentId: String?,
        agentName: String = "Unassigned",
        author: String = "GOD Orchestrator",
    ): AgentTask? {
        val index = tasks.indexOfFirst { it.id == taskId }
        if (index == -1) return null

        val current = tasks[index]
        val activity = TaskActivityEntry(
            author = author,
            action = "Assigned agent",
            detail = "Assigned to $agentName",
        )
        val updated = current.copy(
            assignedAgentId = agentId,
            updatedTimestamp = System.currentTimeMillis(),
            activityLog = current.activityLog + activity,
        )
        tasks[index] = updated
        return updated
    }

    fun setWorktreeBranch(taskId: String, branch: String?): AgentTask? {
        val index = tasks.indexOfFirst { it.id == taskId }
        if (index == -1) return null

        val current = tasks[index]
        val updated = current.copy(
            worktreeBranch = branch,
            updatedTimestamp = System.currentTimeMillis(),
        )
        tasks[index] = updated
        return updated
    }

    fun deleteTask(taskId: String): Boolean {
        return tasks.removeAll { it.id == taskId }
    }

    fun clear() {
        tasks.clear()
    }

    suspend fun saveToFile(projectDir: File) = withContext(Dispatchers.IO) {
        try {
            val dir = File(projectDir, ".pocketdev")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "tasks.json")

            val array = JSONArray()
            tasks.forEach { task ->
                val obj = JSONObject().apply {
                    put("id", task.id)
                    put("title", task.title)
                    put("description", task.description)
                    put("priority", task.priority.name)
                    put("status", task.status.name)
                    if (task.assignedAgentId != null) put("assignedAgentId", task.assignedAgentId)
                    put("createdTimestamp", task.createdTimestamp)
                    put("updatedTimestamp", task.updatedTimestamp)
                    if (task.worktreeBranch != null) put("worktreeBranch", task.worktreeBranch)

                    val depArray = JSONArray()
                    task.dependencyTaskIds.forEach { depArray.put(it) }
                    put("dependencyTaskIds", depArray)

                    val actArray = JSONArray()
                    task.activityLog.forEach { act ->
                        val actObj = JSONObject().apply {
                            put("id", act.id)
                            put("timestamp", act.timestamp)
                            put("author", act.author)
                            put("action", act.action)
                            put("detail", act.detail)
                        }
                        actArray.put(actObj)
                    }
                    put("activityLog", actArray)
                }
                array.put(obj)
            }
            file.writeText(array.toString(2))
        } catch (_: Exception) {
            // Ignore persistence errors gracefully
        }
    }

    suspend fun loadFromFile(projectDir: File) = withContext(Dispatchers.IO) {
        try {
            val file = File(File(projectDir, ".pocketdev"), "tasks.json")
            if (!file.exists()) return@withContext

            val text = file.readText()
            val array = JSONArray(text)
            tasks.clear()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val priorityName = obj.optString("priority", TaskPriority.MEDIUM.name)
                val priority = try { TaskPriority.valueOf(priorityName) } catch (_: Exception) { TaskPriority.MEDIUM }
                val statusName = obj.optString("status", TaskStatus.TODO.name)
                val status = try { TaskStatus.valueOf(statusName) } catch (_: Exception) { TaskStatus.TODO }

                val deps = mutableListOf<String>()
                val depArray = obj.optJSONArray("dependencyTaskIds")
                if (depArray != null) {
                    for (d in 0 until depArray.length()) deps.add(depArray.getString(d))
                }

                val activities = mutableListOf<TaskActivityEntry>()
                val actArray = obj.optJSONArray("activityLog")
                if (actArray != null) {
                    for (a in 0 until actArray.length()) {
                        val actObj = actArray.getJSONObject(a)
                        activities.add(
                            TaskActivityEntry(
                                id = actObj.optString("id", UUID.randomUUID().toString()),
                                timestamp = actObj.optLong("timestamp", System.currentTimeMillis()),
                                author = actObj.optString("author", "Unknown"),
                                action = actObj.optString("action", ""),
                                detail = actObj.optString("detail", ""),
                            ),
                        )
                    }
                }

                val task = AgentTask(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    title = obj.getString("title"),
                    description = obj.optString("description", ""),
                    priority = priority,
                    status = status,
                    assignedAgentId = if (obj.has("assignedAgentId")) obj.getString("assignedAgentId") else null,
                    createdTimestamp = obj.optLong("createdTimestamp", System.currentTimeMillis()),
                    updatedTimestamp = obj.optLong("updatedTimestamp", System.currentTimeMillis()),
                    dependencyTaskIds = deps,
                    worktreeBranch = if (obj.has("worktreeBranch")) obj.getString("worktreeBranch") else null,
                    activityLog = activities,
                )
                tasks.add(task)
            }
        } catch (_: Exception) {
            // Fallback gracefully
        }
    }
}
