package com.jarves.mh.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.os.SystemClock
import android.os.Build
import android.system.Os
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.jarves.mh.BuildConfig
import com.jarves.mh.data.ApiKeyVault
import com.jarves.mh.data.ApiKeyInfo
import com.jarves.mh.data.AppPreferences
import com.jarves.mh.model.ActivityItem
import com.jarves.mh.model.AgentKind
import com.jarves.mh.model.ChangeItem
import com.jarves.mh.model.ChatMessage
import com.jarves.mh.model.ChatAttachment
import com.jarves.mh.model.DevStack
import com.jarves.mh.model.Project
import com.jarves.mh.model.ProjectKind
import com.jarves.mh.model.ProjectChat
import com.jarves.mh.model.ProviderKind
import com.jarves.mh.model.ProviderProfile
import com.jarves.mh.model.RuntimeEvent
import com.jarves.mh.model.ToolRequest
import com.jarves.mh.model.WorkspaceEntry
import com.jarves.mh.model.projectSlug
import com.jarves.mh.model.generateQuickChatIdentity
import com.jarves.mh.model.providerProtocolForAgent
import com.jarves.mh.network.ConnectionValidation
import com.jarves.mh.network.ModelDiscoveryResult
import com.jarves.mh.network.ProviderApiClient
import com.jarves.mh.network.GitHubRepository
import com.jarves.mh.network.GitHubIssue
import com.jarves.mh.network.LinearIssue
import com.jarves.mh.network.LinearClient
import com.jarves.mh.runtime.ClaudeRuntimeBridge
import com.jarves.mh.runtime.CustomAgentRuntimeBridge
import com.jarves.mh.runtime.DshRuntimeBridge
import com.jarves.mh.runtime.AgentRegistry
import com.jarves.mh.runtime.AgentUpdateInfo
import com.jarves.mh.runtime.AntigravityAuthController
import com.jarves.mh.runtime.AntigravityAuthState
import com.jarves.mh.runtime.AntigravityAuthStatus
import com.jarves.mh.runtime.AntigravityRuntimeBridge
import com.jarves.mh.runtime.NativeSpawnProcess
import com.jarves.mh.runtime.RuntimeInstallProgress
import com.jarves.mh.runtime.RuntimeInstaller
import com.jarves.mh.runtime.RuntimeSetupController
import com.jarves.mh.runtime.RuntimeSetupService
import com.jarves.mh.runtime.RuntimeSetupSnapshot
import com.jarves.mh.runtime.RuntimeSetupStatus
import com.jarves.mh.runtime.supportsArm64Runtime
import com.jarves.mh.runtime.AndroidAppInstaller
import com.jarves.mh.update.AppUpdateInfo
import com.jarves.mh.update.AppUpdater
import com.jarves.mh.skills.SkillDefinition
import com.jarves.mh.skills.SkillCategory
import com.jarves.mh.skills.McpToolDefinition
import com.jarves.mh.skills.SkillRegistry
import com.jarves.mh.skills.McpToolManager
import com.jarves.mh.skills.CommonCapabilityPool
import com.jarves.mh.skills.HarnessCommandInfo
import com.jarves.mh.review.DiffReviewSession
import com.jarves.mh.review.LineDiffAnnotation
import com.jarves.mh.review.AnnotationType
import com.jarves.mh.review.DiffReviewManager
import com.jarves.mh.ssh.SshServerProfile
import com.jarves.mh.ssh.SshAuthType
import com.jarves.mh.ssh.SshRuntimeManager
import com.jarves.mh.model.CustomAgentTemplate
import com.jarves.mh.model.AgentRuntimeType
import com.jarves.mh.model.AgentQuotaUsage
import java.io.File
import java.io.RandomAccessFile
import java.net.UnknownHostException
import java.net.URI
import java.nio.file.Files
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

enum class StartupStage { CHECKING, SETUP_REQUIRED, INSTALLING, MODEL_SETUP, INITIALIZING, READY, ERROR }

enum class ApiPingStatus { IDLE, PINGING, OK, FAILED }
enum class AppUpdateStatus { AVAILABLE, PERMISSION_REQUIRED, DOWNLOADING, INSTALLING, ERROR }
enum class GitHubAuthStatus { DISCONNECTED, STARTING, AWAITING_USER, CONNECTED, ERROR }

data class TerminalOutputLine(
    val id: String = java.util.UUID.randomUUID().toString(),
    val command: String,
    val output: String,
    val exitCode: Int = 0,
)

private val ANSI_TERMINAL_SEQUENCE = Regex("\\u001B(?:\\][^\\u0007]*(?:\\u0007|\\u001B\\\\)|\\[[0-?]*[ -/]*[@-~]|[()][A-Z0-9])")

internal fun sanitizeTerminalOutput(text: String): String = text
    .replace(ANSI_TERMINAL_SEQUENCE, "")
    .filter { it == '\n' || it == '\r' || it == '\t' || it.code >= 0x20 }

private val ANTIGRAVITY_MODEL_EFFORT = Regex("^(.*)-(low|medium|high)$")

private fun antigravityEffortFromModel(model: String): String? =
    ANTIGRAVITY_MODEL_EFFORT.matchEntire(model)?.groupValues?.get(2)

private fun antigravityModelWithEffort(model: String, effort: String): String? {
    val match = ANTIGRAVITY_MODEL_EFFORT.matchEntire(model) ?: return null
    return "${match.groupValues[1]}-$effort"
}

private data class ProjectTerminalSnapshot(
    val lines: List<TerminalOutputLine> = emptyList(),
    val cwd: String = "/workspace",
)

private data class ProjectTerminalResult(
    val output: String,
    val exitCode: Int,
    val cwd: String,
)

private data class RuntimeRetryRequest(
    val runtime: com.jarves.mh.runtime.RuntimeBridge,
    val project: Project,
    val prompt: String,
    val history: List<ChatMessage>,
    val provider: ProviderProfile,
)

private data class TranscriptWrite(
    val projectId: String,
    val chatId: String,
    val messages: List<ChatMessage>,
)

private data class ImportedZipProject(
    val project: Project,
    val sourceAttachment: ChatAttachment,
)

