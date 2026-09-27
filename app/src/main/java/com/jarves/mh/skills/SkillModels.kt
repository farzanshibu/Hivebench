package com.jarves.mh.skills

import java.util.UUID

/**
 * Categories of specialized skills agents can equip dynamically.
 */
enum class SkillCategory(val label: String, val iconEmoji: String) {
    DEBUGGING("Debugging", "🐛"),
    TESTING("Testing & QA", "🧪"),
    SECURITY("Security Audit", "🛡️"),
    ARCHITECTURE("Architecture", "📐"),
    DEVOPS("DevOps & Docker", "🐳"),
    API_INTEGRATION("API & Network", "🌐"),
    CODE_OPTIMIZATION("Optimization", "⚡"),
    DATABASE("Database & Storage", "🗄️"),
}

/**
 * Reusable capability skill that can be acquired by autonomous agents.
 */
data class SkillDefinition(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val category: SkillCategory,
    val instructions: String,
    val requiredTools: List<String> = emptyList(), // e.g. ["terminal", "filesystem", "browser"]
    val enabled: Boolean = true,
    val version: String = "1.0.0",
    val author: String = "PocketDev Builtin",
)

/**
 * Definition of an MCP / Common Manager tool available to agents.
 */
data class McpToolDefinition(
    val name: String,
    val description: String,
    val parametersJsonSchema: String,
    val category: String = "GENERAL",
    val requiresApproval: Boolean = false,
)

/**
 * An invocation of a tool by an agent.
 */
data class McpToolCall(
    val id: String = UUID.randomUUID().toString(),
    val toolName: String,
    val arguments: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * Result returned from a tool execution.
 */
data class McpToolResult(
    val callId: String,
    val toolName: String,
    val success: Boolean,
    val output: String,
    val executionTimeMs: Long = 0L,
)
