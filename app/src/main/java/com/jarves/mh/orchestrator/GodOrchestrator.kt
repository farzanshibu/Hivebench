package com.jarves.mh.orchestrator

import com.jarves.mh.model.AgentInboxMessage
import com.jarves.mh.model.AgentInstance
import com.jarves.mh.model.AgentMessageType
import com.jarves.mh.model.AgentRole
import com.jarves.mh.model.AgentStatus
import com.jarves.mh.model.AgentTask
import com.jarves.mh.model.CircuitBreakerState
import com.jarves.mh.model.TaskActivityEntry
import com.jarves.mh.model.TaskPriority
import com.jarves.mh.model.TaskStatus
import java.util.UUID

/**
 * Swarm Audit Event recording all orchestrator actions.
 */
data class SwarmAuditEvent(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // "DECOMPOSITION", "DELEGATION", "HANDOFF", "CIRCUIT_BREAKER", "STATUS_CHANGE"
    val agentName: String,
    val summary: String,
    val details: String = "",
)

/**
 * GOD Orchestrator - Master Brain of the Munder-Difflin Multi-Agent Swarm.
 * Handles goal decomposition, intelligent task delegation, inter-agent handoffs,
 * circuit breaker safety, and audit logging.
 */
class GodOrchestrator(
    val taskBoardManager: TaskBoardManager,
    val inboxManager: AgentInboxManager,
    val blackboardMemoryStore: BlackboardMemoryStore,
) {
    private val agents = mutableMapOf<String, AgentInstance>()
    private val auditLog = mutableListOf<SwarmAuditEvent>()
    var circuitBreaker: CircuitBreakerState = CircuitBreakerState()
        private set

    init {
        initializeDefaultSwarm()
    }

    private fun initializeDefaultSwarm() {
        val defaultAgents = listOf(
            AgentInstance(
                id = "god-master",
                name = "GOD Orchestrator",
                role = AgentRole.GOD_ORCHESTRATOR,
                status = AgentStatus.IDLE,
                avatarEmoji = "👑",
                quotaLimit = 500_000L,
            ),
            AgentInstance(
                id = "arch-ada",
                name = "Ada Architect",
                role = AgentRole.ARCHITECT,
                status = AgentStatus.IDLE,
                avatarEmoji = "📐",
                quotaLimit = 150_000L,
            ),
            AgentInstance(
                id = "coder-devon",
                name = "Devon Coder",
                role = AgentRole.CODER,
                status = AgentStatus.IDLE,
                avatarEmoji = "⚡",
                quotaLimit = 250_000L,
            ),
            AgentInstance(
                id = "rev-rhea",
                name = "Rhea Reviewer",
                role = AgentRole.REVIEWER,
                status = AgentStatus.IDLE,
                avatarEmoji = "🔍",
                quotaLimit = 100_000L,
            ),
            AgentInstance(
                id = "test-tess",
                name = "Tess Tester",
                role = AgentRole.TESTER,
                status = AgentStatus.IDLE,
                avatarEmoji = "🧪",
                quotaLimit = 100_000L,
            ),
            AgentInstance(
                id = "browser-blink",
                name = "Blink Browser",
                role = AgentRole.BROWSER_AGENT,
                status = AgentStatus.IDLE,
                avatarEmoji = "🌐",
                quotaLimit = 100_000L,
            ),
        )
        defaultAgents.forEach { agents[it.id] = it }
    }

    fun getAllAgents(): List<AgentInstance> = agents.values.toList()

    fun getAgent(agentId: String): AgentInstance? = agents[agentId]

    fun getAuditLog(): List<SwarmAuditEvent> = auditLog.toList()

    /**
     * Decomposes a high-level user goal into structured subtasks with dependencies and roles.
     */
    fun decomposeGoal(goal: String): List<AgentTask> {
        val trimmed = goal.trim()
        if (trimmed.isEmpty()) return emptyList()

        recordAudit(
            type = "DECOMPOSITION",
            agentName = "GOD Orchestrator",
            summary = "Decomposed high-level goal",
            details = trimmed,
        )

        // Generate branch slug for Coder worktree
        val branchSlug = "agent/" + trimmed.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .take(24)
            .trim('-')

        // 1. Architect Task
        val archTask = taskBoardManager.createTask(
            title = "Architect & Design: $trimmed",
            description = "Formulate implementation plan, interface contracts, and update shared blackboard.",
            priority = TaskPriority.HIGH,
            status = TaskStatus.TODO,
            assignedAgentId = "arch-ada",
            author = "GOD Orchestrator",
        )

        // 2. Coder Task (Depends on Architect)
        val coderTask = taskBoardManager.createTask(
            title = "Implement: $trimmed",
            description = "Develop required classes and features in isolated worktree.",
            priority = TaskPriority.HIGH,
            status = TaskStatus.BACKLOG,
            assignedAgentId = "coder-devon",
            dependencyTaskIds = listOf(archTask.id),
            worktreeBranch = branchSlug,
            author = "GOD Orchestrator",
        )

        // 3. Reviewer Task (Depends on Coder)
        val reviewerTask = taskBoardManager.createTask(
            title = "Review Changes: $trimmed",
            description = "Inspect git diff, check architecture compliance and verify security.",
            priority = TaskPriority.MEDIUM,
            status = TaskStatus.BACKLOG,
            assignedAgentId = "rev-rhea",
            dependencyTaskIds = listOf(coderTask.id),
            worktreeBranch = branchSlug,
            author = "GOD Orchestrator",
        )

        // 4. Tester Task (Depends on Reviewer)
        val testerTask = taskBoardManager.createTask(
            title = "Test & Verify: $trimmed",
            description = "Run automated tests, inspect regressions, and confirm build passing.",
            priority = TaskPriority.MEDIUM,
            status = TaskStatus.BACKLOG,
            assignedAgentId = "test-tess",
            dependencyTaskIds = listOf(reviewerTask.id),
            worktreeBranch = branchSlug,
            author = "GOD Orchestrator",
        )

        // Send Inbox Notification to Architect to kick off the pipeline
        inboxManager.sendMessage(
            fromAgentId = "god-master",
            toAgentId = "arch-ada",
            type = AgentMessageType.DELEGATION,
            subject = "New Pipeline: ${archTask.title}",
            content = "You have been assigned to specify the architecture for: \"$trimmed\". Post architectural discoveries to the blackboard.",
            taskId = archTask.id,
        )

        // Set Architect to THINKING
        updateAgentStatus("arch-ada", AgentStatus.THINKING, archTask.id)

        // Record blackboard plan
        blackboardMemoryStore.putEntry(
            key = "active_goal",
            category = "ARCHITECTURE",
            value = trimmed,
            authorAgentId = "god-master",
            authorRole = AgentRole.GOD_ORCHESTRATOR,
        )

        return listOf(archTask, coderTask, reviewerTask, testerTask)
    }

    /**
     * Executes handoff from one agent to the next in sequence.
     */
    fun executeHandoff(
        fromAgentId: String,
        toAgentId: String,
        taskId: String,
        notes: String = "",
    ): Boolean {
        val fromAgent = agents[fromAgentId] ?: return false
        val toAgent = agents[toAgentId] ?: return false
        val task = taskBoardManager.getTask(taskId) ?: return false

        // Send handoff message
        inboxManager.sendMessage(
            fromAgentId = fromAgentId,
            toAgentId = toAgentId,
            type = AgentMessageType.HANDOFF,
            subject = "Handoff: ${task.title}",
            content = "Handoff from ${fromAgent.name} to ${toAgent.name}. Notes: $notes",
            taskId = taskId,
        )

        // Reassign task
        taskBoardManager.assignAgent(taskId, toAgentId, toAgent.name, author = fromAgent.name)

        // Update statuses
        updateAgentStatus(fromAgentId, AgentStatus.IDLE, null)
        updateAgentStatus(toAgentId, AgentStatus.WORKING, taskId)

        recordAudit(
            type = "HANDOFF",
            agentName = fromAgent.name,
            summary = "Handed off task to ${toAgent.name}",
            details = "Task: ${task.title}. Notes: $notes",
        )

        return true
    }

    /**
     * Update an agent's operational status.
     */
    fun updateAgentStatus(agentId: String, newStatus: AgentStatus, taskId: String? = null) {
        val current = agents[agentId] ?: return
        agents[agentId] = current.copy(
            status = newStatus,
            currentTaskId = taskId ?: current.currentTaskId,
            lastActiveTimestamp = System.currentTimeMillis(),
        )

        recordAudit(
            type = "STATUS_CHANGE",
            agentName = current.name,
            summary = "Status updated to ${newStatus.label}",
            details = if (taskId != null) "Active Task ID: $taskId" else "",
        )
    }

    /**
     * Record tokens used for quota tracking.
     */
    fun recordTokenUsage(agentId: String, tokens: Long) {
        val current = agents[agentId] ?: return
        val newTotal = current.tokensUsed + tokens
        agents[agentId] = current.copy(tokensUsed = newTotal)

        if (newTotal >= current.quotaLimit) {
            updateAgentStatus(agentId, AgentStatus.BLOCKED)
            recordAudit(
                type = "CIRCUIT_BREAKER",
                agentName = current.name,
                summary = "Agent quota exceeded (${newTotal} / ${current.quotaLimit})",
                details = "Agent paused to prevent runaway API billing.",
            )
        }
    }

    /**
     * Circuit breaker check to prevent runaway loops or repeated failures.
     */
    fun recordExecutionError(agentId: String, error: String) {
        val current = agents[agentId]
        val consecutive = circuitBreaker.consecutiveErrors + 1
        if (consecutive >= circuitBreaker.maxConsecutiveErrors) {
            tripCircuitBreaker("Circuit breaker tripped: $consecutive consecutive errors. Last error: $error")
        } else {
            circuitBreaker = circuitBreaker.copy(consecutiveErrors = consecutive)
        }

        recordAudit(
            type = "ERROR",
            agentName = current?.name ?: "Unknown",
            summary = "Agent execution error",
            details = error,
        )
    }

    fun recordExecutionSuccess(agentId: String) {
        circuitBreaker = circuitBreaker.copy(consecutiveErrors = 0)
    }

    fun tripCircuitBreaker(reason: String) {
        circuitBreaker = circuitBreaker.copy(
            isTripped = true,
            trippedReason = reason,
            trippedTimestamp = System.currentTimeMillis(),
        )

        // Pause all working agents
        agents.forEach { (id, agent) ->
            if (agent.status == AgentStatus.WORKING || agent.status == AgentStatus.THINKING) {
                agents[id] = agent.copy(status = AgentStatus.BLOCKED)
            }
        }

        recordAudit(
            type = "CIRCUIT_BREAKER",
            agentName = "GOD Orchestrator",
            summary = "CIRCUIT BREAKER TRIPPED",
            details = reason,
        )
    }

    fun resetCircuitBreaker() {
        circuitBreaker = circuitBreaker.copy(
            consecutiveErrors = 0,
            isTripped = false,
            trippedReason = null,
        )
        // Resume agents to idle
        agents.forEach { (id, agent) ->
            if (agent.status == AgentStatus.BLOCKED) {
                agents[id] = agent.copy(status = AgentStatus.IDLE)
            }
        }

        recordAudit(
            type = "CIRCUIT_BREAKER",
            agentName = "GOD Orchestrator",
            summary = "Circuit breaker reset by user",
            details = "All blocked agents restored to IDLE.",
        )
    }

    /**
     * Automated task progression: when a task completes, checks downstream dependents.
     * If all dependencies for a downstream task are satisfied, automatically wakes up the
     * assigned agent, sets status to WORKING, moves task to TODO, and sends an inbox notification.
     */
    fun onTaskCompleted(completedTaskId: String) {
        val allTasks = taskBoardManager.getAllTasks()
        val downstreamTasks = allTasks.filter { it.dependencyTaskIds.contains(completedTaskId) }

        for (downstream in downstreamTasks) {
            val allDepsDone = downstream.dependencyTaskIds.all { depId ->
                allTasks.firstOrNull { it.id == depId }?.status == TaskStatus.DONE
            }
            if (allDepsDone && downstream.status == TaskStatus.BACKLOG) {
                taskBoardManager.updateTaskStatus(downstream.id, TaskStatus.TODO, author = "GOD Orchestrator (Auto-Progression)")
                val assignedAgent = downstream.assignedAgentId?.let { agents[it] }
                if (assignedAgent != null) {
                    updateAgentStatus(assignedAgent.id, AgentStatus.WORKING, downstream.id)
                    inboxManager.sendMessage(
                        fromAgentId = "god-master",
                        toAgentId = assignedAgent.id,
                        type = AgentMessageType.DELEGATION,
                        subject = "Ready to Execute: ${downstream.title}",
                        content = "All prerequisite tasks have been completed. You are unblocked to work on this task.",
                        taskId = downstream.id,
                    )
                }
                recordAudit(
                    type = "AUTO_PROGRESSION",
                    agentName = "GOD Orchestrator",
                    summary = "Unblocked dependent task",
                    details = "Task: ${downstream.title}",
                )
            }
        }
    }

    private val scheduledTasks = mutableListOf<com.jarves.mh.model.ScheduledTask>()

    fun getAllScheduledTasks(): List<com.jarves.mh.model.ScheduledTask> = scheduledTasks.toList()

    fun addScheduledTask(
        title: String,
        description: String = "",
        intervalMinutes: Int = 30,
        isRecurring: Boolean = false,
        targetAgentId: String = "god-master",
    ): com.jarves.mh.model.ScheduledTask {
        val task = com.jarves.mh.model.ScheduledTask(
            title = title,
            description = description,
            intervalMinutes = intervalMinutes,
            isRecurring = isRecurring,
            targetAgentId = targetAgentId,
            nextRunMillis = System.currentTimeMillis() + intervalMinutes * 60 * 1000L,
        )
        scheduledTasks.add(task)
        recordAudit(
            type = "SCHEDULER",
            agentName = "GOD Orchestrator",
            summary = "Registered scheduled task",
            details = "${task.title} (Every ${intervalMinutes}m, Recurring: $isRecurring)",
        )
        return task
    }

    fun removeScheduledTask(id: String): Boolean = scheduledTasks.removeAll { it.id == id }

    fun toggleScheduledTask(id: String): Boolean {
        val index = scheduledTasks.indexOfFirst { it.id == id }
        if (index == -1) return false
        val current = scheduledTasks[index]
        scheduledTasks[index] = current.copy(enabled = !current.enabled)
        return true
    }

    private fun recordAudit(type: String, agentName: String, summary: String, details: String = "") {
        auditLog.add(
            0,
            SwarmAuditEvent(
                type = type,
                agentName = agentName,
                summary = summary,
                details = details,
            ),
        )
    }
}
