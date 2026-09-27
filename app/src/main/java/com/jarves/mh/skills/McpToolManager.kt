package com.jarves.mh.skills

import java.io.File
import kotlin.system.measureTimeMillis

/**
 * Common Tool Manager & Model Context Protocol (MCP) tool dispatcher.
 * Exposes a standardized tool catalog and executes capability calls for agents.
 */
class McpToolManager(
    private val projectDir: File,
    private val terminalRunner: suspend (String) -> String = { "Terminal output placeholder" },
    private val gitRunner: suspend (List<String>) -> String = { "Git output placeholder" },
) {

    private val toolCatalog = listOf(
        McpToolDefinition(
            name = "filesystem_read",
            description = "Read file contents within the project workspace safely.",
            parametersJsonSchema = "{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\",\"description\":\"Relative path to the file\"}},\"required\":[\"path\"]}",
            category = "FILESYSTEM",
        ),
        McpToolDefinition(
            name = "filesystem_write",
            description = "Write or overwrite file contents with directory auto-creation.",
            parametersJsonSchema = "{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"},\"content\":{\"type\":\"string\"}},\"required\":[\"path\",\"content\"]}",
            category = "FILESYSTEM",
            requiresApproval = true,
        ),
        McpToolDefinition(
            name = "terminal_exec",
            description = "Execute a command inside the Linux PRoot / guest environment.",
            parametersJsonSchema = "{\"type\":\"object\",\"properties\":{\"command\":{\"type\":\"string\"}},\"required\":[\"command\"]}",
            category = "TERMINAL",
            requiresApproval = true,
        ),
        McpToolDefinition(
            name = "git_status",
            description = "Get porcelain status of the repository and active worktree.",
            parametersJsonSchema = "{\"type\":\"object\",\"properties\":{}}",
            category = "GIT",
        ),
        McpToolDefinition(
            name = "git_diff",
            description = "Inspect staged or unstaged diffs for reviewed paths.",
            parametersJsonSchema = "{\"type\":\"object\",\"properties\":{\"staged\":{\"type\":\"boolean\"}},\"required\":[]}",
            category = "GIT",
        ),
        McpToolDefinition(
            name = "browser_evaluate",
            description = "Evaluate JavaScript in the active WebView inspection session.",
            parametersJsonSchema = "{\"type\":\"object\",\"properties\":{\"script\":{\"type\":\"string\"}},\"required\":[\"script\"]}",
            category = "BROWSER",
        ),
        McpToolDefinition(
            name = "memory_recall",
            description = "Retrieve facts and conventions from the shared blackboard memory.",
            parametersJsonSchema = "{\"type\":\"object\",\"properties\":{\"category\":{\"type\":\"string\"}},\"required\":[]}",
            category = "MEMORY",
        ),
        McpToolDefinition(
            name = "docker_compose_up",
            description = "Launch local service dependencies via Docker Compose in PRoot.",
            parametersJsonSchema = "{\"type\":\"object\",\"properties\":{\"service\":{\"type\":\"string\"}},\"required\":[]}",
            category = "DEVOPS",
            requiresApproval = true,
        ),
    )

    fun getRegisteredTools(): List<McpToolDefinition> = toolCatalog

    fun getToolsForCategory(category: String): List<McpToolDefinition> =
        toolCatalog.filter { it.category.equals(category, ignoreCase = true) }

    /**
     * Executes a tool call and captures execution time and output.
     */
    suspend fun executeTool(call: McpToolCall): McpToolResult {
        var output = ""
        var isSuccess = true

        val timeMs = measureTimeMillis {
            try {
                when (call.toolName) {
                    "filesystem_read" -> {
                        val path = call.arguments["path"] ?: throw IllegalArgumentException("Missing 'path' argument")
                        val file = File(projectDir, path).canonicalFile
                        if (!file.path.startsWith(projectDir.canonicalPath)) {
                            throw SecurityException("Path traversal outside project root is forbidden")
                        }
                        if (!file.exists()) {
                            throw NoSuchFileException(file, reason = "File does not exist")
                        }
                        output = file.readText()
                    }
                    "filesystem_write" -> {
                        val path = call.arguments["path"] ?: throw IllegalArgumentException("Missing 'path' argument")
                        val content = call.arguments["content"] ?: ""
                        val file = File(projectDir, path).canonicalFile
                        if (!file.path.startsWith(projectDir.canonicalPath)) {
                            throw SecurityException("Path traversal outside project root is forbidden")
                        }
                        file.parentFile?.mkdirs()
                        file.writeText(content)
                        output = "Successfully wrote ${content.length} characters to $path"
                    }
                    "terminal_exec" -> {
                        val cmd = call.arguments["command"] ?: throw IllegalArgumentException("Missing 'command' argument")
                        output = terminalRunner(cmd)
                    }
                    "git_status" -> {
                        output = gitRunner(listOf("status", "--short"))
                    }
                    "git_diff" -> {
                        val staged = call.arguments["staged"]?.toBoolean() ?: false
                        val args = if (staged) listOf("diff", "--cached") else listOf("diff")
                        output = gitRunner(args)
                    }
                    "browser_evaluate" -> {
                        val script = call.arguments["script"] ?: ""
                        output = "Browser evaluation queued: $script"
                    }
                    "memory_recall" -> {
                        val cat = call.arguments["category"] ?: "ALL"
                        output = "Retrieved blackboard memory for category: $cat"
                    }
                    "docker_compose_up" -> {
                        val service = call.arguments["service"] ?: "all"
                        output = terminalRunner("docker compose up -d $service")
                    }
                    else -> {
                        isSuccess = false
                        output = "Unknown MCP tool: ${call.toolName}"
                    }
                }
            } catch (e: Exception) {
                isSuccess = false
                output = "Tool execution error: ${e.message}"
            }
        }

        return McpToolResult(
            callId = call.id,
            toolName = call.toolName,
            success = isSuccess,
            output = output,
            executionTimeMs = timeMs,
        )
    }
}
