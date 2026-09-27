package com.jarves.mh.model

import java.util.UUID

/**
 * Supported execution runtime architectures for coding agents.
 */
enum class AgentRuntimeType(val displayName: String, val iconEmoji: String) {
    BUILTIN_CLAUDE("Claude Code", "⚡"),
    CODEX("Codex / GPT", "🤖"),
    OPEN_CODE("OpenCode Agent", "🔓"),
    COMMAND_CODE("Command Code CLI", "⌨️"),
    GEMINI_CODE("Gemini Code Assist", "✨"),
    PI_AGENT("Pi Autonomous Agent", "🥧"),
    DOCKER_RUNNER("Docker / Compose Tool", "🐳"),
    CUSTOM_CLI("Custom Guest CLI", "🛠️"),
}

/**
 * Customizable template for instantiating dynamic autonomous agents.
 */
data class CustomAgentTemplate(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val avatarEmoji: String = "🤖",
    val roleTitle: String,
    val runtimeType: AgentRuntimeType = AgentRuntimeType.BUILTIN_CLAUDE,
    val systemPrompt: String,
    val preferredModel: String = "gemini-1.5-pro",
    val preferredEffort: String = "high",
    val enabledSkills: List<String> = emptyList(),
    val enabledTools: List<String> = listOf("filesystem_read", "filesystem_write", "terminal_exec", "git_status"),
    val isCustom: Boolean = true,
)

/**
 * Real usage and quota metrics tracked for each agent.
 */
data class AgentQuotaUsage(
    val agentId: String,
    val agentName: String,
    val inputTokens: Long = 0L,
    val outputTokens: Long = 0L,
    val cachedTokens: Long = 0L,
    val toolCallsCount: Int = 0,
    val totalSessionsCount: Int = 1,
    val estimatedCostUsd: Double = 0.0,
)
