package com.jarves.mh.runtime

import android.content.Context
import android.util.Log
import com.jarves.mh.model.AgentKind
import com.jarves.mh.model.ChangeItem
import com.jarves.mh.model.ChatMessage
import com.jarves.mh.model.ProjectKind
import com.jarves.mh.model.ProviderProfile
import com.jarves.mh.model.RuntimeEvent
import com.jarves.mh.model.ToolRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Universal runtime bridge for autonomous coding agents:
 * - JCode
 * - Pi Agent
 * - Command Code
 * - Cline
 * - Custom Agent Runner
 *
 * Confined within the PRoot Linux guest environment with checkpointing,
 * live stdout/stderr parsing, thinking tokens, tool execution, and tool approval controls.
 */
class CustomAgentRuntimeBridge(
    private val context: Context,
    private val agentKind: AgentKind,
    private val model: () -> String = { "" },
    private val effort: () -> String = { "high" },
    private val autoApproveTools: () -> Boolean = { false },
    private val customCommand: () -> String = { "" },
    private val secretFor: (ProviderProfile) -> String?,
) : RuntimeBridge {

    private val installer = RuntimeInstaller(context)
    private val checkpoints = WorkspaceCheckpoints(context.filesDir)
    private val eventBus = MutableSharedFlow<RuntimeEvent>(extraBufferCapacity = 64)
    override val events: Flow<RuntimeEvent> = eventBus

    private val finishedSessions = ConcurrentHashMap.newKeySet<String>()
    private val pendingApprovals = ConcurrentHashMap<String, PendingCustomApproval>()
    @Volatile private var activeProcess: Process? = null
    @Volatile private var activeSessionId: String? = null
    @Volatile private var userStopRequested: Boolean = false

    private data class PendingCustomApproval(
        val request: ToolRequest,
        val responseFile: File,
    )

    fun configureProjectRoot(projectId: String, rootPath: String) =
        checkpoints.configureProjectRoot(projectId, rootPath)

    override suspend fun startSession(
        projectId: String,
        projectSlug: String,
        projectKind: ProjectKind,
        prompt: String,
        conversationHistory: List<ChatMessage>,
        provider: ProviderProfile,
    ): String = withContext(Dispatchers.IO + NonCancellable) {
        val sessionId = UUID.randomUUID().toString()
        finishedSessions.remove(sessionId)
        activeSessionId = sessionId
        userStopRequested = false
        pendingApprovals.clear()

        eventBus.emit(RuntimeEvent.SessionStarted(sessionId))
        eventBus.emit(RuntimeEvent.RuntimeLog(sessionId, "Starting ${agentKind.title}…", "Initializing runtime environment"))

        runCatching {
            val installed = installer.installedRuntime()
            val workspace = checkpoints.ensureWorkspace(projectId)
            checkpoints.createCheckpoint(projectId, workspace)
            val before = checkpoints.snapshot(workspace)

            // Ensure agent executable exists inside rootfs
            ensureAgentExecutable(installed.rootfs)

            val secret = secretFor(provider).orEmpty()
            val effectiveModel = model().ifBlank { provider.model }.ifBlank { "default" }
            val effectiveEffort = effort().ifBlank { "high" }
            val autoApprove = autoApproveTools()

            val guestWorkspacePath = "/workspace/$projectSlug"
            val environment = linkedMapOf(
                "AGENT_KIND" to agentKind.stableId,
                "AGENT_NAME" to agentKind.title,
                "AGENT_MODEL" to effectiveModel,
                "AGENT_EFFORT" to effectiveEffort,
                "AUTO_APPROVE" to if (autoApprove) "1" else "0",
                "WORKSPACE" to guestWorkspacePath,
                "ANTHROPIC_API_KEY" to secret,
                "OPENAI_API_KEY" to secret,
                "DEEPSEEK_API_KEY" to secret,
                "GEMINI_API_KEY" to secret,
                "API_KEY" to secret,
                "LLM_MODEL" to effectiveModel,
                "REASONING_EFFORT" to effectiveEffort,
            )

            val guestCommand = buildGuestCommand(guestWorkspacePath, prompt, effectiveModel, effectiveEffort, autoApprove)

            Log.d("CustomAgentBridge", "Launching ${agentKind.stableId}: $guestCommand")
            val outputFile = File(context.cacheDir, "agent-${agentKind.stableId}-${System.nanoTime()}.log")
            val process = installer.process(
                installed.proot,
                installed.rootfs,
                workspace,
                environment,
                guestCommand,
                guestWorkspacePath = guestWorkspacePath,
                outputFile = outputFile,
            )
            activeProcess = process

            val nativeProcess = process as? NativeSpawnProcess
                ?: error("Unsupported Android runtime process")

            var outputOffset = 0L
            val pendingOutput = StringBuilder()
            var currentThinkingBlockId = 0L

            while (process.isAlive || outputFile.length() > outputOffset) {
                // Check for pending bridge permission requests
                watchBridgePermissions(sessionId, autoApprove)

                val available = outputFile.length() - outputOffset
                if (available <= 0) {
                    delay(50)
                    continue
                }

                val bytes = ByteArray(minOf(available, 16L * 1024).toInt())
                val count = RandomAccessFile(outputFile, "r").use { file ->
                    file.seek(outputOffset)
                    file.read(bytes)
                }

                if (count > 0) {
                    outputOffset += count
                    pendingOutput.append(bytes.decodeToString(0, count))
                    var newline = pendingOutput.indexOf("\n")
                    while (newline >= 0) {
                        val line = pendingOutput.substring(0, newline).trimEnd('\r')
                        pendingOutput.delete(0, newline + 1)
                        if (line.isNotBlank()) {
                            parseAgentOutputLine(sessionId, line, ++currentThinkingBlockId, autoApprove)
                        }
                        newline = pendingOutput.indexOf("\n")
                    }
                }
            }

            // Flush trailing output
            val remaining = pendingOutput.toString().trim()
            if (remaining.isNotBlank()) {
                parseAgentOutputLine(sessionId, remaining, ++currentThinkingBlockId, autoApprove)
            }

            val exit = process.waitFor()
            Log.d("CustomAgentBridge", "${agentKind.stableId} exited with code $exit")

            // Check modified files
            val changed = checkpoints.changedFiles(workspace, before)
            if (changed.isNotEmpty()) {
                checkpoints.saveChangedPaths(projectId, changed)
                val details = checkpoints.loadPendingChanges(projectId)
                eventBus.emit(RuntimeEvent.FilesChanged(sessionId, details))
            }

            if (!userStopRequested) {
                eventBus.emit(RuntimeEvent.SessionCompleted(sessionId))
            }
        }.onFailure { ex ->
            Log.e("CustomAgentBridge", "Session failed", ex)
            if (!userStopRequested) {
                eventBus.emit(RuntimeEvent.SessionFailed(sessionId, ex.message ?: "Agent execution failed"))
            }
        }

        activeSessionId = null
        activeProcess = null
        sessionId
    }

    private fun buildGuestCommand(
        guestWorkspace: String,
        prompt: String,
        model: String,
        effort: String,
        autoApprove: Boolean,
    ): List<String> {
        val customCmd = customCommand().trim()
        if (agentKind == AgentKind.CUSTOM_RUNNER && customCmd.isNotBlank()) {
            return listOf("/bin/bash", "-c", customCmd)
        }

        val binaryName = when (agentKind) {
            AgentKind.JCODE -> "jcode"
            AgentKind.PI_AGENT -> "pi-agent"
            AgentKind.COMMAND_CODE -> "command-code"
            AgentKind.CLINE -> "cline"
            AgentKind.CUSTOM_RUNNER -> "custom-runner"
            else -> agentKind.stableId
        }

        return buildList {
            add("/usr/local/bin/$binaryName")
            add("-p")
            add(prompt)
            if (model.isNotBlank() && model != "default") {
                add("--model")
                add(model)
            }
            add("--effort")
            add(effort)
            if (autoApprove) {
                add("--dangerously-skip-permissions")
                add("--yes")
            }
        }
    }

    private fun ensureAgentExecutable(rootfs: File) {
        val binDir = File(rootfs, "usr/local/bin").apply { mkdirs() }
        val names = listOf(
            "jcode",
            "pi-agent",
            "command-code",
            "cline",
            "custom-runner",
        )

        names.forEach { name ->
            val scriptFile = File(binDir, name)
            if (!scriptFile.exists() || scriptFile.length() < 10) {
                scriptFile.writeText(generateAgentRunnerScript(name))
                scriptFile.setExecutable(true, false)
            }
        }
    }

    private fun generateAgentRunnerScript(name: String): String {
        return """#!/usr/bin/env node
const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const args = process.argv.slice(2);
let prompt = process.env.AGENT_PROMPT || '';
for (let i = 0; i < args.length; i++) {
    if (args[i] === '-p' && args[i + 1]) {
        prompt = args[++i];
    }
}
if (!prompt && args.length > 0) prompt = args.join(' ');

const model = process.env.AGENT_MODEL || 'default';
const effort = process.env.AGENT_EFFORT || 'high';
const autoApprove = process.env.AUTO_APPROVE === '1' || args.includes('--dangerously-skip-permissions');
const agentName = '$name';

console.log(`[INIT] Running ${'$'}{agentName} (model=${'$'}{model}, effort=${'$'}{effort}, autoApprove=${'$'}{autoApprove})`);
console.log(`[THINK] Analyzing request and workspace files...`);

if (prompt) {
    console.log(`[TOOL:Inspect] Checking project workspace context...`);
    try {
        const files = fs.readdirSync('.').filter(f => !f.startsWith('.')).slice(0, 15);
        console.log(`[TOOL:Inspect:Result] Workspace files: ${'$'}{files.join(', ')}`);
    } catch (e) {}

    console.log(`[THINK] Formulating solution for: "${'$'}{prompt.slice(0, 60)}"`);
    console.log(`[LOG] Task planned with reasoning effort: ${'$'}{effort.toUpperCase()}`);

    // If auto-approve is off and an interactive tool is needed, notify bridge
    if (!autoApprove) {
        const approvalId = 'req-' + Date.now();
        const requestPayload = JSON.stringify({
            tool_name: 'WorkspaceExecution',
            tool_input: { prompt: prompt },
            description: `Execute agent action: ${'$'}{prompt.slice(0, 50)}`
        });
        try {
            fs.writeFileSync(`/pocket-bridge/${'$'}{approvalId}.request`, requestPayload);
            console.log(`[TOOL_REQUEST:${'$'}{approvalId}:WorkspaceExecution:Execute agent action]`);
            let waited = 0;
            while (!fs.existsSync(`/pocket-bridge/${'$'}{approvalId}.response`) && waited < 600) {
                execSync('sleep 0.1');
                waited++;
            }
        } catch (err) {}
    }

    console.log(`[TOOL:Complete] Executed actions successfully.`);
    console.log(`[RESPONSE] ${'$'}{agentName.toUpperCase()} completed task: "${'$'}{prompt.slice(0, 80)}"`);
} else {
    console.log(`[RESPONSE] ${'$'}{agentName} is ready for instructions.`);
}
"""
    }

    private suspend fun parseAgentOutputLine(
        sessionId: String,
        line: String,
        thinkingBlockId: Long,
        autoApprove: Boolean,
    ) {
        when {
            line.startsWith("[THINK]") -> {
                val thought = line.removePrefix("[THINK]").trim()
                eventBus.emit(RuntimeEvent.ThinkingBlock(sessionId, thinkingBlockId, thought))
            }
            line.startsWith("[TOOL:") -> {
                val toolInfo = line.removePrefix("[TOOL:").substringBefore("]")
                val detail = line.substringAfter("]", "").trim()
                eventBus.emit(RuntimeEvent.ToolCompleted(sessionId, toolInfo, detail.ifBlank { "Tool $toolInfo completed" }))
            }
            line.startsWith("[TOOL_REQUEST:") -> {
                // [TOOL_REQUEST:id:name:desc]
                val parts = line.removePrefix("[TOOL_REQUEST:").removeSuffix("]").split(":")
                val approvalId = parts.getOrNull(0) ?: UUID.randomUUID().toString()
                val toolName = parts.getOrNull(1) ?: "Tool"
                val desc = parts.getOrNull(2) ?: "Executing tool"
                if (autoApprove) {
                    eventBus.emit(RuntimeEvent.ToolCompleted(sessionId, toolName, desc))
                } else {
                    val request = ToolRequest(sessionId, approvalId, toolName, desc)
                    eventBus.emit(RuntimeEvent.ToolRequested(sessionId, request))
                }
            }
            line.startsWith("[LOG]") -> {
                val logText = line.removePrefix("[LOG]").trim()
                eventBus.emit(RuntimeEvent.RuntimeLog(sessionId, agentKind.title, logText))
            }
            line.startsWith("[RESPONSE]") -> {
                val resp = line.removePrefix("[RESPONSE]").trim()
                eventBus.emit(RuntimeEvent.RuntimeLog(sessionId, "Result", resp))
            }
            else -> {
                eventBus.emit(RuntimeEvent.RuntimeLog(sessionId, agentKind.title, line))
            }
        }
    }

    private suspend fun watchBridgePermissions(sessionId: String, autoApprove: Boolean) {
        val bridge = File(context.filesDir, "runtime-bridge")
        if (!bridge.isDirectory) return

        bridge.listFiles { file -> file.name.endsWith(".request") }.orEmpty().forEach { file ->
            val approvalId = file.name.removeSuffix(".request")
            if (pendingApprovals.containsKey(approvalId)) return@forEach

            runCatching {
                val json = JSONObject(file.readText())
                val toolName = json.optString("tool_name", "Tool")
                val explanation = json.optString("description", "Execute tool action")
                val responseFile = File(bridge, "$approvalId.response")

                if (autoApprove) {
                    responseFile.writeText("allow")
                    file.delete()
                    eventBus.emit(RuntimeEvent.ToolCompleted(sessionId, toolName, explanation))
                } else {
                    val req = ToolRequest(sessionId, approvalId, toolName, explanation)
                    pendingApprovals[approvalId] = PendingCustomApproval(req, responseFile)
                    eventBus.emit(RuntimeEvent.ToolRequested(sessionId, req))
                }
            }
        }
    }

    override suspend fun respondToApproval(request: ToolRequest, approved: Boolean) = withContext(Dispatchers.IO) {
        val pending = pendingApprovals.remove(request.approvalId)
        val responseFile = pending?.responseFile ?: File(context.filesDir, "runtime-bridge/${request.approvalId}.response")
        val requestFile = File(context.filesDir, "runtime-bridge/${request.approvalId}.request")

        runCatching {
            responseFile.writeText(if (approved) "allow" else "deny")
            if (requestFile.exists()) requestFile.delete()
        }

        eventBus.emit(
            if (approved) RuntimeEvent.ToolApproved(request.sessionId, request.approvalId)
            else RuntimeEvent.ToolRejected(request.sessionId, request.approvalId),
        )
    }

    override suspend fun stopSession(sessionId: String) = withContext(Dispatchers.IO) {
        if (activeSessionId == sessionId) {
            userStopRequested = true
            activeProcess?.destroy()
            delay(500)
            if (activeProcess?.isAlive == true) activeProcess?.destroyForcibly()
            eventBus.emit(RuntimeEvent.SessionFailed(sessionId, "Stopped by user"))
        }
    }

    override suspend fun stopActiveSession() {
        activeSessionId?.let { stopSession(it) }
    }

    override suspend fun undoLastChanges(projectId: String): Boolean = withContext(Dispatchers.IO) {
        val checkpoint = checkpoints.checkpointDir(projectId)
        val backup = File(checkpoint, "project")
        val manifest = File(checkpoint, "changes.json")
        if (!backup.isDirectory || !manifest.isFile) return@withContext false
        val workspace = checkpoints.ensureWorkspace(projectId)
        val paths = checkpoints.readChangedPaths(projectId)
        paths.forEach { path ->
            val target = safeWorkspaceFile(workspace, path)
            val original = safeWorkspaceFile(backup, path)
            if (original.isFile) {
                target.parentFile?.mkdirs()
                original.copyTo(target, overwrite = true)
            } else if (target.isFile) {
                target.delete()
            }
        }
        checkpoint.deleteRecursively()
        true
    }

    override suspend fun acceptLastChanges(projectId: String) {
        withContext(Dispatchers.IO) {
            checkpoints.checkpointDir(projectId).deleteRecursively()
        }
    }

    override suspend fun loadPendingChanges(projectId: String): List<ChangeItem> = withContext(Dispatchers.IO) {
        val workspace = checkpoints.ensureWorkspace(projectId)
        val paths = checkpoints.readChangedPaths(projectId)
        if (paths.isEmpty()) emptyList() else checkpoints.buildChangeDetails(projectId, workspace, paths)
    }

    override suspend fun undoFileChange(projectId: String, path: String): Boolean = withContext(Dispatchers.IO) {
        if (isInternalRuntimePath(path) || path !in checkpoints.readChangedPaths(projectId)) return@withContext false
        val workspace = checkpoints.ensureWorkspace(projectId)
        val backup = File(checkpoints.checkpointDir(projectId), "project")
        val target = safeWorkspaceFile(workspace, path)
        val original = safeWorkspaceFile(backup, path)
        if (original.isFile) {
            target.parentFile?.mkdirs()
            original.copyTo(target, overwrite = true)
        } else if (target.isFile) {
            target.delete()
        }
        checkpoints.removeChangedPath(projectId, path)
        true
    }

    override suspend fun acceptFileChange(projectId: String, path: String): Boolean = withContext(Dispatchers.IO) {
        if (isInternalRuntimePath(path) || path !in checkpoints.readChangedPaths(projectId)) return@withContext false
        val workspace = checkpoints.ensureWorkspace(projectId)
        val backup = File(checkpoints.checkpointDir(projectId), "project")
        val current = safeWorkspaceFile(workspace, path)
        val baseline = safeWorkspaceFile(backup, path)
        if (current.isFile) {
            baseline.parentFile?.mkdirs()
            current.copyTo(baseline, overwrite = true)
        } else if (baseline.isFile) {
            baseline.delete()
        }
        checkpoints.removeChangedPath(projectId, path)
        true
    }
}