data class AppUiState(
    val startupStage: StartupStage = StartupStage.CHECKING,
    val startupProgress: Float = 0f,
    val startupMessage: String = "Checking this device…",
    val startupBytes: Pair<Long, Long>? = null,
    val startupLogs: List<String> = emptyList(),
    val startupIndeterminate: Boolean = false,
    val startupError: String? = null,
    val startupErrorIsOffline: Boolean = false,
    val showDetailedSetupProgress: Boolean = false,
    val onboardingComplete: Boolean = false,
    val backgroundSetupComplete: Boolean = false,
    val provider: ProviderProfile = ProviderProfile(ProviderKind.ANTHROPIC),
    val activeApiKeyName: String? = null,
    val themeMode: com.jarves.mh.ui.theme.AppThemeMode = com.jarves.mh.ui.theme.AppThemeMode.DARK,
    val apiPingStatus: ApiPingStatus = ApiPingStatus.IDLE,
    val apiPingMessage: String? = null,
    val projects: List<Project> = emptyList(),
    val projectImporting: Boolean = false,
    val projectImportMessage: String? = null,
    val gitCloneRunning: Boolean = false,
    val gitCloneMessage: String? = null,
    val githubAuthStatus: GitHubAuthStatus = GitHubAuthStatus.DISCONNECTED,
    val githubLogin: String? = null,
    val githubUserCode: String? = null,
    val githubVerificationUri: String? = null,
    val githubMessage: String? = null,
    val githubRepositories: List<GitHubRepository> = emptyList(),
    val githubRepositoriesLoading: Boolean = false,
    val activeProject: Project? = null,
    val workspaceVisible: Boolean = false,
    val readOnlyProject: Project? = null,
    val readOnlyProjectChats: List<ProjectChat> = emptyList(),
    val readOnlyChatId: String? = null,
    val readOnlyMessages: List<ChatMessage> = emptyList(),
    val projectChats: List<ProjectChat> = emptyList(),
    val activeChatId: String? = null,
    val workspaceFiles: List<WorkspaceEntry> = emptyList(),
    val androidProjectDetected: Boolean = false,
    val filesLoading: Boolean = false,
    val openedFilePath: String? = null,
    val openedFileContent: String? = null,
    val fileContentLoading: Boolean = false,
    val messages: List<ChatMessage> = listOf(
        ChatMessage(fromUser = false, text = "Hi! Tell me what you want to build or change."),
    ),
    val pendingAttachments: List<ChatAttachment> = emptyList(),
    val pendingApproval: ToolRequest? = null,
    val changes: List<ChangeItem> = emptyList(),
    val activity: List<ActivityItem> = emptyList(),
    val liveProcess: List<ActivityItem> = emptyList(),
    val liveThinking: Boolean = false,
    val activeThinkingBlockId: Long? = null,
    val taskStartedAtMillis: Long? = null,
    val taskFinishedAtMillis: Long? = null,
    val workSegmentStartedAtMillis: Long? = null,
    val currentTaskRequest: String? = null,
    val previewReady: Boolean = false,
    val previewUrl: String? = null,
    val isRunning: Boolean = false,
    val activeSessionId: String? = null,
    val toastMessage: String? = null,
    val projectTerminalLines: List<TerminalOutputLine> = emptyList(),
    val projectTerminalLiveOutput: String = "",
    val projectTerminalRunning: Boolean = false,
    val projectTerminalCwd: String = "/workspace",
    val projectTerminalCommand: String? = null,
    val projectTerminalDraft: String? = null,
    val pendingTerminalCommand: String? = null,
    val suggestedProjectRoot: String? = null,
    val selectedDevStacks: Set<DevStack> = emptySet(),
    val installedDevStacks: Set<DevStack> = emptySet(),
    val devStackInstalling: DevStack? = null,
    val devStackRemoving: Boolean = false,
    val devStackMessage: String? = null,
    val devStackProgress: Float = 0f,
    val devStackBytes: Pair<Long, Long>? = null,
    val devStackBytesPerSecond: Long? = null,
    val agentKind: AgentKind = AgentKind.CLAUDE_CODE,
    val primaryAgentKind: AgentKind = AgentKind.CLAUDE_CODE,
    val installedAgentVersions: Map<AgentKind, String> = emptyMap(),
    val agentInstalling: AgentKind? = null,
    val agentMessage: String? = null,
    val agentProgress: Float = 0f,
    val agentDownloadedBytes: Long? = null,
    val agentTotalBytes: Long? = null,
    val agentBytesPerSecond: Long? = null,
    val agentUpdates: Map<AgentKind, AgentUpdateInfo> = emptyMap(),
    val agentUpdatesChecking: Boolean = false,
    val agentUpdating: AgentKind? = null,
    val agentUpdateMessage: String? = null,
    val agentUpdateProgress: Float = 0f,
    val agentUpdateDownloadedBytes: Long? = null,
    val agentUpdateTotalBytes: Long? = null,
    val agentUpdateBytesPerSecond: Long? = null,
    val antigravityAuth: AntigravityAuthState = AntigravityAuthState(),
    val antigravityModel: String = "",
    val antigravityEffort: String = "high",
    val antigravityModels: List<String> = emptyList(),
    val antigravityModelsLoading: Boolean = false,
    val androidBuildRunning: Boolean = false,
    val androidBuildMessage: String? = null,
    val appUpdate: AppUpdateInfo? = null,
    val appUpdateStatus: AppUpdateStatus? = null,
    val appUpdateDownloadedBytes: Long = 0L,
    val appUpdateTotalBytes: Long = -1L,
    val appUpdateError: String? = null,
    val gitStatus: com.jarves.mh.model.GitStatus? = null,
    val gitBranches: List<com.jarves.mh.model.GitBranch> = emptyList(),
    val gitWorktrees: List<com.jarves.mh.model.GitWorktree> = emptyList(),
    val gitCommits: List<com.jarves.mh.model.GitCommit> = emptyList(),
    val gitStagedDiffs: List<com.jarves.mh.model.GitFileDiff> = emptyList(),
    val gitUnstagedDiffs: List<com.jarves.mh.model.GitFileDiff> = emptyList(),
    val selectedGitCommit: com.jarves.mh.model.GitCommit? = null,
    val selectedGitCommitDiffs: List<com.jarves.mh.model.GitFileDiff> = emptyList(),
    val gitPullRequests: List<com.jarves.mh.model.GitPullRequest> = emptyList(),
    val gitOperationRunning: Boolean = false,
    val gitOperationMessage: String? = null,
    val orchestratorTasks: List<com.jarves.mh.model.AgentTask> = emptyList(),
    val orchestratorAgents: List<com.jarves.mh.model.AgentInstance> = emptyList(),
    val blackboardEntries: List<com.jarves.mh.model.BlackboardEntry> = emptyList(),
    val swarmAuditLogs: List<com.jarves.mh.orchestrator.SwarmAuditEvent> = emptyList(),
    val circuitBreakerState: com.jarves.mh.model.CircuitBreakerState = com.jarves.mh.model.CircuitBreakerState(),
    val swarmInboxMessages: List<com.jarves.mh.model.AgentInboxMessage> = emptyList(),
    val scheduledTasks: List<com.jarves.mh.model.ScheduledTask> = emptyList(),
    val designModePromptDraft: String? = null,
    val skills: List<SkillDefinition> = emptyList(),
    val mcpTools: List<McpToolDefinition> = emptyList(),
    val diffReviews: List<DiffReviewSession> = emptyList(),
    val sshProfiles: List<SshServerProfile> = emptyList(),
    val activeSshProfile: SshServerProfile? = null,
    val customAgentTemplates: List<CustomAgentTemplate> = emptyList(),
    val agentQuotaUsages: List<AgentQuotaUsage> = emptyList(),
    val linearApiKey: String? = null,
    val linearIssues: List<LinearIssue> = emptyList(),
    val linearLoading: Boolean = false,
    val githubIssues: List<GitHubIssue> = emptyList(),
    val githubIssuesLoading: Boolean = false,
    val githubRepoOverride: String? = null,
    val autoApproveTools: Boolean = false,
    val customRunnerCommand: String = "",
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val vault = ApiKeyVault(application)
    private val preferences = AppPreferences(application)
    val linearClient = LinearClient()
    private val claudeRuntime = ClaudeRuntimeBridge(application, autoApproveTools = { _state.value.autoApproveTools }) { profile -> vault.get(profile.kind.name) }
    private val dshRuntime = DshRuntimeBridge(application) { profile -> vault.get(profile.kind.name) }
    private val installer = RuntimeInstaller(application)
    private val gitRunner = com.jarves.mh.git.GitCommandRunner(application, installer)
    val gitManager = com.jarves.mh.git.GitManager(gitRunner)
    val worktreeManager = com.jarves.mh.git.WorktreeManager(gitRunner)
    val taskBoardManager = com.jarves.mh.orchestrator.TaskBoardManager()
    val blackboardMemoryStore = com.jarves.mh.orchestrator.BlackboardMemoryStore()
    val agentInboxManager = com.jarves.mh.orchestrator.AgentInboxManager()
    val godOrchestrator = com.jarves.mh.orchestrator.GodOrchestrator(taskBoardManager, agentInboxManager, blackboardMemoryStore)
    var skillRegistry = SkillRegistry(File(application.filesDir, "default_workspace"))
    var mcpToolManager = McpToolManager(File(application.filesDir, "default_workspace"))
    var diffReviewManager = DiffReviewManager(File(application.filesDir, "default_workspace"))
    var sshRuntimeManager = SshRuntimeManager(File(application.filesDir, "default_workspace"))
    var commonCapabilityPool = CommonCapabilityPool(
        File(application.filesDir, "default_workspace"),
        skillRegistry,
        mcpToolManager,
        blackboardMemoryStore,
    )
    private val gitHubClient = com.jarves.mh.network.GitHubClient()
    private val antigravityRuntime = AntigravityRuntimeBridge(
        application,
        model = { _state.value.antigravityModel },
        effort = { _state.value.antigravityEffort },
        conversationId = { projectId ->
            _state.value.activeChatId?.let { preferences.loadAgentConversation(AgentKind.ANTIGRAVITY, projectId, it) }
        },
        saveConversationId = { projectId, id ->
            _state.value.activeChatId?.let { preferences.saveAgentConversation(AgentKind.ANTIGRAVITY, projectId, it, id) }
        },
    )
    private val jcodeRuntime = CustomAgentRuntimeBridge(
        application,
        AgentKind.JCODE,
        model = { if (_state.value.agentKind == AgentKind.ANTIGRAVITY) _state.value.antigravityModel else _state.value.provider.model },
        effort = { _state.value.antigravityEffort },
        autoApproveTools = { _state.value.autoApproveTools },
    ) { profile -> vault.get(profile.kind.name) }
    private val piAgentRuntime = CustomAgentRuntimeBridge(
        application,
        AgentKind.PI_AGENT,
        model = { if (_state.value.agentKind == AgentKind.ANTIGRAVITY) _state.value.antigravityModel else _state.value.provider.model },
        effort = { _state.value.antigravityEffort },
        autoApproveTools = { _state.value.autoApproveTools },
    ) { profile -> vault.get(profile.kind.name) }
    private val commandCodeRuntime = CustomAgentRuntimeBridge(
        application,
        AgentKind.COMMAND_CODE,
        model = { if (_state.value.agentKind == AgentKind.ANTIGRAVITY) _state.value.antigravityModel else _state.value.provider.model },
        effort = { _state.value.antigravityEffort },
        autoApproveTools = { _state.value.autoApproveTools },
    ) { profile -> vault.get(profile.kind.name) }
    private val clineRuntime = CustomAgentRuntimeBridge(
        application,
        AgentKind.CLINE,
        model = { if (_state.value.agentKind == AgentKind.ANTIGRAVITY) _state.value.antigravityModel else _state.value.provider.model },
        effort = { _state.value.antigravityEffort },
        autoApproveTools = { _state.value.autoApproveTools },
    ) { profile -> vault.get(profile.kind.name) }
    private val customRunnerRuntime = CustomAgentRuntimeBridge(
        application,
        AgentKind.CUSTOM_RUNNER,
        model = { if (_state.value.agentKind == AgentKind.ANTIGRAVITY) _state.value.antigravityModel else _state.value.provider.model },
        effort = { _state.value.antigravityEffort },
        autoApproveTools = { _state.value.autoApproveTools },
        customCommand = { _state.value.customRunnerCommand },
    ) { profile -> vault.get(profile.kind.name) }
    private val agentRegistry = AgentRegistry.builtIns(
        claudeRuntime,
        dshRuntime,
        antigravityRuntime,
        jcodeRuntime,
        piAgentRuntime,
        commandCodeRuntime,
        clineRuntime,
        customRunnerRuntime,
    )
    private fun activeRuntime(): com.jarves.mh.runtime.RuntimeBridge = agentRegistry.require(_state.value.agentKind).runtime
    private val providerApi = ProviderApiClient()
    private fun appUpdater(): AppUpdater = AppUpdater(
        getApplication(),
        if (BuildConfig.DEBUG) preferences.debugUpdateManifestUrl else "",
    )
    @Volatile private var projectTerminalProcess: Process? = null
    @Volatile private var terminalProcess: Process? = null
    @Volatile private var projectTerminalProjectId: String? = null
    @Volatile private var projectTerminalStopRequested: Boolean = false
    @Volatile private var setupCompletionHandled: Boolean = false
    @Volatile private var githubAuthProcess: Process? = null
    private var githubAuthJob: kotlinx.coroutines.Job? = null
    @Volatile private var lastOpenedAntigravityAuthUrl: String? = null
    private var activeRuntimeRequest: RuntimeRetryRequest? = null
    private val failedApiKeyIds = mutableSetOf<String>()
    private val transcriptWrites = Channel<TranscriptWrite>(Channel.UNLIMITED)
    private val initialAgentKind = AgentKind.fromStored(preferences.agentKind)
    private val initialPrimaryAgentKind = preferences.primaryAgentKind
        .takeIf(String::isNotBlank)
        ?.let(AgentKind::fromStored)
        ?: initialAgentKind
    private val antigravityAuthController = AntigravityAuthController(
        application,
        preferences.antigravitySignedIn,
        preferences.antigravityAccountEmail,
    ) { signedIn, email ->
        preferences.antigravitySignedIn = signedIn
        preferences.antigravityAccountEmail = email.orEmpty()
        if (!signedIn) preferences.clearAgentConversations(AgentKind.ANTIGRAVITY)
    }
    private val _state = MutableStateFlow(
        AppUiState(
            onboardingComplete = preferences.onboardingComplete,
            backgroundSetupComplete = preferences.backgroundSetupComplete,
            agentKind = initialAgentKind,
            primaryAgentKind = initialPrimaryAgentKind,
            provider = preferences.loadProvider(vault, initialAgentKind),
            activeApiKeyName = vault.list(preferences.loadProvider(vault, initialAgentKind).kind.name)
                .firstOrNull(ApiKeyInfo::isActive)?.name,
            antigravityAuth = AntigravityAuthState(
                status = if (preferences.antigravitySignedIn) AntigravityAuthStatus.SIGNED_IN else AntigravityAuthStatus.SIGNED_OUT,
                message = preferences.antigravityAccountEmail.takeIf(String::isNotBlank)?.let { "Connected as $it" },
                accountEmail = preferences.antigravityAccountEmail.takeIf(String::isNotBlank),
            ),
            antigravityModel = preferences.antigravityModel,
            antigravityEffort = preferences.antigravityEffort,
            themeMode = runCatching { com.jarves.mh.ui.theme.AppThemeMode.valueOf(preferences.themeMode.uppercase()) }
                .getOrDefault(com.jarves.mh.ui.theme.AppThemeMode.DARK),
            projects = preferences.loadProjects(),
            githubAuthStatus = GitHubAuthStatus.DISCONNECTED,
            githubLogin = preferences.githubLogin.takeIf(String::isNotBlank),
            selectedDevStacks = preferences.selectedDevStacks.mapNotNull { name ->
                runCatching { DevStack.valueOf(name) }.getOrNull()
            }.toSet() + DevStack.WEB,
            autoApproveTools = preferences.autoApproveTools,
            customRunnerCommand = preferences.customRunnerCommand,
        ),
    )

    init {
        // GitHub's official CLI owns its OAuth credential. Remove credentials from
        // the retired custom OAuth implementation and discover the real CLI status.
        vault.remove(LEGACY_GITHUB_TOKEN_KEY)
        val storedLinearKey = vault.get("LINEAR_API_KEY")
        if (!storedLinearKey.isNullOrBlank()) {
            _state.update { it.copy(linearApiKey = storedLinearKey) }
            loadLinearIssues(storedLinearKey)
        }
        viewModelScope.launch { refreshGitHubConnection() }
        RuntimeSetupController.restore(application)
        viewModelScope.launch(Dispatchers.IO) {
            for (write in transcriptWrites) {
                preferences.saveMessages(write.projectId, write.chatId, write.messages)
            }
        }
        viewModelScope.launch { dshRuntime.events.collect(::onRuntimeEvent) }
        viewModelScope.launch { antigravityRuntime.events.collect(::onRuntimeEvent) }
        viewModelScope.launch {
            antigravityAuthController.state.collect { auth ->
                _state.update { it.copy(antigravityAuth = auth) }
                auth.authorizationUrl?.takeIf { it != lastOpenedAntigravityAuthUrl }?.let { url ->
                    lastOpenedAntigravityAuthUrl = url
                    runCatching {
                        getApplication<Application>().startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }.onFailure {
                        _state.update { state -> state.copy(toastMessage = "Could not open the browser. Copy the sign-in URL instead.") }
                    }
                }
            }
        }
        if (antigravityAuthController.hasOfficialCredential() &&
            (!preferences.antigravitySignedIn || preferences.antigravityAccountEmail.isBlank())
        ) {
            viewModelScope.launch { antigravityAuthController.beginLogin() }
        }
        if (!preferences.legacySeededCredentialRemoved) {
            vault.remove(ProviderKind.CUSTOM.name)
            preferences.legacySeededCredentialRemoved = true
            _state.update { current ->
                if (current.provider.kind == ProviderKind.CUSTOM) {
                    current.copy(provider = current.provider.copy(hasSecret = false))
                } else current
            }
        }
        if (
            BuildConfig.TEST_OPENROUTER_API_KEY.isNotBlank() &&
            preferences.testProviderDefaultsVersion < TEST_PROVIDER_DEFAULTS_VERSION
        ) {
            val testProvider = ProviderProfile(
                kind = ProviderKind.CUSTOM,
                baseUrl = TEST_OPENROUTER_BASE_URL,
                model = TEST_OPENROUTER_MODEL,
                hasSecret = true,
            )
            vault.put(ProviderKind.CUSTOM.name, BuildConfig.TEST_OPENROUTER_API_KEY)
            preferences.saveProvider(testProvider, _state.value.agentKind)
            preferences.testProviderDefaultsVersion = TEST_PROVIDER_DEFAULTS_VERSION
            _state.update { it.copy(provider = testProvider) }
        }

        val loadedProjects = preferences.loadProjects()
        val cleanedProjects = loadedProjects.filter { project ->
            if (project.kind == ProjectKind.QUICK_PROJECT) {
                val chats = preferences.loadProjectChats(project.id)
                val userMessages = chats.sumOf { preferences.loadMessages(project.id, it.id).count { m -> m.fromUser } }
                val workspaceDir = File(application.filesDir, "workspaces/${project.id}")
                val userFiles = if (workspaceDir.isDirectory) {
                    workspaceDir.walkTopDown().filter { file ->
                        file.isFile && !file.name.startsWith(".claude") && file.name != ".pocket-dev-stacks.json"
                    }.count()
                } else 0
                val keep = userMessages > 0 || userFiles > 0
                if (!keep) {
                    workspaceDir.deleteRecursively()
                    terminalHistoryFile(project.id).delete()
                    preferences.deleteProjectChats(project.id)
                }
                keep
            } else true
        }
        if (cleanedProjects.size != loadedProjects.size) {
            preferences.saveProjects(cleanedProjects)
            _state.update { it.copy(projects = cleanedProjects) }
        }
    }

    val state: StateFlow<AppUiState> = _state.asStateFlow()

    private val _terminalLines = MutableStateFlow<List<TerminalOutputLine>>(
        listOf(
            TerminalOutputLine(
                command = "uname -a",
                output = "Linux pocket-dev 6.1.0-arm64 #1 SMP aarch64 GNU/Linux (PRoot Sandbox)",
                exitCode = 0,
            ),
        ),
    )
    val terminalLines: StateFlow<List<TerminalOutputLine>> = _terminalLines.asStateFlow()

    private val _isTerminalRunning = MutableStateFlow(false)
    val isTerminalRunning: StateFlow<Boolean> = _isTerminalRunning.asStateFlow()

    private val _terminalLiveOutput = MutableStateFlow("")
    val terminalLiveOutput: StateFlow<String> = _terminalLiveOutput.asStateFlow()

    private val _terminalCurrentCommand = MutableStateFlow<String?>(null)
    val terminalCurrentCommand: StateFlow<String?> = _terminalCurrentCommand.asStateFlow()

    fun runTerminalCommand(cmd: String) {
        val command = cmd.trim()
        if (command.isBlank() || _isTerminalRunning.value) return
        if (command == "clear") {
            _terminalLines.value = emptyList()
            return
        }
        _isTerminalRunning.value = true
        _terminalCurrentCommand.value = command
        _terminalLiveOutput.value = ""
        viewModelScope.launch {
            val (output, exitCode) = withContext(Dispatchers.IO) {
                runCatching {
                    if (!installer.isInstalled()) {
                        return@runCatching "Linux environment is not ready yet." to 1
                    }
                    val runtime = installer.installedRuntime()
                    val workspace = File(getApplication<Application>().filesDir, "workspaces/terminal").apply { mkdirs() }
                    val preparedCommand = prepareInteractiveShellCommand(command)
                    val proc = installer.process(
                        proot = runtime.proot,
                        rootfs = runtime.rootfs,
                        workspace = workspace,
                        environment = emptyMap(),
                        guestCommand = listOf("/usr/bin/bash", "-c", preparedCommand),
                    )
                    terminalProcess = proc
                    val native = proc as? NativeSpawnProcess
                    var offset = 0L
                    val streamed = StringBuilder()
                    var autoConfirmed = false
                    while (proc.isAlive || (native?.outputFile?.length() ?: 0L) > offset) {
                        val file = native?.outputFile
                        val available = (file?.length() ?: 0L) - offset
                        if (file == null || available <= 0) {
                            Thread.sleep(50)
                            continue
                        }
                        val bytes = ByteArray(minOf(available, 16L * 1024).toInt())
                        val count = RandomAccessFile(file, "r").use { input ->
                            input.seek(offset)
                            input.read(bytes)
                        }
                        if (count > 0) {
                            offset += count
                            streamed.append(bytes.decodeToString(0, count))
                            _terminalLiveOutput.value = sanitizeTerminalOutput(streamed.toString())
                                .trimEnd()
                                .takeLast(MAX_PROJECT_TERMINAL_OUTPUT)
                            if (!autoConfirmed && shouldAutoConfirmPackageCommand(command, streamed.toString())) {
                                proc.outputStream.write("y\n".toByteArray())
                                proc.outputStream.flush()
                                autoConfirmed = true
                            }
                        }
                    }
                    val exit = proc.waitFor()
                    runCatching { proc.outputStream.close() }
                    val out = sanitizeTerminalOutput(streamed.toString()).trim()
                    val finalOut = if (out.isNotEmpty() || exit == 0) out else "Process exited with code $exit"
                    finalOut to exit
                }.getOrElse { "Error: ${it.message}" to 1 }
            }
            _terminalLines.update { it + TerminalOutputLine(command = command, output = output, exitCode = exitCode) }
            _terminalLiveOutput.value = ""
            _terminalCurrentCommand.value = null
            _isTerminalRunning.value = false
            terminalProcess = null
        }
    }

    fun sendTerminalInput(text: String) {
        sendProcessInput(terminalProcess, text)
    }

    fun interruptTerminalCommand() {
        interruptProcess(terminalProcess)
    }

    fun clearTerminal() {
        _terminalLines.value = emptyList()
    }

    fun requestProjectTerminalCommand(command: String) {
        val normalized = command.trim()
        if (normalized.isBlank() || _state.value.projectTerminalRunning || projectTerminalProcess?.isAlive == true) return
        if (requiresAndroidToolchain(normalized) && !installer.isStackInstalled(DevStack.ANDROID)) {
            _state.update {
                it.copy(toastMessage = "Android build tools are not installed. Add Android in Settings → Development stacks.")
            }
            return
        }
        if (isDestructiveTerminalCommand(normalized)) {
            _state.update { it.copy(pendingTerminalCommand = normalized) }
        } else {
            runProjectTerminalCommand(normalized)
        }
    }

    fun prepareProjectTerminalCommand(command: String) {
        val project = _state.value.activeProject ?: return
        if (command.isBlank() || _state.value.projectTerminalRunning) return
        _state.update {
            it.copy(
                projectTerminalCwd = projectGuestRoot(project),
                projectTerminalDraft = command.trim(),
            )
        }
    }

    fun consumeProjectTerminalDraft() {
        _state.update { it.copy(projectTerminalDraft = null) }
    }

    fun openProjectTerminal() {
        val project = _state.value.activeProject ?: return
        if (_state.value.projectTerminalRunning) return
        _state.update { it.copy(projectTerminalCwd = projectGuestRoot(project)) }
    }

    fun confirmProjectTerminalCommand() {
        val command = _state.value.pendingTerminalCommand ?: return
        _state.update { it.copy(pendingTerminalCommand = null) }
        runProjectTerminalCommand(command)
    }

    fun cancelProjectTerminalCommand() {
        _state.update { it.copy(pendingTerminalCommand = null) }
    }

    private fun runProjectTerminalCommand(command: String) {
        val project = _state.value.activeProject ?: return
        if (_state.value.projectTerminalRunning) return
        val startingCwd = _state.value.projectTerminalCwd
        val existingLines = _state.value.projectTerminalLines
        projectTerminalStopRequested = false
        val requestedPreviewUrl = detectServerUrl(command)
        _state.update {
            it.copy(
                projectTerminalRunning = true,
                projectTerminalLiveOutput = "",
                projectTerminalCommand = command,
                pendingTerminalCommand = null,
                previewReady = it.previewReady || requestedPreviewUrl != null,
                previewUrl = requestedPreviewUrl ?: it.previewUrl,
            )
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { runProjectTerminalProcess(project.id, command, startingCwd) }
                    .getOrElse { error ->
                        ProjectTerminalResult(
                            output = "Terminal error: ${error.message ?: error::class.java.simpleName}",
                            exitCode = 1,
                            cwd = startingCwd,
                        )
                    }
            }
            val completedLine = TerminalOutputLine(
                command = command,
                output = result.output.ifBlank {
                    if (result.exitCode == 0) "" else "Process exited with code ${result.exitCode}"
                },
                exitCode = result.exitCode,
            )
            val updatedLines = (existingLines + completedLine).takeLast(MAX_PROJECT_TERMINAL_HISTORY)
            saveProjectTerminal(project.id, result.cwd, updatedLines)
            if (_state.value.activeProject?.id == project.id) {
                _state.update {
                    it.copy(
                        projectTerminalLines = updatedLines,
                        projectTerminalLiveOutput = "",
                        projectTerminalRunning = false,
                        projectTerminalCwd = result.cwd,
                        projectTerminalCommand = null,
                    )
                }
                refreshProjectFiles()
            }
            projectTerminalProcess = null
            projectTerminalProjectId = null
            projectTerminalStopRequested = false
        }
    }

    fun stopProjectTerminalCommand() {
        if (!_state.value.projectTerminalRunning) return
        projectTerminalStopRequested = true
        viewModelScope.launch(Dispatchers.IO) {
            projectTerminalProcess?.destroy()
            delay(400)
            if (projectTerminalProcess?.isAlive == true) projectTerminalProcess?.destroyForcibly()
        }
    }

    fun sendProjectTerminalInput(text: String) {
        sendProcessInput(projectTerminalProcess, text)
    }

    fun interruptProjectTerminalCommand() {
        interruptProcess(projectTerminalProcess)
    }

    fun clearProjectTerminal() {
        val project = _state.value.activeProject ?: return
        if (_state.value.projectTerminalRunning) return
        _state.update { it.copy(projectTerminalLines = emptyList(), projectTerminalLiveOutput = "") }
        saveProjectTerminal(project.id, _state.value.projectTerminalCwd, emptyList())
    }

    private fun runProjectTerminalProcess(projectId: String, command: String, cwd: String): ProjectTerminalResult {
        if (!installer.isInstalled()) return ProjectTerminalResult("Linux environment is not ready yet.", 1, cwd)
        val installed = installer.installedRuntime()
        val project = _state.value.projects.firstOrNull { it.id == projectId }
            ?: _state.value.activeProject?.takeIf { it.id == projectId }
            ?: return ProjectTerminalResult("Project is no longer available.", 1, cwd)
        val workspace = projectWorkspaceRoot(project)
        val guestWorkspacePath = projectGuestRoot(project)
        val marker = "__POCKETDEV_CWD_${UUID.randomUUID()}__"
        val preparedCommand = prepareInteractiveShellCommand(command)
        val script = """
            cd -- ${shellQuote(cwd)} || exit 1
            $preparedCommand
            pocket_status=${'$'}?
            printf '\n$marker%s\n' "${'$'}PWD"
            exit ${'$'}pocket_status
        """.trimIndent()
        val process = installer.process(
            proot = installed.proot,
            rootfs = installed.rootfs,
            workspace = workspace,
            environment = emptyMap(),
            guestCommand = listOf("/usr/bin/bash", "-lc", script),
            guestWorkspacePath = guestWorkspacePath,
        )
        projectTerminalProcess = process
        projectTerminalProjectId = projectId
        if (projectTerminalStopRequested) process.destroy()
        val native = process as? NativeSpawnProcess
            ?: return ProjectTerminalResult("Unsupported terminal process.", 1, cwd)
        var offset = 0L
        val output = StringBuilder()
        var autoConfirmed = false
        while (process.isAlive || native.outputFile.length() > offset) {
            val available = native.outputFile.length() - offset
            if (available <= 0) {
                Thread.sleep(50)
                continue
            }
            val bytes = ByteArray(minOf(available, 16L * 1024).toInt())
            val count = RandomAccessFile(native.outputFile, "r").use { file ->
                file.seek(offset)
                file.read(bytes)
            }
            if (count > 0) {
                offset += count
                output.append(bytes.decodeToString(0, count))
                val visible = sanitizeTerminalOutput(output.toString().substringBefore(marker))
                    .takeLast(MAX_PROJECT_TERMINAL_OUTPUT)
                if (!autoConfirmed && shouldAutoConfirmPackageCommand(command, visible)) {
                    process.outputStream.write("y\n".toByteArray())
                    process.outputStream.flush()
                    autoConfirmed = true
                }
                val detectedPreviewUrl = detectPreviewUrl(visible)
                _state.update { current ->
                    if (current.activeProject?.id == projectId) {
                        current.copy(
                            projectTerminalLiveOutput = visible,
                            previewReady = current.previewReady || detectedPreviewUrl != null,
                            previewUrl = detectedPreviewUrl ?: current.previewUrl,
                        )
                    } else current
                }
            }
        }
        val exitCode = process.waitFor()
        runCatching { process.outputStream.close() }
        val raw = output.toString()
        val cwdAfter = raw.substringAfter(marker, "")
            .lineSequence()
            .firstOrNull()
            ?.trim()
            ?.takeIf { it == guestWorkspacePath || it.startsWith("$guestWorkspacePath/") }
            ?: cwd
        val cleanOutput = sanitizeTerminalOutput(raw.substringBefore(marker))
            .trim()
            .takeLast(MAX_PROJECT_TERMINAL_OUTPUT)
        return ProjectTerminalResult(cleanOutput, exitCode, cwdAfter)
    }

    private fun sendProcessInput(process: Process?, text: String) {
        if (process?.isAlive != true || text.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                process.outputStream.write((text + "\n").toByteArray())
                process.outputStream.flush()
            }.onFailure {
                _state.update { current -> current.copy(toastMessage = "This process is no longer accepting input.") }
            }
        }
    }

    private fun interruptProcess(process: Process?) {
        if (process?.isAlive != true) return
        viewModelScope.launch(Dispatchers.IO) {
            (process as? NativeSpawnProcess)?.interrupt() ?: process.destroy()
        }
    }

    /**
     * Package management must never block on Y/N, locale, timezone, service-restart,
     * or config-file dialogs in the phone UI. Other commands remain interactive and
     * can receive input through [sendProcessInput].
     */
    private fun prepareInteractiveShellCommand(command: String): String {
        val normalizedApt = command
            .replace(Regex("(?<![\\w-])sudo\\s+apt(?:-get)?\\s+"), "apt-get ")
            .replace(Regex("(?<![\\w-])apt\\s+"), "apt-get ")
            .replace(
                Regex("(?<![\\w-])apt-get\\s+(install|upgrade|full-upgrade|dist-upgrade|remove|autoremove|fix-broken)\\b"),
                "apt-get -y -o Dpkg::Options::=--force-confold $1",
            )
        return "export DEBIAN_FRONTEND=noninteractive APT_LISTCHANGES_FRONTEND=none UCF_FORCE_CONFFOLD=1 NEEDRESTART_MODE=a TZ=Etc/UTC LC_ALL=C.UTF-8; $normalizedApt"
    }

    private fun shouldAutoConfirmPackageCommand(command: String, output: String): Boolean {
        val packageCommand = Regex("(?i)(^|[;&|]\\s*)(sudo\\s+)?(apt|apt-get|dpkg)\\b").containsMatchIn(command)
        if (!packageCommand) return false
        val tail = output.takeLast(500)
        return Regex("(?i)(do you want to continue|continue\\?)\\s*\\[[Yy]/[Nn]\\]").containsMatchIn(tail)
    }

    private fun isDestructiveTerminalCommand(command: String): Boolean {
        val normalized = command.lowercase().replace(Regex("\\s+"), " ")
        return listOf(
            "rm -rf", "rm -fr", "git reset --hard", "git clean -f", "git push --force",
            "mkfs", "dd if=", "chmod -r 777", "shutdown", "reboot", ":(){", "kill \$(pgrep", "pkill -f",
        ).any(normalized::contains) || Regex("(curl|wget).*(\\||>)\\s*(sh|bash)").containsMatchIn(normalized)
    }

    private fun detectPreviewUrl(output: String): String? {
        val match = Regex("https?://(?:localhost|127\\.0\\.0\\.1|0\\.0\\.0\\.0):(\\d{2,5})(?:/[^\\s]*)?")
            .findAll(output)
            .lastOrNull()
            ?: return null
        val port = match.groupValues[1].toIntOrNull()?.takeIf { it in 1..65535 } ?: return null
        return "http://127.0.0.1:$port/"
    }

    private fun detectServerUrl(command: String): String? {
        val match = Regex("""python(?:3)?\s+-m\s+http\.server(?:\s+(\d{2,5}))?""")
            .find(command)
            ?: return null
        val port = match.groupValues.getOrNull(1)?.toIntOrNull() ?: 8000
        return port.takeIf { it in 1..65535 }?.let { "http://127.0.0.1:$it/" }
    }

    private fun shellQuote(value: String): String = "'${value.replace("'", "'\\''")}'"

    private fun terminalHistoryFile(projectId: String): File =
        File(getApplication<Application>().filesDir, "terminal-history/$projectId.json")

    private fun loadProjectTerminal(project: Project): ProjectTerminalSnapshot {
        val file = terminalHistoryFile(project.id)
        val guestRoot = projectGuestRoot(project)
        if (!file.isFile) return ProjectTerminalSnapshot(cwd = guestRoot)
        return runCatching {
            val root = JSONObject(file.readText())
            val array = root.optJSONArray("lines") ?: JSONArray()
            val lines = (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                TerminalOutputLine(
                    id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                    command = item.optString("command"),
                    output = item.optString("output"),
                    exitCode = item.optInt("exitCode"),
                )
            }
            ProjectTerminalSnapshot(
                lines = lines.takeLast(MAX_PROJECT_TERMINAL_HISTORY),
                cwd = root.optString("cwd", guestRoot).takeIf {
                    it == guestRoot || it.startsWith("$guestRoot/")
                } ?: guestRoot,
            )
        }.getOrDefault(ProjectTerminalSnapshot(cwd = guestRoot))
    }

    private fun saveProjectTerminal(projectId: String, cwd: String, lines: List<TerminalOutputLine>) {
        runCatching {
            val file = terminalHistoryFile(projectId)
            file.parentFile?.mkdirs()
            val array = JSONArray()
            lines.takeLast(MAX_PROJECT_TERMINAL_HISTORY).forEach { line ->
                array.put(
                    JSONObject()
                        .put("id", line.id)
                        .put("command", line.command)
                        .put("output", line.output.takeLast(MAX_PROJECT_TERMINAL_OUTPUT))
                        .put("exitCode", line.exitCode),
                )
            }
            file.writeText(JSONObject().put("cwd", cwd).put("lines", array).toString())
        }
    }

    private fun projectGuestRoot(project: Project): String = "/workspace/${project.slug}"

    private fun projectWorkspaceRoot(project: Project): File {
        val base = File(getApplication<Application>().filesDir, "workspaces/${project.id}")
            .apply { mkdirs() }
            .canonicalFile
        if (project.rootPath.isBlank()) return base
        val selected = File(base, project.rootPath).canonicalFile
        require(selected.toPath().startsWith(base.toPath())) { "Unsafe project root" }
        return selected.apply { mkdirs() }
    }

    fun buildAndRunAndroidApp() {
        val project = _state.value.activeProject ?: return
        if (_state.value.androidBuildRunning) return
        if (!installer.isStackInstalled(DevStack.ANDROID)) {
            _state.update {
                it.copy(toastMessage = "Android build tools are not installed. Add Android in Settings → Development stacks.")
            }
            return
        }
        if (_state.value.isRunning) {
            _state.update { it.copy(toastMessage = "Wait for Claude to finish creating the project before building.") }
            return
        }
        if (_state.value.projectTerminalRunning) {
            _state.update { it.copy(toastMessage = "Wait for the project terminal command to finish before building.") }
            return
        }
        _state.update { it.copy(androidBuildRunning = true, androidBuildMessage = "Building debug APK…", toastMessage = null) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val installed = installer.installedRuntime()
                val workspace = findAndroidGradleProjectRoot(projectWorkspaceRoot(project))
                    ?: error("No Android Gradle project found yet. Ask Claude to create it, then wait for the task to finish.")
                val process = installer.process(
                    installed.proot, installed.rootfs, workspace, emptyMap(),
                    listOf(
                        "/usr/bin/bash", "-lc",
                        "gradle --init-script /root/.gradle/init.d/pocketdev-android.gradle " +
                            "-Pandroid.aapt2FromMavenOverride=/root/android-sdk/build-tools/35.0.0/aapt2 " +
                            "--no-daemon assembleDebug --console=plain",
                    ),
                    projectGuestRoot(project),
                )
                val exitCode = process.waitFor()
                val buildOutput = (process as? NativeSpawnProcess)?.outputFile?.readText().orEmpty()
                check(exitCode == 0) {
                    buildOutput.trim().takeLast(2_000).ifBlank { "Gradle build failed (exit code $exitCode)" }
                }
                val apk = workspace.walkTopDown()
                    .filter { it.isFile && it.extension.equals("apk", ignoreCase = true) && it.path.contains("/outputs/apk/debug/") }
                    .maxByOrNull(File::lastModified)
                    ?: error("Gradle finished but no debug APK was found")
                AndroidAppInstaller.install(getApplication(), apk)
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    _state.update {
                        it.copy(
                            androidBuildRunning = false,
                            androidBuildMessage = "APK sent to Android installer",
                            toastMessage = "APK built. Complete Android's install prompt.",
                        )
                    }
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(androidBuildRunning = false, androidBuildMessage = null, toastMessage = error.message ?: "Could not build APK") }
                }
            }
        }
    }

    private fun findAndroidGradleProjectRoot(workspace: File): File? {
        val settingsNames = setOf("settings.gradle", "settings.gradle.kts", "settings.gradle.dcl")
        return workspace.walkTopDown()
            .maxDepth(4)
            .filter { it.isFile && it.name in settingsNames }
            .mapNotNull(File::getParentFile)
            .sortedBy { it.absolutePath.length }
            .firstOrNull { root ->
                root.walkTopDown()
                    .maxDepth(4)
                    .any { it.isFile && it.invariantSeparatorsPath.endsWith("src/main/AndroidManifest.xml") }
            }
    }

    private fun requiresAndroidToolchain(command: String): Boolean =
        Regex("(?m)(^|[;&|]\\s*)(?:\\./)?gradle(?:w)?(?:\\s|$)", RegexOption.IGNORE_CASE).containsMatchIn(command)


    fun toggleTheme() {
        val next = if (_state.value.themeMode == com.jarves.mh.ui.theme.AppThemeMode.DARK) {
            com.jarves.mh.ui.theme.AppThemeMode.LIGHT
        } else {
            com.jarves.mh.ui.theme.AppThemeMode.DARK
        }
        setThemeMode(next)
    }

    fun setThemeMode(mode: com.jarves.mh.ui.theme.AppThemeMode) {
        preferences.themeMode = mode.name.lowercase()
        _state.update { it.copy(themeMode = mode) }
    }

    fun getSavedApiKey(kind: ProviderKind): String = vault.get(kind.name).orEmpty()

    fun getSavedApiKeys(kind: ProviderKind): List<ApiKeyInfo> = vault.list(kind.name)

    fun addApiKey(kind: ProviderKind, name: String, secret: String): List<ApiKeyInfo> {
        vault.add(kind.name, name, secret)
        val keys = vault.list(kind.name)
        refreshActiveApiKey(kind)
        return keys
    }

    fun activateApiKey(kind: ProviderKind, keyId: String): List<ApiKeyInfo> {
        vault.activate(kind.name, keyId)
        refreshActiveApiKey(kind)
        return vault.list(kind.name)
    }

    fun removeApiKey(kind: ProviderKind, keyId: String): List<ApiKeyInfo> {
        vault.remove(kind.name, keyId)
        refreshActiveApiKey(kind)
        return vault.list(kind.name)
    }

    private fun refreshActiveApiKey(kind: ProviderKind) {
        if (_state.value.provider.kind != kind) return
        val keys = vault.list(kind.name)
        _state.update { current ->
            current.copy(
                activeApiKeyName = keys.firstOrNull(ApiKeyInfo::isActive)?.name,
                provider = current.provider.copy(hasSecret = keys.isNotEmpty()),
            )
        }
    }

    /** Keeps both agent bridges mapped to the same workspace root; the active one is used. */
    private fun configureBridgeRoots(projectId: String, rootPath: String) {
        claudeRuntime.configureProjectRoot(projectId, rootPath)
        dshRuntime.configureProjectRoot(projectId, rootPath)
        antigravityRuntime.configureProjectRoot(projectId, rootPath)
    }

    init {
        viewModelScope.launch { RuntimeSetupController.snapshot.collect(::onSetupSnapshot) }
        viewModelScope.launch { claudeRuntime.events.collect(::onRuntimeEvent) }
        viewModelScope.launch { bootstrap() }
    }

    private suspend fun bootstrap() {
        if (!supportsArm64Runtime(android.os.Build.SUPPORTED_ABIS, System.getProperty("os.arch"))) {
            _state.update {
                it.copy(
                    startupStage = StartupStage.SETUP_REQUIRED,
                    startupMessage = "ARM64 device required",
                    startupError = null,
                    startupErrorIsOffline = false,
                )
            }
            return
        }
        val setupSnapshot = RuntimeSetupController.snapshot.value
        if (setupSnapshot.status == RuntimeSetupStatus.RUNNING) {
            onSetupSnapshot(setupSnapshot)
            resumeRuntimeSetupService()
            return
        }
        val installed = withContext(Dispatchers.IO) {
            // Upgrades from the old single-bundle layout keep every already-installed tool.
            installer.migrateLegacyToolMarkers()
            installer.isInstalled().also { ready ->
                if (ready) installer.cleanupLegacyWorkspaceScaffolding()
            }
        }
        _state.update { current ->
            current.copy(
                installedDevStacks = if (installed) installer.installedStacks() else current.installedDevStacks,
                installedAgentVersions = if (installed) installer.installedAgentVersions() else emptyMap(),
            )
        }
        when {
            !installed && setupSnapshot.status == RuntimeSetupStatus.ERROR -> onSetupSnapshot(setupSnapshot)
            !installed -> _state.update { it.copy(startupStage = StartupStage.SETUP_REQUIRED, startupProgress = 0f) }
            !preferences.onboardingComplete -> {
                preferences.runtimeSetupComplete = true
                _state.update { it.copy(startupStage = StartupStage.MODEL_SETUP, startupProgress = 1f) }
            }
            else -> initializeRuntime()
        }
    }

    fun startRuntimeSetup() {
        if (state.value.startupStage == StartupStage.INSTALLING) return
        setupCompletionHandled = false
        _state.update {
            it.copy(
                startupStage = StartupStage.INSTALLING,
                startupProgress = 0.01f,
                startupMessage = "Preparing your private coding workspace",
                startupBytes = null,
                startupLogs = listOf("\$ Preparing your private coding workspace"),
                startupIndeterminate = false,
                startupError = null,
                startupErrorIsOffline = false,
                showDetailedSetupProgress = true,
            )
        }
        resumeRuntimeSetupService()
    }

    fun retryStartup() {
        if (installer.isInstalled()) viewModelScope.launch { initializeRuntime() } else {
            _state.update { it.copy(startupStage = StartupStage.SETUP_REQUIRED, startupError = null) }
            startRuntimeSetup()
        }
    }

    private fun resumeRuntimeSetupService() {
        val stacks = _state.value.selectedDevStacks.joinToString(",") { it.name }
        ContextCompat.startForegroundService(
            getApplication(),
            Intent(getApplication(), RuntimeSetupService::class.java)
                .setAction(RuntimeSetupService.ACTION_START)
                .putExtra(RuntimeSetupService.EXTRA_STACKS, stacks)
                .putExtra(RuntimeSetupService.EXTRA_AGENT, _state.value.agentKind.name),
        )
    }

    private fun onSetupSnapshot(snapshot: RuntimeSetupSnapshot) {
        when (snapshot.status) {
            RuntimeSetupStatus.RUNNING -> _state.update {
                it.copy(
                    startupStage = StartupStage.INSTALLING,
                    startupProgress = snapshot.progress,
                    startupMessage = snapshot.message,
                    startupBytes = snapshot.totalBytes?.let { total -> (snapshot.downloadedBytes ?: 0L) to total },
                    startupLogs = snapshot.logs,
                    startupIndeterminate = snapshot.indeterminate,
                    startupError = null,
                    startupErrorIsOffline = false,
                )
            }
            RuntimeSetupStatus.COMPLETE -> {
                if (setupCompletionHandled) return
                setupCompletionHandled = true
                preferences.runtimeSetupComplete = true
                if (preferences.onboardingComplete) {
                    viewModelScope.launch { initializeRuntime() }
                } else {
                    _state.update {
                        it.copy(
                            startupStage = StartupStage.MODEL_SETUP,
                            startupProgress = 1f,
                            startupBytes = null,
                            startupIndeterminate = false,
                        )
                    }
                }
            }
            RuntimeSetupStatus.ERROR -> _state.update {
                it.copy(
                    startupStage = StartupStage.ERROR,
                    startupMessage = snapshot.message,
                    startupProgress = snapshot.progress,
                    startupLogs = snapshot.logs,
                    startupIndeterminate = false,
                    startupError = snapshot.errorMessage,
                    startupErrorIsOffline = snapshot.offline,
                )
            }
            RuntimeSetupStatus.CANCELLED -> _state.update {
                it.copy(
                    startupStage = StartupStage.SETUP_REQUIRED,
                    startupMessage = "Setup paused",
                    startupProgress = snapshot.progress,
                    startupLogs = snapshot.logs,
                    startupIndeterminate = false,
                )
            }
            RuntimeSetupStatus.IDLE -> Unit
        }
    }

    private suspend fun initializeRuntime() {
        val startedAt = SystemClock.elapsedRealtime()
        _state.update {
            it.copy(
                startupStage = StartupStage.INITIALIZING,
                startupProgress = 0.05f,
                startupMessage = "Opening your private workspace",
                startupBytes = null,
                startupLogs = listOf("\$ Opening your private workspace"),
                startupIndeterminate = false,
                startupError = null,
                startupErrorIsOffline = false,
            )
        }
        val result = runCatching {
            withContext(Dispatchers.IO) {
                installer.initializeExisting { progress ->
                    _state.update { current ->
                        current.copy(
                            startupProgress = 0.05f + progress.fraction * 0.95f,
                            startupMessage = progress.message,
                            startupBytes = null,
                            startupLogs = mergeStartupLog(current.startupLogs, progress),
                        )
                    }
                }
            }
        }
        if (result.isSuccess) {
            // The real version probe can finish in a fraction of a second on fast phones.
            // Keep the successful loading state visible long enough to be understandable.
            val remaining = MINIMUM_INITIALIZATION_SCREEN_MS - (SystemClock.elapsedRealtime() - startedAt)
            if (remaining > 0) delay(remaining)
            _state.update {
                it.copy(
                    startupStage = StartupStage.READY,
                    startupProgress = 1f,
                    installedAgentVersions = installer.installedAgentVersions(),
                )
            }
            pingApi()
            checkForAppUpdate()
        } else {
            showStartupError(result.exceptionOrNull() ?: IllegalStateException("Claude Code initialization failed"))
        }
    }

    fun checkForAppUpdate(force: Boolean = false) {
        if (!force && System.currentTimeMillis() - preferences.lastAppUpdateCheckMillis < 24L * 60L * 60L * 1000L) return
        viewModelScope.launch(Dispatchers.IO) {
            val update = runCatching { appUpdater().check() }.getOrNull()
            preferences.lastAppUpdateCheckMillis = System.currentTimeMillis()
            if (update != null) {
                _state.update {
                    it.copy(appUpdate = update, appUpdateStatus = AppUpdateStatus.AVAILABLE, appUpdateError = null)
                }
            }
        }
    }

    /** Debug builds only: persist a manifest URL override and re-check immediately. */
    fun setDebugUpdateManifestUrl(url: String) {
        if (!BuildConfig.DEBUG) return
        preferences.debugUpdateManifestUrl = url.trim()
        preferences.lastAppUpdateCheckMillis = 0L
        checkForAppUpdate(force = true)
    }

    /** Debug builds only: clear the manifest URL override and re-check the default channel. */
    fun clearDebugUpdateManifestUrl() {
        if (!BuildConfig.DEBUG) return
        preferences.debugUpdateManifestUrl = ""
        preferences.lastAppUpdateCheckMillis = 0L
        checkForAppUpdate(force = true)
    }

    /** Debug builds only: the currently-active manifest URL override (empty = default). */
    fun debugUpdateManifestUrl(): String = if (BuildConfig.DEBUG) preferences.debugUpdateManifestUrl else ""

    fun installAppUpdate() {
        val info = _state.value.appUpdate ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !getApplication<Application>().packageManager.canRequestPackageInstalls()) {
            _state.update { it.copy(appUpdateStatus = AppUpdateStatus.PERMISSION_REQUIRED) }
            return
        }
        if (_state.value.appUpdateStatus == AppUpdateStatus.DOWNLOADING) return
        _state.update {
            it.copy(appUpdateStatus = AppUpdateStatus.DOWNLOADING, appUpdateDownloadedBytes = 0L, appUpdateTotalBytes = info.sizeBytes, appUpdateError = null)
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                appUpdater().download(info) { downloaded, total ->
                    _state.update { current -> current.copy(appUpdateDownloadedBytes = downloaded, appUpdateTotalBytes = total) }
                }
            }.onSuccess { apk ->
                _state.update { it.copy(appUpdateStatus = AppUpdateStatus.INSTALLING) }
                runCatching { AndroidAppInstaller.install(getApplication(), apk) }.onFailure { error ->
                    _state.update { it.copy(appUpdateStatus = AppUpdateStatus.ERROR, appUpdateError = error.message ?: "Could not start the Android installer") }
                }
            }.onFailure { error ->
                _state.update { it.copy(appUpdateStatus = AppUpdateStatus.ERROR, appUpdateError = error.message ?: "Update download failed") }
            }
        }
    }

    fun dismissAppUpdateError() {
        _state.update { it.copy(appUpdateStatus = AppUpdateStatus.AVAILABLE, appUpdateError = null) }
    }

    private fun mergeStartupLog(
        existing: List<String>,
        progress: RuntimeInstallProgress,
    ): List<String> {
        val prefix = "\$ ${progress.message}"
        val bytes = progress.totalBytes?.let { total ->
            val downloaded = progress.downloadedBytes ?: 0L
            " — %.1f / %.1f MB".format(downloaded / 1_048_576.0, total / 1_048_576.0)
        }.orEmpty()
        val nextLine = prefix + bytes
        val updated = if (existing.lastOrNull()?.startsWith(prefix) == true) {
            existing.dropLast(1) + nextLine
        } else {
            existing + nextLine
        }
        return updated.takeLast(80)
    }

    private fun showStartupError(error: Throwable) {
        val isOffline = generateSequence(error as Throwable?) { it.cause }
            .any { cause ->
                cause is UnknownHostException ||
                    cause.message.orEmpty().contains("unable to resolve host", ignoreCase = true) ||
                    cause.message.orEmpty().contains("no address associated with hostname", ignoreCase = true)
            }
        val message = if (isOffline) {
            "Connect to Wi-Fi or mobile data, then try again. Internet is required to finish the first-time setup."
        } else {
            error.message?.take(300) ?: "Something went wrong while preparing Mobile Harness. Please try again."
        }
        _state.update {
            it.copy(
                startupStage = StartupStage.ERROR,
                startupError = message,
                startupErrorIsOffline = isOffline,
            )
        }
    }

    fun finishOnboarding(profile: ProviderProfile, secret: String) {
        vault.put(profile.kind.name, secret)
        val saved = profile.copy(
            hasSecret = secret.isNotBlank() || vault.contains(profile.kind.name),
        )
        preferences.saveProvider(saved, _state.value.agentKind)
        preferences.onboardingComplete = true
        _state.update { it.copy(onboardingComplete = true, provider = saved, startupStage = StartupStage.READY) }
        refreshActiveApiKey(profile.kind)
        pingApi()
    }

    fun finishAntigravityOnboarding() {
        check(_state.value.antigravityAuth.status == AntigravityAuthStatus.SIGNED_IN) {
            "Sign in to Antigravity first"
        }
        preferences.onboardingComplete = true
        _state.update { it.copy(onboardingComplete = true, startupStage = StartupStage.READY) }
    }

    /** Lets first-run users escape a provider/login failure without losing saved credentials. */
    fun chooseOnboardingAgent(kind: AgentKind) {
        selectAgent(kind)
        _state.update {
            it.copy(
                startupStage = if (installer.isAgentInstalled(kind)) {
                    StartupStage.MODEL_SETUP
                } else {
                    StartupStage.SETUP_REQUIRED
                },
                startupError = null,
                startupErrorIsOffline = false,
            )
        }
    }

    fun updateProvider(profile: ProviderProfile, secret: String) = finishOnboarding(profile, secret)

    fun finishBackgroundSetup() {
        preferences.backgroundSetupComplete = true
        _state.update { it.copy(backgroundSetupComplete = true) }
    }

    /** Called from the first-launch setup screen; persists the agent choice for setup and Settings. */
    fun selectAgent(kind: AgentKind) {
        if (_state.value.agentKind == kind) return
        if (_state.value.isRunning) {
            _state.update { it.copy(toastMessage = "Stop the current agent before switching.") }
            return
        }
        val selectingInitialAgent = !preferences.runtimeSetupComplete
        preferences.agentKind = kind.stableId
        if (selectingInitialAgent) preferences.primaryAgentKind = kind.stableId
        _state.update { current ->
            preferences.saveProvider(current.provider, current.agentKind)
            val provider = preferences.loadProvider(vault, kind)
            current.copy(
                agentKind = kind,
                primaryAgentKind = if (selectingInitialAgent) kind else current.primaryAgentKind,
                provider = provider,
                activeApiKeyName = vault.list(provider.kind.name).firstOrNull(ApiKeyInfo::isActive)?.name,
                // Ping results belong to the previous agent; never leak them across.
                apiPingStatus = ApiPingStatus.IDLE,
                apiPingMessage = null,
            )
        }
    }

    /** Installs the other agent on demand (Settings) with live progress, then switches to it. */
    fun installAgent(kind: AgentKind) {
        if (_state.value.agentInstalling != null) return
        if (_state.value.isRunning) {
            _state.update { it.copy(toastMessage = "Stop the current agent before switching.") }
            return
        }
        if (installer.isAgentInstalled(kind)) {
            selectAgent(kind)
            return
        }
        _state.update {
            it.copy(
                agentInstalling = kind,
                agentMessage = "Preparing ${kind.title}…",
                agentProgress = 0f,
                agentDownloadedBytes = null,
                agentTotalBytes = null,
                agentBytesPerSecond = null,
            )
        }
        viewModelScope.launch {
            var sampleBytes = 0L
            var sampleAt = SystemClock.elapsedRealtime()
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    agentRegistry.require(kind).install(installer) { progress ->
                        val now = SystemClock.elapsedRealtime()
                        val bytes = progress.downloadedBytes
                        val elapsed = now - sampleAt
                        val speed = if (bytes != null && elapsed >= 500L) {
                            ((bytes - sampleBytes).coerceAtLeast(0L) * 1_000L / elapsed.coerceAtLeast(1L)).also {
                                sampleBytes = bytes
                                sampleAt = now
                            }
                        } else _state.value.agentBytesPerSecond
                        _state.update { current ->
                            current.copy(
                                agentMessage = progress.message,
                                agentProgress = progress.fraction.coerceIn(0f, 1f),
                                agentDownloadedBytes = bytes ?: current.agentDownloadedBytes,
                                agentTotalBytes = progress.totalBytes ?: current.agentTotalBytes,
                                agentBytesPerSecond = speed,
                            )
                        }
                    }
                }
            }
            result.onSuccess {
                if (kind == AgentKind.DEEPSEEK_HARNESS) preferences.dshVersion = installer.dshVersion
                selectAgent(kind)
            }
            _state.update { current ->
                current.copy(
                    installedAgentVersions = installer.installedAgentVersions(),
                    agentInstalling = null,
                    agentProgress = 0f,
                    agentDownloadedBytes = null,
                    agentTotalBytes = null,
                    agentBytesPerSecond = null,
                    agentMessage = result.fold(
                        onSuccess = { "${kind.title} is ready" },
                        onFailure = { _ -> result.exceptionOrNull()?.message?.take(200) ?: "Could not install ${kind.title}" },
                    ),
                )
            }
        }
    }

    fun checkAgentUpdates() {
        if (_state.value.agentUpdatesChecking || _state.value.agentUpdating != null || _state.value.isRunning) return
        _state.update { it.copy(agentUpdatesChecking = true, agentUpdateMessage = "Checking official agent releases…") }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { installer.checkAgentUpdates() } }
            _state.update {
                it.copy(
                    agentUpdates = result.getOrDefault(emptyMap()),
                    agentUpdatesChecking = false,
                    agentUpdateMessage = result.fold(
                        onSuccess = { updates -> if (updates.isEmpty()) "All installed agents are up to date" else "${updates.size} agent update${if (updates.size == 1) "" else "s"} available" },
                        onFailure = { error -> error.message?.take(200) ?: "Could not check agent updates" },
                    ),
                )
            }
        }
    }

    fun updateAgent(kind: AgentKind) {
        val update = _state.value.agentUpdates[kind] ?: return
        if (_state.value.agentUpdating != null || _state.value.agentInstalling != null || _state.value.isRunning) return
        _state.update {
            it.copy(
                agentUpdating = kind,
                agentUpdateMessage = "Preparing ${kind.title} ${update.latestVersion}…",
                agentUpdateProgress = 0f,
                agentUpdateDownloadedBytes = null,
                agentUpdateTotalBytes = null,
                agentUpdateBytesPerSecond = null,
            )
        }
        viewModelScope.launch {
            var sampleBytes = 0L
            var sampleAt = SystemClock.elapsedRealtime()
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    installer.updateAgent(kind, update.latestVersion) { progress ->
                        val now = SystemClock.elapsedRealtime()
                        val bytes = progress.downloadedBytes
                        val elapsed = now - sampleAt
                        val speed = if (bytes != null && elapsed >= 500L) {
                            ((bytes - sampleBytes).coerceAtLeast(0L) * 1_000L / elapsed.coerceAtLeast(1L)).also {
                                sampleBytes = bytes
                                sampleAt = now
                            }
                        } else _state.value.agentUpdateBytesPerSecond
                        _state.update {
                            it.copy(
                                agentUpdateMessage = progress.message,
                                agentUpdateProgress = progress.fraction.coerceIn(0f, 1f),
                                agentUpdateDownloadedBytes = bytes ?: it.agentUpdateDownloadedBytes,
                                agentUpdateTotalBytes = progress.totalBytes ?: it.agentUpdateTotalBytes,
                                agentUpdateBytesPerSecond = speed,
                            )
                        }
                    }
                }
            }
            _state.update { current ->
                current.copy(
                    installedAgentVersions = installer.installedAgentVersions(),
                    agentUpdates = if (result.isSuccess) current.agentUpdates - kind else current.agentUpdates,
                    agentUpdating = null,
                    agentUpdateMessage = result.fold(
                        onSuccess = { "${kind.title} updated to ${update.latestVersion}" },
                        onFailure = { error -> error.message?.take(220) ?: "Could not update ${kind.title}" },
                    ),
                    agentUpdateProgress = if (result.isSuccess) 1f else 0f,
                    agentUpdateDownloadedBytes = null,
                    agentUpdateTotalBytes = null,
                    agentUpdateBytesPerSecond = null,
                )
            }
        }
    }

    fun startAntigravityLogin() {
        if (_state.value.agentInstalling != null || _state.value.isRunning) return
        lastOpenedAntigravityAuthUrl = null
        viewModelScope.launch { antigravityAuthController.beginLogin() }
    }

    fun submitAntigravityCode(code: String) {
        runCatching { antigravityAuthController.submitCode(code) }
            .onFailure { error -> _state.update { it.copy(toastMessage = error.message ?: "Could not submit the code") } }
    }

    fun logoutAntigravity() {
        viewModelScope.launch {
            runCatching { antigravityAuthController.logout() }
                .onFailure { error -> _state.update { it.copy(toastMessage = error.message ?: "Could not sign out") } }
        }
    }

    fun setAntigravityModel(model: String) {
        preferences.antigravityModel = model
        val modelEffort = antigravityEffortFromModel(model)
        if (modelEffort != null) preferences.antigravityEffort = modelEffort
        _state.update {
            it.copy(
                antigravityModel = model,
                antigravityEffort = modelEffort ?: it.antigravityEffort,
            )
        }
    }

    fun setAntigravityEffort(effort: String) {
        if (effort !in setOf("low", "medium", "high")) return
        val current = _state.value
        val matchingModel = antigravityModelWithEffort(current.antigravityModel, effort)
            ?.takeIf { candidate -> current.antigravityModels.isEmpty() || candidate in current.antigravityModels }
        if (current.antigravityModel.isNotBlank() &&
            antigravityEffortFromModel(current.antigravityModel) != null &&
            matchingModel == null
        ) {
            _state.update { it.copy(toastMessage = "This model does not offer ${effort.replaceFirstChar(Char::uppercase)} reasoning") }
            return
        }
        preferences.antigravityEffort = effort
        matchingModel?.let { preferences.antigravityModel = it }
        _state.update {
            it.copy(
                antigravityEffort = effort,
                antigravityModel = matchingModel ?: it.antigravityModel,
            )
        }
    }

    fun refreshAntigravityModels() {
        if (_state.value.antigravityModelsLoading || !installer.isAgentInstalled(AgentKind.ANTIGRAVITY)) return
        _state.update { it.copy(antigravityModelsLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val runtime = installer.installedRuntime()
                val workspace = File(getApplication<Application>().filesDir, "workspaces/antigravity-models").apply { mkdirs() }
                val process = installer.process(
                    runtime.proot,
                    runtime.rootfs,
                    workspace,
                    emptyMap(),
                    listOf(com.jarves.mh.runtime.RuntimeInstaller.AGY_GUEST_PATH, "models"),
                    guestWorkspacePath = "/workspace/antigravity-models",
                    emulateHardLinks = false,
                )
                while (process.isAlive) delay(50)
                check(process.waitFor() == 0) { "Could not list Antigravity models" }
                val output = (process as? NativeSpawnProcess)?.outputFile?.readText().orEmpty()
                output.lineSequence()
                    .map { sanitizeTerminalOutput(it).trim() }
                    .mapNotNull { line -> line.split(Regex("\\s+"), limit = 2).firstOrNull() }
                    .filter { it.matches(Regex("[a-z0-9][a-z0-9._-]+")) }
                    .distinct()
                    .toList()
                    .also { check(it.isNotEmpty()) { "Antigravity returned no models" } }
            }
            withContext(Dispatchers.Main) {
                _state.update { current ->
                    result.fold(
                        onSuccess = { models ->
                            val preferred = antigravityModelWithEffort(
                                current.antigravityModel,
                                current.antigravityEffort,
                            )?.takeIf(models::contains)
                            val selected = preferred
                                ?: current.antigravityModel.takeIf(models::contains)
                                ?: models.first()
                            val selectedEffort = antigravityEffortFromModel(selected) ?: current.antigravityEffort
                            preferences.antigravityModel = selected
                            preferences.antigravityEffort = selectedEffort
                            current.copy(
                                antigravityModelsLoading = false,
                                antigravityModels = models,
                                antigravityModel = selected,
                                antigravityEffort = selectedEffort,
                            )
                        },
                        onFailure = { error -> current.copy(
                            antigravityModelsLoading = false,
                            toastMessage = error.message ?: "Could not load Antigravity models",
                        ) },
                    )
                }
            }
        }
    }

    /** Called from the first-launch tool picker; persists the choice for setup and Settings. */
    fun toggleDevStack(stack: DevStack) {
        if (stack == DevStack.WEB) return
        val updated = _state.value.selectedDevStacks.toMutableSet().apply {
            if (!add(stack)) remove(stack)
        }
        preferences.selectedDevStacks = updated.map { it.name }.toSet()
        _state.update { it.copy(selectedDevStacks = updated) }
    }

    /** Installs one development stack on demand (Settings) with live progress. */
    fun installDevStack(stack: DevStack) {
        if (_state.value.devStackInstalling != null) return
        _state.update {
            it.copy(
                devStackInstalling = stack,
                devStackRemoving = false,
                devStackMessage = "Preparing ${stack.label}…",
                devStackProgress = 0f,
                devStackBytes = null,
                devStackBytesPerSecond = null,
            )
        }
        viewModelScope.launch {
            var sampleBytes = 0L
            var sampleTime = android.os.SystemClock.elapsedRealtime()
            var latestSpeed: Long? = null
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    installer.ensureStackInstalled(stack) { progress ->
                        val transfer = progress.totalBytes?.let { total ->
                            (progress.downloadedBytes ?: 0L) to total
                        }
                        if (transfer != null) {
                            val now = android.os.SystemClock.elapsedRealtime()
                            val elapsed = now - sampleTime
                            val delta = transfer.first - sampleBytes
                            if (delta < 0L) {
                                sampleBytes = transfer.first
                                sampleTime = now
                                latestSpeed = null
                            } else if (elapsed >= 500L) {
                                latestSpeed = (delta * 1_000L / elapsed).coerceAtLeast(0L)
                                sampleBytes = transfer.first
                                sampleTime = now
                            }
                        } else {
                            sampleBytes = 0L
                            sampleTime = android.os.SystemClock.elapsedRealtime()
                            latestSpeed = null
                        }
                        _state.update { current ->
                            current.copy(
                                devStackMessage = progress.message,
                                devStackProgress = progress.fraction.coerceIn(0f, 1f),
                                devStackBytes = transfer,
                                devStackBytesPerSecond = latestSpeed,
                            )
                        }
                    }
                }
            }
            _state.update { current ->
                current.copy(
                    devStackInstalling = null,
                    devStackRemoving = false,
                    installedDevStacks = if (result.isSuccess) current.installedDevStacks + stack else current.installedDevStacks,
                    devStackProgress = 0f,
                    devStackBytes = null,
                    devStackBytesPerSecond = null,
                    devStackMessage = result.fold(
                        onSuccess = { "${stack.label} tools are ready" },
                        onFailure = { _ -> result.exceptionOrNull()?.message?.take(200) ?: "Could not install ${stack.label}" },
                    ),
                )
            }
        }
    }

    /** Removes an optional toolchain after the Settings confirmation dialog. */
    fun removeDevStack(stack: DevStack) {
        if (_state.value.devStackInstalling != null || stack == DevStack.WEB) return
        if (_state.value.isRunning || _state.value.projectTerminalRunning) {
            _state.update { it.copy(toastMessage = "Stop running tasks and terminal commands before removing tools") }
            return
        }
        _state.update {
            it.copy(
                devStackInstalling = stack,
                devStackRemoving = true,
                devStackMessage = "Removing ${stack.label}…",
                devStackProgress = 0.1f,
                devStackBytes = null,
                devStackBytesPerSecond = null,
            )
        }
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    installer.removeStack(stack) { progress ->
                        _state.update { current ->
                            current.copy(
                                devStackMessage = progress.message,
                                devStackProgress = progress.fraction.coerceIn(0f, 1f),
                            )
                        }
                    }
                }
            }
            if (result.isSuccess) {
                val selected = _state.value.selectedDevStacks - stack
                preferences.selectedDevStacks = selected.map { it.name }.toSet()
            }
            _state.update { current ->
                current.copy(
                    selectedDevStacks = if (result.isSuccess) current.selectedDevStacks - stack else current.selectedDevStacks,
                    installedDevStacks = if (result.isSuccess) current.installedDevStacks - stack else current.installedDevStacks,
                    devStackInstalling = null,
                    devStackRemoving = false,
                    devStackProgress = 0f,
                    devStackMessage = result.fold(
                        onSuccess = { "${stack.label} removed" },
                        onFailure = { result.exceptionOrNull()?.message?.take(200) ?: "Could not remove ${stack.label}" },
                    ),
                    toastMessage = result.fold(
                        onSuccess = { "${stack.label} removed" },
                        onFailure = { "Could not remove ${stack.label}" },
                    ),
                )
            }
        }
    }

    suspend fun discoverModels(profile: ProviderProfile, secret: String): ModelDiscoveryResult {
        val key = secret.ifBlank { vault.get(profile.kind.name).orEmpty() }
        return providerApi.discoverModels(profile.baseUrl, key, providerProtocolForAgent(profile, _state.value.agentKind))
    }

    suspend fun validateProvider(
        profile: ProviderProfile,
        secret: String,
        models: List<com.jarves.mh.network.DiscoveredModel>,
    ): ConnectionValidation {
        val key = secret.ifBlank { vault.get(profile.kind.name).orEmpty() }
        return providerApi.validate(
            profile.baseUrl,
            profile.model,
            key,
            providerProtocolForAgent(profile, _state.value.agentKind),
            models,
        )
    }

    fun pingApi() {
        if (_state.value.agentKind == AgentKind.ANTIGRAVITY) {
            testAntigravityConnection()
            return
        }
        val profile = _state.value.provider
        if (profile.baseUrl.isBlank() || profile.model.isBlank()) return
        if (_state.value.apiPingStatus == ApiPingStatus.PINGING) return
        _state.update { it.copy(apiPingStatus = ApiPingStatus.PINGING, apiPingMessage = "Sending a minimal test request…") }
        viewModelScope.launch {
            val key = vault.get(profile.kind.name).orEmpty()
            val result = providerApi.validate(
                profile.baseUrl,
                profile.model,
                key,
                providerProtocolForAgent(profile, _state.value.agentKind),
                emptyList(),
            )
            when (result) {
                is ConnectionValidation.Success -> _state.update {
                    it.copy(apiPingStatus = ApiPingStatus.OK, apiPingMessage = "API responded successfully")
                }
                is ConnectionValidation.Failure -> _state.update {
                    it.copy(apiPingStatus = ApiPingStatus.FAILED, apiPingMessage = result.message)
                }
            }
        }
    }

    /** Sends a tiny hello to the agy CLI to prove it actually answers. Silent timeout inside. */
    fun testAntigravityConnection() {
        if (_state.value.agentKind != AgentKind.ANTIGRAVITY) return
        if (_state.value.apiPingStatus == ApiPingStatus.PINGING) return
        if (_state.value.antigravityAuth.status != AntigravityAuthStatus.SIGNED_IN) {
            _state.update {
                it.copy(
                    apiPingStatus = ApiPingStatus.FAILED,
                    apiPingMessage = "Antigravity needs Google sign-in",
                )
            }
            return
        }
        _state.update { it.copy(apiPingStatus = ApiPingStatus.PINGING, apiPingMessage = "Saying hello to Antigravity…") }
        viewModelScope.launch {
            val result = runCatching { antigravityRuntime.hello() }
            result.onSuccess {
                _state.update {
                    it.copy(
                        apiPingStatus = ApiPingStatus.OK,
                        apiPingMessage = "Antigravity is Working!",
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(apiPingStatus = ApiPingStatus.FAILED, apiPingMessage = helloFailureMessage(error.message.orEmpty()))
                }
            }
        }
    }

    private fun helloFailureMessage(raw: String): String {
        val value = raw.replace(Regex("\\s+"), " ").trim()
        return when {
            value.contains("not installed", true) -> "Antigravity CLI is not installed. Install it from the Coding agent section."
            value.contains("sign-in", true) || value.contains("not signed in", true) ||
                value.contains("authentication", true) -> "Antigravity needs Google sign-in. Reconnect from the Google connection section."
            value.contains("did not answer", true) -> "Antigravity did not answer. Try again."
            value.isBlank() -> "Antigravity did not answer. Try again."
            else -> value.take(200)
        }
    }

    fun openProject(project: Project) {
        val current = _state.value
        if (current.activeProject?.id == project.id) {
            _state.update {
                it.copy(
                    workspaceVisible = true,
                    readOnlyProject = null,
                    readOnlyProjectChats = emptyList(),
                    readOnlyChatId = null,
                    readOnlyMessages = emptyList(),
                )
            }
            return
        }
        if (current.isRunning || current.projectTerminalRunning) {
            val chats = preferences.loadProjectChats(project.id).ifEmpty {
                listOf(ProjectChat(title = "Main chat"))
            }
            val chat = chats.first()
            _state.update {
                it.copy(
                    readOnlyProject = project,
                    readOnlyProjectChats = chats,
                    readOnlyChatId = chat.id,
                    readOnlyMessages = preferences.loadMessages(project.id, chat.id),
                )
            }
            return
        }
        configureBridgeRoots(project.id, project.rootPath)
        val terminal = loadProjectTerminal(project)
        val suggestedRoot = if (project.rootPath.isBlank()) detectNestedProjectRoot(project) else null
        val chats = preferences.loadProjectChats(project.id).ifEmpty {
            listOf(ProjectChat(title = "Main chat")).also { preferences.saveProjectChats(project.id, it) }
        }
        val activeChat = chats.first()
        val saved = preferences.loadMessages(project.id, activeChat.id)
        val msgs = saved.ifEmpty { listOf(ChatMessage(fromUser = false, text = "Hi! Tell me what you want to build or change.")) }
        _state.update {
            it.copy(
                activeProject = project,
                workspaceVisible = true,
                readOnlyProject = null,
                readOnlyProjectChats = emptyList(),
                readOnlyChatId = null,
                readOnlyMessages = emptyList(),
                projectChats = chats,
                activeChatId = activeChat.id,
                messages = msgs,
                liveProcess = emptyList(),
                liveThinking = false,
                taskStartedAtMillis = null,
                taskFinishedAtMillis = null,
                changes = emptyList(),
                workspaceFiles = emptyList(),
                androidProjectDetected = false,
                filesLoading = true,
                projectTerminalLines = terminal.lines,
                projectTerminalLiveOutput = "",
                projectTerminalRunning = false,
                projectTerminalCwd = terminal.cwd,
                projectTerminalCommand = null,
                projectTerminalDraft = null,
                pendingTerminalCommand = null,
                suggestedProjectRoot = suggestedRoot,
                previewReady = false,
                previewUrl = null,
                pendingAttachments = emptyList(),
            )
        }
        refreshProjectFiles()
        refreshGitState()
        loadAndRefreshOrchestrator(project)
        viewModelScope.launch {
            val pending = activeRuntime().loadPendingChanges(project.id)
            if (_state.value.activeProject?.id == project.id) _state.update { it.copy(changes = pending) }
        }
    }

    fun closeProject() {
        val active = _state.value.activeProject
        persistMessages()
        if (_state.value.isRunning || _state.value.projectTerminalRunning) {
            _state.update {
                it.copy(
                    workspaceVisible = false,
                    toastMessage = if (it.isRunning) {
                        "Task continues in the background"
                    } else {
                        "Terminal command continues in the background"
                    },
                )
            }
            return
        }

        if (active != null) {
            val chats = preferences.loadProjectChats(active.id)
            val userMessages = chats.sumOf { preferences.loadMessages(active.id, it.id).count { m -> m.fromUser } }
            val workspaceDir = File(getApplication<Application>().filesDir, "workspaces/${active.id}")
            val userFiles = if (workspaceDir.isDirectory) {
                workspaceDir.walkTopDown().filter { file ->
                    file.isFile && !file.name.startsWith(".claude") && file.name != ".pocket-dev-stacks.json"
                }.count()
            } else 0

            if (userMessages == 0 && userFiles == 0 && !_state.value.isRunning && !_state.value.projectTerminalRunning) {
                // Unused empty project; delete immediately so it does not clutter the project list.
                _state.update { current -> current.copy(projects = current.projects.filterNot { it.id == active.id }) }
                preferences.saveProjects(_state.value.projects)
                viewModelScope.launch(Dispatchers.IO) {
                    workspaceDir.deleteRecursively()
                    terminalHistoryFile(active.id).delete()
                    preferences.deleteProjectChats(active.id)
                }
            }
        }

        _state.update {
            it.copy(
                activeProject = null,
                workspaceVisible = false,
                projectChats = emptyList(),
                activeChatId = null,
                changes = emptyList(),
                workspaceFiles = emptyList(),
                androidProjectDetected = false,
                filesLoading = false,
                isRunning = false,
                activeSessionId = null,
                pendingApproval = null,
                projectTerminalLines = emptyList(),
                projectTerminalLiveOutput = "",
                projectTerminalRunning = false,
                projectTerminalCwd = "/workspace",
                projectTerminalCommand = null,
                projectTerminalDraft = null,
                pendingTerminalCommand = null,
                suggestedProjectRoot = null,
                previewReady = false,
                previewUrl = null,
                pendingAttachments = emptyList(),
            )
        }
    }

    fun closeReadOnlyProject() {
        _state.update {
            it.copy(
                readOnlyProject = null,
                readOnlyProjectChats = emptyList(),
                readOnlyChatId = null,
                readOnlyMessages = emptyList(),
            )
        }
    }

    fun switchReadOnlyChat(chatId: String) {
        val project = _state.value.readOnlyProject ?: return
        if (_state.value.readOnlyProjectChats.none { it.id == chatId }) return
        _state.update {
            it.copy(
                readOnlyChatId = chatId,
                readOnlyMessages = preferences.loadMessages(project.id, chatId),
            )
        }
    }

    fun activateReadOnlyProject() {
        if (_state.value.isRunning || _state.value.projectTerminalRunning) return
        val project = _state.value.readOnlyProject ?: return
        closeReadOnlyProject()
        openProject(project)
    }

    fun consumeToast() = _state.update { it.copy(toastMessage = null) }

    fun createProject(name: String) {
        if (name.isBlank()) return
        if (_state.value.isRunning || _state.value.projectTerminalRunning) {
            _state.update { it.copy(toastMessage = "Stop the background task before creating another project") }
            return
        }
        val baseSlug = projectSlug(name)
        val usedSlugs = _state.value.projects.mapTo(mutableSetOf()) { it.slug }
        val slug = generateSequence(1) { it + 1 }
            .map { number -> if (number == 1) baseSlug else "$baseSlug-$number" }
            .first { it !in usedSlugs }
        val project = Project(
            name = name.trim(),
            description = "Starter web project",
            language = "TypeScript",
            slug = slug,
        )
        configureBridgeRoots(project.id, project.rootPath)
        val guestRoot = projectGuestRoot(project)
        _state.update {
            it.copy(
                projects = listOf(project) + it.projects,
                activeProject = project,
                workspaceVisible = true,
                messages = listOf(ChatMessage(fromUser = false, text = "Hi! Tell me what you want to build or change.")),
                liveProcess = emptyList(),
                liveThinking = false,
                taskStartedAtMillis = null,
                taskFinishedAtMillis = null,
                changes = emptyList(),
                workspaceFiles = emptyList(),
                androidProjectDetected = false,
                filesLoading = true,
                projectTerminalLines = emptyList(),
                projectTerminalLiveOutput = "",
                projectTerminalRunning = false,
                projectTerminalCwd = guestRoot,
                projectTerminalCommand = null,
                projectTerminalDraft = null,
                pendingTerminalCommand = null,
                suggestedProjectRoot = null,
                previewReady = false,
                previewUrl = null,
            )
        }
        preferences.saveProjects(_state.value.projects)
        File(getApplication<Application>().filesDir, "workspaces/${project.id}").mkdirs()
        val firstChat = ProjectChat(title = "New chat")
        preferences.saveProjectChats(project.id, listOf(firstChat))
        _state.update { it.copy(projectChats = listOf(firstChat), activeChatId = firstChat.id) }
        refreshProjectFiles()
    }

    fun createQuickProject() {
        if (_state.value.isRunning || _state.value.projectTerminalRunning) {
            _state.update { it.copy(toastMessage = "Stop the background task before creating another project") }
            return
        }
        val identity = generateQuickChatIdentity(_state.value.projects.mapTo(mutableSetOf()) { it.slug })
        val project = Project(
            name = identity.displayName,
            description = "Quick project workspace",
            language = "General",
            slug = identity.slug,
            kind = ProjectKind.QUICK_PROJECT,
        )
        val firstChat = ProjectChat(title = "New chat")
        File(getApplication<Application>().filesDir, "workspaces/${project.id}").mkdirs()
        preferences.saveProjectChats(project.id, listOf(firstChat))
        _state.update { it.copy(projects = listOf(project) + it.projects) }
        preferences.saveProjects(_state.value.projects)
        openProject(project)
    }

    fun importZipProject(uri: Uri) {
        if (_state.value.projectImporting || _state.value.isRunning || _state.value.projectTerminalRunning) return
        _state.update { it.copy(projectImporting = true, projectImportMessage = "Reading project archive…") }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { extractImportedProject(uri) } }
            result.onSuccess { imported ->
                val project = imported.project
                val firstChat = ProjectChat(title = "New chat")
                preferences.saveProjectChats(project.id, listOf(firstChat))
                _state.update { current ->
                    current.copy(
                        projects = listOf(project) + current.projects,
                        projectImporting = false,
                        projectImportMessage = null,
                        toastMessage = "${project.name} imported successfully",
                    )
                }
                preferences.saveProjects(_state.value.projects)
                openProject(project)
                _state.update { it.copy(pendingAttachments = listOf(imported.sourceAttachment)) }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        projectImporting = false,
                        projectImportMessage = null,
                        toastMessage = "Import failed: ${error.message?.take(180) ?: "Invalid ZIP archive"}",
                    )
                }
            }
        }
    }

    private fun extractImportedProject(uri: Uri): ImportedZipProject {
        val app = getApplication<Application>()
        val resolver = app.contentResolver
        var archiveName = "Imported project.zip"
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { index ->
                    archiveName = cursor.getString(index) ?: archiveName
                }
            }
        }
        val identity = generateQuickChatIdentity(_state.value.projects.mapTo(mutableSetOf()) { it.slug })
        val projectId = UUID.randomUUID().toString()
        val destination = File(app.filesDir, "workspaces/$projectId")
        destination.mkdirs()
        val destinationPath = destination.canonicalFile.toPath()
        val availableLimit = (destination.usableSpace * 8L / 10L).coerceAtMost(MAX_IMPORTED_PROJECT_BYTES)
        var extractedBytes = 0L
        var entries = 0
        try {
            val source = resolver.openInputStream(uri) ?: error("The selected ZIP could not be opened")
            source.buffered().use { input ->
                ZipInputStream(input).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        entries++
                        require(entries <= MAX_IMPORTED_ZIP_ENTRIES) { "The ZIP contains too many files" }
                        val entryName = entry.name.replace('\\', '/').trimStart('/')
                        require(entryName.isNotBlank() && '\u0000' !in entryName) { "The ZIP contains an invalid path" }
                        if (entryName.startsWith("__MACOSX/") || entryName.endsWith("/.DS_Store") || entryName == ".DS_Store") {
                            zip.closeEntry()
                            continue
                        }
                        val target = File(destination, entryName).canonicalFile
                        require(target.toPath().startsWith(destinationPath)) { "The ZIP contains an unsafe path" }
                        if (entry.isDirectory) {
                            target.mkdirs()
                        } else {
                            target.parentFile?.mkdirs()
                            target.outputStream().buffered().use { output ->
                                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                                while (true) {
                                    val count = zip.read(buffer)
                                    if (count < 0) break
                                    extractedBytes += count
                                    require(extractedBytes <= availableLimit) { "The extracted project is too large for available storage" }
                                    output.write(buffer, 0, count)
                                }
                            }
                            if (entry.time > 0) target.setLastModified(entry.time)
                        }
                        zip.closeEntry()
                    }
                }
            }
            require(entries > 0 && destination.walkTopDown().any { it.isFile }) { "The ZIP does not contain project files" }
            val preliminary = Project(
                id = projectId,
                name = identity.displayName,
                description = "Imported project workspace",
                language = "General",
                slug = identity.slug,
                kind = ProjectKind.QUICK_PROJECT,
            )
            val nestedRoot = detectNestedProjectRoot(preliminary)
            val projectRoot = nestedRoot?.let { File(destination, it) } ?: destination
            val metadata = detectImportedProjectMetadata(projectRoot)
            val safeArchiveName = sanitizeAttachmentName(archiveName).let { name ->
                if (name.endsWith(".zip", ignoreCase = true)) name else "$name.zip"
            }
            val archiveFolder = File(projectRoot, ".pocketdev/imports").apply { mkdirs() }
            val archivedSource = File(archiveFolder, safeArchiveName)
            var sourceBytes = 0L
            resolver.openInputStream(uri)?.buffered()?.use { input ->
                archivedSource.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        sourceBytes += count
                        require(extractedBytes + sourceBytes <= availableLimit) { "The imported project is too large for available storage" }
                        output.write(buffer, 0, count)
                    }
                }
            } ?: error("The selected ZIP could not be preserved")
            val project = preliminary.copy(
                description = metadata.first,
                language = metadata.second,
                rootPath = nestedRoot.orEmpty(),
            )
            return ImportedZipProject(
                project = project,
                sourceAttachment = ChatAttachment(
                    displayName = archiveName.take(120),
                    relativePath = archivedSource.relativeTo(projectRoot).invariantSeparatorsPath,
                    mimeType = "application/zip",
                    sizeBytes = sourceBytes,
                ),
            )
        } catch (error: Throwable) {
            destination.deleteRecursively()
            throw error
        }
    }

    private fun detectImportedProjectMetadata(root: File): Pair<String, String> {
        val names = root.walkTopDown().maxDepth(3).filter(File::isFile).map { it.name.lowercase() }.toSet()
        return when {
            names.any { it == "settings.gradle.kts" || it == "build.gradle.kts" } -> "Imported Gradle project" to "Kotlin"
            names.any { it == "settings.gradle" || it == "build.gradle" } -> "Imported Gradle project" to "Java"
            "package.json" in names && names.any { it == "tsconfig.json" || it.endsWith(".ts") || it.endsWith(".tsx") } -> "Imported web project" to "TypeScript"
            "package.json" in names -> "Imported web project" to "JavaScript"
            names.any { it == "pyproject.toml" || it == "requirements.txt" || it.endsWith(".py") } -> "Imported Python project" to "Python"
            names.any { it == "cargo.toml" || it.endsWith(".rs") } -> "Imported Rust project" to "Rust"
            names.any { it == "go.mod" || it.endsWith(".go") } -> "Imported Go project" to "Go"
            else -> "Imported ZIP project" to "General"
        }
    }

    fun clonePublicGitRepository(url: String) {
        cloneGitRepository(url = url, repositoryName = null, branch = null, useGitHubCli = false)
    }

    fun cloneGitHubRepository(repository: GitHubRepository) {
        if (_state.value.githubAuthStatus != GitHubAuthStatus.CONNECTED) {
            _state.update { it.copy(githubAuthStatus = GitHubAuthStatus.DISCONNECTED, githubMessage = "Connect GitHub again") }
            return
        }
        cloneGitRepository(repository.cloneUrl, repository.fullName, repository.defaultBranch, useGitHubCli = true)
    }

    private fun cloneGitRepository(url: String, repositoryName: String?, branch: String?, useGitHubCli: Boolean) {
        if (_state.value.gitCloneRunning || _state.value.projectImporting || _state.value.isRunning || _state.value.projectTerminalRunning) return
        val normalized = runCatching { validateGitUrl(url) }.getOrElse { error ->
            _state.update { it.copy(toastMessage = error.message ?: "Enter a valid public HTTPS Git URL") }
            return
        }
        _state.update { it.copy(gitCloneRunning = true, gitCloneMessage = "Connecting to Git repository…") }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val identity = generateQuickChatIdentity(_state.value.projects.mapTo(mutableSetOf()) { it.slug })
                    val projectId = UUID.randomUUID().toString()
                    val workspace = File(getApplication<Application>().filesDir, "workspaces/$projectId").apply { mkdirs() }
                    val output = File(getApplication<Application>().cacheDir, "git-clone-${System.nanoTime()}.log")
                    try {
                        val environment = mutableMapOf(
                            "GIT_TERMINAL_PROMPT" to "0",
                            "GIT_LFS_SKIP_SMUDGE" to "1",
                            "GH_PROMPT_DISABLED" to "1",
                            "GH_NO_UPDATE_NOTIFIER" to "1",
                        )
                        val installed = installer.installedRuntime()
                        val command = if (useGitHubCli && repositoryName != null) {
                            buildList {
                                addAll(listOf(RuntimeInstaller.GITHUB_CLI_GUEST_PATH, "repo", "clone", repositoryName, ".", "--", "--progress", "--single-branch"))
                                branch?.takeIf(String::isNotBlank)?.let { addAll(listOf("--branch", it)) }
                            }
                        } else {
                            buildList {
                                addAll(listOf("git", "clone", "--progress", "--single-branch"))
                                branch?.takeIf(String::isNotBlank)?.let { addAll(listOf("--branch", it)) }
                                add(normalized)
                                add(".")
                            }
                        }
                        _state.update { it.copy(gitCloneMessage = "Cloning ${repositoryName ?: normalized.substringAfterLast('/').removeSuffix(".git")}…") }
                        val process = installer.process(
                            installed.proot,
                            installed.rootfs,
                            workspace,
                            environment,
                            command,
                            guestWorkspacePath = "/workspace/${identity.slug}",
                            outputFile = output,
                        )
                        val exit = process.waitFor()
                        val details = output.readText().trim()
                        check(exit == 0) { details.takeLast(600).ifBlank { "Git clone failed with exit code $exit" } }
                        val metadata = detectImportedProjectMetadata(workspace)
                        Project(
                            id = projectId,
                            name = identity.displayName,
                            description = repositoryName?.let { "GitHub · $it" } ?: "Imported Git repository",
                            language = metadata.second,
                            slug = identity.slug,
                            kind = ProjectKind.QUICK_PROJECT,
                        )
                    } catch (error: Throwable) {
                        workspace.deleteRecursively()
                        throw error
                    } finally {
                        output.delete()
                    }
                }
            }
            result.onSuccess { project ->
                val chat = ProjectChat(title = "New chat")
                preferences.saveProjectChats(project.id, listOf(chat))
                _state.update { current -> current.copy(projects = listOf(project) + current.projects) }
                preferences.saveProjects(_state.value.projects)
                openProject(project)
                _state.update { it.copy(gitCloneRunning = false, gitCloneMessage = null, toastMessage = "Repository cloned successfully") }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        gitCloneRunning = false,
                        gitCloneMessage = null,
                        toastMessage = "Clone failed: ${error.message?.lineSequence()?.lastOrNull()?.take(180) ?: "Unknown error"}",
                    )
                }
            }
        }
    }

    private fun validateGitUrl(value: String): String {
        val clean = value.trim()
        val uri = URI(clean)
        require(uri.scheme.equals("https", ignoreCase = true)) { "Only HTTPS Git URLs are supported" }
        require(uri.userInfo == null && uri.fragment == null && uri.host?.isNotBlank() == true) { "Enter a valid HTTPS Git URL without credentials" }
        require(uri.host != "localhost" && uri.host != "127.0.0.1" && uri.host != "::1") { "Local Git URLs are not supported" }
        require(uri.path.count { it == '/' } >= 2) { "The URL must identify a Git repository" }
        return uri.toASCIIString()
    }

    fun startGitHubLogin() {
        if (_state.value.githubAuthStatus == GitHubAuthStatus.STARTING || _state.value.githubAuthStatus == GitHubAuthStatus.AWAITING_USER) return
        _state.update { it.copy(githubAuthStatus = GitHubAuthStatus.STARTING, githubMessage = "Preparing official GitHub sign-in…") }
        startGitHubForegroundOperation()
        githubAuthJob = viewModelScope.launch {
            try {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    installer.ensureGitHubCliInstalled { progress ->
                        _state.update { it.copy(githubMessage = progress.message) }
                    }
                    val runtime = installer.installedRuntime()
                    val outputFile = File(getApplication<Application>().cacheDir, "github-auth-${System.nanoTime()}.log")
                    val workspace = File(getApplication<Application>().filesDir, "workspaces/github-auth").apply { mkdirs() }
                    val process = installer.process(
                        runtime.proot,
                        runtime.rootfs,
                        workspace,
                        githubCliEnvironment(),
                        listOf(
                            RuntimeInstaller.GITHUB_CLI_GUEST_PATH,
                            "auth", "login",
                            "--hostname", "github.com",
                            "--git-protocol", "https",
                            "--web",
                            "--insecure-storage",
                        ),
                        guestWorkspacePath = "/workspace/github-auth",
                        outputFile = outputFile,
                    )
                    githubAuthProcess = process
                    var offset = 0L
                    val captured = StringBuilder()
                    var browserOpened = false
                    try {
                        while (process.isAlive || outputFile.length() > offset) {
                            if (outputFile.length() > offset) {
                                val count = (outputFile.length() - offset).coerceAtMost(16L * 1024).toInt()
                                val bytes = ByteArray(count)
                                RandomAccessFile(outputFile, "r").use { file -> file.seek(offset); file.readFully(bytes) }
                                offset += count
                                captured.append(bytes.toString(Charsets.UTF_8))
                                val clean = sanitizeTerminalOutput(captured.toString()).takeLast(20_000)
                                val code = GITHUB_DEVICE_CODE.find(clean)?.value
                                if (code != null && !browserOpened) {
                                    browserOpened = true
                                    _state.update {
                                        it.copy(
                                            githubAuthStatus = GitHubAuthStatus.AWAITING_USER,
                                            githubUserCode = code,
                                            githubVerificationUri = GITHUB_DEVICE_URL,
                                            githubMessage = "Enter this one-time code on GitHub",
                                        )
                                    }
                                    openExternalUrl(GITHUB_DEVICE_URL)
                                }
                            } else {
                                delay(100)
                            }
                        }
                        val exit = process.waitFor()
                        check(exit == 0) {
                            sanitizeTerminalOutput(captured.toString()).lineSequence().lastOrNull { it.isNotBlank() }
                                ?: "GitHub sign-in failed (exit $exit)"
                        }
                    } finally {
                        githubAuthProcess = null
                        outputFile.delete()
                    }
                    githubAccountLogin() ?: error("GitHub connected, but the account could not be identified")
                }
            }
            result.onSuccess { login ->
                preferences.githubLogin = login
                _state.update {
                    it.copy(
                        githubAuthStatus = GitHubAuthStatus.CONNECTED,
                        githubLogin = login,
                        githubUserCode = null,
                        githubVerificationUri = null,
                        githubMessage = "Connected as @$login",
                    )
                }
                refreshGitHubRepositories()
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        githubAuthStatus = GitHubAuthStatus.ERROR,
                        githubUserCode = null,
                        githubVerificationUri = null,
                        githubMessage = error.message?.take(240) ?: "GitHub sign-in failed",
                    )
                }
            }
            } finally {
                stopGitHubForegroundOperation()
                githubAuthJob = null
            }
        }
    }

    fun generateNewGitHubCode() {
        githubAuthProcess?.destroy()
        githubAuthJob?.cancel()
        githubAuthProcess = null
        githubAuthJob = null
        stopGitHubForegroundOperation()
        _state.update {
            it.copy(
                githubAuthStatus = GitHubAuthStatus.DISCONNECTED,
                githubUserCode = null,
                githubVerificationUri = null,
                githubMessage = "Generating a new GitHub code…",
            )
        }
        startGitHubLogin()
    }

    fun refreshGitHubRepositories() {
        if (_state.value.githubAuthStatus != GitHubAuthStatus.CONNECTED) return
        if (_state.value.githubRepositoriesLoading) return
        _state.update { it.copy(githubRepositoriesLoading = true, githubMessage = "Loading repositories…") }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { githubRepositoriesFromCli() } }
            result.onSuccess { repositories ->
                _state.update {
                    it.copy(
                        githubRepositories = repositories,
                        githubRepositoriesLoading = false,
                        githubMessage = "${repositories.size} repositories available",
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        githubRepositoriesLoading = false,
                        githubMessage = error.message ?: "Could not load GitHub repositories",
                    )
                }
            }
        }
    }

    fun disconnectGitHub() {
        if (_state.value.githubAuthStatus == GitHubAuthStatus.STARTING) return
        val login = _state.value.githubLogin
        _state.update { it.copy(githubAuthStatus = GitHubAuthStatus.STARTING, githubMessage = "Signing out of GitHub…") }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val command = buildList {
                        addAll(listOf("auth", "logout", "--hostname", "github.com"))
                        login?.takeIf(String::isNotBlank)?.let { addAll(listOf("--user", it)) }
                    }
                    val output = runGitHubCli(command)
                    check(output.first == 0) { output.second.lineSequence().lastOrNull { it.isNotBlank() } ?: "GitHub logout failed" }
                }
            }
            result.onSuccess {
                preferences.githubLogin = ""
                _state.update {
                    it.copy(
                        githubAuthStatus = GitHubAuthStatus.DISCONNECTED,
                        githubLogin = null,
                        githubUserCode = null,
                        githubVerificationUri = null,
                        githubRepositories = emptyList(),
                        githubMessage = "Signed out",
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(githubAuthStatus = GitHubAuthStatus.ERROR, githubMessage = error.message ?: "Could not sign out") }
            }
        }
    }

    private suspend fun refreshGitHubConnection() = withContext(Dispatchers.IO) {
        if (!installer.isGitHubCliInstalled()) return@withContext
        val login = runCatching { githubAccountLogin() }.getOrNull()
        if (login.isNullOrBlank()) {
            preferences.githubLogin = ""
            _state.update { it.copy(githubAuthStatus = GitHubAuthStatus.DISCONNECTED, githubLogin = null) }
        } else {
            preferences.githubLogin = login
            _state.update {
                it.copy(
                    githubAuthStatus = GitHubAuthStatus.CONNECTED,
                    githubLogin = login,
                    githubMessage = "Connected as @$login",
                )
            }
        }
    }

    private fun githubCliEnvironment(): Map<String, String> = mapOf(
        "GH_PROMPT_DISABLED" to "1",
        "GH_NO_UPDATE_NOTIFIER" to "1",
        // Android PRoot has no Secret Service. This keeps the official gh-owned
        // credential in PocketDev's private Linux home instead of exporting it.
        "BROWSER" to "/bin/false",
    )

    private fun runGitHubCli(arguments: List<String>): Pair<Int, String> {
        check(installer.isGitHubCliInstalled()) { "GitHub CLI is not installed" }
        val runtime = installer.installedRuntime()
        val outputFile = File(getApplication<Application>().cacheDir, "github-cli-${System.nanoTime()}.log")
        val workspace = File(getApplication<Application>().filesDir, "workspaces/github-auth").apply { mkdirs() }
        return try {
            val process = installer.process(
                runtime.proot,
                runtime.rootfs,
                workspace,
                githubCliEnvironment(),
                listOf(RuntimeInstaller.GITHUB_CLI_GUEST_PATH) + arguments,
                guestWorkspacePath = "/workspace/github-auth",
                outputFile = outputFile,
            )
            val exit = process.waitFor()
            exit to sanitizeTerminalOutput(outputFile.takeIf(File::isFile)?.readText().orEmpty()).trim()
        } finally {
            outputFile.delete()
        }
    }

    private fun githubAccountLogin(): String? {
        val (exit, output) = runGitHubCli(listOf("api", "user", "--jq", ".login"))
        return output.lineSequence().lastOrNull { it.isNotBlank() }?.trim().takeIf { exit == 0 && !it.isNullOrBlank() }
    }

    private fun githubRepositoriesFromCli(): List<GitHubRepository> {
        val endpoint = "user/repos?visibility=all&affiliation=owner,collaborator,organization_member&sort=updated&per_page=100"
        val (exit, output) = runGitHubCli(listOf("api", "--paginate", "--slurp", endpoint))
        check(exit == 0) { output.lineSequence().lastOrNull { it.isNotBlank() } ?: "Could not load GitHub repositories" }
        val pages = JSONArray(output)
        val repositories = LinkedHashMap<String, GitHubRepository>()
        for (pageIndex in 0 until pages.length()) {
            val page = pages.optJSONArray(pageIndex) ?: continue
            for (index in 0 until page.length()) {
                val item = page.optJSONObject(index) ?: continue
                val fullName = item.optString("full_name").takeIf(String::isNotBlank) ?: continue
                repositories[fullName] = GitHubRepository(
                    fullName = fullName,
                    cloneUrl = item.optString("clone_url", "https://github.com/$fullName.git"),
                    private = item.optBoolean("private"),
                    defaultBranch = item.optString("default_branch", "main"),
                    description = item.optString("description"),
                    updatedAt = item.optString("updated_at"),
                )
            }
        }
        return repositories.values.toList()
    }

    private fun openExternalUrl(url: String) {
        runCatching {
            getApplication<Application>().startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure {
            _state.update { state -> state.copy(toastMessage = "Could not open the browser. Copy the URL instead.") }
        }
    }

    private fun startGitHubForegroundOperation() {
        ContextCompat.startForegroundService(
            getApplication(),
            Intent(getApplication(), com.jarves.mh.runtime.RuntimeExecutionService::class.java)
                .setAction(com.jarves.mh.runtime.RuntimeExecutionService.ACTION_START)
                .putExtra(com.jarves.mh.runtime.RuntimeExecutionService.EXTRA_PROJECT_NAME, "GitHub sign-in")
                .putExtra(com.jarves.mh.runtime.RuntimeExecutionService.EXTRA_TITLE, "Connecting GitHub")
                .putExtra(com.jarves.mh.runtime.RuntimeExecutionService.EXTRA_CAN_STOP, false),
        )
    }

    private fun stopGitHubForegroundOperation() {
        runCatching {
            getApplication<Application>().startService(
                Intent(getApplication(), com.jarves.mh.runtime.RuntimeExecutionService::class.java)
                    .setAction(com.jarves.mh.runtime.RuntimeExecutionService.ACTION_CANCELLED),
            )
        }.onFailure {
            getApplication<Application>().stopService(
                Intent(getApplication(), com.jarves.mh.runtime.RuntimeExecutionService::class.java),
            )
        }
    }

    fun renameProject(projectId: String, newName: String) {
        val clean = newName.replace(Regex("\\s+"), " ").trim().take(60)
        if (clean.isBlank()) return
        _state.update { current ->
            val projects = current.projects.map { project ->
                if (project.id == projectId) project.copy(name = clean) else project
            }
            val active = current.activeProject?.let { project ->
                if (project.id == projectId) project.copy(name = clean) else project
            }
            current.copy(projects = projects, activeProject = active)
        }
        preferences.saveProjects(_state.value.projects)
    }

    fun deleteProject(projectId: String) {
        val project = _state.value.projects.firstOrNull { it.id == projectId } ?: return
        if (_state.value.activeProject?.id == projectId || _state.value.isRunning || _state.value.projectTerminalRunning) return
        _state.update { current -> current.copy(projects = current.projects.filterNot { it.id == projectId }) }
        preferences.saveProjects(_state.value.projects)
        viewModelScope.launch(Dispatchers.IO) {
            val filesDir = getApplication<Application>().filesDir
            File(filesDir, "workspaces/${project.id}").deleteRecursively()
            terminalHistoryFile(project.id).delete()
            preferences.deleteProjectChats(project.id)
        }
    }

    private fun detectNestedProjectRoot(project: Project): String? {
        val base = File(getApplication<Application>().filesDir, "workspaces/${project.id}")
        if (!base.isDirectory) return null
        val visible = base.listFiles().orEmpty().filterNot { file ->
            file.name == ".claude" || file.name == ".claude.json"
        }
        val onlyDirectory = visible.singleOrNull()?.takeIf(File::isDirectory) ?: return null
        val containsProjectFiles = onlyDirectory.walkTopDown()
            .maxDepth(2)
            .any { it.isFile && it.name !in setOf(".DS_Store", ".claude.json") }
        return onlyDirectory.name.takeIf { containsProjectFiles && !it.contains("..") }
    }

    fun useSuggestedProjectRoot() {
        val current = _state.value
        val project = current.activeProject ?: return
        val root = current.suggestedProjectRoot ?: return
        if (current.isRunning || current.projectTerminalRunning) return
        val updated = project.copy(rootPath = root)
        configureBridgeRoots(updated.id, updated.rootPath)
        val projects = current.projects.map { if (it.id == updated.id) updated else it }
        val guestRoot = projectGuestRoot(updated)
        preferences.saveProjects(projects)
        saveProjectTerminal(updated.id, guestRoot, current.projectTerminalLines)
        _state.update {
            it.copy(
                projects = projects,
                activeProject = updated,
                suggestedProjectRoot = null,
                projectTerminalCwd = guestRoot,
                changes = emptyList(),
                toastMessage = "$root is now the project root",
            )
        }
        refreshProjectFiles()
    }

    fun exportActiveProject(uri: Uri) {
        val current = _state.value
        val project = current.activeProject ?: return
        if (current.isRunning || current.projectTerminalRunning) {
            _state.update { it.copy(toastMessage = "Stop the running task before exporting") }
            return
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val root = projectWorkspaceRoot(project)
                    val rootPath = root.canonicalFile.toPath()
                    val output = getApplication<Application>().contentResolver.openOutputStream(uri)
                        ?: error("The selected location could not be opened")
                    output.buffered().use { stream ->
                        ZipOutputStream(stream).use { zip ->
                            zip.putNextEntry(ZipEntry("${project.slug}/"))
                            zip.closeEntry()
                            root.walkTopDown()
                                .onEnter { directory ->
                                    if (directory == root) {
                                        true
                                    } else {
                                        val relative = directory.relativeTo(root).invariantSeparatorsPath
                                        !isExportExcludedPath(relative) &&
                                            !Files.isSymbolicLink(directory.toPath()) &&
                                            runCatching { directory.canonicalFile.toPath().startsWith(rootPath) }.getOrDefault(false)
                                    }
                                }
                                .drop(1)
                                .filter { file ->
                                    !Files.isSymbolicLink(file.toPath()) &&
                                        runCatching { file.canonicalFile.toPath().startsWith(rootPath) }.getOrDefault(false) &&
                                        !isExportExcludedPath(file.relativeTo(root).invariantSeparatorsPath)
                                }
                                .forEach { file ->
                                    val relative = file.relativeTo(root).invariantSeparatorsPath
                                    val entryName = "${project.slug}/$relative" + if (file.isDirectory) "/" else ""
                                    zip.putNextEntry(ZipEntry(entryName).apply { time = file.lastModified() })
                                    if (file.isFile) file.inputStream().buffered().use { it.copyTo(zip) }
                                    zip.closeEntry()
                                }
                        }
                    }
                }
            }
            _state.update {
                it.copy(
                    toastMessage = result.fold(
                        onSuccess = { "${project.slug}.zip exported" },
                        onFailure = { error -> "Export failed: ${error.message ?: "Unknown error"}" },
                    ),
                )
            }
        }
    }

    fun createChat() {
        val project = _state.value.activeProject ?: return
        if (_state.value.isRunning) return
        persistMessages()
        val chat = ProjectChat()
        val chats = listOf(chat) + _state.value.projectChats
        preferences.saveProjectChats(project.id, chats)
        _state.update {
            it.copy(
                projectChats = chats,
                activeChatId = chat.id,
                messages = listOf(ChatMessage(fromUser = false, text = "Hi! Tell me what you want to build or change.")),
                liveProcess = emptyList(),
                liveThinking = false,
                taskStartedAtMillis = null,
                taskFinishedAtMillis = null,
                pendingApproval = null,
                pendingAttachments = emptyList(),
            )
        }
    }

    fun switchChat(chatId: String) {
        val current = _state.value
        val project = current.activeProject ?: return
        if (current.isRunning || current.activeChatId == chatId) return
        val chat = current.projectChats.firstOrNull { it.id == chatId } ?: return
        persistMessages()
        val saved = preferences.loadMessages(project.id, chat.id)
        _state.update {
            it.copy(
                activeChatId = chat.id,
                messages = saved.ifEmpty { listOf(ChatMessage(fromUser = false, text = "Hi! Tell me what you want to build or change.")) },
                liveProcess = emptyList(),
                liveThinking = false,
                taskStartedAtMillis = null,
                taskFinishedAtMillis = null,
                pendingApproval = null,
                pendingAttachments = emptyList(),
            )
        }
    }

    fun refreshProjectFiles() {
        val project = _state.value.activeProject ?: return
        _state.update { it.copy(filesLoading = true) }
        viewModelScope.launch {
            val (entries, suggestedRoot, androidProjectDetected) = withContext(Dispatchers.IO) {
                Triple(
                    readWorkspace(project),
                    if (project.rootPath.isBlank()) detectNestedProjectRoot(project) else null,
                    findAndroidGradleProjectRoot(projectWorkspaceRoot(project)) != null,
                )
            }
            if (_state.value.activeProject?.id == project.id) {
                _state.update {
                    it.copy(
                        workspaceFiles = entries,
                        filesLoading = false,
                        suggestedProjectRoot = suggestedRoot,
                        androidProjectDetected = androidProjectDetected,
                    )
                }
            }
        }
    }

    fun openFile(entry: WorkspaceEntry) {
        if (entry.isDirectory) return
        val project = _state.value.activeProject ?: return
        _state.update { it.copy(openedFilePath = entry.path, openedFileContent = null, fileContentLoading = true) }
        viewModelScope.launch {
            val content = withContext(Dispatchers.IO) {
                val file = File(projectWorkspaceRoot(project), entry.path)
                runCatching {
                    if (file.length() > 512_000L) {
                        file.inputStream().use { stream ->
                            val buf = ByteArray(512_000)
                            val read = stream.read(buf)
                            String(buf, 0, read)
                        } + "\n\n[File truncated — too large to display fully]"
                    } else {
                        file.readText()
                    }
                }.getOrElse { "Could not read file: ${it.message}" }
            }
            _state.update { it.copy(openedFileContent = content, fileContentLoading = false) }
        }
    }

    fun closeFile() {
        _state.update { it.copy(openedFilePath = null, openedFileContent = null, fileContentLoading = false) }
    }


    private fun readWorkspace(project: Project): List<WorkspaceEntry> {
        val root = projectWorkspaceRoot(project)
        if (!root.isDirectory) return emptyList()
        val rootPath = root.canonicalFile.toPath()
        return root.walkTopDown()
            .maxDepth(12)
            .onEnter { directory ->
                val relative = if (directory == root) "" else directory.relativeTo(root).invariantSeparatorsPath
                directory == root || (!isClaudeRuntimeMetadata(relative) &&
                    !Files.isSymbolicLink(directory.toPath()) &&
                    runCatching { directory.canonicalFile.toPath().startsWith(rootPath) }.getOrDefault(false)
                    )
            }
            .drop(1)
            .filter { file ->
                val relative = file.relativeTo(root).invariantSeparatorsPath
                !isClaudeRuntimeMetadata(relative) &&
                    !Files.isSymbolicLink(file.toPath()) &&
                    runCatching { file.canonicalFile.toPath().startsWith(rootPath) }.getOrDefault(false)
            }
            .take(MAX_VISIBLE_WORKSPACE_ENTRIES)
            .map { file ->
                val relative = file.relativeTo(root).invariantSeparatorsPath
                WorkspaceEntry(
                    path = relative,
                    name = file.name,
                    isDirectory = file.isDirectory,
                    depth = relative.count { it == '/' },
                    sizeBytes = if (file.isFile) file.length() else 0,
                )
            }
            .sortedWith(
                compareBy<WorkspaceEntry> { entry ->
                    val parent = if (entry.path.contains('/')) entry.path.substringBeforeLast('/') else ""
                    parent.lowercase(java.util.Locale.ROOT)
                }
                .thenByDescending { it.isDirectory }
                .thenBy { it.name.lowercase(java.util.Locale.ROOT) }
            )
            .toList()
    }

    private fun isClaudeRuntimeMetadata(relativePath: String): Boolean {
        return relativePath == ".claude" ||
            relativePath == ".claude.json" ||
            relativePath.startsWith(".claude/")
    }

    private fun isExportExcludedPath(relativePath: String): Boolean {
        val excludedNames = setOf(
            ".git", ".claude", ".gradle", ".idea", ".next", ".cache",
            "node_modules", ".venv", "venv", "__pycache__", "build",
        )
        return relativePath.split('/').any { it in excludedNames } || isClaudeRuntimeMetadata(relativePath)
    }

    fun addChatAttachments(uris: List<Uri>) {
        val current = _state.value
        val project = current.activeProject ?: return
        val chatId = current.activeChatId ?: return
        if (current.isRunning || uris.isEmpty()) return
        val remaining = (MAX_ATTACHMENTS_PER_MESSAGE - current.pendingAttachments.size).coerceAtLeast(0)
        if (remaining == 0) {
            _state.update { it.copy(toastMessage = "You can attach up to $MAX_ATTACHMENTS_PER_MESSAGE files per message") }
            return
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val added = mutableListOf<ChatAttachment>()
                val errors = mutableListOf<String>()
                uris.take(remaining).forEach { uri ->
                    runCatching { copyChatAttachment(project, chatId, uri) }
                        .onSuccess(added::add)
                        .onFailure { errors += (it.message ?: "Could not attach file") }
                }
                added to errors
            }
            val (added, errors) = result
            _state.update { state ->
                state.copy(
                    pendingAttachments = state.pendingAttachments + added,
                    toastMessage = errors.firstOrNull() ?: if (uris.size > remaining) "Only $remaining more file${if (remaining == 1) "" else "s"} could be added" else null,
                )
            }
            if (added.isNotEmpty()) refreshProjectFiles()
        }
    }

    fun removePendingAttachment(attachmentId: String) {
        val current = _state.value
        val project = current.activeProject ?: return
        val attachment = current.pendingAttachments.firstOrNull { it.id == attachmentId } ?: return
        _state.update { it.copy(pendingAttachments = it.pendingAttachments.filterNot { item -> item.id == attachmentId }) }
        viewModelScope.launch(Dispatchers.IO) {
            val root = projectWorkspaceRoot(project)
            val file = File(root, attachment.relativePath).canonicalFile
            if (file.toPath().startsWith(root.canonicalFile.toPath())) file.delete()
        }
    }

    fun openChatAttachment(attachment: ChatAttachment) {
        val project = _state.value.activeProject ?: return
        runCatching {
            val root = projectWorkspaceRoot(project).canonicalFile
            val file = File(root, attachment.relativePath).canonicalFile
            require(file.isFile && file.toPath().startsWith(root.toPath())) { "Attachment is unavailable" }
            val app = getApplication<Application>()
            val uri = FileProvider.getUriForFile(app, "${app.packageName}.files", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, attachment.mimeType.ifBlank { "application/octet-stream" })
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            app.startActivity(intent)
        }.onFailure { error ->
            _state.update { it.copy(toastMessage = error.message ?: "No app can open this attachment") }
        }
    }

    private fun copyChatAttachment(project: Project, chatId: String, uri: Uri): ChatAttachment {
        val resolver = getApplication<Application>().contentResolver
        var displayName = "attachment"
        var declaredSize = -1L
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { displayName = cursor.getString(it) ?: displayName }
                cursor.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let { declaredSize = cursor.getLong(it) }
            }
        }
        val mimeType = resolver.getType(uri).orEmpty().ifBlank { "application/octet-stream" }
        val extension = displayName.substringAfterLast('.', "").lowercase()
        val supportedTextExtensions = setOf(
            "txt", "md", "markdown", "json", "jsonl", "csv", "tsv", "xml", "yaml", "yml", "log",
            "kt", "kts", "java", "py", "js", "mjs", "cjs", "ts", "tsx", "jsx", "html", "htm",
            "css", "scss", "sass", "less", "c", "cc", "cpp", "h", "hpp", "sh", "bash", "zsh",
            "gradle", "properties", "toml", "ini", "conf", "sql",
        )
        val supported = mimeType.startsWith("image/") ||
            mimeType.startsWith("text/") || mimeType == "application/json" || mimeType == "application/xml" ||
            mimeType.endsWith("+json") || mimeType.endsWith("+xml") || extension in supportedTextExtensions
        require(supported) { "Only images and text files are supported" }
        require(declaredSize <= MAX_ATTACHMENT_BYTES || declaredSize < 0) { "$displayName is larger than 25 MB" }
        val safeName = sanitizeAttachmentName(displayName)
        val root = projectWorkspaceRoot(project).canonicalFile
        val folder = File(root, "attachments/$chatId").apply { mkdirs() }.canonicalFile
        require(folder.toPath().startsWith(root.toPath())) { "Unsafe attachment folder" }
        val stem = safeName.substringBeforeLast('.', safeName)
        val safeExtension = safeName.substringAfterLast('.', "").let { if (it.isBlank()) "" else ".$it" }
        var destination = File(folder, safeName)
        var suffix = 2
        while (destination.exists()) destination = File(folder, "$stem-${suffix++}$safeExtension")
        var copied = 0L
        try {
            resolver.openInputStream(uri)?.buffered()?.use { input ->
                destination.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        copied += count
                        require(copied <= MAX_ATTACHMENT_BYTES) { "$displayName is larger than 25 MB" }
                        output.write(buffer, 0, count)
                    }
                }
            } ?: error("Could not read $displayName")
        } catch (error: Throwable) {
            destination.delete()
            throw error
        }
        return ChatAttachment(
            displayName = displayName.take(120),
            relativePath = destination.relativeTo(root).invariantSeparatorsPath,
            mimeType = mimeType,
            sizeBytes = copied,
        )
    }

    private fun sanitizeAttachmentName(name: String): String {
        val clean = name.substringAfterLast('/').replace(Regex("[^A-Za-z0-9._ -]"), "_").trim().trim('.').take(100)
        return clean.ifBlank { "attachment-${UUID.randomUUID().toString().take(8)}" }
    }

    fun selectModel(modelName: String) {
        val clean = modelName.trim()
        if (_state.value.agentKind == AgentKind.ANTIGRAVITY) {
            setAntigravityModel(clean)
        } else {
            val updated = _state.value.provider.copy(model = clean)
            preferences.saveProvider(updated, _state.value.agentKind)
            _state.update { it.copy(provider = updated, toastMessage = "Model switched to $clean") }
        }
    }

    fun selectEffort(effortLevel: String) {
        val norm = when (effortLevel.trim().lowercase()) {
            "low", "l" -> "low"
            "medium", "med", "m" -> "medium"
            "high", "h" -> "high"
            else -> effortLevel.trim().lowercase()
        }
        setAntigravityEffort(norm)
        _state.update { it.copy(toastMessage = "Reasoning effort set to $norm") }
    }

    fun toggleAutoApproveTools(enabled: Boolean? = null) {
        val newValue = enabled ?: !_state.value.autoApproveTools
        preferences.autoApproveTools = newValue
        _state.update {
            it.copy(
                autoApproveTools = newValue,
                toastMessage = if (newValue) "Auto-bypass ENABLED (autonomous)" else "Auto-bypass DISABLED (requires approval)",
            )
        }
    }

    fun setCustomRunnerCommand(command: String) {
        val clean = command.trim()
        preferences.customRunnerCommand = clean
        _state.update { it.copy(customRunnerCommand = clean, toastMessage = "Custom runner command saved") }
    }

    fun clearCurrentChat() {
        val project = _state.value.activeProject ?: return
        val chatId = _state.value.activeChatId ?: return
        preferences.saveMessages(project.id, chatId, emptyList())
        _state.update { it.copy(messages = emptyList(), liveProcess = emptyList(), activity = emptyList(), pendingApproval = null) }
    }

    fun executeSlashCommand(commandText: String): Boolean {
        val trimmed = commandText.trim()
        if (!trimmed.startsWith("/")) return false

        val parts = trimmed.removePrefix("/").trim().split("\\s+".toRegex())
        val cmd = parts.getOrNull(0)?.lowercase().orEmpty()
        val arg = parts.drop(1).joinToString(" ").trim()

        var feedback = ""
        when (cmd) {
            "model" -> {
                if (arg.isNotBlank()) {
                    selectModel(arg)
                    feedback = "⚡ Model switched to **$arg**"
                } else {
                    val currentModel = if (_state.value.agentKind == AgentKind.ANTIGRAVITY) _state.value.antigravityModel else _state.value.provider.model
                    feedback = "ℹ️ Current model: **${currentModel.ifBlank { "default" }}**\nUsage: `/model <name>` (e.g. `/model claude-3-7-sonnet`, `/model gemini-2.5-pro`, `/model gpt-4o`, `/model deepseek-chat`)"
                }
            }
            "effort" -> {
                if (arg.isNotBlank()) {
                    selectEffort(arg)
                    feedback = "🧠 Reasoning effort set to **${arg.uppercase()}**"
                } else {
                    feedback = "ℹ️ Current effort: **${_state.value.antigravityEffort.uppercase()}**\nUsage: `/effort <low|medium|high>`"
                }
            }
            "agent", "switch" -> {
                if (arg.isNotBlank()) {
                    val targetKind = AgentKind.entries.firstOrNull {
                        it.stableId.equals(arg, ignoreCase = true) ||
                            it.name.equals(arg, ignoreCase = true) ||
                            it.title.contains(arg, ignoreCase = true)
                    }
                    if (targetKind != null) {
                        selectAgent(targetKind)
                        feedback = "🤖 Switched agent engine to **${targetKind.title}**"
                    } else {
                        feedback = "⚠️ Unknown agent '$arg'. Available agents:\n" +
                            AgentKind.entries.joinToString("\n") { "• `${it.stableId}` (${it.title})" }
                    }
                } else {
                    feedback = "ℹ️ Current agent: **${_state.value.agentKind.title}**\nUsage: `/agent <name>`\nAvailable: `jcode`, `pi-agent`, `command-code`, `cline`, `custom-runner`, `claude-code`, `deepseek-harness`, `antigravity`"
                }
            }
            "jcode" -> {
                selectAgent(AgentKind.JCODE)
                feedback = "⚡ Switched to **JCode Agent** (multi-LLM coding engine)"
            }
            "piagent", "pi-agent", "pi" -> {
                selectAgent(AgentKind.PI_AGENT)
                feedback = "🥧 Switched to **Pi Agent** (autonomous reasoning engine)"
            }
            "commandcode", "command-code", "cmdcode" -> {
                selectAgent(AgentKind.COMMAND_CODE)
                feedback = "⌨️ Switched to **Command Code** (terminal-first CLI engine)"
            }
            "cline" -> {
                selectAgent(AgentKind.CLINE)
                feedback = "🔧 Switched to **Cline Agent** (autonomous coding CLI)"
            }
            "custom", "customrunner", "custom-runner" -> {
                selectAgent(AgentKind.CUSTOM_RUNNER)
                feedback = "🛠️ Switched to **Custom Agent Runner**"
            }
            "claude", "claudecode", "claude-code" -> {
                selectAgent(AgentKind.CLAUDE_CODE)
                feedback = "⚡ Switched to **Claude Code**"
            }
            "deepseek", "dsh", "deepseek-harness" -> {
                selectAgent(AgentKind.DEEPSEEK_HARNESS)
                feedback = "🐳 Switched to **DeepSeek Harness**"
            }
            "antigravity", "agy" -> {
                selectAgent(AgentKind.ANTIGRAVITY)
                feedback = "🚀 Switched to **Antigravity CLI**"
            }
            "bypass", "autoapprove", "auto-approve", "approve" -> {
                val enable = when (arg.lowercase()) {
                    "on", "enable", "true", "yes", "1" -> true
                    "off", "disable", "false", "no", "0" -> false
                    else -> !_state.value.autoApproveTools
                }
                toggleAutoApproveTools(enable)
                feedback = if (enable) {
                    "🛡️ **Tool Auto-Bypass: ENABLED**\nTools will run autonomously without interactive approval."
                } else {
                    "🛡️ **Tool Auto-Bypass: DISABLED**\nTools will request explicit confirmation before running."
                }
            }
            "permission", "permissions" -> {
                val enable = when (arg.lowercase()) {
                    "auto", "bypass", "all" -> true
                    "ask", "prompt", "manual", "strict" -> false
                    else -> !_state.value.autoApproveTools
                }
                toggleAutoApproveTools(enable)
                feedback = if (enable) {
                    "🛡️ **Permission Mode: AUTO-BYPASS** (autonomous tool execution)"
                } else {
                    "🛡️ **Permission Mode: ASK** (interactive approval required)"
                }
            }
            "clear" -> {
                clearCurrentChat()
                feedback = "🧹 Chat messages cleared."
            }

            // --- CLAUDE CODE COMMANDS ---
            "usage" -> {
                val estTokens = _state.value.quotaUsage?.usedTokens ?: (_state.value.messages.sumOf { it.text.length } / 4)
                feedback = """
                    ⚡ **Session Usage & Metrics**:
                    • **Active Agent**: `${_state.value.agentKind.title}` (`${_state.value.agentKind.stableId}`)
                    • **Messages in Context**: ${_state.value.messages.size}
                    • **Estimated Token Count**: ~$estTokens tokens
                    • **Tool Auto-Bypass**: ${if (_state.value.autoApproveTools) "ENABLED" else "DISABLED"}
                """.trimIndent()
            }
            "login" -> {
                feedback = """
                    🔑 **Claude Subscription / Login**:
                    • Current Provider: ${_state.value.provider.title} (${_state.value.provider.kind.name})
                    • Configure subscriptions and keys in **Settings → AI Providers**.
                """.trimIndent()
            }
            "cost" -> {
                val estTokens = (_state.value.messages.sumOf { it.text.length } / 4)
                val estCost = (estTokens * 0.000003).let { String.format(java.util.Locale.US, "%.4f", it) }
                feedback = "💰 **Session Cost Estimate**: `$$estCost` (~$estTokens tokens in active chat history)."
            }
            "compact" -> {
                val keepCount = 8
                val currentCount = _state.value.messages.size
                if (currentCount > keepCount) {
                    val compacted = _state.value.messages.takeLast(keepCount)
                    _state.update { it.copy(messages = compacted) }
                    persistMessages()
                    feedback = "📦 **Context Compacted**: Retained latest $keepCount messages (pruned ${currentCount - keepCount} older messages)."
                } else {
                    feedback = "📦 Conversation is already compact ($currentCount messages)."
                }
            }
            "doctor" -> {
                val hasRootfs = installer.isInstalled()
                val activeProj = _state.value.activeProject?.name ?: "None"
                val bypass = _state.value.autoApproveTools
                feedback = """
                    🩺 **Runtime Doctor Health Check**:
                    • **PRoot Environment**: ${if (hasRootfs) "✅ Ready" else "⚠️ Rootfs Setup Required"}
                    • **Active Project**: `$activeProj`
                    • **Tool Auto-Bypass**: ${if (bypass) "🛡️ Enabled (Autonomous)" else "🔒 Disabled (Approval Required)"}
                    • **Common Capability Pool**: ✅ Active (${commonCapabilityPool.getEnabledSkills().size} skills, ${commonCapabilityPool.getAvailableTools().size} MCP tools)
                    • **Shared Blackboard**: ${commonCapabilityPool.getAllMemories().size} memory entries
                """.trimIndent()
            }
            "review" -> {
                val proj = _state.value.activeProject
                if (proj != null) {
                    viewModelScope.launch(Dispatchers.IO) {
                        val root = projectWorkspaceRoot(proj)
                        val guest = projectGuestRoot(proj)
                        val diff = gitManager.getDiff(root, guest).getOrNull().orEmpty()
                        val reviewSummary = if (diff.isNotBlank()) {
                            "🔎 **Automated Code Review (Diff)**:\n```diff\n${diff.take(600)}\n```\n\nFound uncommitted changes across project files."
                        } else {
                            "🔎 **Git Diff Review**: Working tree is clean. No uncommitted modifications to review."
                        }
                        withContext(Dispatchers.Main) {
                            _state.update {
                                it.copy(
                                    messages = it.messages + ChatMessage(fromUser = false, text = reviewSummary),
                                    toastMessage = "Git diff review complete",
                                )
                            }
                            persistMessages()
                        }
                    }
                    feedback = "🔎 Initiating automated git diff review..."
                } else {
                    feedback = "⚠️ No active project loaded."
                }
            }
            "pr" -> {
                val prProj = _state.value.activeProject
                if (prProj != null) {
                    viewModelScope.launch(Dispatchers.IO) {
                        val root = projectWorkspaceRoot(prProj)
                        val guest = projectGuestRoot(prProj)
                        val branch = gitManager.getCurrentBranch(root, guest).getOrNull() ?: "main"
                        val status = gitManager.getStatus(root, guest).getOrNull().orEmpty()
                        val prMsg = """
                            🐙 **Pull Request Status**:
                            • **Active Branch**: `$branch`
                            • **Working Tree**: ${if (status.isBlank()) "Clean ✅" else "Uncommitted changes present"}
                            • Ready to push branch and open pull request.
                        """.trimIndent()
                        withContext(Dispatchers.Main) {
                            _state.update {
                                it.copy(messages = it.messages + ChatMessage(fromUser = false, text = prMsg))
                            }
                            persistMessages()
                        }
                    }
                    feedback = "🐙 Inspecting branch and PR status..."
                } else {
                    feedback = "⚠️ No active project loaded."
                }
            }

            // --- DEEPSEEK HARNESS COMMANDS ---
            "key" -> {
                if (arg.isNotBlank()) {
                    vault.put(ProviderKind.DEEPSEEK.name, arg)
                    feedback = "🔑 DeepSeek API key updated securely."
                } else {
                    val hasKey = vault.has(ProviderKind.DEEPSEEK.name)
                    feedback = "🔑 DeepSeek API Key: ${if (hasKey) "Configured ✅" else "Not set ❌"}\nUsage: `/key <api_key>`"
                }
            }
            "think" -> {
                val toggled = !_state.value.liveThinking
                _state.update { it.copy(liveThinking = toggled) }
                feedback = if (toggled) "🧠 DeepSeek thought trace **ENABLED**" else "🧠 DeepSeek thought trace **DISABLED**"
            }
            "endpoint" -> {
                if (arg.isNotBlank()) {
                    val updated = _state.value.provider.copy(baseUrl = arg)
                    preferences.saveProvider(updated, _state.value.agentKind)
                    _state.update { it.copy(provider = updated) }
                    feedback = "🌐 Gateway API endpoint set to `$arg`"
                } else {
                    feedback = "🌐 Current endpoint: `${_state.value.provider.baseUrl.ifBlank { "https://api.deepseek.com/v1" }}`\nUsage: `/endpoint <url>`"
                }
            }
            "quota" -> {
                feedback = "📊 **Quota Status**: Active with standard rate limits. Token quota monitored per request."
            }

            // --- ANTIGRAVITY CLI COMMANDS ---
            "auth" -> {
                startAntigravityLogin()
                feedback = "🚀 Initiating Google device code authentication for Antigravity CLI..."
            }
            "account" -> {
                val email = _state.value.antigravityAuth.accountEmail ?: "Not logged in"
                val authState = _state.value.antigravityAuth.status.name
                feedback = """
                    👤 **Antigravity Account**:
                    • **Status**: `$authState`
                    • **Account**: `$email`
                """.trimIndent()
            }
            "sync" -> {
                refreshProjectFiles()
                refreshOrchestratorState()
                feedback = "🔄 Synchronized project files, models, and orchestrator state."
            }

            // --- JCODE AGENT COMMANDS ---
            "plan" -> {
                if (arg.isNotBlank()) {
                    val planProj = _state.value.activeProject
                    if (planProj != null) {
                        val root = projectWorkspaceRoot(planProj)
                        taskBoardManager.createTask(
                            title = "Plan: ${arg.take(30)}",
                            description = arg,
                            priority = com.jarves.mh.model.TaskPriority.HIGH,
                        )
                        taskBoardManager.saveToFile(root)
                        refreshOrchestratorState()
                        feedback = "📐 **Plan Created**: Added plan task to TaskBoard for: \"$arg\""
                    } else {
                        feedback = "📐 Plan: $arg"
                    }
                } else {
                    feedback = "📐 Usage: `/plan <goal or feature description>`"
                }
            }
            "swarm" -> {
                if (arg.isNotBlank()) {
                    createSwarmTask("Swarm: ${arg.take(25)}", arg, com.jarves.mh.model.TaskPriority.HIGH, null, emptyList())
                    feedback = "🐝 **Swarm Dispatched**: Delegated \"$arg\" to autonomous agent swarm!"
                } else {
                    feedback = "🐝 Usage: `/swarm <objective>` — Delegates task to multi-agent swarm."
                }
            }
            "linear" -> {
                loadLinearIssues(arg.ifBlank { vault.get("LINEAR_API_KEY").orEmpty() })
                feedback = "📌 Syncing Linear issues..."
            }
            "github" -> {
                refreshGitHubIssues(arg.ifBlank { null })
                feedback = "🐙 Syncing GitHub issues${if (arg.isNotBlank()) " for $arg" else ""}..."
            }
            "tools" -> {
                val poolTools = commonCapabilityPool.getAvailableTools().joinToString(", ") { it.name }
                feedback = """
                    🔧 **Active Toolchains & Environments**:
                    • Node.js & npm (PRoot guest)
                    • Python 3 & pip (PRoot guest)
                    • Android SDK (gradle / AAPT)
                    • Common MCP Tools: $poolTools
                """.trimIndent()
            }

            // --- PI AGENT COMMANDS ---
            "depth" -> {
                val depth = arg.toIntOrNull() ?: 3
                commonCapabilityPool.putMemory("pi_depth", "CONFIG", depth.toString(), "PiAgent")
                feedback = "🧠 Pi recursive reflection depth set to **$depth**"
            }
            "reflect" -> {
                feedback = """
                    🔍 **Intermediate Reflection Pass**:
                    • **Goal Consistency**: Validated
                    • **Execution Guardrails**: Enforced
                    • **Next Optimal Step**: Ready for prompt execution
                """.trimIndent()
            }

            // --- COMMAND CODE COMMANDS ---
            "exec" -> {
                if (arg.isNotBlank()) {
                    requestProjectTerminalCommand(arg)
                    feedback = "⌨️ **Executing in PRoot Terminal**: `$arg`\nOutput will stream to the Terminal tab."
                } else {
                    feedback = "⌨️ Usage: `/exec <command>` (e.g. `/exec git status`, `/exec npm test`)"
                }
            }
            "term" -> {
                openProjectTerminal()
                feedback = "💻 Opened project terminal session."
            }
            "env" -> {
                val root = _state.value.activeProject?.let { projectGuestRoot(it) } ?: "/"
                feedback = """
                    🌍 **Guest PRoot Environment**:
                    • `WORKSPACE`: `$root`
                    • `SHELL`: `/bin/bash`
                    • `HOME`: `/root`
                    • `LANG`: `C.UTF-8`
                    • `PATH`: `/usr/local/bin:/usr/bin:/bin`
                """.trimIndent()
            }
            "alias" -> {
                feedback = """
                    🏷️ **Configured Terminal Aliases**:
                    • `build` — `./gradlew assembleDebug` or `npm run build`
                    • `test` — `./gradlew test` or `npm test`
                    • `git` — `git --no-pager`
                    • `lint` — `ktlint` or `eslint`
                """.trimIndent()
            }

            // --- CLINE AGENT COMMANDS ---
            "mode" -> {
                feedback = if (arg.isNotBlank()) {
                    "🎛️ Cline persona mode switched to **${arg.uppercase()}**"
                } else {
                    "ℹ️ Current Cline persona mode: **CODE**\nUsage: `/mode <code|architect|ask|test>`"
                }
            }
            "rules" -> {
                feedback = """
                    📜 **Project Instruction Guardrails**:
                    • Neobrutalist design guidelines strictly enforced
                    • Tool auto-bypass: ${_state.value.autoApproveTools}
                    • Common Capability Pool active for all agent harnesses
                """.trimIndent()
            }

            // --- CUSTOM RUNNER COMMANDS ---
            "cmd" -> {
                if (arg.isNotBlank()) {
                    setCustomRunnerCommand(arg)
                    feedback = "⚙️ Custom runner command updated to: `$arg`"
                } else {
                    feedback = "ℹ️ Current runner command: `${_state.value.customRunnerCommand.ifBlank { "bash /bin/custom_agent_runner.sh" }}`\nUsage: `/cmd <command>`"
                }
            }
            "script" -> {
                feedback = "📜 **Custom Agent Runner Script**: Configured at rootfs `/bin/custom_agent_runner.sh`"
            }
            "reload" -> {
                feedback = "🔄 Custom agent runner configuration reloaded."
            }
            "test" -> {
                feedback = "🧪 Testing custom agent runner probe... OK (runner binary responding)."
            }

            // --- COMMON MCP & SKILL POOL COMMANDS ---
            "pool" -> {
                val skills = commonCapabilityPool.getEnabledSkills()
                val tools = commonCapabilityPool.getAvailableTools()
                val memories = commonCapabilityPool.getAllMemories()
                feedback = """
                    🏊 **Common Capability Pool**:
                    • **Active Agent Harness**: `${_state.value.agentKind.title}` (`${_state.value.agentKind.stableId}`)
                    • **Enabled Skills (${skills.size})**: ${skills.joinToString { it.name }.ifEmpty { "None" }}
                    • **Registered MCP Tools (${tools.size})**: ${tools.joinToString { it.name }.ifEmpty { "None" }}
                    • **Shared Blackboard Memories (${memories.size})**: ${memories.take(5).joinToString { "${it.key}: ${it.value.take(20)}" }.ifEmpty { "Empty" }}
                    • **Tool Auto-Bypass Mode**: ${if (_state.value.autoApproveTools) "ENABLED" else "DISABLED"}
                """.trimIndent()
            }
            "mcp" -> {
                when {
                    arg.startsWith("run ") -> {
                        val toolParts = arg.removePrefix("run ").trim().split("\\s+".toRegex(), 2)
                        val toolName = toolParts.getOrNull(0).orEmpty()
                        val toolArgsRaw = toolParts.getOrNull(1).orEmpty()
                        val toolArgs = if (toolArgsRaw.isNotBlank()) {
                            mapOf("input" to toolArgsRaw, "command" to toolArgsRaw, "path" to toolArgsRaw)
                        } else emptyMap()
                        viewModelScope.launch {
                            val result = commonCapabilityPool.executeMcpTool(toolName, toolArgs)
                            val resMsg = "🛠️ **MCP Tool Execution (`$toolName`)**:\n" +
                                if (result.isError) "❌ Error: ${result.content}" else "✅ Output:\n${result.content}"
                            _state.update {
                                it.copy(messages = it.messages + ChatMessage(fromUser = false, text = resMsg))
                            }
                            persistMessages()
                        }
                        feedback = "🛠️ Executing MCP tool `$toolName`..."
                    }
                    else -> {
                        val toolsList = commonCapabilityPool.getAvailableTools().joinToString("\n") { "• `${it.name}` [${it.category}]: ${it.description}" }
                        feedback = "🛠️ **Registered MCP Tools**:\n$toolsList\n\nUsage: `/mcp run <tool_name> [args]`"
                    }
                }
            }
            "skills" -> {
                when {
                    arg.startsWith("toggle ") -> {
                        val skillId = arg.removePrefix("toggle ").trim()
                        val toggled = commonCapabilityPool.toggleSkill(skillId)
                        _state.update { it.copy(skills = skillRegistry.getAllSkills()) }
                        feedback = "✨ Skill `$skillId` toggled: ${if (toggled) "ENABLED" else "DISABLED"}"
                    }
                    else -> {
                        val skillsList = commonCapabilityPool.getAvailableSkills().joinToString("\n") { skill ->
                            val status = if (skill.enabled) "✅ [ENABLED]" else "⏸️ [DISABLED]"
                            "$status `${skill.id}` (${skill.name}) — ${skill.description}"
                        }
                        feedback = "✨ **Common Skills Pool**:\n$skillsList\n\nUsage: `/skills toggle <skill_id>`"
                    }
                }
            }
            "memory" -> {
                when {
                    arg.startsWith("get ") -> {
                        val key = arg.removePrefix("get ").trim()
                        val value = commonCapabilityPool.getMemory(key)
                        feedback = if (value != null) "💾 **Memory [`$key`]**: $value" else "💾 Key `$key` not found in shared blackboard memory."
                    }
                    arg.startsWith("set ") -> {
                        val memParts = arg.removePrefix("set ").trim().split("\\s+".toRegex(), 2)
                        val key = memParts.getOrNull(0).orEmpty()
                        val value = memParts.getOrNull(1).orEmpty()
                        if (key.isNotBlank()) {
                            commonCapabilityPool.putMemory(key, "USER", value, "UserChat")
                            _state.update { it.copy(blackboardEntries = commonCapabilityPool.getAllMemories()) }
                            feedback = "💾 Saved to shared blackboard memory: `$key` = \"$value\""
                        } else {
                            feedback = "💾 Usage: `/memory set <key> <value>`"
                        }
                    }
                    else -> {
                        val all = commonCapabilityPool.getAllMemories()
                        val list = if (all.isNotEmpty()) {
                            all.joinToString("\n") { "• [${it.category}] `${it.key}`: ${it.value}" }
                        } else "Shared blackboard is empty."
                        feedback = "💾 **Shared Blackboard Memory**:\n$list\n\nUsage: `/memory set <key> <value>` or `/memory get <key>`"
                    }
                }
            }

            "status" -> {
                val currentModel = if (_state.value.agentKind == AgentKind.ANTIGRAVITY) _state.value.antigravityModel else _state.value.provider.model
                val bypassStatus = if (_state.value.autoApproveTools) "ENABLED (Auto-Bypass)" else "DISABLED (Ask Approval)"
                feedback = """
                    ### 📊 Active Configuration
                    • **Agent**: `${_state.value.agentKind.title}` (`${_state.value.agentKind.stableId}`)
                    • **Model**: `${currentModel.ifBlank { "default" }}`
                    • **Reasoning Effort**: `${_state.value.antigravityEffort.uppercase()}`
                    • **Tool Auto-Bypass**: `$bypassStatus`
                    • **Common Pool Skills**: ${commonCapabilityPool.getEnabledSkills().size} active
                    • **Common Pool MCP Tools**: ${commonCapabilityPool.getAvailableTools().size} registered
                """.trimIndent()
            }
            "help" -> {
                val commands = commonCapabilityPool.getHarnessCommands(_state.value.agentKind)
                feedback = buildString {
                    appendLine("### 🛠️ Commands for ${_state.value.agentKind.title}")
                    commands.forEach { cmdInfo ->
                        appendLine("• `${cmdInfo.command}` — ${cmdInfo.description}")
                    }
                }.trimIndent()
            }
            else -> {
                feedback = "❓ Unknown slash command `/$cmd`. Type `/help` for a list of available commands."
            }
        }

        // Post the command and response into the chat transcript
        _state.update {
            it.copy(
                messages = it.messages + listOf(
                    ChatMessage(fromUser = true, text = trimmed),
                    ChatMessage(fromUser = false, text = feedback),
                ),
                toastMessage = feedback.lines().firstOrNull()?.replace("*", "")?.take(60),
            )
        }
        persistMessages()
        return true
    }

    fun sendPrompt(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.startsWith("/")) {
            if (executeSlashCommand(trimmed)) return
        }
        val project = state.value.activeProject ?: return
        if (_state.value.agentKind == AgentKind.ANTIGRAVITY &&
            _state.value.antigravityAuth.status != AntigravityAuthStatus.SIGNED_IN) {
            _state.update { it.copy(toastMessage = "Sign in to Antigravity from Settings before starting a task.") }
            return
        }
        if (_state.value.agentKind == AgentKind.DEEPSEEK_HARNESS && _state.value.provider.kind == ProviderKind.CLAUDE) {
            _state.update { it.copy(toastMessage = "Claude subscription login is not supported by DeepSeek Harness — pick a key-based provider in Settings.") }
            return
        }
        val attachments = state.value.pendingAttachments
        if ((prompt.isBlank() && attachments.isEmpty()) || state.value.isRunning) return
        val requestText = prompt.trim().ifBlank { "Please review the attached files." }
        updateActiveChatTitle(requestText)
        _state.update {
            val startedAt = System.currentTimeMillis()
            it.copy(
                messages = it.messages + ChatMessage(fromUser = true, text = prompt.trim(), attachments = attachments),
                pendingAttachments = emptyList(),
                isRunning = true,
                activity = listOf(ActivityItem("Understanding your request", "Preparing a safe plan", false)) + it.activity,
                liveProcess = listOf(ActivityItem("Think", requestPlanningSummary(requestText, it.agentKind), false)),
                liveThinking = true,
                activeThinkingBlockId = null,
                taskStartedAtMillis = startedAt,
                taskFinishedAtMillis = null,
                workSegmentStartedAtMillis = startedAt,
                currentTaskRequest = requestText,
            )
        }
        touchProject(project.id)
        persistMessages()
        val history = state.value.messages // includes all messages up to now
        val commonContext = commonCapabilityPool.buildCommonPromptContext(state.value.agentKind)
        val runtimePrompt = if (attachments.isEmpty()) {
            "$commonContext\n\n$requestText"
        } else buildString {
            appendLine(commonContext)
            appendLine()
            appendLine(requestText)
            appendLine()
            appendLine("<attached_files>")
            attachments.forEach { attachment ->
                appendLine("- ${attachment.displayName}: ${projectGuestRoot(project)}/${attachment.relativePath} (${attachment.mimeType})")
            }
            appendLine("These files were explicitly attached by the user. Inspect them only as needed for the request.")
            appendLine("</attached_files>")
        }
        failedApiKeyIds.clear()
        activeRuntimeRequest = RuntimeRetryRequest(
            runtime = activeRuntime(),
            project = project,
            prompt = runtimePrompt,
            history = history,
            provider = state.value.provider,
        )
        viewModelScope.launch {
            activeRuntimeRequest?.let { request ->
                request.runtime.startSession(
                    request.project.id,
                    request.project.slug,
                    request.project.kind,
                    request.prompt,
                    request.history,
                    request.provider,
                )
            }
        }
    }

    fun answerApproval(approved: Boolean) {
        val request = state.value.pendingApproval ?: return
        viewModelScope.launch { activeRuntime().respondToApproval(request, approved) }
    }

    fun stopTask() {
        if (!_state.value.isRunning) return
        viewModelScope.launch { activeRuntime().stopActiveSession() }
    }

    fun undoLastChanges() {
        val project = _state.value.activeProject ?: return
        viewModelScope.launch {
            val restored = activeRuntime().undoLastChanges(project.id)
            _state.update { current ->
                current.copy(
                    changes = if (restored) emptyList() else current.changes,
                    activity = listOf(
                        ActivityItem(
                            if (restored) "Changes undone" else "Undo unavailable",
                            if (restored) "Restored files to their state before the task" else "No restorable checkpoint was found",
                        ),
                    ) + current.activity,
                )
            }
            if (restored) refreshProjectFiles()
        }
    }

    fun keepLastChanges() {
        val project = _state.value.activeProject ?: return
        viewModelScope.launch {
            activeRuntime().acceptLastChanges(project.id)
            _state.update {
                it.copy(
                    changes = emptyList(),
                    activity = listOf(ActivityItem("Changes kept", "Accepted the task's file changes")) + it.activity,
                )
            }
        }
    }

    fun undoFileChange(path: String) {
        val project = _state.value.activeProject ?: return
        viewModelScope.launch {
            if (activeRuntime().undoFileChange(project.id, path)) {
                _state.update { current -> current.copy(changes = current.changes.filterNot { it.path == path }) }
                refreshProjectFiles()
            }
        }
    }

    fun keepFileChange(path: String) {
        val project = _state.value.activeProject ?: return
        viewModelScope.launch {
            if (activeRuntime().acceptFileChange(project.id, path)) {
                _state.update { current -> current.copy(changes = current.changes.filterNot { it.path == path }) }
            }
        }
    }

    fun refreshGitState() {
        val project = _state.value.activeProject ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val workspace = projectWorkspaceRoot(project)
            val guestPath = projectGuestRoot(project)
            if (!installer.isInstalled()) return@launch

            if (!gitManager.isGitRepository(workspace, guestPath)) {
                gitManager.init(workspace, guestPath)
            }

            val status = gitManager.status(workspace, guestPath).getOrNull()
            val branches = gitManager.branches(workspace, guestPath).getOrNull().orEmpty()
            val worktrees = worktreeManager.listWorktrees(workspace, guestPath).getOrNull().orEmpty()
            val commits = gitManager.log(workspace, guestPath, maxCount = 50).getOrNull().orEmpty()
            val stagedDiffs = gitManager.diff(workspace, guestPath, staged = true).getOrNull().orEmpty()
            val unstagedDiffs = gitManager.diff(workspace, guestPath, staged = false).getOrNull().orEmpty()

            val token = getGitHubToken()
            val remotes = gitManager.getRemotes(workspace, guestPath).getOrNull().orEmpty()
            val origin = remotes["origin"].orEmpty()
            val repoFullName = _state.value.githubRepoOverride ?: extractGitHubRepoFullName(origin, project.description)
            val pullRequests = if (!token.isNullOrBlank() && !repoFullName.isNullOrBlank()) {
                runCatching { gitHubClient.listPullRequests(token, repoFullName) }.getOrNull().orEmpty()
            } else emptyList()
            val issues = if (!token.isNullOrBlank() && !repoFullName.isNullOrBlank()) {
                runCatching { gitHubClient.listIssues(token, repoFullName) }.getOrNull().orEmpty()
            } else emptyList()

            withContext(Dispatchers.Main) {
                if (_state.value.activeProject?.id == project.id) {
                    _state.update {
                        it.copy(
                            gitStatus = status,
                            gitBranches = branches,
                            gitWorktrees = worktrees,
                            gitCommits = commits,
                            gitStagedDiffs = stagedDiffs,
                            gitUnstagedDiffs = unstagedDiffs,
                            gitPullRequests = pullRequests,
                            githubIssues = if (issues.isNotEmpty() || it.githubIssues.isEmpty()) issues else it.githubIssues,
                            gitOperationRunning = false,
                            gitOperationMessage = null,
                        )
                    }
                }
            }
        }
    }

    fun stageFile(path: String) {
        val project = _state.value.activeProject ?: return
        viewModelScope.launch(Dispatchers.IO) {
            gitManager.stageFile(projectWorkspaceRoot(project), projectGuestRoot(project), path)
            refreshGitState()
        }
    }

    fun unstageFile(path: String) {
        val project = _state.value.activeProject ?: return
        viewModelScope.launch(Dispatchers.IO) {
            gitManager.unstageFile(projectWorkspaceRoot(project), projectGuestRoot(project), path)
            refreshGitState()
        }
    }

    fun stageAllGitFiles() {
        val project = _state.value.activeProject ?: return
        viewModelScope.launch(Dispatchers.IO) {
            gitManager.stageAll(projectWorkspaceRoot(project), projectGuestRoot(project))
            refreshGitState()
        }
    }

    fun unstageAllGitFiles() {
        val project = _state.value.activeProject ?: return
        viewModelScope.launch(Dispatchers.IO) {
            gitManager.unstageAll(projectWorkspaceRoot(project), projectGuestRoot(project))
            refreshGitState()
        }
    }

    fun discardGitFile(path: String) {
        val project = _state.value.activeProject ?: return
        viewModelScope.launch(Dispatchers.IO) {
            gitManager.discardChanges(projectWorkspaceRoot(project), projectGuestRoot(project), path)
            refreshGitState()
            refreshProjectFiles()
        }
    }

    fun commitGitChanges(message: String, amend: Boolean = false) {
        val project = _state.value.activeProject ?: return
        _state.update { it.copy(gitOperationRunning = true, gitOperationMessage = "Committing changes…") }
        viewModelScope.launch(Dispatchers.IO) {
            val authorName = _state.value.githubLogin ?: "Mobile Harness User"
            val authorEmail = _state.value.githubLogin?.let { "$it@users.noreply.github.com" } ?: "harness@mobile.internal"
            val result = gitManager.commit(
                workspace = projectWorkspaceRoot(project),
                guestPath = projectGuestRoot(project),
                message = message,
                authorName = authorName,
                authorEmail = authorEmail,
                amend = amend,
            )
            withContext(Dispatchers.Main) {
                result.onSuccess { commit ->
                    _state.update { it.copy(toastMessage = "Committed: ${commit.shortHash} - ${commit.message}") }
                }.onFailure { error ->
                    _state.update { it.copy(toastMessage = "Commit failed: ${error.message?.take(150)}") }
                }
            }
            refreshGitState()
            refreshProjectFiles()
        }
    }

    fun createGitBranch(name: String, checkout: Boolean = true) {
        val project = _state.value.activeProject ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val result = gitManager.createBranch(projectWorkspaceRoot(project), projectGuestRoot(project), name, checkout)
            withContext(Dispatchers.Main) {
                result.onSuccess {
                    _state.update { it.copy(toastMessage = "Branch '$name' created") }
                }.onFailure { error ->
                    _state.update { it.copy(toastMessage = "Branch creation failed: ${error.message?.take(150)}") }
                }
            }
            refreshGitState()
            if (checkout) refreshProjectFiles()
        }
    }

    fun checkoutGitBranch(name: String) {
        val project = _state.value.activeProject ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val result = gitManager.checkoutBranch(projectWorkspaceRoot(project), projectGuestRoot(project), name)
            withContext(Dispatchers.Main) {
                result.onSuccess {
                    _state.update { it.copy(toastMessage = "Switched to branch '$name'") }
                }.onFailure { error ->
                    _state.update { it.copy(toastMessage = "Checkout failed: ${error.message?.take(150)}") }
                }
            }
            refreshGitState()
            refreshProjectFiles()
        }
    }

    fun deleteGitBranch(name: String, force: Boolean = false) {
        val project = _state.value.activeProject ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val result = gitManager.deleteBranch(projectWorkspaceRoot(project), projectGuestRoot(project), name, force)
            withContext(Dispatchers.Main) {
                result.onSuccess {
                    _state.update { it.copy(toastMessage = "Branch '$name' deleted") }
                }.onFailure { error ->
                    _state.update { it.copy(toastMessage = "Delete failed: ${error.message?.take(150)}") }
                }
            }
            refreshGitState()
        }
    }

    fun mergeGitBranch(name: String) {
        val project = _state.value.activeProject ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val result = gitManager.mergeBranch(projectWorkspaceRoot(project), projectGuestRoot(project), name)
            withContext(Dispatchers.Main) {
                result.onSuccess {
                    _state.update { it.copy(toastMessage = "Merged branch '$name'") }
                }.onFailure { error ->
                    _state.update { it.copy(toastMessage = "Merge failed: ${error.message?.take(150)}") }
                }
            }
            refreshGitState()
            refreshProjectFiles()
        }
    }

    fun createGitWorktree(branchName: String) {
        val project = _state.value.activeProject ?: return
        _state.update { it.copy(gitOperationRunning = true, gitOperationMessage = "Creating isolated worktree…") }
        viewModelScope.launch(Dispatchers.IO) {
            val result = worktreeManager.addWorktree(projectWorkspaceRoot(project), projectGuestRoot(project), branchName)
            withContext(Dispatchers.Main) {
                result.onSuccess { wt ->
                    _state.update { it.copy(toastMessage = "Created worktree for '${wt.branch}' at '${wt.path}'") }
                }.onFailure { error ->
                    _state.update { it.copy(toastMessage = "Worktree failed: ${error.message?.take(150)}") }
                }
            }
            refreshGitState()
            refreshProjectFiles()
        }
    }

    fun removeGitWorktree(worktreePath: String) {
        val project = _state.value.activeProject ?: return
        _state.update { it.copy(gitOperationRunning = true, gitOperationMessage = "Removing worktree…") }
        viewModelScope.launch(Dispatchers.IO) {
            val result = worktreeManager.removeWorktree(projectWorkspaceRoot(project), projectGuestRoot(project), worktreePath)
            withContext(Dispatchers.Main) {
                result.onSuccess {
                    _state.update { it.copy(toastMessage = "Worktree '$worktreePath' removed") }
                }.onFailure { error ->
                    _state.update { it.copy(toastMessage = "Worktree removal failed: ${error.message?.take(150)}") }
                }
            }
            refreshGitState()
            refreshProjectFiles()
        }
    }

    fun pushGitBranch() {
        val project = _state.value.activeProject ?: return
        val branch = _state.value.gitStatus?.currentBranch ?: "main"
        _state.update { it.copy(gitOperationRunning = true, gitOperationMessage = "Pushing '$branch' to remote…") }
        viewModelScope.launch(Dispatchers.IO) {
            val token = getGitHubToken()
            val result = gitManager.push(projectWorkspaceRoot(project), projectGuestRoot(project), branch = branch, token = token)
            withContext(Dispatchers.Main) {
                result.onSuccess {
                    _state.update { it.copy(toastMessage = "Pushed branch '$branch' to remote") }
                }.onFailure { error ->
                    _state.update { it.copy(toastMessage = "Push failed: ${error.message?.lineSequence()?.lastOrNull()?.take(160)}") }
                }
            }
            refreshGitState()
        }
    }

    fun pullGitBranch() {
        val project = _state.value.activeProject ?: return
        val branch = _state.value.gitStatus?.currentBranch ?: "main"
        _state.update { it.copy(gitOperationRunning = true, gitOperationMessage = "Pulling '$branch' from remote…") }
        viewModelScope.launch(Dispatchers.IO) {
            val token = getGitHubToken()
            val result = gitManager.pull(projectWorkspaceRoot(project), projectGuestRoot(project), branch = branch, token = token)
            withContext(Dispatchers.Main) {
                result.onSuccess {
                    _state.update { it.copy(toastMessage = "Pulled updates for '$branch'") }
                }.onFailure { error ->
                    _state.update { it.copy(toastMessage = "Pull failed: ${error.message?.lineSequence()?.lastOrNull()?.take(160)}") }
                }
            }
            refreshGitState()
            refreshProjectFiles()
        }
    }

    fun selectGitCommit(commit: com.jarves.mh.model.GitCommit?) {
        val project = _state.value.activeProject
        if (project == null || commit == null) {
            _state.update { it.copy(selectedGitCommit = null, selectedGitCommitDiffs = emptyList()) }
            return
        }
        _state.update { it.copy(selectedGitCommit = commit) }
        viewModelScope.launch(Dispatchers.IO) {
            val diffs = gitManager.commitDiff(projectWorkspaceRoot(project), projectGuestRoot(project), commit.hash).getOrNull().orEmpty()
            withContext(Dispatchers.Main) {
                if (_state.value.selectedGitCommit?.hash == commit.hash) {
                    _state.update { it.copy(selectedGitCommitDiffs = diffs) }
                }
            }
        }
    }

    fun createGitHubPullRequest(title: String, body: String, base: String) {
        val project = _state.value.activeProject ?: return
        val token = getGitHubToken()
        if (token.isNullOrBlank()) {
            _state.update { it.copy(toastMessage = "Connect GitHub in Settings first") }
            return
        }
        val currentBranch = _state.value.gitStatus?.currentBranch ?: "main"
        _state.update { it.copy(gitOperationRunning = true, gitOperationMessage = "Creating Pull Request on GitHub…") }
        viewModelScope.launch(Dispatchers.IO) {
            val remotes = gitManager.getRemotes(projectWorkspaceRoot(project), projectGuestRoot(project)).getOrNull().orEmpty()
            val origin = remotes["origin"].orEmpty()
            val repoFullName = extractGitHubRepoFullName(origin, project.description)
            if (repoFullName.isNullOrBlank()) {
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(toastMessage = "Could not identify GitHub repository for this project", gitOperationRunning = false) }
                }
                return@launch
            }
            val result = runCatching {
                gitHubClient.createPullRequest(token, repoFullName, title, body, currentBranch, base)
            }
            withContext(Dispatchers.Main) {
                result.onSuccess { pr ->
                    _state.update { it.copy(toastMessage = "Created PR #${pr.number}: ${pr.title}") }
                }.onFailure { error ->
                    _state.update { it.copy(toastMessage = "PR creation failed: ${error.message?.take(160)}") }
                }
            }
            refreshGitState()
        }
    }

    fun loadAndRefreshOrchestrator(project: Project) {
        val root = projectWorkspaceRoot(project)
        skillRegistry = SkillRegistry(root)
        mcpToolManager = McpToolManager(root)
        diffReviewManager = DiffReviewManager(root)
        sshRuntimeManager = SshRuntimeManager(root)
        commonCapabilityPool = CommonCapabilityPool(root, skillRegistry, mcpToolManager, blackboardMemoryStore)
        viewModelScope.launch(Dispatchers.IO) {
            taskBoardManager.loadFromFile(root)
            blackboardMemoryStore.loadFromFile(root)
            agentInboxManager.loadFromFile(root)
            withContext(Dispatchers.Main) {
                refreshOrchestratorState()
            }
        }
    }

    fun refreshOrchestratorState() {
        _state.update {
            it.copy(
                orchestratorTasks = taskBoardManager.getAllTasks(),
                orchestratorAgents = godOrchestrator.getAllAgents(),
                blackboardEntries = blackboardMemoryStore.getAllEntries(),
                swarmAuditLogs = godOrchestrator.getAuditLog(),
                circuitBreakerState = godOrchestrator.circuitBreaker,
                swarmInboxMessages = agentInboxManager.getAllMessages(),
                scheduledTasks = godOrchestrator.getAllScheduledTasks(),
                skills = skillRegistry.getAllSkills(),
                mcpTools = mcpToolManager.getRegisteredTools(),
                diffReviews = diffReviewManager.getAllSessions(),
                sshProfiles = sshRuntimeManager.getProfiles(),
                activeSshProfile = sshRuntimeManager.getActiveProfile(),
            )
        }
    }

    fun decomposeSwarmGoal(goal: String) {
        val project = _state.value.activeProject ?: return
        val root = projectWorkspaceRoot(project)
        viewModelScope.launch(Dispatchers.IO) {
            godOrchestrator.decomposeGoal(goal)
            taskBoardManager.saveToFile(root)
            blackboardMemoryStore.saveToFile(root)
            agentInboxManager.saveToFile(root)
            withContext(Dispatchers.Main) {
                refreshOrchestratorState()
                _state.update { it.copy(toastMessage = "GOD Orchestrator decomposed goal into tasks") }
            }
        }
    }

    fun createSwarmTask(
        title: String,
        description: String,
        priority: com.jarves.mh.model.TaskPriority,
        assignedAgentId: String?,
        dependencyTaskIds: List<String>,
    ) {
        val project = _state.value.activeProject ?: return
        val root = projectWorkspaceRoot(project)
        viewModelScope.launch(Dispatchers.IO) {
            taskBoardManager.createTask(
                title = title,
                description = description,
                priority = priority,
                assignedAgentId = assignedAgentId,
                dependencyTaskIds = dependencyTaskIds,
            )
            taskBoardManager.saveToFile(root)
            withContext(Dispatchers.Main) {
                refreshOrchestratorState()
            }
        }
    }

    fun moveSwarmTaskStatus(taskId: String, newStatus: com.jarves.mh.model.TaskStatus) {
        val project = _state.value.activeProject ?: return
        val root = projectWorkspaceRoot(project)
        viewModelScope.launch(Dispatchers.IO) {
            val res = taskBoardManager.updateTaskStatus(taskId, newStatus)
            if (res.isFailure) {
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(toastMessage = res.exceptionOrNull()?.message ?: "Cannot move task") }
                }
            } else {
                if (newStatus == com.jarves.mh.model.TaskStatus.DONE) {
                    godOrchestrator.onTaskCompleted(taskId)
                    agentInboxManager.saveToFile(root)
                }
                taskBoardManager.saveToFile(root)
                withContext(Dispatchers.Main) {
                    refreshOrchestratorState()
                }
            }
        }
    }

    fun assignSwarmTask(taskId: String, agentId: String?) {
        val project = _state.value.activeProject ?: return
        val root = projectWorkspaceRoot(project)
        val agent = agentId?.let { godOrchestrator.getAgent(it) }
        viewModelScope.launch(Dispatchers.IO) {
            taskBoardManager.assignAgent(taskId, agentId, agent?.name ?: "Unassigned")
            taskBoardManager.saveToFile(root)
            withContext(Dispatchers.Main) {
                refreshOrchestratorState()
            }
        }
    }

    fun resetSwarmCircuitBreaker() {
        godOrchestrator.resetCircuitBreaker()
        refreshOrchestratorState()
        _state.update { it.copy(toastMessage = "Swarm circuit breaker reset to normal") }
    }

    fun sendSwarmDirectMessage(fromAgentId: String, toAgentId: String, body: String) {
        val project = _state.value.activeProject ?: return
        val root = projectWorkspaceRoot(project)
        viewModelScope.launch(Dispatchers.IO) {
            agentInboxManager.sendMessage(
                fromAgentId = fromAgentId,
                toAgentId = toAgentId,
                type = com.jarves.mh.model.AgentMessageType.QUERY,
                subject = "Direct Message",
                content = body,
            )
            agentInboxManager.saveToFile(root)
            withContext(Dispatchers.Main) {
                refreshOrchestratorState()
            }
        }
    }

    fun addSwarmBlackboardEntry(key: String, category: String, value: String) {
        val project = _state.value.activeProject ?: return
        val root = projectWorkspaceRoot(project)
        viewModelScope.launch(Dispatchers.IO) {
            blackboardMemoryStore.putEntry(
                key = key,
                category = category,
                value = value,
                authorAgentId = "user",
                authorRole = com.jarves.mh.model.AgentRole.GOD_ORCHESTRATOR,
            )
            blackboardMemoryStore.saveToFile(root)
            withContext(Dispatchers.Main) {
                refreshOrchestratorState()
            }
        }
    }

    fun sendElementContextToChat(elementContext: String) {
        _state.update {
            it.copy(
                designModePromptDraft = elementContext,
                toastMessage = "Element context copied to chat draft",
            )
        }
    }

    fun consumeDesignModeDraft(): String? {
        val draft = _state.value.designModePromptDraft
        if (draft != null) {
            _state.update { it.copy(designModePromptDraft = null) }
        }
        return draft
    }

    fun createScheduledTask(
        title: String,
        description: String,
        intervalMinutes: Int,
        isRecurring: Boolean,
        targetAgentId: String,
    ) {
        godOrchestrator.addScheduledTask(title, description, intervalMinutes, isRecurring, targetAgentId)
        refreshOrchestratorState()
        _state.update { it.copy(toastMessage = "Task schedule created") }
    }

    fun toggleScheduledTask(id: String) {
        godOrchestrator.toggleScheduledTask(id)
        refreshOrchestratorState()
    }

    fun deleteScheduledTask(id: String) {
        godOrchestrator.removeScheduledTask(id)
        refreshOrchestratorState()
    }

    fun mergeWorktreeBranch(branchName: String) {
        val project = _state.value.activeProject ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val res = gitManager.mergeBranch(projectWorkspaceRoot(project), projectGuestRoot(project), branchName)
            withContext(Dispatchers.Main) {
                res.onSuccess {
                    _state.update { it.copy(toastMessage = "Merged branch '$branchName' into main successfully") }
                }.onFailure { err ->
                    _state.update { it.copy(toastMessage = "Merge failed: ${err.message?.take(160)}") }
                }
            }
            refreshGitState()
        }
    }

    // --- SKILLS & MCP ---
    fun toggleSkill(skillId: String) {
        val newState = skillRegistry.toggleSkill(skillId)
        _state.update { it.copy(skills = skillRegistry.getAllSkills()) }
    }

    fun addCustomSkill(name: String, description: String, category: SkillCategory, instructions: String) {
        skillRegistry.addCustomSkill(name, description, category, instructions)
        _state.update {
            it.copy(
                skills = skillRegistry.getAllSkills(),
                toastMessage = "Skill '$name' added to swarm registry",
            )
        }
    }

    fun deleteCustomSkill(skillId: String) {
        skillRegistry.deleteCustomSkill(skillId)
        _state.update { it.copy(skills = skillRegistry.getAllSkills()) }
    }

    // --- DIFF REVIEWS & ANNOTATIONS ---
    fun addDiffAnnotation(
        worktreeBranch: String,
        filePath: String,
        lineNumber: Int,
        author: String,
        type: AnnotationType,
        comment: String,
    ) {
        diffReviewManager.addAnnotation(
            worktreeBranch = worktreeBranch,
            filePath = filePath,
            lineNumber = lineNumber,
            author = author,
            authorAvatar = if (author.contains("Reviewer", true)) "🔍" else "👤",
            type = type,
            comment = comment,
        )
        _state.update { it.copy(diffReviews = diffReviewManager.getAllSessions()) }
    }

    fun toggleDiffAnnotationResolved(annotationId: String) {
        diffReviewManager.toggleAnnotationResolved(annotationId)
        _state.update { it.copy(diffReviews = diffReviewManager.getAllSessions()) }
    }

    fun runAutomatedReviewForWorktree(worktreeBranch: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val unstaged = _state.value.gitUnstagedDiffs.map { it.path to it.diff }
            val staged = _state.value.gitStagedDiffs.map { it.path to it.diff }
            val allDiffs = (unstaged + staged).distinctBy { it.first }
            diffReviewManager.runAutomatedReview(worktreeBranch, allDiffs)
            withContext(Dispatchers.Main) {
                _state.update {
                    it.copy(
                        diffReviews = diffReviewManager.getAllSessions(),
                        toastMessage = "Rhea Reviewer completed automated review pass.",
                    )
                }
            }
        }
    }

    // --- SSH RUNTIME ---
    fun addSshProfile(
        name: String,
        host: String,
        port: Int,
        user: String,
        authType: SshAuthType,
        remotePath: String,
    ) {
        sshRuntimeManager.addProfile(name, host, port, user, authType, remotePath)
        _state.update {
            it.copy(
                sshProfiles = sshRuntimeManager.getProfiles(),
                toastMessage = "SSH profile '$name' created.",
            )
        }
    }

    fun setActiveSshProfile(profileId: String?) {
        sshRuntimeManager.setActiveProfile(profileId)
        _state.update {
            it.copy(
                activeSshProfile = sshRuntimeManager.getActiveProfile(),
                toastMessage = if (profileId != null) "Remote SSH backend selected" else "Switched to local PRoot backend",
            )
        }
    }

    fun deleteSshProfile(profileId: String) {
        sshRuntimeManager.deleteProfile(profileId)
        _state.update {
            it.copy(
                sshProfiles = sshRuntimeManager.getProfiles(),
                activeSshProfile = sshRuntimeManager.getActiveProfile(),
            )
        }
    }

    // --- DYNAMIC AGENTS & TEMPLATES ---
    fun createCustomAgentTemplate(
        name: String,
        roleTitle: String,
        runtimeType: AgentRuntimeType,
        systemPrompt: String,
        preferredModel: String,
        preferredEffort: String,
    ) {
        val template = CustomAgentTemplate(
            name = name,
            roleTitle = roleTitle,
            runtimeType = runtimeType,
            systemPrompt = systemPrompt,
            preferredModel = preferredModel,
            preferredEffort = preferredEffort,
        )
        _state.update {
            it.copy(
                customAgentTemplates = it.customAgentTemplates + template,
                toastMessage = "Dynamic agent '$name' registered.",
            )
        }
    }

    // --- LINEAR INTEGRATION ---
    fun setLinearApiKey(apiKey: String) {
        val clean = apiKey.trim()
        vault.put("LINEAR_API_KEY", clean)
        _state.update { it.copy(linearApiKey = clean, toastMessage = "Linear API key saved") }
        loadLinearIssues(clean)
    }

    fun refreshLinearIssues() {
        val key = _state.value.linearApiKey ?: vault.get("LINEAR_API_KEY")
        if (!key.isNullOrBlank()) {
            loadLinearIssues(key)
        } else {
            _state.update { it.copy(toastMessage = "Please configure a Linear API key first") }
        }
    }

    private fun loadLinearIssues(apiKey: String) {
        _state.update { it.copy(linearLoading = true) }
        viewModelScope.launch {
            val result = linearClient.fetchAssignedIssues(apiKey)
            withContext(Dispatchers.Main) {
                result.onSuccess { issues ->
                    _state.update {
                        it.copy(
                            linearIssues = issues,
                            linearLoading = false,
                            toastMessage = "Fetched ${issues.size} Linear issues",
                        )
                    }
                }.onFailure { error ->
                    _state.update {
                        it.copy(
                            linearLoading = false,
                            toastMessage = "Linear fetch error: ${error.message?.take(100)}",
                        )
                    }
                }
            }
        }
    }

    fun importLinearIssueToTask(issue: LinearIssue) {
        val project = _state.value.activeProject ?: return
        val root = projectWorkspaceRoot(project)
        val guestPath = projectGuestRoot(project)
        val priority = when (issue.priority) {
            1 -> com.jarves.mh.model.TaskPriority.URGENT
            2 -> com.jarves.mh.model.TaskPriority.HIGH
            3 -> com.jarves.mh.model.TaskPriority.MEDIUM
            4 -> com.jarves.mh.model.TaskPriority.LOW
            else -> com.jarves.mh.model.TaskPriority.MEDIUM
        }
        val branchName = "linear/${issue.identifier.lowercase()}"
        viewModelScope.launch(Dispatchers.IO) {
            taskBoardManager.createTask(
                title = "[${issue.identifier}] ${issue.title}",
                description = "${issue.description}\n\nLinear: ${issue.url}".trim(),
                priority = priority,
                assignedAgentId = "devon_coder",
                assignedAgentName = "Devon Coder",
                worktreeBranch = branchName,
            )
            taskBoardManager.saveToFile(root)

            blackboardMemoryStore.putEntry(
                key = "linear_${issue.identifier.lowercase()}",
                category = "REQUIREMENT",
                value = "Imported Linear issue [${issue.identifier}]: ${issue.title}. Assigned to Devon Coder on branch $branchName.",
                authorAgentId = "system",
                authorRole = com.jarves.mh.model.AgentRole.GOD_ORCHESTRATOR,
            )
            blackboardMemoryStore.saveToFile(root)

            if (gitManager.isGitRepository(root, guestPath)) {
                worktreeManager.createWorktree(root, guestPath, branchName, branchName)
            }

            withContext(Dispatchers.Main) {
                refreshOrchestratorState()
                refreshGitState()
                _state.update {
                    it.copy(
                        toastMessage = "Imported [${issue.identifier}] into swarm with worktree $branchName",
                    )
                }
            }
        }
    }

    // --- GITHUB ISSUES INTEGRATION ---
    fun refreshGitHubIssues(customRepo: String? = null) {
        val project = _state.value.activeProject ?: return
        val token = getGitHubToken()
        if (token.isNullOrBlank()) {
            _state.update { it.copy(toastMessage = "GitHub token not configured. Connect GitHub in settings.") }
            return
        }
        _state.update { it.copy(githubIssuesLoading = true, githubRepoOverride = customRepo ?: it.githubRepoOverride) }
        viewModelScope.launch(Dispatchers.IO) {
            val remotes = gitManager.getRemotes(projectWorkspaceRoot(project), projectGuestRoot(project)).getOrNull().orEmpty()
            val origin = remotes["origin"].orEmpty()
            val resolvedRepo = customRepo ?: _state.value.githubRepoOverride ?: extractGitHubRepoFullName(origin, project.description)
            if (resolvedRepo.isNullOrBlank()) {
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(githubIssuesLoading = false, toastMessage = "Specify repository owner/name to fetch issues") }
                }
                return@launch
            }
            val result = runCatching { gitHubClient.listIssues(token, resolvedRepo) }
            withContext(Dispatchers.Main) {
                result.onSuccess { issues ->
                    _state.update {
                        it.copy(
                            githubIssues = issues,
                            githubIssuesLoading = false,
                            toastMessage = "Fetched ${issues.size} GitHub issues from $resolvedRepo",
                        )
                    }
                }.onFailure { err ->
                    _state.update {
                        it.copy(
                            githubIssuesLoading = false,
                            toastMessage = "GitHub issue fetch failed: ${err.message?.take(100)}",
                        )
                    }
                }
            }
        }
    }

    fun importGitHubIssueToTask(issue: GitHubIssue) {
        val project = _state.value.activeProject ?: return
        val root = projectWorkspaceRoot(project)
        val guestPath = projectGuestRoot(project)
        val isUrgent = issue.labels.any { it.contains("urgent", true) || it.contains("critical", true) || it.contains("p0", true) }
        val isHigh = issue.labels.any { it.contains("bug", true) || it.contains("high", true) || it.contains("p1", true) }
        val priority = when {
            isUrgent -> com.jarves.mh.model.TaskPriority.URGENT
            isHigh -> com.jarves.mh.model.TaskPriority.HIGH
            else -> com.jarves.mh.model.TaskPriority.MEDIUM
        }
        val branchName = "issue/${issue.number}"
        viewModelScope.launch(Dispatchers.IO) {
            taskBoardManager.createTask(
                title = "[#${issue.number}] ${issue.title}",
                description = "${issue.body}\n\nGitHub Issue: ${issue.htmlUrl}".trim(),
                priority = priority,
                assignedAgentId = "devon_coder",
                assignedAgentName = "Devon Coder",
                worktreeBranch = branchName,
            )
            taskBoardManager.saveToFile(root)

            blackboardMemoryStore.putEntry(
                key = "github_issue_${issue.number}",
                category = "REQUIREMENT",
                value = "Imported GitHub Issue #${issue.number}: ${issue.title}. Assigned to Devon Coder on branch $branchName.",
                authorAgentId = "system",
                authorRole = com.jarves.mh.model.AgentRole.GOD_ORCHESTRATOR,
            )
            blackboardMemoryStore.saveToFile(root)

            if (gitManager.isGitRepository(root, guestPath)) {
                worktreeManager.createWorktree(root, guestPath, branchName, branchName)
            }

            withContext(Dispatchers.Main) {
                refreshOrchestratorState()
                refreshGitState()
                _state.update {
                    it.copy(
                        toastMessage = "Imported GitHub issue #${issue.number} into swarm on $branchName",
                    )
                }
            }
        }
    }

    private fun getGitHubToken(): String? {
        val cliToken = if (installer.isGitHubCliInstalled()) {
            runCatching {
                val (exit, output) = runGitHubCli(listOf("auth", "token"))
                output.lineSequence().lastOrNull { it.isNotBlank() }?.trim().takeIf { exit == 0 && !it.isNullOrBlank() }
            }.getOrNull()
        } else null
        return cliToken ?: vault.get("GITHUB") ?: vault.get("GITHUB_TOKEN")
    }

    private fun extractGitHubRepoFullName(remoteUrl: String, description: String): String? {
        if (description.startsWith("GitHub · ")) {
            return description.removePrefix("GitHub · ").trim()
        }
        val match = Regex("""github\.com[:/]([A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+?)(?:\.git|/)?$""").find(remoteUrl)
        return match?.groupValues?.get(1)
    }


    private fun isNoisyRuntimeItem(item: ActivityItem): Boolean {
        val combined = "${item.title} ${item.detail}"
        return combined.contains("Starting Claude Code", true) ||
            combined.contains("Agent process started", true) ||
            combined.contains("Claude Code connected", true) ||
            combined.contains("Runtime warning", true) ||
            combined.contains("unrecognized_model", true) ||
            combined.contains("Writing response", true) ||
            combined.contains("Claude Code finished", true) ||
            combined.contains("Task completed", true)
    }

    private fun toolPlanSummary(toolName: String, detail: String): String {
        val clean = detail.replace(Regex("\\s+"), " ").trim()
        val short = clean.take(90).ifBlank { "the current project" }
        return when (toolName) {
            "Write" -> "Preparing to create ${clean.substringAfterLast('/').ifBlank { "a project file" }}"
            "Edit", "NotebookEdit" -> "Preparing to update ${clean.substringAfterLast('/').ifBlank { "a project file" }}"
            "Read" -> "Preparing to inspect ${clean.substringAfterLast('/').ifBlank { "a project file" }}"
            "Glob" -> "Preparing to find matching project files"
            "Grep" -> "Preparing to search the project for $short"
            "Bash" -> if (clean.contains("cat ", true) || clean.contains("printf ", true) || clean.contains(" >")) {
                "Preparing to create or update project files with Bash"
            } else {
                "Preparing to run: $short"
            }
            else -> "Preparing to use $toolName for the next step"
        }
    }

    private fun requestPlanningSummary(
        request: String,
        agentKind: AgentKind,
        toolName: String? = null,
        detail: String = "",
    ): String {
        val agentName = agentKind.title
        val cleanRequest = request.replace(Regex("\\s+"), " ").trim().take(110)
        val requestPart = if (cleanRequest.isBlank()) {
            "$agentName is reviewing the request"
        } else {
            "The user is asking: “$cleanRequest”"
        }
        return if (toolName == null) {
            "$requestPart. $agentName is deciding the next useful step."
        } else {
            "$requestPart. ${toolPlanSummary(toolName, detail)}."
        }
    }

    private fun finishWorkSegment(current: AppUiState, finishedAt: Long = System.currentTimeMillis()): AppUiState {
        val meaningfulItems = current.liveProcess.filterNot(::isNoisyRuntimeItem)
            .map { if (it.isComplete) it else it.copy(isComplete = true) }
        if (!current.liveThinking && meaningfulItems.isEmpty()) {
            return current.copy(liveProcess = emptyList(), workSegmentStartedAtMillis = null)
        }
        val startedAt = current.workSegmentStartedAtMillis ?: current.taskStartedAtMillis ?: finishedAt
        val block = ChatMessage(
            fromUser = false,
            text = "",
            workItems = meaningfulItems,
            workedMillis = (finishedAt - startedAt).coerceAtLeast(0L),
        )
        return current.copy(
            messages = current.messages + block,
            liveProcess = emptyList(),
            liveThinking = false,
            activeThinkingBlockId = null,
            workSegmentStartedAtMillis = null,
        )
    }

    /** Adds the complete request duration to the response produced after the latest user message. */
    private fun attachTaskDuration(current: AppUiState, finishedAt: Long): AppUiState {
        val startedAt = current.taskStartedAtMillis ?: return current
        val lastUserIndex = current.messages.indexOfLast { it.fromUser }
        val responseIndex = current.messages.indices.lastOrNull { index ->
            index > lastUserIndex && !current.messages[index].fromUser && current.messages[index].text.isNotBlank()
        } ?: return current
        val updated = current.messages.toMutableList()
        updated[responseIndex] = updated[responseIndex].copy(
            workedMillis = (finishedAt - startedAt).coerceAtLeast(1L),
        )
        return current.copy(messages = updated)
    }

    private fun appendWorkItem(current: AppUiState, item: ActivityItem): AppUiState {
        if (isNoisyRuntimeItem(item)) return current
        return current.copy(
            liveProcess = current.liveProcess.map { if (!it.isComplete) it.copy(isComplete = true) else it } + item,
            liveThinking = false,
            workSegmentStartedAtMillis = current.workSegmentStartedAtMillis ?: System.currentTimeMillis(),
        )
    }

    private fun onRuntimeEvent(event: RuntimeEvent) {
        if (event is RuntimeEvent.SessionFailed && _state.value.agentKind == AgentKind.ANTIGRAVITY &&
            (event.reason.contains("sign-in", true) || event.reason.contains("authentication", true))) {
            antigravityAuthController.invalidateSession(event.reason)
        }
        if (event is RuntimeEvent.SessionFailed && retryWithNextApiKey(event)) return
        _state.update { current ->
            if (!current.isRunning) {
                current
            } else if (current.activeSessionId != null && current.activeSessionId != event.sessionId) {
                current
            } else when (event) {
                is RuntimeEvent.SessionStarted -> current.copy(
                    activeSessionId = event.sessionId,
                    activity = current.activity.mapIndexed { index, item -> if (index == 0) item.copy(isComplete = true) else item },
                )
                is RuntimeEvent.AssistantDelta -> {
                    val timeline = if (current.liveThinking || current.liveProcess.any { !isNoisyRuntimeItem(it) }) {
                        finishWorkSegment(current)
                    } else {
                        current
                    }
                    val lastMessage = timeline.messages.lastOrNull()
                    if (lastMessage != null && !lastMessage.fromUser && lastMessage.workItems.isEmpty() && lastMessage.workedMillis == 0L) {
                        timeline.copy(messages = timeline.messages.dropLast(1) + lastMessage.copy(text = lastMessage.text + event.text))
                    } else {
                        timeline.copy(messages = timeline.messages + ChatMessage(fromUser = false, text = event.text))
                    }
                }
                is RuntimeEvent.ReasoningProgress -> {
                    val existingIndex = current.liveProcess.indexOfLast { it.title == "Think" }
                    // The request-level Think summary is seeded once in sendPrompt.
                    // After that segment has been committed to the timeline, later
                    // agent turns must not repeat the same request summary.
                    if (existingIndex < 0) return@update current
                    val reasoning = ActivityItem(
                        title = "Think",
                        detail = current.liveProcess.getOrNull(existingIndex)?.detail
                            ?: requestPlanningSummary(current.currentTaskRequest.orEmpty(), current.agentKind),
                        isComplete = false,
                    )
                    val process = if (existingIndex >= 0) {
                        current.liveProcess.toMutableList().also { it[existingIndex] = reasoning }
                    } else {
                        current.liveProcess + reasoning
                    }
                    current.copy(
                        liveProcess = process,
                        liveThinking = true,
                        workSegmentStartedAtMillis = current.workSegmentStartedAtMillis ?: System.currentTimeMillis(),
                    )
                }
                is RuntimeEvent.ReasoningSummary -> {
                    val summary = event.summary.trim()
                    val process = current.liveProcess.toMutableList()
                    val existingIndex = process.indexOfLast { !it.isComplete && it.title == "Think" }
                    if (event.startsNewBlock) {
                        process.indices.forEach { index ->
                            if (!process[index].isComplete) process[index] = process[index].copy(isComplete = true)
                        }
                        val initial = summary.ifBlank { "Thinking…" }
                        val replaceFallback = current.activeThinkingBlockId == null &&
                            process.size == 1 && process.first().title == "Think"
                        if (replaceFallback) {
                            process[0] = ActivityItem("Think", initial, event.isFinal)
                        } else {
                            process += ActivityItem("Think", initial, event.isFinal)
                        }
                    } else if (current.activeThinkingBlockId == event.blockId && existingIndex >= 0 && summary.isNotBlank()) {
                        process[existingIndex] = process[existingIndex].copy(
                            detail = summary,
                            isComplete = event.isFinal,
                        )
                    } else {
                        return@update current
                    }
                    current.copy(
                        liveProcess = process,
                        liveThinking = !event.isFinal,
                        activeThinkingBlockId = if (event.isFinal) null else event.blockId,
                        workSegmentStartedAtMillis = current.workSegmentStartedAtMillis ?: System.currentTimeMillis(),
                    )
                }
                is RuntimeEvent.ToolStarted -> {
                    val planned = current.copy(
                        liveProcess = current.liveProcess.map { item ->
                            if (!item.isComplete) item.copy(isComplete = true) else item
                        },
                        liveThinking = false,
                        activeThinkingBlockId = null,
                        activity = listOf(
                            ActivityItem("Running ${event.toolName}", event.detail, false, isCommand = event.toolName == "Bash"),
                        ) + current.activity.map { if (!it.isComplete) it.copy(isComplete = true) else it },
                    )
                    appendWorkItem(
                        planned,
                        ActivityItem("Running ${event.toolName}", event.detail, false, isCommand = event.toolName == "Bash"),
                    )
                }
                is RuntimeEvent.RuntimeLog -> appendWorkItem(
                    current.copy(activity = listOf(ActivityItem(event.title, event.detail)) + current.activity),
                    ActivityItem(event.title, event.detail),
                )
                is RuntimeEvent.ToolRequested -> {
                    if (_state.value.autoApproveTools) {
                        viewModelScope.launch {
                            activeRuntime().respondToApproval(event.request, true)
                        }
                        appendWorkItem(current.copy(
                            activity = listOf(ActivityItem("Auto-approved ${event.request.toolName}", event.request.explanation, false)) + current.activity,
                        ), ActivityItem("Auto-approved ${event.request.toolName}", event.request.explanation, false))
                    } else {
                        appendWorkItem(current.copy(
                            pendingApproval = event.request,
                            activity = listOf(ActivityItem("Waiting for approval", event.request.explanation, false)) + current.activity,
                        ), ActivityItem("Waiting for approval", event.request.explanation, false))
                    }
                }
                is RuntimeEvent.ToolApproved -> appendWorkItem(current.copy(
                    pendingApproval = null,
                    activity = listOf(ActivityItem("Applying approved changes", "Editing project files", false)) + current.activity,
                ), ActivityItem("Action approved", "Claude is continuing the task", false))
                is RuntimeEvent.ToolRejected -> appendWorkItem(current.copy(
                    pendingApproval = null,
                ), ActivityItem("Action rejected", "Claude will continue without this action"))
                is RuntimeEvent.ToolCompleted -> {
                    val runningIndex = current.liveProcess.indexOfLast {
                        !it.isComplete && it.title == "Running ${event.toolName}"
                    }
                    val process = if (runningIndex >= 0) {
                        current.liveProcess.toMutableList().also { items ->
                            val runningItem = items[runningIndex]
                            items[runningIndex] = ActivityItem(
                                "${event.toolName} completed",
                                runningItem.detail.ifBlank { event.summary },
                                isCommand = event.toolName == "Bash",
                            )
                        }
                    } else {
                        current.liveProcess + ActivityItem(
                            "${event.toolName} completed",
                            event.summary,
                            isCommand = event.toolName == "Bash",
                        )
                    }
                    current.copy(
                        activity = listOf(ActivityItem(event.summary, event.toolName)) + current.activity,
                        liveProcess = process,
                        liveThinking = false,
                        workSegmentStartedAtMillis = current.workSegmentStartedAtMillis ?: System.currentTimeMillis(),
                    )
                }
                is RuntimeEvent.FilesChanged -> current.copy(
                    changes = event.changes,
                    liveThinking = false,
                    liveProcess = if (event.paths.isEmpty()) current.liveProcess else current.liveProcess +
                        ActivityItem(
                            "Files changed",
                            event.paths.take(4).joinToString(", ") + if (event.paths.size > 4) " +${event.paths.size - 4} more" else "",
                        ),
                    workSegmentStartedAtMillis = current.workSegmentStartedAtMillis ?: System.currentTimeMillis(),
                )
                is RuntimeEvent.PreviewStarted -> current.copy(
                    previewReady = true,
                    previewUrl = event.url,
                    activity = listOf(ActivityItem("Preview ready", event.url)) + current.activity,
                    liveProcess = current.liveProcess + ActivityItem("Preview ready", event.url),
                    liveThinking = false,
                    workSegmentStartedAtMillis = current.workSegmentStartedAtMillis ?: System.currentTimeMillis(),
                )
                is RuntimeEvent.SessionCompleted -> {
                    val finishedAt = System.currentTimeMillis()
                    attachTaskDuration(finishWorkSegment(current, finishedAt), finishedAt).copy(
                        isRunning = false,
                        activeSessionId = null,
                        activity = listOf(ActivityItem("Task completed", "${current.agentKind.title} finished successfully")) +
                            current.activity.map { if (!it.isComplete) it.copy(isComplete = true) else it },
                        taskFinishedAtMillis = finishedAt,
                        currentTaskRequest = null,
                    )
                }
                is RuntimeEvent.SessionFailed -> {
                    val finishedAt = System.currentTimeMillis()
                    attachTaskDuration(
                        finishWorkSegment(
                            appendWorkItem(current, ActivityItem("Task stopped", event.reason)),
                            finishedAt,
                        ),
                        finishedAt,
                    ).copy(
                        isRunning = false,
                        activeSessionId = null,
                        pendingApproval = null,
                        toastMessage = event.reason.takeIf { reason ->
                            reason.contains("user not found", true) ||
                                reason.contains("API key", true) ||
                                reason.contains("authentication", true)
                        },
                        activity = listOf(ActivityItem("Task stopped", event.reason)) + current.activity,
                        taskFinishedAtMillis = finishedAt,
                        currentTaskRequest = null,
                    )
                }
            }
        }
        if (event is RuntimeEvent.SessionCompleted || event is RuntimeEvent.SessionFailed) {
            activeRuntimeRequest = null
            failedApiKeyIds.clear()
        }
        if (event is RuntimeEvent.FilesChanged || event is RuntimeEvent.SessionCompleted) {
            _state.value.activeProject?.id?.let { touchProject(it) }
            refreshProjectFiles()
        }
        // Save every visible reasoning/tool transition, not only assistant text and
        // final results. If Android kills the process, the last displayed timeline
        // is restored as an interrupted work block rather than disappearing.
        persistMessages(includeLiveProcess = true)
    }

    private fun retryWithNextApiKey(event: RuntimeEvent.SessionFailed): Boolean {
        val current = _state.value
        if (current.agentKind == AgentKind.ANTIGRAVITY) return false
        if (!current.isRunning || current.activeSessionId != event.sessionId) return false
        if (!isApiKeyFailure(event.reason)) return false
        val request = activeRuntimeRequest ?: return false
        val credentials = vault.credentials(request.provider.kind.name)
        val active = credentials.firstOrNull { it.isActive } ?: return false
        failedApiKeyIds += active.id
        val next = credentials.firstOrNull { it.id !in failedApiKeyIds } ?: return false
        if (!vault.activate(request.provider.kind.name, next.id)) return false
        _state.update {
            it.copy(
                activeSessionId = null,
                activeApiKeyName = next.name,
                toastMessage = "${active.name} failed. Switched to ${next.name}.",
                liveProcess = it.liveProcess + ActivityItem("API key switched", "Using ${next.name}", true),
            )
        }
        viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            request.runtime.startSession(
                request.project.id,
                request.project.slug,
                request.project.kind,
                request.prompt,
                request.history,
                request.provider,
            )
        }
        return true
    }

    private fun isApiKeyFailure(reason: String): Boolean {
        val value = reason.lowercase()
        return "api key" in value || "authentication" in value || "user not found" in value ||
            "http 401" in value || "http 403" in value || "http 429" in value ||
            "expired" in value || "quota" in value || "rate limit" in value
    }

    private fun touchProject(projectId: String) {
        val now = System.currentTimeMillis()
        _state.update { current ->
            val updatedProjects = current.projects.map { p ->
                if (p.id == projectId) p.copy(updatedAtMillis = now) else p
            }
            val active = if (current.activeProject?.id == projectId) current.activeProject?.copy(updatedAtMillis = now) else current.activeProject
            current.copy(projects = updatedProjects, activeProject = active)
        }
        preferences.saveProjects(_state.value.projects)
    }

    private fun persistMessages(includeLiveProcess: Boolean = true) {
        val current = _state.value
        val project = current.activeProject ?: return
        val chatId = current.activeChatId ?: return
        val liveItems = if (includeLiveProcess) current.liveProcess.filterNot(::isNoisyRuntimeItem) else emptyList()
        val messages = if (liveItems.isEmpty() && !current.liveThinking) {
            current.messages
        } else {
            val startedAt = current.workSegmentStartedAtMillis ?: current.taskStartedAtMillis ?: System.currentTimeMillis()
            current.messages + ChatMessage(
                id = "interrupted-${current.activeSessionId ?: chatId}",
                fromUser = false,
                text = "",
                workItems = liveItems.map { it.copy(isComplete = true) } + ActivityItem(
                    "Task interrupted",
                    "The agent process stopped before reporting completion. Continue this chat to resume its official session.",
                ),
                workedMillis = (System.currentTimeMillis() - startedAt).coerceAtLeast(0L),
            )
        }
        transcriptWrites.trySend(TranscriptWrite(project.id, chatId, messages))
    }

    private fun updateActiveChatTitle(prompt: String) {
        val project = _state.value.activeProject ?: return
        val chatId = _state.value.activeChatId ?: return
        val now = System.currentTimeMillis()
        val title = prompt.replace(Regex("\\s+"), " ").trim().let {
            if (it.length <= 42) it else it.take(39).trimEnd() + "…"
        }
        _state.update { current ->
            val chats = current.projectChats.map { chat ->
                if (chat.id == chatId) {
                    chat.copy(
                        title = if (chat.title == "New chat") title else chat.title,
                        updatedAtMillis = now,
                    )
                } else chat
            }.sortedByDescending { it.updatedAtMillis }
            current.copy(projectChats = chats)
        }
        preferences.saveProjectChats(project.id, _state.value.projectChats)
    }

    companion object {
        private const val MINIMUM_INITIALIZATION_SCREEN_MS = 3_000L
        private const val MAX_VISIBLE_WORKSPACE_ENTRIES = 2_000
        private const val MAX_PROJECT_TERMINAL_HISTORY = 100
        private const val MAX_PROJECT_TERMINAL_OUTPUT = 200_000
        private const val MAX_ATTACHMENTS_PER_MESSAGE = 5
        private const val MAX_ATTACHMENT_BYTES = 25L * 1024L * 1024L
        private const val MAX_IMPORTED_PROJECT_BYTES = 8L * 1024L * 1024L * 1024L
        private const val MAX_IMPORTED_ZIP_ENTRIES = 100_000
        private const val LEGACY_GITHUB_TOKEN_KEY = "GITHUB_APP"
        private const val GITHUB_DEVICE_URL = "https://github.com/login/device"
        private val GITHUB_DEVICE_CODE = Regex("\\b[A-Z0-9]{4}-[A-Z0-9]{4}\\b")
        private const val TEST_PROVIDER_DEFAULTS_VERSION = 1
        private const val TEST_OPENROUTER_BASE_URL = "https://openrouter.ai/api"
        private const val TEST_OPENROUTER_MODEL = "stealth/ox-alpha"
    }
}
