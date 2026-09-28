package com.hivebench.app.ui

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
import com.hivebench.app.BuildConfig
import com.hivebench.app.data.ApiKeyVault
import com.hivebench.app.data.ApiKeyInfo
import com.hivebench.app.data.AppPreferences
import com.hivebench.app.model.ActivityItem
import com.hivebench.app.model.AgentKind
import com.hivebench.app.model.ChangeItem
import com.hivebench.app.model.ChatMessage
import com.hivebench.app.model.ChatAttachment
import com.hivebench.app.model.DevStack
import com.hivebench.app.model.Project
import com.hivebench.app.model.ProjectKind
import com.hivebench.app.model.ProjectChat
import com.hivebench.app.model.ProviderKind
import com.hivebench.app.model.ProviderProfile
import com.hivebench.app.model.RuntimeEvent
import com.hivebench.app.model.ToolRequest
import com.hivebench.app.model.WorkspaceEntry
import com.hivebench.app.model.projectSlug
import com.hivebench.app.model.generateQuickChatIdentity
import com.hivebench.app.model.providerProtocolForAgent
import com.hivebench.app.network.ConnectionValidation
import com.hivebench.app.network.ModelDiscoveryResult
import com.hivebench.app.network.ProviderApiClient
import com.hivebench.app.network.GitHubRepository
import com.hivebench.app.network.GitHubIssue
import com.hivebench.app.network.LinearIssue
import com.hivebench.app.network.LinearClient
import com.hivebench.app.runtime.AgentUpdateInfo
import com.hivebench.app.runtime.NativeSpawnProcess
import com.hivebench.app.runtime.RuntimeInstallProgress
import com.hivebench.app.runtime.RuntimeInstaller
import com.hivebench.app.runtime.RuntimeSetupController
import com.hivebench.app.runtime.RuntimeSetupService
import com.hivebench.app.runtime.RuntimeSetupSnapshot
import com.hivebench.app.runtime.RuntimeSetupStatus
import com.hivebench.app.runtime.supportsArm64Runtime
import com.hivebench.app.runtime.AndroidAppInstaller
import com.hivebench.app.update.AppUpdateInfo
import com.hivebench.app.update.AppUpdater
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
    val startupError: String? = null,
    val startupErrorIsOffline: Boolean = false,
    val showDetailedSetupProgress: Boolean = false,
    val onboardingComplete: Boolean = false,
    val backgroundSetupComplete: Boolean = false,
    val themeMode: com.hivebench.app.ui.theme.AppThemeMode = com.hivebench.app.ui.theme.AppThemeMode.DARK,
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
    val changes: List<ChangeItem> = emptyList(),
    val activity: List<ActivityItem> = emptyList(),
    val previewReady: Boolean = false,
    val previewUrl: String? = null,
    val isRunning: Boolean = false,
    val activeSessionId: String? = null,
    val toastMessage: String? = null,
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
    val antigravityModel: String = "",
    val antigravityEffort: String = "high",
    val discoveredModels: Map<ProviderKind, List<com.hivebench.app.network.DiscoveredModel>> = emptyMap(),
    val agentAuth: Map<AgentKind, com.hivebench.app.runtime.AgentAuthState> = emptyMap(),
    val androidBuildRunning: Boolean = false,
    val androidBuildMessage: String? = null,
    val appUpdate: AppUpdateInfo? = null,
    val appUpdateStatus: AppUpdateStatus? = null,
    val appUpdateDownloadedBytes: Long = 0L,
    val appUpdateTotalBytes: Long = -1L,
    val appUpdateError: String? = null,
    val gitStatus: com.hivebench.app.model.GitStatus? = null,
    val gitBranches: List<com.hivebench.app.model.GitBranch> = emptyList(),
    val gitWorktrees: List<com.hivebench.app.model.GitWorktree> = emptyList(),
    val gitCommits: List<com.hivebench.app.model.GitCommit> = emptyList(),
    val gitStagedDiffs: List<com.hivebench.app.model.GitFileDiff> = emptyList(),
    val gitUnstagedDiffs: List<com.hivebench.app.model.GitFileDiff> = emptyList(),
    val selectedGitCommit: com.hivebench.app.model.GitCommit? = null,
    val selectedGitCommitDiffs: List<com.hivebench.app.model.GitFileDiff> = emptyList(),
    val gitPullRequests: List<com.hivebench.app.model.GitPullRequest> = emptyList(),
    val gitOperationRunning: Boolean = false,
    val gitOperationMessage: String? = null,
    /** The open project has no Git repository yet (the Git tab offers to create one). */
    val gitRepositoryMissing: Boolean = false,
    val linearApiKey: String? = null,
    val linearIssues: List<LinearIssue> = emptyList(),
    val linearLoading: Boolean = false,
    val githubIssues: List<GitHubIssue> = emptyList(),
    val githubIssuesLoading: Boolean = false,
    val githubRepoOverride: String? = null,
    val autoApproveTools: Boolean = false,
    val customRunnerCommand: String = "",
    /** Parallel agent TUI sessions of the active project (see [com.hivebench.app.session.AgentSessionStore]). */
    val agentSessions: List<com.hivebench.app.session.AgentSessionEntry> = emptyList(),
    val activeAgentSessionId: String? = null,
    /** Live multi-agent office (hive) of the active project. */
    val hive: com.hivebench.app.hive.HiveSnapshot = com.hivebench.app.hive.HiveSnapshot(),
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val vault = ApiKeyVault(application)
    private val preferences = AppPreferences(application)
    val linearClient = LinearClient()
    private val installer = RuntimeInstaller(application)
    private val gitRunner = com.hivebench.app.git.GitCommandRunner(application, installer)
    val gitManager = com.hivebench.app.git.GitManager(gitRunner)
    val worktreeManager = com.hivebench.app.git.WorktreeManager(gitRunner)
    private val gitHubClient = com.hivebench.app.network.GitHubClient()
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
    private val initialAgentKind = AgentKind.fromStored(preferences.agentKind)
    private val initialPrimaryAgentKind = preferences.primaryAgentKind
        .takeIf(String::isNotBlank)
        ?.let(AgentKind::fromStored)
        ?: initialAgentKind
    private val _state = MutableStateFlow(
        AppUiState(
            onboardingComplete = preferences.onboardingComplete,
            backgroundSetupComplete = preferences.backgroundSetupComplete,
            agentKind = initialAgentKind,
            primaryAgentKind = initialPrimaryAgentKind,
            antigravityModel = preferences.antigravityModel,
            antigravityEffort = preferences.antigravityEffort,
            themeMode = runCatching { com.hivebench.app.ui.theme.AppThemeMode.valueOf(preferences.themeMode.uppercase()) }
                .getOrDefault(com.hivebench.app.ui.theme.AppThemeMode.DARK),
            projects = preferences.loadProjects(),
            githubAuthStatus = GitHubAuthStatus.DISCONNECTED,
            githubLogin = preferences.githubLogin.takeIf(String::isNotBlank),
            selectedDevStacks = preferences.selectedDevStacks.mapNotNull { name ->
                runCatching { DevStack.valueOf(name) }.getOrNull()
            }.toSet() + DevStack.WEB,
            autoApproveTools = preferences.autoApproveTools,
            customRunnerCommand = preferences.customRunnerCommand,
            discoveredModels = preferences.loadDiscoveredModels(),
),
)

    init {
        // GitHub's official CLI owns its OAuth credential. Remove credentials from
        // the retired custom OAuth implementation and discover the real CLI status.
        refreshAgentAuth()
        watchPreviewServer()
        // Keystore decryption is slow on first use; keep it off the main thread at cold start.
        viewModelScope.launch(Dispatchers.IO) {
            vault.remove(LEGACY_GITHUB_TOKEN_KEY)
            if (!preferences.legacySeededCredentialRemoved) {
                vault.remove(ProviderKind.CUSTOM.name)
                preferences.legacySeededCredentialRemoved = true
            }
            val storedLinearKey = vault.get("LINEAR_API_KEY")
            if (!storedLinearKey.isNullOrBlank()) {
                _state.update { it.copy(linearApiKey = storedLinearKey) }
                loadLinearIssues(storedLinearKey)
            }
        }
        viewModelScope.launch { refreshGitHubConnection() }
        RuntimeSetupController.restore(application)

        // Dropping empty quick projects reads every chat and scans workspaces (node_modules can
        // hold tens of thousands of files), so it must never run on the main thread.
        viewModelScope.launch(Dispatchers.IO) {
        val loadedProjects = preferences.loadProjects()
        val cleanedProjects = loadedProjects.filter { project ->
            if (project.kind == ProjectKind.QUICK_PROJECT) {
                val chats = preferences.loadProjectChats(project.id)
                val userMessages = chats.sumOf { preferences.loadMessages(project.id, it.id).count { m -> m.fromUser } }
                val workspaceDir = File(application.filesDir, "workspaces/${project.id}")
                val hasUserFiles = workspaceDir.isDirectory && workspaceDir.walkTopDown().any { file ->
                    file.isFile && !file.name.startsWith(".claude") && file.name != ".pocket-dev-stacks.json" &&
                        !file.path.contains("/.hive/")
                }
                val keep = userMessages > 0 || hasUserFiles
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
            _state.update { current -> current.copy(projects = current.projects.filter { p -> cleanedProjects.any { it.id == p.id } }) }
        }
        }
    }

    val state: StateFlow<AppUiState> = _state.asStateFlow()


    private fun shellQuote(value: String): String = "'${value.replace("'", "'\\''")}'"

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
                it.copy(toastMessage = "Android build tools are not installed. Add Android in Settings → Developer tools.")
            }
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
        val next = if (_state.value.themeMode == com.hivebench.app.ui.theme.AppThemeMode.DARK) {
            com.hivebench.app.ui.theme.AppThemeMode.LIGHT
        } else {
            com.hivebench.app.ui.theme.AppThemeMode.DARK
        }
        setThemeMode(next)
    }

    fun setThemeMode(mode: com.hivebench.app.ui.theme.AppThemeMode) {
        preferences.themeMode = mode.name.lowercase()
        _state.update { it.copy(themeMode = mode) }
    }

    init {
        viewModelScope.launch { RuntimeSetupController.snapshot.collect(::onSetupSnapshot) }
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
        val (installed, stacks, versions) = withContext(Dispatchers.IO) {
            // Upgrades from the old single-bundle layout keep every already-installed tool.
            installer.migrateLegacyToolMarkers()
            val ready = installer.isInstalled()
            if (ready) installer.cleanupLegacyWorkspaceScaffolding()
            Triple(ready, if (ready) installer.installedStacks() else null, if (ready) installer.installedAgentVersions() else emptyMap())
        }
        _state.update { current ->
            current.copy(
                installedDevStacks = stacks ?: current.installedDevStacks,
                installedAgentVersions = versions,
)
        }
        when {
            !installed && setupSnapshot.status == RuntimeSetupStatus.ERROR -> onSetupSnapshot(setupSnapshot)
            !installed -> _state.update { it.copy(startupStage = StartupStage.SETUP_REQUIRED, startupProgress = 0f) }
            // Agents sign in from the Agents tab (their own login or an API key),
            // so first run no longer stops at a provider picker.
            !preferences.onboardingComplete -> {
                preferences.runtimeSetupComplete = true
                completeOnboardingWithoutProvider()
                initializeRuntime()
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
                    startupError = null,
                    startupErrorIsOffline = false,
)
            }
            RuntimeSetupStatus.COMPLETE -> {
                if (setupCompletionHandled) return
                setupCompletionHandled = true
                preferences.runtimeSetupComplete = true
                if (!preferences.onboardingComplete) completeOnboardingWithoutProvider()
                viewModelScope.launch { initializeRuntime() }
            }
            RuntimeSetupStatus.ERROR -> _state.update {
                it.copy(
                    startupStage = StartupStage.ERROR,
                    startupMessage = snapshot.message,
                    startupProgress = snapshot.progress,
                    startupLogs = snapshot.logs,
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
            val versions = withContext(Dispatchers.IO) { installer.installedAgentVersions() }
            _state.update {
                it.copy(
                    startupStage = StartupStage.READY,
                    startupProgress = 1f,
                    installedAgentVersions = versions,
)
            }
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
            } else if (force) {
                val message = if (BuildConfig.APP_UPDATE_MANIFEST_URL.isBlank() && preferences.debugUpdateManifestUrl.isBlank()) {
                    "Update checks aren't set up for this build yet"
                } else "Hivebench is up to date"
                _state.update { it.copy(toastMessage = message) }
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
            error.message?.take(300) ?: "Something went wrong while preparing Hivebench. Please try again."
        }
        _state.update {
            it.copy(
                startupStage = StartupStage.ERROR,
                startupError = message,
                startupErrorIsOffline = isOffline,
)
        }
    }

    private fun completeOnboardingWithoutProvider() {
        preferences.onboardingComplete = true
        _state.update { it.copy(onboardingComplete = true) }
    }

    /** Lets first-run users escape a provider/login failure without losing saved credentials. */
    fun chooseOnboardingAgent(kind: AgentKind) {
        selectAgent(kind)
        if (installer.isAgentInstalled(kind)) {
            completeOnboardingWithoutProvider()
            viewModelScope.launch { initializeRuntime() }
        } else {
            _state.update { it.copy(startupStage = StartupStage.SETUP_REQUIRED, startupError = null, startupErrorIsOffline = false) }
        }
    }

    fun finishBackgroundSetup() {
        preferences.backgroundSetupComplete = true
        _state.update { it.copy(backgroundSetupComplete = true) }
    }

    /** Called from the first-launch setup screen; persists the agent choice for setup and Settings. */
    fun selectAgent(kind: AgentKind) {
        if (_state.value.agentKind == kind) return
        val selectingInitialAgent = !preferences.runtimeSetupComplete
        preferences.agentKind = kind.stableId
        if (selectingInitialAgent) preferences.primaryAgentKind = kind.stableId
        _state.update { current ->
            current.copy(
                agentKind = kind,
                primaryAgentKind = if (selectingInitialAgent) kind else current.primaryAgentKind,
            )
        }
    }

    /** Installs the other agent on demand (Settings) with live progress, then switches to it. */
    fun installAgent(kind: AgentKind) {
        if (_state.value.agentInstalling != null) return
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
                    installer.ensureAgentInstalled(kind) { progress ->
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
                refreshAgentAuth()
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
        if (_state.value.agentUpdatesChecking || _state.value.agentUpdating != null) return
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

    /**
     * Keeps previewUrl/previewReady in sync with whatever dev server is actually
     * listening for the open project, whether a terminal, an agent session or a
     * hive agent started it; clears it again when the server stops.
     */
    private fun watchPreviewServer() {
        viewModelScope.launch {
            var projectId: String? = null
            while (true) {
                delay(3_000L)
                val project = _state.value.activeProject
                if (project?.id != projectId) {
                    projectId = project?.id
                    com.hivebench.app.browser.PreviewServerWatcher.reset()
                }
                if (project == null) continue
                val url = withContext(Dispatchers.IO) { com.hivebench.app.browser.PreviewServerWatcher.detect() }
                _state.update { current ->
                    if (current.activeProject?.id != project.id) current
                    else if (url == current.previewUrl && (url != null) == current.previewReady) current
                    else current.copy(previewUrl = url ?: current.previewUrl, previewReady = url != null)
                }
            }
        }
    }

    /** Where files attached from a terminal key row go for the open project (null without a project). */
    fun terminalAttachmentTarget(): com.hivebench.app.terminal.TerminalAttachmentTarget? {
        val project = _state.value.activeProject ?: return null
        return com.hivebench.app.terminal.TerminalAttachmentTarget(
            hostDir = File(projectWorkspaceRoot(project), ".pocketdev/attachments"),
            guestDir = "${projectGuestRoot(project)}/.pocketdev/attachments",
        )
    }

    /** A login bash shell in a PTY: in the project folder, or in a scratch workspace for the home Terminal. */
    suspend fun shellLaunch(project: Project?): RuntimeInstaller.GuestLaunch = withContext(Dispatchers.IO) {
        val runtime = installer.installedRuntime()
        val environment = mapOf("TERM" to "xterm-256color", "COLORTERM" to "truecolor")
        val command = listOf("/usr/bin/bash", "-l")
        if (project != null) {
            installer.guestLaunch(runtime.proot, runtime.rootfs, projectWorkspaceRoot(project), environment, command, projectGuestRoot(project))
        } else {
            val workspace = File(getApplication<Application>().filesDir, "workspaces/terminal").apply { mkdirs() }
            installer.guestLaunch(runtime.proot, runtime.rootfs, workspace, environment, command)
        }
    }

    /** Re-reads which agents are signed in (credential files) or have API keys saved. */
    fun refreshAgentAuth() {
        viewModelScope.launch {
            val auth = withContext(Dispatchers.IO) {
                com.hivebench.app.runtime.AgentCatalog.specs.mapValues { (kind, spec) ->
                    com.hivebench.app.runtime.AgentAuthState(
                        account = spec.credentialFiles.takeIf { it.isNotEmpty() }
                            ?.any { installer.guestFile(it).isFile },
                        apiKeyEnvs = spec.apiKeys
                            .filter { vault.contains(com.hivebench.app.runtime.AgentCatalog.keyId(kind, it.envVar)) }
                            .map { it.envVar }
                            .toSet(),
                    )
                }
            }
            _state.update { it.copy(agentAuth = auth) }
        }
    }

    fun saveAgentApiKey(kind: AgentKind, envVar: String, secret: String, baseUrl: String?) {
        val spec = com.hivebench.app.runtime.AgentCatalog.spec(kind)
        val option = spec.apiKeys.firstOrNull { it.envVar == envVar } ?: return
        vault.put(com.hivebench.app.runtime.AgentCatalog.keyId(kind, envVar), secret.trim())
        option.baseUrlEnv?.let { baseEnv ->
            val id = com.hivebench.app.runtime.AgentCatalog.keyId(kind, baseEnv)
            if (baseUrl.isNullOrBlank()) vault.remove(id) else vault.put(id, baseUrl.trim().trimEnd('/'))
        }
        refreshAgentAuth()
    }

    fun removeAgentApiKey(kind: AgentKind, envVar: String) {
        val spec = com.hivebench.app.runtime.AgentCatalog.spec(kind)
        vault.remove(com.hivebench.app.runtime.AgentCatalog.keyId(kind, envVar))
        spec.apiKeys.firstOrNull { it.envVar == envVar }?.baseUrlEnv?.let {
            vault.remove(com.hivebench.app.runtime.AgentCatalog.keyId(kind, it))
        }
        refreshAgentAuth()
    }

    /** Signs out by deleting the agent's own credential files; saved API keys are kept. */
    fun signOutAgent(kind: AgentKind) {
        val spec = com.hivebench.app.runtime.AgentCatalog.spec(kind)
        viewModelScope.launch(Dispatchers.IO) {
            spec.credentialFiles.forEach { installer.guestFile(it).delete() }
            refreshAgentAuth()
        }
    }

    /** Terminal registry key for an agent session in the active project. */
    fun agentSessionKey(kind: AgentKind, purpose: com.hivebench.app.runtime.AgentLaunchPurpose): String = when (purpose) {
        com.hivebench.app.runtime.AgentLaunchPurpose.SESSION ->
            "agent:${_state.value.activeProject?.id ?: "none"}:${kind.stableId}"
        else -> "auth:${kind.stableId}"
    }

    /** Host launch that runs [kind]'s TUI (or its sign-in flow) inside the Linux runtime. */
    suspend fun agentLaunch(
        kind: AgentKind,
        purpose: com.hivebench.app.runtime.AgentLaunchPurpose,
        extraArgs: List<String> = emptyList(),
        extraEnv: Map<String, String> = emptyMap(),
    ): RuntimeInstaller.GuestLaunch = withContext(Dispatchers.IO) {
        val spec = com.hivebench.app.runtime.AgentCatalog.spec(kind)
        val runtime = installer.installedRuntime()
        if (kind == AgentKind.DEEPSEEK_HARNESS) installer.ensureDshAndroidCompatibility()
        val environment = buildMap {
            put("TERM", "xterm-256color")
            put("COLORTERM", "truecolor")
            putAll(spec.environment)
            spec.apiKeys.forEach { option ->
                vault.get(com.hivebench.app.runtime.AgentCatalog.keyId(kind, option.envVar))?.let { put(option.envVar, it) }
                option.baseUrlEnv?.let { baseEnv ->
                    vault.get(com.hivebench.app.runtime.AgentCatalog.keyId(kind, baseEnv))?.let { put(baseEnv, it) }
                }
            }
            putAll(extraEnv)
        }
        val command = when (purpose) {
            com.hivebench.app.runtime.AgentLaunchPurpose.SESSION ->
                if (kind == AgentKind.CUSTOM_RUNNER) {
                    val base = _state.value.customRunnerCommand.ifBlank { "exec bash -l" }
                    val args = extraArgs.joinToString("") { " '" + it.replace("'", "'\\''") + "'" }
                    listOf("/usr/bin/bash", "-lc", base + args)
                } else spec.launch + extraArgs
            com.hivebench.app.runtime.AgentLaunchPurpose.LOGIN -> spec.login ?: spec.launch
            com.hivebench.app.runtime.AgentLaunchPurpose.API_KEY_LOGIN ->
                listOf("/usr/bin/bash", "-lc", requireNotNull(spec.apiKeyLogin) { "${kind.title} reads its API key directly" })
        }
        val project = _state.value.activeProject
        if (purpose == com.hivebench.app.runtime.AgentLaunchPurpose.SESSION && project != null) {
            installer.guestLaunch(
                runtime.proot, runtime.rootfs, projectWorkspaceRoot(project), environment, command,
                guestWorkspacePath = projectGuestRoot(project),
                emulateHardLinks = spec.emulateHardLinks,
            )
        } else {
            val workspace = File(getApplication<Application>().filesDir, "workspaces/agent-auth").apply { mkdirs() }
            installer.guestLaunch(
                runtime.proot, runtime.rootfs, workspace, environment, command,
                guestWorkspacePath = "/workspace/agent-auth",
                emulateHardLinks = spec.emulateHardLinks,
            )
        }
    }

    // ---- Multi-agent office (hive) --------------------------------------------------

    /** The active project's office: god + hired agents, each a parallel CLI session. */
    val hive: com.hivebench.app.hive.HiveEngine by lazy {
        com.hivebench.app.hive.HiveEngine(
            getApplication(),
            viewModelScope,
            object : com.hivebench.app.hive.HiveHost {
                override suspend fun launch(agent: com.hivebench.app.hive.HiveAgent, extraArgs: List<String>, extraEnv: Map<String, String>) =
                    agentLaunch(agent.cli, com.hivebench.app.runtime.AgentLaunchPurpose.SESSION, extraArgs, extraEnv)
                override fun transcriptFile(sessionId: String) = claudeTranscriptFile(sessionId)
                override fun connectionEnv(connections: List<String>) = hiveConnectionEnv(connections)
                override fun autoApproveTools() = _state.value.autoApproveTools
                override fun isInstalled(kind: AgentKind) =
                    kind == AgentKind.CUSTOM_RUNNER || _state.value.installedAgentVersions.containsKey(kind)
                override fun toast(message: String) { _state.update { it.copy(toastMessage = message) } }
            },
        ).also { engine ->
            hiveStarted = true
            viewModelScope.launch { engine.snapshot.collect { snap -> _state.update { it.copy(hive = snap) } } }
        }
    }

    @Volatile private var hiveStarted = false

    override fun onCleared() {
        // Agent PTYs are app-scoped and keep running; only the engine's webhook socket must go.
        if (hiveStarted) hive.close()
        super.onCleared()
    }

    private fun hiveConnectionEnv(connections: List<String>): Map<String, String> = buildMap {
        connections.mapNotNull { com.hivebench.app.hive.HiveConnections.byId(it) }.forEach { conn ->
            conn.envVars.forEach { env ->
                val value = vault.get("hive-conn:${conn.id}") ?: if (conn.id == "linear") _state.value.linearApiKey else null
                value?.takeIf { it.isNotBlank() }?.let { put(env, it) }
            }
        }
    }

    fun hiveConnectionConfigured(id: String): Boolean =
        !vault.get("hive-conn:$id").isNullOrBlank() || (id == "linear" && !_state.value.linearApiKey.isNullOrBlank())

    fun saveHiveConnection(id: String, secret: String) {
        if (secret.isBlank()) vault.remove("hive-conn:$id") else vault.put("hive-conn:$id", secret.trim())
        _state.update { it.copy(toastMessage = if (secret.isBlank()) "Connection removed" else "Connection saved") }
    }

    // ---- Parallel agent sessions ------------------------------------------------
    // Every session is its own PTY in TerminalSessions, so switching never stops
    // the others; they keep running in the background until closed explicitly.

    private val agentSessionStore = com.hivebench.app.session.AgentSessionStore(application.filesDir)

    private fun loadAgentSessions(project: Project) {
        val kind = _state.value.agentKind
        val sessions = agentSessionStore.load(project.id).ifEmpty {
            listOf(com.hivebench.app.session.AgentSessionEntry(projectId = project.id, kind = kind, title = "${kind.title} 1"))
                .also { agentSessionStore.save(project.id, it) }
        }
        val active = sessions.firstOrNull { com.hivebench.app.terminal.TerminalSessions.isRunning(it.terminalKey) } ?: sessions.first()
        _state.update { it.copy(agentSessions = sessions, activeAgentSessionId = active.id) }
    }

    private fun saveAgentSessions(sessions: List<com.hivebench.app.session.AgentSessionEntry>, activeId: String?) {
        val project = _state.value.activeProject ?: return
        agentSessionStore.save(project.id, sessions)
        _state.update { it.copy(agentSessions = sessions, activeAgentSessionId = activeId) }
    }

    fun activeAgentSession(): com.hivebench.app.session.AgentSessionEntry? =
        _state.value.agentSessions.firstOrNull { it.id == _state.value.activeAgentSessionId }

    /** Adds a new parallel session running [kind] and makes it the visible one. */
    fun newAgentSession(kind: AgentKind = _state.value.agentKind) {
        val project = _state.value.activeProject ?: return
        val current = _state.value.agentSessions
        val number = current.count { it.kind == kind } + 1
        val entry = com.hivebench.app.session.AgentSessionEntry(projectId = project.id, kind = kind, title = "${kind.title} $number")
        saveAgentSessions(current + entry, entry.id)
    }

    /** Shows another session; the one being left keeps running. */
    fun switchAgentSession(id: String) {
        if (_state.value.agentSessions.none { it.id == id }) return
        _state.update { it.copy(activeAgentSessionId = id) }
    }

    fun renameAgentSession(id: String, title: String) {
        val clean = title.trim().take(48).ifBlank { return }
        saveAgentSessions(
            _state.value.agentSessions.map { if (it.id == id) it.copy(title = clean) else it },
            _state.value.activeAgentSessionId,
        )
    }

    /** Stops the session's process and forgets it. */
    fun closeAgentSession(id: String) {
        val project = _state.value.activeProject ?: return
        val entry = _state.value.agentSessions.firstOrNull { it.id == id } ?: return
        com.hivebench.app.terminal.TerminalSessions.close(entry.terminalKey)
        var remaining = _state.value.agentSessions.filterNot { it.id == id }
        if (remaining.isEmpty()) {
            val kind = _state.value.agentKind
            remaining = listOf(com.hivebench.app.session.AgentSessionEntry(projectId = project.id, kind = kind, title = "${kind.title} 1"))
        }
        val activeId = _state.value.activeAgentSessionId.takeIf { active -> remaining.any { it.id == active } }
            ?: (remaining.firstOrNull { com.hivebench.app.terminal.TerminalSessions.isRunning(it.terminalKey) } ?: remaining.last()).id
        saveAgentSessions(remaining, activeId)
    }

    /** Stops the session's process but keeps it in the list so it can be resumed. */
    fun stopAgentSession(id: String) {
        val entry = _state.value.agentSessions.firstOrNull { it.id == id } ?: return
        com.hivebench.app.terminal.TerminalSessions.close(entry.terminalKey)
    }

    /**
     * Launch for [entry]. Claude Code sessions are pinned to their own
     * `--session-id` and resumed with `--resume` so restarting keeps the conversation.
     */
    suspend fun agentSessionLaunch(entry: com.hivebench.app.session.AgentSessionEntry): RuntimeInstaller.GuestLaunch {
        val args = when {
            entry.kind == AgentKind.CLAUDE_CODE ->
                if (entry.launched && claudeTranscriptExists(entry.resumeId)) listOf("--resume", entry.resumeId)
                else listOf("--session-id", entry.resumeId)
            // Other CLIs resume their most recent conversation (codex resume --last, opencode --continue, …).
            entry.launched -> com.hivebench.app.runtime.AgentCatalog.spec(entry.kind).resumeArgs
            else -> emptyList()
        }
        return agentLaunch(entry.kind, com.hivebench.app.runtime.AgentLaunchPurpose.SESSION, extraArgs = args)
    }

    /** Call once the session's process actually started, so later starts resume it. */
    fun markAgentSessionLaunched(id: String) {
        val entry = _state.value.agentSessions.firstOrNull { it.id == id } ?: return
        if (entry.launched) return
        saveAgentSessions(
            _state.value.agentSessions.map { if (it.id == id) it.copy(launched = true) else it },
            _state.value.activeAgentSessionId,
        )
    }

    /** Whether restarting [entry] continues its previous conversation. */
    fun agentSessionResumable(entry: com.hivebench.app.session.AgentSessionEntry): Boolean =
        entry.kind == AgentKind.CLAUDE_CODE || com.hivebench.app.runtime.AgentCatalog.spec(entry.kind).resumeArgs.isNotEmpty()

    /** True when Claude Code has a transcript for [sessionId] (so `--resume` will work). */
    internal fun claudeTranscriptExists(sessionId: String): Boolean {
        val projects = installer.guestFile("/root/.claude/projects")
        return projects.listFiles()?.any { File(it, "$sessionId.jsonl").isFile } == true
    }

    /** Host path of Claude Code's transcript for [sessionId], if any. */
    internal fun claudeTranscriptFile(sessionId: String): File? =
        installer.guestFile("/root/.claude/projects").listFiles()
            ?.map { File(it, "$sessionId.jsonl") }
            ?.firstOrNull { it.isFile }

    fun updateAgent(kind: AgentKind) {
        val update = _state.value.agentUpdates[kind] ?: return
        if (_state.value.agentUpdating != null || _state.value.agentInstalling != null) return
        // Swapping an agent's files under a live TUI breaks that session mid-task.
        val binary = com.hivebench.app.runtime.AgentCatalog.spec(kind).launch.firstOrNull()
        if (binary != null && com.hivebench.app.terminal.TerminalSessions.isBinaryRunning(binary)) {
            _state.update { it.copy(toastMessage = "Stop the running ${kind.title} sessions before updating") }
            return
        }
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
                changes = emptyList(),
                workspaceFiles = emptyList(),
                androidProjectDetected = false,
                filesLoading = true,
                suggestedProjectRoot = suggestedRoot,
                previewReady = false,
                previewUrl = null,
                pendingAttachments = emptyList(),
)
        }
        refreshProjectFiles()
        refreshGitState()
        loadAgentSessions(project)
        hive.open(project.id, projectWorkspaceRoot(project), projectGuestRoot(project))
    }

    fun closeProject() {
        val active = _state.value.activeProject

        if (active != null) {
            val chats = preferences.loadProjectChats(active.id)
            val userMessages = chats.sumOf { preferences.loadMessages(active.id, it.id).count { m -> m.fromUser } }
            val workspaceDir = File(getApplication<Application>().filesDir, "workspaces/${active.id}")
            val userFiles = if (workspaceDir.isDirectory) {
                workspaceDir.walkTopDown().filter { file ->
                    file.isFile && !file.name.startsWith(".claude") && file.name != ".pocket-dev-stacks.json" &&
                        !file.path.contains("/.hive/")
                }.count()
            } else 0

            val liveAgents = com.hivebench.app.terminal.TerminalSessions.runningKeys().any {
                it.startsWith("agent:${active.id}:") || it.startsWith("hive:${active.id}:")
            }
            if (!liveAgents && userMessages == 0 && userFiles == 0) {
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

        // The office stops routing; its agents' terminals keep running and are adopted on reopen.
        if (hiveStarted) hive.close()
        _state.update {
            it.copy(
                agentSessions = emptyList(),
                activeAgentSessionId = null,
                activeProject = null,
                workspaceVisible = false,
                projectChats = emptyList(),
                activeChatId = null,
                changes = emptyList(),
                workspaceFiles = emptyList(),
                androidProjectDetected = false,
                filesLoading = false,
                activeSessionId = null,
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
        val project = _state.value.readOnlyProject ?: return
        closeReadOnlyProject()
        openProject(project)
    }

    fun consumeToast() = _state.update { it.copy(toastMessage = null) }

    fun createProject(name: String) {
        if (name.isBlank()) return
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
        val guestRoot = projectGuestRoot(project)
        _state.update {
            it.copy(
                projects = listOf(project) + it.projects,
                activeProject = project,
                workspaceVisible = true,
                messages = listOf(ChatMessage(fromUser = false, text = "Hi! Tell me what you want to build or change.")),
                changes = emptyList(),
                workspaceFiles = emptyList(),
                androidProjectDetected = false,
                filesLoading = true,
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
        if (_state.value.projectImporting) return
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
            val safeArchiveName = archiveName.replace(Regex("[^A-Za-z0-9._-]"), "_").take(96).ifBlank { "project" }.let { name ->
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
        if (_state.value.gitCloneRunning || _state.value.projectImporting) return
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
        // credential in Hivebench's private Linux home instead of exporting it.
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
            Intent(getApplication(), com.hivebench.app.runtime.RuntimeExecutionService::class.java)
                .setAction(com.hivebench.app.runtime.RuntimeExecutionService.ACTION_START)
                .putExtra(com.hivebench.app.runtime.RuntimeExecutionService.EXTRA_PROJECT_NAME, "GitHub sign-in")
                .putExtra(com.hivebench.app.runtime.RuntimeExecutionService.EXTRA_TITLE, "Connecting GitHub")
                .putExtra(com.hivebench.app.runtime.RuntimeExecutionService.EXTRA_CAN_STOP, false),
)
    }

    private fun stopGitHubForegroundOperation() {
        runCatching {
            getApplication<Application>().startService(
                Intent(getApplication(), com.hivebench.app.runtime.RuntimeExecutionService::class.java)
                    .setAction(com.hivebench.app.runtime.RuntimeExecutionService.ACTION_CANCELLED),
)
        }.onFailure {
            getApplication<Application>().stopService(
                Intent(getApplication(), com.hivebench.app.runtime.RuntimeExecutionService::class.java),
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
        if (_state.value.activeProject?.id == projectId) return
        _state.update { current -> current.copy(projects = current.projects.filterNot { it.id == projectId }) }
        preferences.saveProjects(_state.value.projects)
        viewModelScope.launch(Dispatchers.IO) {
            val filesDir = getApplication<Application>().filesDir
            File(filesDir, "workspaces/${project.id}").deleteRecursively()
            terminalHistoryFile(project.id).delete()
            preferences.deleteProjectChats(project.id)
        }
    }

    /** History files written by the old line-based terminal; deleted with their project. */
    private fun terminalHistoryFile(projectId: String): File =
        File(getApplication<Application>().filesDir, "terminal-history/$projectId.json")

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
        val updated = project.copy(rootPath = root)
        val projects = current.projects.map { if (it.id == updated.id) updated else it }
        val guestRoot = projectGuestRoot(updated)
        preferences.saveProjects(projects)
        _state.update {
            it.copy(
                projects = projects,
                activeProject = updated,
                suggestedProjectRoot = null,
                changes = emptyList(),
                toastMessage = "$root is now the project root",
)
        }
        refreshProjectFiles()
    }

    fun exportActiveProject(uri: Uri) {
        val current = _state.value
        val project = current.activeProject ?: return
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

    fun refreshGitState() {
        val project = _state.value.activeProject ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val workspace = projectWorkspaceRoot(project)
            val guestPath = projectGuestRoot(project)
            if (!installer.isInstalled()) {
                _state.update { it.copy(gitOperationRunning = false) }
                return@launch
            }

            // Never create a repository behind the user's back; the Git tab asks first.
            if (!gitManager.isGitRepository(workspace, guestPath)) {
                _state.update {
                    if (it.activeProject?.id != project.id) it
                    else it.copy(gitRepositoryMissing = true, gitStatus = null, gitBranches = emptyList(), gitCommits = emptyList(), gitOperationRunning = false)
                }
                return@launch
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
                            gitRepositoryMissing = false,
)
                    }
                }
            }
        }
    }

    /** Creates a Git repository (branch main) for the open project, then loads its state. */
    fun initGitRepository() {
        val project = _state.value.activeProject ?: return
        _state.update { it.copy(gitOperationRunning = true, gitOperationMessage = "Initializing repository…") }
        viewModelScope.launch(Dispatchers.IO) {
            val result = gitManager.init(projectWorkspaceRoot(project), projectGuestRoot(project))
            if (result.isFailure) {
                _state.update { it.copy(gitOperationRunning = false, gitOperationMessage = result.exceptionOrNull()?.message) }
            } else {
                refreshGitState()
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
            val authorName = _state.value.githubLogin ?: "Hivebench User"
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

    fun selectGitCommit(commit: com.hivebench.app.model.GitCommit?) {
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
            _state.update { it.copy(toastMessage = "Connect GitHub first: Git tab → Connect") }
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

    /**
     * Agent sessions the browser inspector can push into: this project's sessions
     * (running ones first), then "new session" for every installed agent.
     */
    fun browserAgentTargets(): List<com.hivebench.app.browser.BrowserAgentTarget> {
        val sessions = _state.value.agentSessions
            .sortedByDescending { com.hivebench.app.terminal.TerminalSessions.isRunning(it.terminalKey) }
            .map { entry ->
                val running = com.hivebench.app.terminal.TerminalSessions.isRunning(entry.terminalKey)
                com.hivebench.app.browser.BrowserAgentTarget(
                    id = entry.id,
                    title = entry.title,
                    subtitle = "${entry.kind.title} · ${if (running) "running" else "starts when sent"}",
                    running = running,
                )
            }
        val installed = AgentKind.entries.filter {
            it == AgentKind.CUSTOM_RUNNER || _state.value.installedAgentVersions.containsKey(it)
        }
        val fresh = installed.map { kind ->
            com.hivebench.app.browser.BrowserAgentTarget(
                id = "new:${kind.stableId}",
                title = "New ${kind.title} session",
                subtitle = "Starts ${kind.title} in this project",
                running = false,
            )
        }
        return sessions + fresh
    }

    /**
     * Types [text] into an agent session's prompt (bracketed paste, not submitted)
     * so the user reviews it in the agent's own TUI. A stopped session is started
     * first and the paste waits until its startup output has settled.
     * [onReady] runs once the session exists, e.g. to switch to the Session tab.
     */
    fun pushToAgent(targetId: String, text: String, onReady: () -> Unit) {
        viewModelScope.launch {
            if (_state.value.activeProject == null) {
                _state.update { it.copy(toastMessage = "Open a project first") }
                return@launch
            }
            val entry = if (targetId.startsWith("new:")) {
                val kind = AgentKind.entries.firstOrNull { it.stableId == targetId.removePrefix("new:") } ?: return@launch
                val before = _state.value.agentSessions.map { it.id }.toSet()
                newAgentSession(kind)
                // Only paste into the session this call created, never a previously active one.
                activeAgentSession()?.takeIf { it.id !in before && it.kind == kind }
            } else {
                _state.value.agentSessions.firstOrNull { it.id == targetId }?.also { switchAgentSession(it.id) }
            } ?: return@launch
            val key = entry.terminalKey
            val fresh = !com.hivebench.app.terminal.TerminalSessions.isRunning(key)
            if (fresh) {
                val launch = runCatching { agentSessionLaunch(entry) }.getOrElse { error ->
                    _state.update { it.copy(toastMessage = "Could not start ${entry.title}: ${error.message}") }
                    return@launch
                }
                com.hivebench.app.terminal.TerminalSessions.restart(getApplication(), key, entry.title, launch)
            }
            onReady()
            // A freshly started TUI prints its banner (and may show a trust prompt) first.
            val deadline = SystemClock.uptimeMillis() + if (fresh) 60_000L else 3_000L
            delay(if (fresh) 2_500L else 150L)
            while (SystemClock.uptimeMillis() < deadline && !com.hivebench.app.terminal.TerminalSessions.isQuiet(key, 1_200L)) delay(300L)
            val emulator = com.hivebench.app.terminal.TerminalSessions.get(key)?.emulator
            if (emulator == null) {
                _state.update { it.copy(toastMessage = "${entry.title} is not running") }
                return@launch
            }
            emulator.paste(text)
            _state.update { it.copy(toastMessage = "Added to ${entry.title} — review and press Enter") }
        }
    }

    /** Saves a browser screenshot into the project and returns the path agents see, or null without a project. */
    fun saveBrowserCapture(bitmap: android.graphics.Bitmap): String? {
        val project = _state.value.activeProject ?: return null
        val name = "capture-${java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.US).format(java.util.Date())}.png"
        val dir = File(projectWorkspaceRoot(project), ".pocketdev/captures").apply { mkdirs() }
        return runCatching {
            java.io.FileOutputStream(File(dir, name)).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            "${projectGuestRoot(project)}/.pocketdev/captures/$name"
        }.getOrNull()
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
