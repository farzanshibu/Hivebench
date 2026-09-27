package com.jarves.mh.model

import java.util.UUID

/**
 * Roles an agent can assume in the Munder-Difflin multi-agent swarm architecture.
 */
enum class AgentRole(
    val title: String,
    val description: String,
    val defaultEmoji: String,
) {
    GOD_ORCHESTRATOR(
        "GOD Orchestrator",
        "Master brain: decomposes goals, assigns tasks, audits swarm, monitors circuit breakers",
        "👑",
    ),
    ARCHITECT(
        "System Architect",
        "Designs specifications, database schemas, interfaces, and shared blackboard docs",
        "📐",
    ),
    CODER(
        "Core Coder",
        "Implements features, writes unit code, and works in isolated Git worktrees",
        "⚡",
    ),
    REVIEWER(
        "Code Reviewer",
        "Reviews diffs, verifies style and quality, approves/rejects commits and PRs",
        "🔍",
    ),
    TESTER(
        "QA & Test Engineer",
        "Runs test suites, writes test cases, and validates functional requirements",
        "🧪",
    ),
    BROWSER_AGENT(
        "Design & Browser Agent",
        "Inspects DOM elements, verifies layout styling, and automates web flows",
        "🌐",
    ),
}

/**
 * Real-time operational lifecycle status of an agent.
 */
enum class AgentStatus(val label: String) {
    IDLE("Idle"),
    THINKING("Thinking"),
    WORKING("Working"),
    WAITING_FOR_INPUT("Waiting for input"),
    BLOCKED("Blocked"),
    COMPLETED("Completed"),
    FAILED("Failed"),
}

/**
 * An active agent instance inside the swarm.
 */
data class AgentInstance(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val role: AgentRole,
    val status: AgentStatus = AgentStatus.IDLE,
    val avatarEmoji: String = role.defaultEmoji,
    val currentTaskId: String? = null,
    val worktreeBranch: String? = null,
    val tokensUsed: Long = 0L,
    val quotaLimit: Long = 100_000L,
    val lastActiveTimestamp: Long = System.currentTimeMillis(),
    val isAutonomous: Boolean = true,
)

/**
 * Task priority in the Jira-like task board.
 */
enum class TaskPriority(val label: String, val weight: Int) {
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3),
    CRITICAL("Critical", 4),
}

/**
 * Task status column in the Jira-like task board.
 */
enum class TaskStatus(val label: String) {
    BACKLOG("Backlog"),
    TODO("To Do"),
    IN_PROGRESS("In Progress"),
    IN_REVIEW("In Review"),
    DONE("Done"),
    BLOCKED("Blocked"),
}

/**
 * An activity entry recording changes to a task.
 */
data class TaskActivityEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val author: String,
    val action: String,
    val detail: String = "",
)

/**
 * A Jira-like task assigned to agents.
 */
data class AgentTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.TODO,
    val assignedAgentId: String? = null,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val updatedTimestamp: Long = System.currentTimeMillis(),
    val dependencyTaskIds: List<String> = emptyList(),
    val worktreeBranch: String? = null,
    val activityLog: List<TaskActivityEntry> = emptyList(),
)

/**
 * Message types for the Agent-to-Agent Inbox.
 */
enum class AgentMessageType(val label: String) {
    DELEGATION("Task Delegation"),
    RESULT("Task Result"),
    HANDOFF("Agent Handoff"),
    QUERY("Inquiry"),
    NOTIFICATION("Notification"),
}

/**
 * An inbox message passed between agents.
 */
data class AgentInboxMessage(
    val id: String = UUID.randomUUID().toString(),
    val fromAgentId: String,
    val toAgentId: String,
    val type: AgentMessageType = AgentMessageType.DELEGATION,
    val subject: String,
    val content: String,
    val taskId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
)

/**
 * An entry in the shared project memory / blackboard.
 */
data class BlackboardEntry(
    val id: String = UUID.randomUUID().toString(),
    val key: String,
    val category: String = "GENERAL", // ARCHITECTURE, DISCOVERY, CONVENTION, ENV, DECISION
    val value: String,
    val authorAgentId: String,
    val authorRole: AgentRole = AgentRole.GOD_ORCHESTRATOR,
    val timestamp: Long = System.currentTimeMillis(),
    val confidence: Float = 1.0f,
)

/**
 * Node in the project memory graph.
 */
data class MemoryGraphNode(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val type: String, // "CLASS", "API_ENDPOINT", "FILE", "REQUIREMENT"
    val properties: Map<String, String> = emptyMap(),
    val connectedNodeIds: List<String> = emptyList(),
)

/**
 * Circuit breaker state for runaway agent prevention.
 */
data class CircuitBreakerState(
    val maxConsecutiveErrors: Int = 3,
    val maxToolCallsPerTask: Int = 30,
    val consecutiveErrors: Int = 0,
    val isTripped: Boolean = false,
    val trippedReason: String? = null,
    val trippedTimestamp: Long = 0L,
)

/**
 * A scheduled agent task (one-time or recurring).
 */
data class ScheduledTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val intervalMinutes: Int = 30,
    val isRecurring: Boolean = false,
    val targetAgentId: String = "god-master",
    val lastRunMillis: Long = 0L,
    val nextRunMillis: Long = System.currentTimeMillis() + 30 * 60 * 1000L,
    val enabled: Boolean = true,
)
