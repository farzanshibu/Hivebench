package com.jarves.mh.skills

import com.jarves.mh.model.AgentKind
import com.jarves.mh.model.AgentRole
import com.jarves.mh.model.BlackboardEntry
import com.jarves.mh.orchestrator.BlackboardMemoryStore
import java.io.File

/**
 * Metadata for a harness-specific or universal slash command.
 */
data class HarnessCommandInfo(
    val command: String,
    val label: String,
    val description: String,
    val hasArgs: Boolean = false,
    val argSuggestions: List<String> = emptyList(),
)

/**
 * Common Capability Pool shared across ALL agent harnesses:
 * - Claude Code
 * - DeepSeek Harness
 * - Antigravity CLI
 * - JCode Agent
 * - Pi Agent
 * - Command Code
 * - Cline Agent
 * - Custom Agent Runner
 *
 * Provides a unified catalog of MCP tools, specialized skills, and shared blackboard memory.
 */
class CommonCapabilityPool(
    val projectDir: File,
    val skillRegistry: SkillRegistry,
    val mcpToolManager: McpToolManager,
    val memoryStore: BlackboardMemoryStore,
) {

    fun getAvailableTools(): List<McpToolDefinition> = mcpToolManager.getRegisteredTools()

    fun getAvailableSkills(): List<SkillDefinition> = skillRegistry.getAllSkills()

    fun getEnabledSkills(): List<SkillDefinition> = skillRegistry.getEnabledSkills()

    fun toggleSkill(skillId: String): Boolean = skillRegistry.toggleSkill(skillId)

    suspend fun executeMcpTool(toolName: String, arguments: Map<String, String> = emptyMap()): McpToolResult {
        val call = McpToolCall(toolName = toolName, arguments = arguments)
        return mcpToolManager.executeTool(call)
    }

    fun putMemory(
        key: String,
        category: String,
        value: String,
        authorAgent: String = "CommonPool",
    ): BlackboardEntry {
        return memoryStore.putEntry(
            key = key,
            category = category,
            value = value,
            authorAgentId = authorAgent,
            authorRole = AgentRole.DEVELOPER,
        )
    }

    fun getMemory(key: String): String? = memoryStore.getEntry(key)?.value

    fun getAllMemories(): List<BlackboardEntry> = memoryStore.getAllEntries()

    /**
     * Builds standard context block injected into prompts across ALL agent harnesses.
     */
    fun buildCommonPromptContext(agentKind: AgentKind): String = buildString {
        val skills = getEnabledSkills()
        val tools = getAvailableTools()
        val memories = getAllMemories().take(10)

        appendLine("<common_capability_pool>")
        appendLine("Active Agent Harness: ${agentKind.title} (${agentKind.stableId})")

        if (skills.isNotEmpty()) {
            appendLine("<active_skills>")
            skills.forEach { skill ->
                appendLine("- [${skill.category.label}] ${skill.name}: ${skill.instructions}")
            }
            appendLine("</active_skills>")
        }

        if (tools.isNotEmpty()) {
            appendLine("<mcp_tool_catalog>")
            tools.forEach { tool ->
                appendLine("- ${tool.name} [${tool.category}]: ${tool.description}")
            }
            appendLine("</mcp_tool_catalog>")
        }

        if (memories.isNotEmpty()) {
            appendLine("<shared_blackboard_memory>")
            memories.forEach { mem ->
                appendLine("- [${mem.category}] ${mem.key}: ${mem.value}")
            }
            appendLine("</shared_blackboard_memory>")
        }

        appendLine("All agents in this project share this common pool of skills, MCP tools, and blackboard memory.")
        appendLine("</common_capability_pool>")
    }

    /**
     * Returns the dedicated slash commands provided by the specified agent harness,
     * combined with common pool utilities.
     */
    fun getHarnessCommands(agentKind: AgentKind): List<HarnessCommandInfo> {
        val harnessSpecific = when (agentKind) {
            AgentKind.CLAUDE_CODE -> listOf(
                HarnessCommandInfo("/usage", "⚡ /usage", "View session token usage & metrics"),
                HarnessCommandInfo("/login", "🔑 /login", "Manage Anthropic / Claude subscription login"),
                HarnessCommandInfo("/switch", "🤖 /switch", "Switch active agent or model", true, listOf("jcode", "pi-agent", "command-code", "cline", "antigravity", "deepseek-harness")),
                HarnessCommandInfo("/effort", "🧠 /effort", "Configure reasoning effort level", true, listOf("low", "medium", "high")),
                HarnessCommandInfo("/model", "💎 /model", "Switch Claude model", true, listOf("claude-3-7-sonnet", "claude-3-5-sonnet", "claude-3-5-haiku")),
                HarnessCommandInfo("/cost", "💰 /cost", "Calculate session API cost estimate"),
                HarnessCommandInfo("/compact", "📦 /compact", "Compact conversation context"),
                HarnessCommandInfo("/doctor", "🩺 /doctor", "Run runtime diagnostic health check"),
                HarnessCommandInfo("/review", "🔎 /review", "Request automated review on active git diff"),
            )
            AgentKind.DEEPSEEK_HARNESS -> listOf(
                HarnessCommandInfo("/key", "🔑 /key", "Set or update DeepSeek API key", true),
                HarnessCommandInfo("/model", "💎 /model", "Switch DeepSeek model", true, listOf("deepseek-chat", "deepseek-reasoner", "deepseek-coder")),
                HarnessCommandInfo("/think", "🧠 /think", "Toggle DeepSeek reasoning thought trace"),
                HarnessCommandInfo("/endpoint", "🌐 /endpoint", "Set custom gateway API endpoint", true),
                HarnessCommandInfo("/quota", "📊 /quota", "Check DeepSeek API balance and quota"),
                HarnessCommandInfo("/switch", "🤖 /switch", "Switch active agent", true, listOf("claude-code", "jcode", "antigravity")),
            )
            AgentKind.ANTIGRAVITY -> listOf(
                HarnessCommandInfo("/auth", "🚀 /auth", "Login with Google account device code"),
                HarnessCommandInfo("/account", "👤 /account", "Inspect connected Google account status"),
                HarnessCommandInfo("/model", "💎 /model", "Switch Gemini/Antigravity model", true, listOf("gemini-2.5-pro", "gemini-2.5-flash")),
                HarnessCommandInfo("/effort", "🧠 /effort", "Set reasoning effort level", true, listOf("low", "medium", "high")),
                HarnessCommandInfo("/quota", "📊 /quota", "Inspect Google account quota status"),
                HarnessCommandInfo("/sync", "🔄 /sync", "Synchronize models and project state"),
                HarnessCommandInfo("/switch", "🤖 /switch", "Switch active agent", true, listOf("claude-code", "jcode", "deepseek-harness")),
            )
            AgentKind.JCODE -> listOf(
                HarnessCommandInfo("/model", "💎 /model", "Select multi-LLM engine model", true, listOf("gemini-2.5-pro", "gpt-4o", "claude-3-7-sonnet", "deepseek-chat")),
                HarnessCommandInfo("/effort", "🧠 /effort", "Set reasoning effort level", true, listOf("low", "medium", "high")),
                HarnessCommandInfo("/plan", "📐 /plan", "Generate structured implementation plan"),
                HarnessCommandInfo("/swarm", "🐝 /swarm", "Delegate prompt to autonomous agent swarm"),
                HarnessCommandInfo("/linear", "📌 /linear", "Fetch & import assigned Linear issues"),
                HarnessCommandInfo("/github", "🐙 /github", "Fetch & import GitHub repository issues"),
                HarnessCommandInfo("/tools", "🔧 /tools", "Inspect active toolchains and packages"),
            )
            AgentKind.PI_AGENT -> listOf(
                HarnessCommandInfo("/model", "💎 /model", "Set Pi agent reasoning model", true, listOf("deepseek-reasoner", "gemini-2.5-pro", "gpt-4o")),
                HarnessCommandInfo("/depth", "🧠 /depth", "Set recursive reflection depth (1-5)", true, listOf("1", "2", "3", "4", "5")),
                HarnessCommandInfo("/reflect", "🔍 /reflect", "Trigger intermediate reflection pass"),
                HarnessCommandInfo("/memory", "💾 /memory", "Inspect agent episodic and graph memory"),
            )
            AgentKind.COMMAND_CODE -> listOf(
                HarnessCommandInfo("/exec", "⌨️ /exec", "Execute PRoot terminal command directly", true),
                HarnessCommandInfo("/term", "💻 /term", "Open interactive Linux terminal tab"),
                HarnessCommandInfo("/env", "🌍 /env", "Inspect guest environment variables"),
                HarnessCommandInfo("/alias", "🏷️ /alias", "View configured terminal aliases"),
            )
            AgentKind.CLINE -> listOf(
                HarnessCommandInfo("/mode", "🎛️ /mode", "Switch Cline persona mode", true, listOf("code", "architect", "ask", "test")),
                HarnessCommandInfo("/model", "💎 /model", "Configure LLM model", true, listOf("claude-3-7-sonnet", "gpt-4o", "deepseek-chat")),
                HarnessCommandInfo("/rules", "📜 /rules", "Inspect project instruction guardrails"),
                HarnessCommandInfo("/approve", "🛡️ /approve", "Toggle tool execution auto-approval"),
            )
            AgentKind.CUSTOM_RUNNER -> listOf(
                HarnessCommandInfo("/cmd", "⚙️ /cmd", "Set custom guest runner command", true),
                HarnessCommandInfo("/script", "📜 /script", "View or edit runner launcher script"),
                HarnessCommandInfo("/reload", "🔄 /reload", "Re-initialize runner script in rootfs"),
                HarnessCommandInfo("/test", "🧪 /test", "Test-run custom agent runner probe"),
            )
        }

        // Common Universal Pool Commands available for ALL harnesses
        val commonPoolCommands = listOf(
            HarnessCommandInfo("/pool", "🏊 /pool", "Inspect Common MCP & Skill Pool"),
            HarnessCommandInfo("/mcp", "🛠️ /mcp", "Run or inspect common MCP tools", true, listOf("filesystem_read", "filesystem_write", "terminal_exec", "git_status", "git_diff", "memory_recall")),
            HarnessCommandInfo("/skills", "✨ /skills", "List and equip common skills"),
            HarnessCommandInfo("/memory", "💾 /memory", "Read or write to shared blackboard memory", true, listOf("list", "get", "set")),
            HarnessCommandInfo("/bypass", "🛡️ /bypass", "Toggle tool permission auto-bypass"),
            HarnessCommandInfo("/help", "❓ /help", "List all harness and pool commands"),
        )

        return harnessSpecific + commonPoolCommands
    }
}
