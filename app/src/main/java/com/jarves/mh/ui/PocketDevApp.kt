package com.jarves.mh.ui

import android.Manifest
import android.app.ActivityManager
import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.net.Uri
import android.provider.Settings
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.widget.Toast
import android.speech.RecognizerIntent
import com.jarves.mh.BuildConfig
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jarves.mh.model.ActivityItem
import com.jarves.mh.model.AgentKind
import com.jarves.mh.model.ChangeItem
import com.jarves.mh.model.ChatMessage
import com.jarves.mh.model.ChatAttachment
import com.jarves.mh.model.DevStack
import com.jarves.mh.model.DEEPSEEK_HARNESS_PROVIDERS
import com.jarves.mh.model.DSH_PROTOCOL_PROVIDERS
import com.jarves.mh.model.DiffLine
import com.jarves.mh.model.DiffLineType
import com.jarves.mh.model.Project
import com.jarves.mh.model.ProjectKind
import com.jarves.mh.model.ProjectChat
import com.jarves.mh.model.ProviderKind
import com.jarves.mh.model.ProviderProfile
import com.jarves.mh.model.inferredDshApiForUrl
import com.jarves.mh.model.providersForAgent
import com.jarves.mh.model.ToolRequest
import com.jarves.mh.model.WorkspaceEntry
import com.jarves.mh.model.projectSlug
import com.jarves.mh.runtime.RuntimeExecutionService
import com.jarves.mh.runtime.RuntimeSetupService
import com.jarves.mh.runtime.supportsArm64Runtime
import com.jarves.mh.runtime.AntigravityAuthStatus
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.jarves.mh.network.ConnectionValidation
import com.jarves.mh.network.DiscoveredModel
import com.jarves.mh.network.ModelDiscoveryResult
import com.jarves.mh.network.GitHubRepository
import com.jarves.mh.ui.theme.PocketBlue
import com.jarves.mh.ui.theme.PocketGreen
import com.jarves.mh.ui.theme.PocketOrange
import com.jarves.mh.ui.theme.NeoLime
import com.jarves.mh.ui.theme.NeoBlack
import com.jarves.mh.ui.theme.NeoDarkBorder
import com.jarves.mh.ui.theme.neoShadow
import com.jarves.mh.ui.theme.neoTactile
import com.jarves.mh.ui.theme.neoBounce
import com.jarves.mh.ui.theme.neoBorder
import com.jarves.mh.ui.theme.PulsingDot
import com.jarves.mh.ui.theme.NeoBadge
import com.jarves.mh.ui.theme.NeoCard
import com.jarves.mh.ui.theme.NeoButton
import java.io.ByteArrayInputStream
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ExtendedFloatingActionButton

private enum class RootScreen(val label: String, val icon: ImageVector) {
    PROJECTS("Projects", Icons.Default.Folder),
    AGENT("Agent", Icons.Default.SmartToy),
    SETTINGS("Settings", Icons.Default.Settings),
}
private enum class WorkspaceTab(val label: String, val icon: ImageVector) {
    CHAT("Chat", Icons.Default.AutoAwesome),
    TASKS("Tasks", Icons.Default.ViewKanban),
    FILES("Files", Icons.Default.Folder),
    CHANGES("Git", Icons.Default.Commit),
    TERMINAL("Terminal", Icons.Default.Terminal),
    PREVIEW("Browser", Icons.Default.Language),
}

@Composable
fun PocketDevApp(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val projectsListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.consumeToast()
        }
    }
    when {
        state.startupStage == StartupStage.CHECKING -> StartupLoadingScreen(
            state = state,
            themeMode = state.themeMode,
            onToggleTheme = viewModel::toggleTheme,
        )
        !state.backgroundSetupComplete && state.startupStage == StartupStage.SETUP_REQUIRED ->
            BackgroundTaskSetupScreen(
                themeMode = state.themeMode,
                onToggleTheme = viewModel::toggleTheme,
                onContinue = viewModel::finishBackgroundSetup,
            )
        state.startupStage == StartupStage.SETUP_REQUIRED -> RuntimeSetupPromptScreen(
            selectedStacks = state.selectedDevStacks,
            selectedAgent = state.agentKind,
            themeMode = state.themeMode,
            onToggleTheme = viewModel::toggleTheme,
            onToggleStack = viewModel::toggleDevStack,
            onSelectAgent = viewModel::selectAgent,
            onDownload = viewModel::startRuntimeSetup,
        )
        state.startupStage == StartupStage.INSTALLING && state.showDetailedSetupProgress ->
            RuntimeInstallationScreen(
                state = state,
                themeMode = state.themeMode,
                onToggleTheme = viewModel::toggleTheme,
            )
        state.startupStage == StartupStage.INSTALLING || state.startupStage == StartupStage.INITIALIZING ->
            StartupLoadingScreen(
                state = state,
                themeMode = state.themeMode,
                onToggleTheme = viewModel::toggleTheme,
            )
        state.startupStage == StartupStage.ERROR -> StartupErrorScreen(
            message = state.startupError,
            isOffline = state.startupErrorIsOffline,
            logs = state.startupLogs,
            themeMode = state.themeMode,
            onToggleTheme = viewModel::toggleTheme,
            onRetry = viewModel::retryStartup,
        )
        state.startupStage == StartupStage.MODEL_SETUP && state.agentKind == AgentKind.ANTIGRAVITY ->
            AntigravityOnboardingScreen(
                state = state,
                onStartLogin = viewModel::startAntigravityLogin,
                onSubmitCode = viewModel::submitAntigravityCode,
                onContinue = viewModel::finishAntigravityOnboarding,
                onSelectAgent = viewModel::chooseOnboardingAgent,
                onToggleTheme = viewModel::toggleTheme,
            )
        state.startupStage == StartupStage.MODEL_SETUP -> ProviderSetupScreen(
            initial = state.provider,
            onboarding = true,
            agentKind = state.agentKind,
            initialStep = 1,
            onSave = viewModel::finishOnboarding,
            onDiscover = viewModel::discoverModels,
            onValidate = viewModel::validateProvider,
            onSelectAgent = viewModel::chooseOnboardingAgent,
            onToggleTheme = viewModel::toggleTheme,
            themeMode = state.themeMode,
        )
        state.startupStage == StartupStage.READY && !state.backgroundSetupComplete ->
            BackgroundTaskSetupScreen(
                themeMode = state.themeMode,
                onToggleTheme = viewModel::toggleTheme,
                onContinue = viewModel::finishBackgroundSetup,
            )
        state.readOnlyProject != null -> ReadOnlyProjectScreen(
            state = state,
            onBack = viewModel::closeReadOnlyProject,
            onSwitchChat = viewModel::switchReadOnlyChat,
            onContinueHere = viewModel::activateReadOnlyProject,
        )
        state.activeProject != null && state.workspaceVisible -> WorkspaceScreen(
            state = state,
            onBack = viewModel::closeProject,
            onSend = viewModel::sendPrompt,
            onStop = viewModel::stopTask,
            onApproval = viewModel::answerApproval,
            onRefreshFiles = viewModel::refreshProjectFiles,
            onOpenFile = viewModel::openFile,
            onCloseFile = viewModel::closeFile,
            onUndoChanges = viewModel::undoLastChanges,
            onKeepChanges = viewModel::keepLastChanges,
            onUndoFileChange = viewModel::undoFileChange,
            onKeepFileChange = viewModel::keepFileChange,
            onCreateChat = viewModel::createChat,
            onSwitchChat = viewModel::switchChat,
            onTerminalRun = viewModel::requestProjectTerminalCommand,
            onTerminalInput = viewModel::sendProjectTerminalInput,
            onTerminalInterrupt = viewModel::interruptProjectTerminalCommand,
            onTerminalPrepare = viewModel::prepareProjectTerminalCommand,
            onTerminalDraftConsumed = viewModel::consumeProjectTerminalDraft,
            onTerminalOpened = viewModel::openProjectTerminal,
            onTerminalStop = viewModel::stopProjectTerminalCommand,
            onTerminalClear = viewModel::clearProjectTerminal,
            onTerminalConfirm = viewModel::confirmProjectTerminalCommand,
            onTerminalCancel = viewModel::cancelProjectTerminalCommand,
            onUseSuggestedProjectRoot = viewModel::useSuggestedProjectRoot,
            onExportProject = viewModel::exportActiveProject,
            onAddAttachments = viewModel::addChatAttachments,
            onRemoveAttachment = viewModel::removePendingAttachment,
            onOpenAttachment = viewModel::openChatAttachment,
            onBuildAndRunAndroid = viewModel::buildAndRunAndroidApp,
            onRefreshGit = viewModel::refreshGitState,
            onStageFile = viewModel::stageFile,
            onUnstageFile = viewModel::unstageFile,
            onStageAll = viewModel::stageAllGitFiles,
            onUnstageAll = viewModel::unstageAllGitFiles,
            onDiscardFile = viewModel::discardGitFile,
            onCommit = viewModel::commitGitChanges,
            onCreateBranch = viewModel::createGitBranch,
            onCheckoutBranch = viewModel::checkoutGitBranch,
            onDeleteBranch = viewModel::deleteGitBranch,
            onMergeBranch = viewModel::mergeGitBranch,
            onCreateWorktree = viewModel::createGitWorktree,
            onRemoveWorktree = viewModel::removeGitWorktree,
            onPushGit = viewModel::pushGitBranch,
            onPullGit = viewModel::pullGitBranch,
            onSelectCommit = viewModel::selectGitCommit,
            onCreatePullRequest = viewModel::createGitHubPullRequest,
            onStartGitHubLogin = viewModel::startGitHubLogin,
            onDecomposeGoal = viewModel::decomposeSwarmGoal,
            onCreateTask = viewModel::createSwarmTask,
            onMoveTaskStatus = viewModel::moveSwarmTaskStatus,
            onAssignTask = viewModel::assignSwarmTask,
            onResetCircuitBreaker = viewModel::resetSwarmCircuitBreaker,
            onSendDirectMessage = viewModel::sendSwarmDirectMessage,
            onAddBlackboardEntry = viewModel::addSwarmBlackboardEntry,
            onSendContextToAgent = viewModel::sendElementContextToChat,
            onConsumeDesignModeDraft = viewModel::consumeDesignModeDraft,
            onSetEffort = viewModel::setAntigravityEffort,
            onCreateSchedule = viewModel::createScheduledTask,
            onToggleSchedule = viewModel::toggleScheduledTask,
            onDeleteSchedule = viewModel::deleteScheduledTask,
            onMergeWorktree = viewModel::mergeWorktreeBranch,
            onToggleSkill = viewModel::toggleSkill,
            onCreateSkill = viewModel::addCustomSkill,
            onToggleAnnotationResolved = viewModel::toggleDiffAnnotationResolved,
            onRunAutomatedReview = viewModel::runAutomatedReviewForWorktree,
            onAddSshProfile = viewModel::addSshProfile,
            onSetActiveSshProfile = viewModel::setActiveSshProfile,
            onDeleteSshProfile = viewModel::deleteSshProfile,
            onSaveLinearApiKey = viewModel::setLinearApiKey,
            onRefreshLinearIssues = viewModel::refreshLinearIssues,
            onImportLinearIssue = viewModel::importLinearIssueToTask,
            onRefreshGitHubIssues = viewModel::refreshGitHubIssues,
            onImportGitHubIssue = viewModel::importGitHubIssueToTask,
            onSelectAgent = viewModel::selectAgent,
            onToggleAutoApprove = viewModel::toggleAutoApproveTools,
            onSelectModel = viewModel::selectModel,
        )
        else -> RootScreenHost(state, viewModel, projectsListState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AntigravityOnboardingScreen(
    state: AppUiState,
    onStartLogin: () -> Unit,
    onSubmitCode: (String) -> Unit,
    onContinue: () -> Unit,
    onSelectAgent: (AgentKind) -> Unit,
    onToggleTheme: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    var code by rememberSaveable { mutableStateOf("") }
    var showAgentPicker by rememberSaveable { mutableStateOf(false) }
    if (showAgentPicker) {
        AgentSwitchSheet(
            selected = AgentKind.ANTIGRAVITY,
            onSelect = { agent ->
                showAgentPicker = false
                onSelectAgent(agent)
            },
            onDismiss = { showAgentPicker = false },
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set up Antigravity") },
                actions = { IconButton(onClick = onToggleTheme) { Icon(Icons.Default.DarkMode, "Toggle theme") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Connect your Google account", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "PocketDev runs Google's official agy CLI inside its private Linux environment. Google handles authentication and agy owns the saved session.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when (state.antigravityAuth.status) {
                AntigravityAuthStatus.SIGNED_OUT, AntigravityAuthStatus.ERROR -> {
                    state.antigravityAuth.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    NeoButton(
                        onClick = onStartLogin,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        containerColor = NeoLime,
                        contentColor = NeoBlack,
                    ) {
                        Text("Sign in with Google", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
                AntigravityAuthStatus.STARTING -> {
                    LinearProgressIndicator(Modifier.fillMaxWidth(), color = NeoLime)
                    Text("Starting the official Antigravity login…")
                }
                AntigravityAuthStatus.COMPLETING -> {
                    LinearProgressIndicator(Modifier.fillMaxWidth(), color = NeoLime)
                    Text("Completing Google sign-in…")
                }
                AntigravityAuthStatus.AWAITING_CODE -> {
                    Text("Google sign-in opened in your browser. Copy the one-time code shown after approval.")
                    state.antigravityAuth.authorizationUrl?.let { url ->
                        NeoButton(
                            onClick = { clipboard.setText(AnnotatedString(url)) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ) {
                            Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Copy sign-in URL", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Authorization code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    )
                    NeoButton(
                        onClick = { onSubmitCode(code); code = "" },
                        enabled = code.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        containerColor = NeoLime,
                        contentColor = NeoBlack,
                    ) { Text("Complete sign-in", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                }
                AntigravityAuthStatus.SIGNED_IN -> {
                    NeoCard(
                        backgroundColor = NeoLime.copy(alpha = 0.18f),
                        borderColor = NeoLime,
                        cornerRadius = 14.dp,
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PulsingDot(color = NeoLime, size = 8.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                state.antigravityAuth.accountEmail?.let { "Connected as $it" } ?: "Google account connected",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    NeoButton(
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        containerColor = NeoLime,
                        contentColor = NeoBlack,
                    ) {
                        Text("Continue", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
            TextButton(
                onClick = { showAgentPicker = true },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("Use another coding agent", fontSize = 12.sp)
            }
            NeoCard(
                backgroundColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                cornerRadius = 14.dp,
            ) {
                Text(
                    "Automatic tool approval is enabled for Antigravity. It can edit project files and run commands without confirmation. Changes remain reviewable in PocketDev.",
                    Modifier.fillMaxWidth().padding(14.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackgroundTaskSetupScreen(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    onToggleTheme: () -> Unit = {},
    onContinue: () -> Unit,
) {
    val context = LocalContext.current
    val powerManager = context.getSystemService(PowerManager::class.java)
    fun notificationsAllowed(): Boolean {
        val runtimeGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        return runtimeGranted && androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    fun batteryUnrestricted(): Boolean = powerManager.isIgnoringBatteryOptimizations(context.packageName)

    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    var notificationGranted by remember { mutableStateOf(notificationsAllowed()) }
    var batteryGranted by remember { mutableStateOf(batteryUnrestricted()) }
    var notificationDenied by rememberSaveable { mutableStateOf(false) }
    var taskProtectionConfirmed by rememberSaveable { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationGranted = notificationsAllowed()
        notificationDenied = !granted
        if (notificationGranted) currentStep = 1
    }
    val notificationSettingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        notificationGranted = notificationsAllowed()
        if (notificationGranted) currentStep = 1
    }
    val batteryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        batteryGranted = batteryUnrestricted()
        if (batteryGranted) currentStep = 2
    }

    LaunchedEffect(Unit) {
        notificationGranted = notificationsAllowed()
        batteryGranted = batteryUnrestricted()
    }

    val currentIcon = when (currentStep) {
        0 -> Icons.Default.Notifications
        1 -> Icons.Default.BatterySaver
        else -> Icons.Default.Shield
    }
    val currentTitle = when (currentStep) {
        0 -> "Task notifications"
        1 -> "Background reliability"
        else -> "Task protection"
    }
    val currentDescription = when (currentStep) {
        0 -> "See live progress and receive an alert when Claude finishes or needs your attention."
        1 -> "Allow Mobile Harness to continue a task when you lock the phone or switch to another app."
        else -> "Keep the CPU awake only while a visible coding task is running, then release it automatically."
    }
    val currentPrivacyNote = when (currentStep) {
        0 -> "Only task progress, completion, and error notifications are sent."
        1 -> "You remain in control and can stop every task from its notification."
        else -> "The screen stays off. Protection is capped at 90 minutes and stops with the task."
    }
    val currentGranted = when (currentStep) {
        0 -> notificationGranted
        1 -> batteryGranted
        else -> taskProtectionConfirmed
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BrandMark(compact = true)
                        Spacer(Modifier.width(9.dp))
                        Text("Mobile Harness", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            if (themeMode == AppThemeMode.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle theme",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(8.dp))
            Text("Prepare for reliable setup", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Setup time depends on the toolchains you choose next. You may leave Mobile Harness in the background while it works.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(18.dp))
            StepDots(currentStep)
            Spacer(Modifier.height(18.dp))

            NeoCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                shadowOffset = 3.5.dp,
            ) {
                Column {
                    PermissionSummaryRow(Icons.Default.Notifications, "Notifications", notificationGranted, currentStep == 0)
                    HorizontalDivider(modifier = Modifier.padding(start = 58.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    PermissionSummaryRow(Icons.Default.BatterySaver, "Background", batteryGranted, currentStep == 1)
                    HorizontalDivider(modifier = Modifier.padding(start = 58.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    PermissionSummaryRow(Icons.Default.Shield, "Task protection", taskProtectionConfirmed, currentStep == 2)
                }
            }

            Spacer(Modifier.height(14.dp))
            NeoCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                shadowOffset = 3.5.dp,
                backgroundColor = NeoLime.copy(alpha = 0.08f),
                borderColor = NeoLime,
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .background(NeoLime, RoundedCornerShape(10.dp))
                                .border(1.5.dp, NeoBlack, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(currentIcon, null, tint = NeoBlack, modifier = Modifier.size(21.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("STEP ${currentStep + 1} OF 3", color = if (isSystemInDarkTheme()) NeoLime else NeoBlack, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
                            Text(currentTitle, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        if (currentGranted) Icon(Icons.Default.Check, "Granted", tint = NeoLime)
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(currentDescription, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 18.sp)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Shield, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(currentPrivacyNote, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, lineHeight = 15.sp)
                    }
                    Spacer(Modifier.height(18.dp))
                    NeoButton(
                        onClick = {
                            when (currentStep) {
                                0 -> when {
                                    notificationGranted -> currentStep = 1
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationDenied -> {
                                        // Targets below API 33 can have notification prompts tied to
                                        // channel creation. Create channels only after this explicit tap.
                                        RuntimeExecutionService.ensureNotificationChannels(context)
                                        RuntimeSetupService.ensureNotificationChannel(context)
                                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    else -> notificationSettingsLauncher.launch(
                                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(
                                            Settings.EXTRA_APP_PACKAGE,
                                            context.packageName,
                                        ),
                                    )
                                }
                                1 -> if (batteryGranted) {
                                    currentStep = 2
                                } else {
                                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    runCatching { batteryLauncher.launch(intent) }
                                        .onFailure {
                                            batteryLauncher.launch(
                                                Intent(
                                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                    Uri.parse("package:${context.packageName}"),
                                                ),
                                            )
                                        }
                                }
                                else -> {
                                    taskProtectionConfirmed = true
                                    onContinue()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        containerColor = NeoLime,
                        contentColor = NeoBlack,
                    ) {
                        Text(
                            when (currentStep) {
                                0 -> if (notificationGranted) "Next" else if (notificationDenied) "Open notification settings" else "Allow notifications"
                                1 -> if (batteryGranted) "Next" else "Open battery settings"
                                else -> "Enable and finish"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp), tint = NeoBlack)
                    }
                    if (currentStep < 2 && !currentGranted) {
                        TextButton(
                            onClick = { currentStep += 1 },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (currentStep == 0) "Continue without notifications" else "Continue without battery exemption")
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "You can change these settings later. Android may still stop exceptionally heavy work when the device is low on memory.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.5.sp,
                lineHeight = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun PermissionSummaryRow(
    icon: ImageVector,
    title: String,
    complete: Boolean,
    active: Boolean,
) {
    val isDark = isSystemInDarkTheme()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (active) NeoLime.copy(alpha = 0.12f) else Color.Transparent)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            null,
            tint = if (active) (if (isDark) NeoLime else NeoBlack) else if (complete) NeoLime else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
        )
        when {
            complete -> Icon(Icons.Default.Check, "Complete", tint = NeoLime, modifier = Modifier.size(18.dp))
            active -> Text("Required", color = if (isDark) NeoLime else NeoBlack, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            else -> Text("Next", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}

private data class DevStackVisuals(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accentColor: Color,
    val tag: String,
)

private fun getDevStackVisuals(stack: DevStack): DevStackVisuals = when (stack) {
    DevStack.WEB -> DevStackVisuals(
        icon = Icons.Default.Language,
        accentColor = Color(0xFF38BDF8),
        tag = "HTML · CSS · JS · TS",
    )
    DevStack.PYTHON -> DevStackVisuals(
        icon = Icons.Default.Terminal,
        accentColor = Color(0xFFFBBF24),
        tag = "python3 + pip + venv",
    )
    DevStack.ANDROID -> DevStackVisuals(
        icon = Icons.Default.Android,
        accentColor = Color(0xFF4ADE80),
        tag = "OpenJDK build tools",
    )
    DevStack.CPP -> DevStackVisuals(
        icon = Icons.Default.Memory,
        accentColor = Color(0xFFA78BFA),
        tag = "gcc + g++ + cmake",
    )
    DevStack.PHP -> DevStackVisuals(
        icon = Icons.Default.Dns,
        accentColor = Color(0xFF818CF8),
        tag = "php-cli + Composer",
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RuntimeSetupPromptScreen(
    selectedStacks: Set<DevStack>,
    selectedAgent: AgentKind = AgentKind.CLAUDE_CODE,
    themeMode: AppThemeMode = AppThemeMode.DARK,
    onToggleTheme: () -> Unit = {},
    onToggleStack: (DevStack) -> Unit,
    onSelectAgent: (AgentKind) -> Unit = {},
    onDownload: () -> Unit,
) {
    val context = LocalContext.current
    val activityManager = context.getSystemService(ActivityManager::class.java)
    val memoryInfo = remember { ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo) }
    val totalRamGb = memoryInfo.totalMem.toDouble() / 1_073_741_824.0
    val totalRamLabel = String.format(java.util.Locale.US, "%.1f", totalRamGb)
    val arm64 = supportsArm64Runtime(Build.SUPPORTED_ABIS, System.getProperty("os.arch"))
    // Android reports usable physical memory after hardware/GPU reservations.
    // RAM is therefore informational; it must not reject nominal 4 GB phones.
    val compatible = arm64

    var currentStep by remember { mutableIntStateOf(0) }
    val setupScrollState = rememberScrollState()

    LaunchedEffect(currentStep) {
        setupScrollState.scrollTo(0)
    }

    if (currentStep > 0) {
        BackHandler { currentStep = 0 }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BrandMark(compact = true)
                        Spacer(Modifier.width(9.dp))
                        Text("Mobile Harness", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    if (currentStep > 0) {
                        IconButton(onClick = { currentStep = 0 }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            if (themeMode == AppThemeMode.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle theme",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 22.dp)
                .verticalScroll(setupScrollState),
        ) {
            Spacer(Modifier.height(8.dp))

            if (currentStep == 0) {
                // Step 0: Device Compatibility & Verification
                Text(
                    text = "DEVICE CHECK",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Ready to build on this phone",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Your phone meets the requirements. Choose your coding tools next and Mobile Harness will handle the setup.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                )

                Spacer(Modifier.height(20.dp))

                // Hardware & Compatibility Specs Card
                NeoCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    shadowOffset = 3.5.dp,
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Speed,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                    )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "System compatibility",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        if (compatible) "Your device is ready" else "This device is unsupported",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            NeoBadge(
                                text = if (compatible) "Ready" else "Unsupported",
                                containerColor = if (compatible) NeoLime else MaterialTheme.colorScheme.error,
                                contentColor = if (compatible) NeoBlack else Color.White,
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp)

                        SpecRow(
                            icon = Icons.Default.Memory,
                            label = "Memory (RAM)",
                            value = "$totalRamLabel GB usable",
                            statusOk = true,
                        )

                        SpecRow(
                            icon = Icons.Default.Code,
                            label = "Processor",
                            value = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
                            statusOk = arm64,
                        )

                        SpecRow(
                            icon = Icons.Default.Storage,
                            label = "Required download",
                            value = "149–774 MB",
                            statusOk = true,
                        )
                        Text(
                            "Based on the tools you select",
                            modifier = Modifier.padding(start = 26.dp),
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                NeoButton(
                    onClick = { currentStep = 1 },
                    enabled = compatible,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    containerColor = NeoLime,
                    contentColor = NeoBlack,
                ) {
                    Text(
                        text = if (compatible) "Continue to tool setup" else "Device not supported",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = NeoBlack,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "You can change tools later",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    "TOOLCHAIN SETUP",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Choose your tools",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Start lightweight. You can install more toolchains later from Settings.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )

                Spacer(Modifier.height(18.dp))
                NeoCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    shadowOffset = 3.5.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(NeoLime, RoundedCornerShape(10.dp))
                                .border(1.5.dp, NeoBlack, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Terminal, null, tint = NeoBlack, modifier = Modifier.size(19.dp))
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (BuildConfig.OFFLINE_RUNTIME_BUNDLES) "Core runtime · 68.8 MB" else "Core runtime · 68.8 MB download",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                            )
                            Text("Ubuntu  ·  Node.js  ·  npm  ·  Git", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                        Icon(Icons.Default.Check, "Included", tint = NeoLime, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text("CODING AGENT", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.9.sp)
                Spacer(Modifier.height(8.dp))

                NeoCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    shadowOffset = 3.5.dp,
                ) {
                    Column {
                        AgentKind.entries.forEachIndexed { index, agent ->
                            AgentChoiceRow(
                                agent = agent,
                                selected = selectedAgent == agent,
                                onClick = { onSelectAgent(agent) },
                            )
                            if (index != AgentKind.entries.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 62.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                )
                            }
                        }
                    }
                }
                Text(
                    "Only the selected optional agent is downloaded. You can install or switch agents later from Settings.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp),
                )

                Spacer(Modifier.height(18.dp))
                Text("OPTIONAL TOOLCHAINS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.9.sp)
                Spacer(Modifier.height(8.dp))

                NeoCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    shadowOffset = 3.5.dp,
                ) {
                    Column {
                        DevStack.entries.forEachIndexed { index, stack ->
                            DevStackChoiceRow(
                                stack = stack,
                                selected = stack == DevStack.WEB || stack in selectedStacks,
                                locked = stack == DevStack.WEB,
                                onClick = { onToggleStack(stack) },
                            )
                            if (index != DevStack.entries.lastIndex) {
                                HorizontalDivider(modifier = Modifier.padding(start = 62.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storage, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        toolchainDownloadSummary(selectedStacks, selectedAgent),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                }
                Spacer(Modifier.height(12.dp))

                NeoButton(
                    onClick = onDownload,
                    enabled = compatible,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    containerColor = NeoLime,
                    contentColor = NeoBlack,
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = NeoBlack,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (compatible) "Install Mobile Harness" else "Device not supported",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                    if (compatible) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp), tint = NeoBlack)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

private const val CORE_RUNTIME_DOWNLOAD_MB = 69
private const val CLAUDE_RUNTIME_DOWNLOAD_MB = 72
private const val DSH_RUNTIME_DOWNLOAD_MB = 27
private const val AGY_RUNTIME_DOWNLOAD_MB = 40
private const val PYTHON_RUNTIME_DOWNLOAD_MB = 55
private const val ANDROID_RUNTIME_DOWNLOAD_MB = 570

private fun setupTimeEstimate(selected: Set<DevStack>): String {
    var minimumMinutes = 3
    var maximumMinutes = 5
    if (DevStack.PYTHON in selected) {
        minimumMinutes += 1
        maximumMinutes += 2
    }
    if (DevStack.ANDROID in selected) {
        minimumMinutes += 7
        maximumMinutes += 10
    }
    if (DevStack.CPP in selected) {
        minimumMinutes += 3
        maximumMinutes += 5
    }
    if (DevStack.PHP in selected) {
        minimumMinutes += 2
        maximumMinutes += 4
    }
    return "$minimumMinutes–$maximumMinutes minutes"
}

private fun stackDownloadLabel(stack: DevStack): String = when {
    stack == DevStack.WEB -> " · included"
    BuildConfig.OFFLINE_RUNTIME_BUNDLES && stack in setOf(DevStack.PYTHON, DevStack.ANDROID) -> " · included"
    !BuildConfig.OFFLINE_RUNTIME_BUNDLES && stack == DevStack.PYTHON -> " · 55 MB"
    !BuildConfig.OFFLINE_RUNTIME_BUNDLES && stack == DevStack.ANDROID -> " · 570 MB"
    else -> ""
}

private fun toolchainDownloadSummary(selected: Set<DevStack>, agent: AgentKind): String {
    if (BuildConfig.OFFLINE_RUNTIME_BUNDLES) return "All selected bundles are included in this offline app"
    val total = CORE_RUNTIME_DOWNLOAD_MB +
        when (agent) {
            AgentKind.CLAUDE_CODE -> CLAUDE_RUNTIME_DOWNLOAD_MB
            AgentKind.DEEPSEEK_HARNESS -> DSH_RUNTIME_DOWNLOAD_MB
            AgentKind.ANTIGRAVITY -> AGY_RUNTIME_DOWNLOAD_MB
            AgentKind.JCODE -> 18.2f
            AgentKind.PI_AGENT -> 14.5f
            AgentKind.COMMAND_CODE -> 21.0f
            AgentKind.CLINE -> 32.4f
            AgentKind.CUSTOM_RUNNER -> 5.0f
        } +
        (if (DevStack.PYTHON in selected) PYTHON_RUNTIME_DOWNLOAD_MB else 0) +
        (if (DevStack.ANDROID in selected) ANDROID_RUNTIME_DOWNLOAD_MB else 0)
    val laterPackages = selected.intersect(setOf(DevStack.CPP, DevStack.PHP))
    return buildString {
        append("Download: ")
        append(total)
        append(" MB")
        if (laterPackages.isNotEmpty()) append(" · C/PHP packages download later")
        if (total >= 500) append(" · Wi-Fi recommended")
    }
}

@Composable
private fun DevStackChoiceRow(
    stack: DevStack,
    selected: Boolean,
    locked: Boolean,
    onClick: () -> Unit,
) {
    val visuals = getDevStackVisuals(stack)
    val conciseDescription = when (stack) {
        DevStack.WEB -> "Included with the Core runtime"
        DevStack.PYTHON -> "Scripts, automation and backends"
        DevStack.ANDROID -> "Java and Kotlin build tools"
        DevStack.CPP -> "Native apps and command-line tools"
        DevStack.PHP -> "PHP sites and Laravel projects"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
            .clickable(enabled = !locked, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(visuals.icon, null, tint = visuals.accentColor, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stack.label + stackDownloadLabel(stack),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(1.dp))
            Text(conciseDescription, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(21.dp)
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(6.dp))
                .border(
                    1.5.dp,
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(6.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(Icons.Default.Check, "Selected", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun AgentChoiceRow(
    agent: AgentKind,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = when (agent) {
        AgentKind.CLAUDE_CODE -> Color(0xFFD97757)
        AgentKind.DEEPSEEK_HARNESS -> Color(0xFF4D6BFE)
        AgentKind.ANTIGRAVITY -> Color(0xFF4285F4)
        AgentKind.JCODE -> Color(0xFF00C853)
        AgentKind.PI_AGENT -> Color(0xFFFF6D00)
        AgentKind.COMMAND_CODE -> Color(0xFFAA00FF)
        AgentKind.CLINE -> Color(0xFF00B0FF)
        AgentKind.CUSTOM_RUNNER -> Color(0xFFFFAB00)
    }
    val mark = when (agent) {
        AgentKind.CLAUDE_CODE -> "CC"
        AgentKind.DEEPSEEK_HARNESS -> "DS"
        AgentKind.ANTIGRAVITY -> "AG"
        AgentKind.JCODE -> "JC"
        AgentKind.PI_AGENT -> "PI"
        AgentKind.COMMAND_CODE -> "CD"
        AgentKind.CLINE -> "CL"
        AgentKind.CUSTOM_RUNNER -> "CR"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(accent.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(mark, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    agent.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
                if (agent == AgentKind.DEEPSEEK_HARNESS) {
                    Spacer(Modifier.width(7.dp))
                    NeoBadge(
                        text = "RECOMMENDED",
                        color = NeoLime,
                        textColor = NeoBlack,
                    )
                }
            }
            Spacer(Modifier.height(1.dp))
            Text(agent.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(1.dp))
            Text(agent.downloadNote, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(21.dp)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(9.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgentSwitchSheet(
    selected: AgentKind,
    onSelect: (AgentKind) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                "Choose coding agent",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Switch if the current service is unavailable. Your existing login and credentials stay saved.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column {
                    AgentKind.entries.forEachIndexed { index, agent ->
                        AgentChoiceRow(
                            agent = agent,
                            selected = agent == selected,
                            onClick = { onSelect(agent) },
                        )
                        if (index != AgentKind.entries.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 62.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SpecRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    statusOk: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (statusOk) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RuntimeInstallationScreen(
    state: AppUiState,
    themeMode: AppThemeMode = AppThemeMode.DARK,
    onToggleTheme: () -> Unit = {},
) {
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BrandMark(compact = true)
                        Spacer(Modifier.width(9.dp))
                        Text("Set up Mobile Harness", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            if (themeMode == AppThemeMode.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle theme",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            StepDots(0)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "STEP 1 OF 3",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeoLime,
                    letterSpacing = 1.1.sp,
                )
                Spacer(Modifier.weight(1f))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(50),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Shield, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Local setup", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Build your workspace",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth().height(42.dp), contentAlignment = Alignment.CenterStart) {
                Text(
                    state.startupMessage,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(14.dp))
            NeoCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                shadowOffset = 3.5.dp,
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 15.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Installation progress", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text("${(state.startupProgress * 100).toInt()}%", color = NeoLime, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { state.startupProgress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp).neoBorder(1.dp, cornerRadius = 4.dp),
                        color = NeoLime,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth().height(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Estimated ${setupTimeEstimate(state.selectedDevStacks)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp,
                        )
                        Spacer(Modifier.weight(1f))
                        state.startupBytes?.let { (downloaded, total) ->
                            Text(
                                "${formatMegabytes(downloaded)} / ${formatMegabytes(total)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.5.sp,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            SetupLogPanel(
                logs = state.startupLogs.ifEmpty { listOf("$ ${state.startupMessage}") },
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "You can leave Mobile Harness in the background and follow setup from the notification.",
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun StartupLoadingScreen(
    state: AppUiState,
    themeMode: AppThemeMode = AppThemeMode.DARK,
    onToggleTheme: () -> Unit = {},
) {
    val view = LocalView.current
    // Runtime download + install can take 10+ minutes; keep the screen on while this
    // screen is visible. Released automatically when setup finishes or leaves.
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    val messages = remember {
        listOf(
            "Setting up your workspace",
            "Preparing your coding tools",
            "Almost ready",
        )
    }
    var messageIndex by remember(state.startupStage) { mutableIntStateOf(0) }
    LaunchedEffect(messages) {
        while (true) {
            delay(3_000)
            messageIndex = (messageIndex + 1) % messages.size
        }
    }
    val logoTransition = rememberInfiniteTransition(label = "startup logo")
    val logoPulse by logoTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_400),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "startup logo pulse",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BrandMark(
                Modifier.graphicsLayer {
                    scaleX = logoPulse
                    scaleY = logoPulse
                    alpha = 0.82f + ((logoPulse - 0.96f) / 0.08f) * 0.18f
                },
            )
            Spacer(Modifier.height(22.dp))
            AnimatedContent(
                targetState = messages[messageIndex],
                label = "startup message",
            ) { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(14.dp))
            AnimatedThinkingDots(dotColor = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun WorkspaceLaunchExperience(state: AppUiState) {
    val pulseTransition = rememberInfiniteTransition(label = "workspace launch")
    val glow by pulseTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.48f,
        animationSpec = infiniteRepeatable(
            animation = keyframes { durationMillis = 1_400 },
            repeatMode = RepeatMode.Reverse,
        ),
        label = "workspace glow",
    )
    val agentVersion = state.installedAgentVersions[state.agentKind]
    val agentReady = state.startupMessage.contains("ready", ignoreCase = true) || state.startupProgress >= 0.75f

    Text(
        "PRIVATE MOBILE WORKSPACE",
        color = MaterialTheme.colorScheme.primary,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.35.sp,
    )
    Spacer(Modifier.height(10.dp))
    Text(
        "Getting everything ready",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(7.dp))
    Text(
        "Restoring your projects and reconnecting your local coding agent.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 13.sp,
        lineHeight = 19.sp,
    )
    Spacer(Modifier.height(22.dp))

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
        tonalElevation = 3.dp,
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(58.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = glow), RoundedCornerShape(18.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(27.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        state.agentKind.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        agentVersion?.let { "Verified CLI · v$it" } ?: "Connecting local agent",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp,
                    )
                }
                Surface(
                    color = PocketGreen.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, PocketGreen.copy(alpha = 0.3f)),
                ) {
                    Text(
                        if (agentReady) "READY" else "STARTING",
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        color = PocketGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(Modifier.height(18.dp))
            LaunchStatusRow(Icons.Default.Shield, "Private Linux environment", "Verified", complete = true)
            Spacer(Modifier.height(13.dp))
            LaunchStatusRow(Icons.Default.Terminal, state.agentKind.title, if (agentReady) "Ready" else "Connecting", complete = agentReady)
            Spacer(Modifier.height(13.dp))
            LaunchStatusRow(Icons.Default.Folder, "Project workspace", "Restoring", complete = false)
        }
    }

    Spacer(Modifier.height(16.dp))
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(17.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(11.dp))
        Text(
            state.startupMessage,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
    Spacer(Modifier.height(12.dp))
    Text(
        "Runs locally on this device · Your project files stay private",
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        fontSize = 10.5.sp,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun LaunchStatusRow(icon: ImageVector, label: String, status: String, complete: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(28.dp)
                .background(
                    if (complete) PocketGreen.copy(alpha = 0.11f) else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(9.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (complete) Icons.Default.Check else icon,
                contentDescription = null,
                tint = if (complete) PocketGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(label, modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(
            status,
            color = if (complete) PocketGreen else MaterialTheme.colorScheme.primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SetupLogPanel(logs: List<String>) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var followLatest by rememberSaveable { mutableStateOf(true) }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(logs.size, logs.lastOrNull()) {
        if (expanded && followLatest) {
            delay(20)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }
    LaunchedEffect(scrollState.isScrollInProgress) {
        if (!scrollState.isScrollInProgress && expanded) {
            followLatest = scrollState.maxValue - scrollState.value < 32
        }
    }

    NeoCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        cornerRadius = 14.dp,
        shadowOffset = 3.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Terminal,
                    contentDescription = null,
                    tint = NeoLime,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (expanded) "Live setup terminal" else logs.lastOrNull().orEmpty(),
                    modifier = Modifier.weight(1f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse setup details" else "Expand setup details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (expanded) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 170.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    logs.forEach { line ->
                        Text(
                            text = line,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        text = "▌",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (!followLatest) {
                    TextButton(
                        onClick = {
                            followLatest = true
                            scope.launch { scrollState.animateScrollTo(scrollState.maxValue) }
                        },
                        modifier = Modifier.align(Alignment.End),
                    ) { Text("Jump to latest") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartupErrorScreen(
    message: String?,
    isOffline: Boolean,
    logs: List<String>,
    themeMode: AppThemeMode = AppThemeMode.DARK,
    onToggleTheme: () -> Unit = {},
    onRetry: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BrandMark(compact = true)
                        Spacer(Modifier.width(9.dp))
                        Text("Mobile Harness", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            if (themeMode == AppThemeMode.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle theme",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .neoShadow(offsetX = 3.dp, offsetY = 3.dp, cornerRadius = 16.dp)
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .border(2.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Warning, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(20.dp))
            Text(
                if (isOffline) "You're offline" else "Mobile Harness couldn't finish starting",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                message ?: "Please try again.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (logs.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                NeoButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(logs.joinToString("\n")))
                        Toast.makeText(context, "Setup log copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Copy setup logs", fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(24.dp))
            if (isOffline) {
                NeoButton(
                    onClick = {
                        val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            Settings.Panel.ACTION_INTERNET_CONNECTIVITY
                        } else {
                            Settings.ACTION_WIRELESS_SETTINGS
                        }
                        context.startActivity(Intent(action))
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Text("Open internet settings", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                NeoButton(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    containerColor = NeoLime,
                    contentColor = NeoBlack,
                ) {
                    Text("Try again", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            } else {
                NeoButton(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    containerColor = NeoLime,
                    contentColor = NeoBlack,
                ) { Text("Try again", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
            }
        }
    }
}

private fun formatMegabytes(bytes: Long): String = "%.1f MB".format(bytes / 1_048_576.0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RootScreenHost(
    state: AppUiState,
    viewModel: MainViewModel,
    projectsListState: LazyListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() },
) {
    var screen by rememberSaveable { mutableStateOf(RootScreen.PROJECTS) }
    var showQuickTerminal by rememberSaveable { mutableStateOf(false) }
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val terminalLines by viewModel.terminalLines.collectAsStateWithLifecycle()
    val isTerminalRunning by viewModel.isTerminalRunning.collectAsStateWithLifecycle()
    val terminalLiveOutput by viewModel.terminalLiveOutput.collectAsStateWithLifecycle()
    val terminalCurrentCommand by viewModel.terminalCurrentCommand.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!keyboardVisible) {
                val isDark = androidx.compose.foundation.isSystemInDarkTheme()
                val navBorder = if (isDark) NeoDarkBorder else NeoBlack
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.border(BorderStroke(2.dp, navBorder)),
                ) {
                    RootScreen.entries.forEach { tab ->
                        val isSelected = screen == tab
                        val tabScale by androidx.compose.animation.core.animateFloatAsState(
                            targetValue = if (isSelected) 1.08f else 1.0f,
                            animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
                            ),
                            label = "tabScale",
                        )
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { screen = tab },
                            icon = {
                                Icon(
                                    tab.icon,
                                    contentDescription = tab.label,
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = tabScale
                                        scaleY = tabScale
                                    },
                                )
                            },
                            label = {
                                Text(
                                    tab.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeoBlack,
                                selectedTextColor = if (isDark) NeoLime else NeoBlack,
                                indicatorColor = NeoLime,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (screen == RootScreen.PROJECTS && !keyboardVisible && !showQuickTerminal) {
                val isDark = androidx.compose.foundation.isSystemInDarkTheme()
                val fabBorder = if (isDark) NeoDarkBorder else NeoBlack
                ExtendedFloatingActionButton(
                    onClick = { showQuickTerminal = true },
                    icon = { Icon(Icons.Default.Terminal, contentDescription = null, tint = NeoBlack) },
                    text = { Text("Terminal", fontWeight = FontWeight.Bold, color = NeoBlack) },
                    containerColor = NeoLime,
                    contentColor = NeoBlack,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .neoShadow(offsetX = 3.5.dp, offsetY = 3.5.dp, cornerRadius = 14.dp)
                        .border(2.dp, fabBorder, RoundedCornerShape(14.dp)),
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (screen) {
                RootScreen.PROJECTS -> ProjectsScreen(
                    state = state,
                    listState = projectsListState,
                    onOpen = viewModel::openProject,
                    onCreate = viewModel::createProject,
                    onCreateQuickProject = viewModel::createQuickProject,
                    onImportZip = viewModel::importZipProject,
                    onCloneGit = viewModel::clonePublicGitRepository,
                    onStartGitHubLogin = viewModel::startGitHubLogin,
                    onGenerateNewGitHubCode = viewModel::generateNewGitHubCode,
                    onRefreshGitHub = viewModel::refreshGitHubRepositories,
                    onDisconnectGitHub = viewModel::disconnectGitHub,
                    onCloneGitHub = viewModel::cloneGitHubRepository,
                    onRenameProject = viewModel::renameProject,
                    onDeleteProject = viewModel::deleteProject,
                    onSettings = { screen = RootScreen.SETTINGS },
                    onPing = viewModel::pingApi,
                    onToggleTheme = viewModel::toggleTheme,
                    onInstallUpdate = viewModel::installAppUpdate,
                )
                RootScreen.AGENT -> AgentScreen(
                    state = state,
                    onSaveProvider = { profile, key ->
                        viewModel.updateProvider(profile, key)
                    },
                    onDiscoverModels = viewModel::discoverModels,
                    onValidateProvider = viewModel::validateProvider,
                    onPing = viewModel::pingApi,
                    getSavedApiKey = viewModel::getSavedApiKey,
                    getSavedApiKeys = viewModel::getSavedApiKeys,
                    onAddApiKey = viewModel::addApiKey,
                    onActivateApiKey = viewModel::activateApiKey,
                    onRemoveApiKey = viewModel::removeApiKey,
                    onSelectAgent = viewModel::selectAgent,
                    onInstallAgent = viewModel::installAgent,
                    onCheckAgentUpdates = viewModel::checkAgentUpdates,
                    onUpdateAgent = viewModel::updateAgent,
                    onStartAntigravityLogin = viewModel::startAntigravityLogin,
                    onSubmitAntigravityCode = viewModel::submitAntigravityCode,
                    onLogoutAntigravity = viewModel::logoutAntigravity,
                    onRefreshAntigravityModels = viewModel::refreshAntigravityModels,
                    onSetAntigravityModel = viewModel::setAntigravityModel,
                    onSetAntigravityEffort = viewModel::setAntigravityEffort,
                )
                RootScreen.SETTINGS -> SettingsScreen(
                    state = state,
                    onSaveProvider = { profile, key ->
                        viewModel.updateProvider(profile, key)
                    },
                    onDiscoverModels = viewModel::discoverModels,
                    onValidateProvider = viewModel::validateProvider,
                    onSetThemeMode = viewModel::setThemeMode,
                    onPing = viewModel::pingApi,
                    onClearTerminal = viewModel::clearTerminal,
                    getSavedApiKey = viewModel::getSavedApiKey,
                    getSavedApiKeys = viewModel::getSavedApiKeys,
                    onAddApiKey = viewModel::addApiKey,
                    onActivateApiKey = viewModel::activateApiKey,
                    onRemoveApiKey = viewModel::removeApiKey,
                    onInstallDevStack = viewModel::installDevStack,
                    onRemoveDevStack = viewModel::removeDevStack,
                    onInstallAgent = viewModel::installAgent,
                    onCheckAgentUpdates = viewModel::checkAgentUpdates,
                    onUpdateAgent = viewModel::updateAgent,
                    onStartAntigravityLogin = viewModel::startAntigravityLogin,
                    onSubmitAntigravityCode = viewModel::submitAntigravityCode,
                    onLogoutAntigravity = viewModel::logoutAntigravity,
                    onRefreshAntigravityModels = viewModel::refreshAntigravityModels,
                    onSetAntigravityModel = viewModel::setAntigravityModel,
                    onSetAntigravityEffort = viewModel::setAntigravityEffort,
                    initialDebugUpdateManifestUrl = viewModel.debugUpdateManifestUrl(),
                    onSetDebugUpdateManifestUrl = viewModel::setDebugUpdateManifestUrl,
                    onClearDebugUpdateManifestUrl = viewModel::clearDebugUpdateManifestUrl,
                )
            }
        }
    }
    if (showQuickTerminal) {
        QuickTerminalSheet(
            onDismiss = { showQuickTerminal = false },
        ) {
            TerminalScreen(
                lines = terminalLines,
                isRunning = isTerminalRunning,
                onRun = viewModel::runTerminalCommand,
                onInput = viewModel::sendTerminalInput,
                onInterrupt = viewModel::interruptTerminalCommand,
                onClear = viewModel::clearTerminal,
                onToggleTheme = viewModel::toggleTheme,
                themeMode = state.themeMode,
                liveOutput = terminalLiveOutput,
                currentCommand = terminalCurrentCommand,
                showThemeAction = false,
                showQuickCommands = true,
                compactHeader = true,
            )
        }
    }
}

@Composable
private fun QuickTerminalSheet(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    var sheetFraction by rememberSaveable { mutableFloatStateOf(0.45f) }
    BackHandler(onBack = onDismiss)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val keyboardVisible = WindowInsets.ime.getBottom(density) > 0
        LaunchedEffect(keyboardVisible) {
            if (keyboardVisible) sheetFraction = 0.85f
        }
        val dragState = rememberDraggableState { dragAmount ->
            sheetFraction = (sheetFraction - dragAmount / maxHeightPx)
                .coerceIn(0.40f, 0.85f)
        }
        Box(Modifier.fillMaxSize()) {
            // Dimmed backdrop — tap to dismiss.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onDismiss,
                    ),
            )
            // This container consumes the keyboard inset outside the sheet rather
            // than turning it into empty padding inside the terminal.
            Box(Modifier.fillMaxSize().imePadding()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(sheetFraction)
                        .draggable(
                            state = dragState,
                            orientation = Orientation.Vertical,
                            startDragImmediately = false,
                        ),
                    shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 8.dp,
                    shadowElevation = 28.dp,
                ) {
                    Column(Modifier.fillMaxSize()) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier
                                    .width(36.dp)
                                    .height(4.dp)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f), CircleShape),
                            )
                        }
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        )
                        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderSetupScreen(
    initial: ProviderProfile,
    onboarding: Boolean,
    agentKind: AgentKind = AgentKind.CLAUDE_CODE,
    initialStep: Int = if (onboarding) 0 else 1,
    onBack: (() -> Unit)? = null,
    onSave: (ProviderProfile, String) -> Unit,
    onDiscover: suspend (ProviderProfile, String) -> ModelDiscoveryResult,
    onValidate: suspend (ProviderProfile, String, List<DiscoveredModel>) -> ConnectionValidation,
    onSelectAgent: (AgentKind) -> Unit,
    onToggleTheme: (() -> Unit)? = null,
    themeMode: AppThemeMode = AppThemeMode.DARK,
) {
    val context = LocalContext.current
    var step by rememberSaveable { mutableIntStateOf(initialStep) }
    var selected by rememberSaveable { mutableStateOf(initial.kind) }
    var baseUrl by rememberSaveable { mutableStateOf(initial.baseUrl.ifBlank { initial.kind.defaultBaseUrl }) }
    var model by rememberSaveable { mutableStateOf(initial.model.ifBlank { initial.kind.defaultModel }) }
    var dshApi by rememberSaveable { mutableStateOf(initial.dshApi.ifBlank { "anthropic-messages" }) }
    var apiKey by rememberSaveable { mutableStateOf("") }
    var showAgentPicker by rememberSaveable { mutableStateOf(false) }

    if (showAgentPicker) {
        AgentSwitchSheet(
            selected = agentKind,
            onSelect = { agent ->
                showAgentPicker = false
                onSelectAgent(agent)
            },
            onDismiss = { showAgentPicker = false },
        )
    }

    val handleBack: (() -> Unit)? = when {
        step > 1 -> { { step = 1 } }
        !onboarding && onBack != null -> onBack
        else -> null
    }

    if (handleBack != null) {
        BackHandler(onBack = handleBack)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (onboarding) "Set up Mobile Harness" else "AI Provider & Settings") },
                navigationIcon = {
                    if (handleBack != null) {
                        IconButton(onClick = handleBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                },
                actions = {
                    if (onToggleTheme != null) {
                        IconButton(onClick = onToggleTheme) {
                            Icon(
                                if (themeMode == AppThemeMode.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle theme",
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
        ) {
            if (onboarding) StepDots(step)
            Spacer(Modifier.height(16.dp))
            when (step) {
                0 -> DeviceCheckStep(context, onContinue = { step = 1 })
                1 -> ProviderChoiceStep(
                    selected = selected,
                    agentKind = agentKind,
                    onSelected = {
                        if (selected != it) {
                            selected = it
                            baseUrl = it.defaultBaseUrl
                            model = it.defaultModel
                            apiKey = ""
                        }
                    },
                    onContinue = { step = 2 },
                    onChangeAgent = { showAgentPicker = true },
                )
                else -> ProviderCredentialsStep(
                    provider = selected,
                    agentKind = agentKind,
                    baseUrl = baseUrl,
                    model = model,
                    dshApi = dshApi,
                    apiKey = apiKey,
                    onBaseUrl = {
                        baseUrl = it
                        if (agentKind == AgentKind.DEEPSEEK_HARNESS && selected == ProviderKind.CUSTOM) {
                            dshApi = inferredDshApiForUrl(it)
                        }
                    },
                    onModel = { model = it },
                    onDshApi = { dshApi = it },
                    onApiKey = { apiKey = it },
                    hasStoredSecret = initial.kind == selected && initial.hasSecret,
                    onDiscover = {
                        val url = if (selected.fixedBaseUrl) selected.defaultBaseUrl else baseUrl.trim()
                        onDiscover(ProviderProfile(selected, url, model.trim(), dshApi = dshApi), apiKey)
                    },
                    onValidate = { models ->
                        val url = if (selected.fixedBaseUrl) selected.defaultBaseUrl else baseUrl.trim()
                        onValidate(ProviderProfile(selected, url, model.trim(), dshApi = dshApi), apiKey, models)
                    },
                    onSave = {
                        val url = if (selected.fixedBaseUrl) selected.defaultBaseUrl else baseUrl.trim()
                        onSave(ProviderProfile(selected, url, model.trim(), dshApi = dshApi), apiKey)
                    },
                    onChangeAgent = { showAgentPicker = true },
                )
            }
        }
    }
}

@Composable
private fun DshApiProtocolPicker(selected: String, onSelected: (String) -> Unit) {
    val options = listOf("anthropic-messages", "openai-completions", "openai-responses")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Gateway protocol",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
        options.forEach { option ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onSelected(option) }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(19.dp)
                        .border(
                            width = if (selected == option) 2.dp else 1.dp,
                            color = if (selected == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected == option) Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                }
                Spacer(Modifier.width(10.dp))
                Text(option, fontSize = 13.sp)
            }
        }
        Text(
            "Pick the protocol your gateway speaks; DeepSeek Harness routes it directly.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun StepDots(step: Int) {
    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) NeoDarkBorder else NeoBlack
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { index ->
            val active = index <= step
            Box(
                Modifier
                    .height(8.dp)
                    .weight(1f)
                    .neoShadow(offsetX = 1.5.dp, offsetY = 1.5.dp, cornerRadius = 4.dp)
                    .background(if (active) NeoLime else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                    .border(1.5.dp, if (active) borderColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(4.dp)),
            )
        }
    }
}

@Composable
private fun DeviceCheckStep(context: Context, onContinue: () -> Unit) {
    val activityManager = context.getSystemService(ActivityManager::class.java)
    val memoryInfo = remember { ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo) }
    val totalRamGb = memoryInfo.totalMem.toDouble() / 1_073_741_824.0
    val totalRamLabel = String.format(java.util.Locale.US, "%.1f", totalRamGb)
    val arm64 = supportsArm64Runtime(Build.SUPPORTED_ABIS, System.getProperty("os.arch"))
    val compatible = arm64
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        BrandMark()
        Text("Your phone is the workspace", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Mobile Harness checks compatibility before downloading the private Linux runtime.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        CheckRow(Icons.Default.Memory, "Memory", "$totalRamLabel GB usable · ${if (totalRamGb >= 7.5) "Full mode" else "Lite mode"}", true)
        CheckRow(Icons.Default.Code, "Processor", Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown", arm64)
        CheckRow(Icons.Default.Storage, "Android", "Android ${Build.VERSION.RELEASE}", true)
        NeoCard(
            cornerRadius = 14.dp,
            shadowOffset = 3.dp,
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ) {
            Text(
                "Only open projects you trust. The local Linux environment is a compatibility layer, not a hardened security sandbox.",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        NeoButton(
            onClick = onContinue,
            enabled = compatible,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            containerColor = NeoLime,
            contentColor = NeoBlack,
        ) {
            Text(if (compatible) "Continue" else "This device is not supported", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun CheckRow(icon: ImageVector, title: String, value: String, passed: Boolean) {
    val isDark = isSystemInDarkTheme()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .neoShadow(offsetX = 2.dp, offsetY = 2.dp, cornerRadius = 12.dp)
                .background(if (passed) NeoLime.copy(alpha = 0.18f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .border(1.5.dp, if (passed) NeoLime else MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                null,
                Modifier.size(22.dp),
                tint = if (passed) (if (isDark) NeoLime else NeoBlack) else MaterialTheme.colorScheme.error,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        Icon(if (passed) Icons.Default.Check else Icons.Default.Warning, null, tint = if (passed) NeoLime else MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun ProviderChoiceStep(
    selected: ProviderKind,
    agentKind: AgentKind,
    onSelected: (ProviderKind) -> Unit,
    onContinue: () -> Unit,
    onChangeAgent: () -> Unit,
) {
    val visibleProviders = remember(agentKind) { providersForAgent(agentKind) }
    Column(Modifier.fillMaxHeight()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "STEP 2 OF 3",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = if (isSystemInDarkTheme()) NeoLime else NeoBlack,
                letterSpacing = 1.1.sp,
            )
            Spacer(Modifier.weight(1f))
            NeoBadge(text = "Secure setup", containerColor = NeoLime, contentColor = NeoBlack)
        }
        Spacer(Modifier.height(10.dp))
        Text("Connect your AI", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Choose how Mobile Harness should access your coding model.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(12.dp))

        NeoCard(
            modifier = Modifier.fillMaxWidth().weight(1f),
            cornerRadius = 16.dp,
            shadowOffset = 3.5.dp,
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(visibleProviders) { index, provider ->
                    ProviderChoiceRow(
                        provider = provider,
                        selected = selected == provider,
                        onClick = { onSelected(provider) },
                    )
                    if (index != visibleProviders.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 68.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.padding(top = 12.dp, start = 2.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Key, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(7.dp))
            Text(
                "API keys are encrypted in Android secure storage.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )
        }
        NeoButton(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(52.dp),
            containerColor = NeoLime,
            contentColor = NeoBlack,
        ) {
            Text("Continue", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp), tint = NeoBlack)
        }
        TextButton(
            onClick = onChangeAgent,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 6.dp),
        ) {
            Text("Use another coding agent", fontSize = 12.sp)
        }
    }
}

@Composable
private fun ProviderChoiceRow(
    provider: ProviderKind,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = when (provider) {
        ProviderKind.CLAUDE -> Color(0xFFD97757)
        ProviderKind.ANTHROPIC -> Color(0xFFE7A26D)
        ProviderKind.LLM_ROUTER -> Color(0xFF5B8DEF)
        ProviderKind.DEEPSEEK -> Color(0xFF4D6BFE)
        ProviderKind.KIMI -> Color(0xFF8B7CF6)
        ProviderKind.OPENCODE_ZEN -> Color(0xFF22C55E)
        ProviderKind.NVIDIA_NIM -> Color(0xFF76B900)
        ProviderKind.CUSTOM -> NeoLime
    }
    val mark = when (provider) {
        ProviderKind.CLAUDE -> "C"
        ProviderKind.ANTHROPIC -> "A"
        ProviderKind.LLM_ROUTER -> "OR"
        ProviderKind.DEEPSEEK -> "DS"
        ProviderKind.KIMI -> "K"
        ProviderKind.OPENCODE_ZEN -> "Z"
        ProviderKind.NVIDIA_NIM -> "NV"
        ProviderKind.CUSTOM -> "<>"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) NeoLime.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(accent.copy(alpha = 0.18f), RoundedCornerShape(9.dp))
                .border(1.5.dp, accent, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(mark, color = if (provider == ProviderKind.CUSTOM && !isSystemInDarkTheme()) NeoBlack else accent, fontSize = if (mark.length > 1) 9.sp else 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    provider.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (provider.experimental) {
                    Spacer(Modifier.width(6.dp))
                    NeoBadge(text = "Beta", containerColor = NeoLime, contentColor = NeoBlack)
                }
            }
            Spacer(Modifier.height(1.dp))
            Text(
                provider.subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(20.dp)
                .border(
                    width = if (selected) 2.dp else 1.5.dp,
                    color = if (selected) NeoLime else MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(10.dp).background(NeoLime, CircleShape))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderCredentialsStep(
    provider: ProviderKind,
    agentKind: AgentKind = AgentKind.CLAUDE_CODE,
    baseUrl: String,
    model: String,
    dshApi: String = "anthropic-messages",
    apiKey: String,
    onBaseUrl: (String) -> Unit,
    onModel: (String) -> Unit,
    onDshApi: (String) -> Unit = {},
    onApiKey: (String) -> Unit,
    hasStoredSecret: Boolean,
    onDiscover: suspend () -> ModelDiscoveryResult,
    onValidate: suspend (List<DiscoveredModel>) -> ConnectionValidation,
    onSave: () -> Unit,
    onChangeAgent: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var models by remember(baseUrl) { mutableStateOf(emptyList<DiscoveredModel>()) }
    var isDiscovering by remember { mutableStateOf(false) }
    var isValidating by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var statusDetails by remember { mutableStateOf<String?>(null) }
    var statusOk by remember { mutableStateOf(false) }
    var showModels by rememberSaveable { mutableStateOf(false) }
    var modelSearch by rememberSaveable { mutableStateOf("") }
    val modelSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val hasKey = apiKey.isNotBlank() || hasStoredSecret
    val filteredModels = remember(models, modelSearch) {
        val query = modelSearch.trim()
        if (query.isEmpty()) models else models.filter {
            it.id.contains(query, ignoreCase = true) || it.displayName.contains(query, ignoreCase = true)
        }
    }

    if (provider == ProviderKind.CLAUDE) {
        ClaudeSubscriptionCredentialsStep(
            token = apiKey,
            hasStoredToken = hasStoredSecret,
            onToken = onApiKey,
            onSave = onSave,
            onChangeAgent = onChangeAgent,
        )
        return
    }

    fun discoverModels(openWhenReady: Boolean = true) {
        scope.launch {
            isDiscovering = true
            status = null
            statusDetails = null
            when (val result = onDiscover()) {
                is ModelDiscoveryResult.Success -> {
                    models = result.models
                    statusOk = true
                    status = "Found ${result.models.size} available model${if (result.models.size == 1) "" else "s"}."
                    if (model.isBlank() && result.models.isNotEmpty()) onModel(result.models.first().id)
                    if (openWhenReady && result.models.isNotEmpty()) showModels = true
                }
                is ModelDiscoveryResult.Failure -> {
                    statusOk = false
                    status = result.message
                    statusDetails = result.providerMessage
                }
            }
            isDiscovering = false
        }
    }

    if (showModels) {
        ModalBottomSheet(
            onDismissRequest = { showModels = false },
            sheetState = modelSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.82f).padding(horizontal = 20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Available models", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "${filteredModels.size} of ${models.size} models",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                        )
                    }
                    IconButton(
                        onClick = { discoverModels(openWhenReady = false) },
                        enabled = !isDiscovering,
                    ) {
                        if (isDiscovering) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Refresh, "Refresh models")
                    }
                }
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = modelSearch,
                    onValueChange = { modelSearch = it },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    placeholder = { Text("Search model name or ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                if (filteredModels.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No matching models", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        items(filteredModels, key = { it.id }) { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onModel(option.id)
                                        status = null
                                        modelSearch = ""
                                        showModels = false
                                    }
                                    .padding(vertical = 14.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(option.displayName, modifier = Modifier.weight(1f, fill = false), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (option.isFree) Text("  FREE", color = Color(0xFF58C99C), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    if (option.displayName != option.id) {
                                        Text(option.id, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                val isDark = isSystemInDarkTheme()
                                val borderColor = if (isDark) NeoDarkBorder else NeoBlack
                                Box(
                                    Modifier.size(20.dp).border(
                                        if (model == option.id) 2.dp else 1.5.dp,
                                        if (model == option.id) NeoLime else borderColor.copy(alpha = 0.6f),
                                        CircleShape,
                                    ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (model == option.id) Box(Modifier.size(9.dp).background(NeoLime, CircleShape))
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                        }
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("STEP 3 OF 3", color = NeoLime, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.weight(1f))
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(6.dp))
                        Text("Encrypted locally", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(provider.title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(
                when {
                    agentKind == AgentKind.DEEPSEEK_HARNESS -> "DeepSeek Harness will connect through this API endpoint."
                    provider.protocol.name.startsWith("OPENAI") -> "Mobile Harness will translate Claude Code requests for this provider."
                    else -> "Claude Code will connect through this API endpoint."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            NeoCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 18.dp,
                shadowOffset = 3.5.dp,
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        baseUrl,
                        { onBaseUrl(it); status = null; statusDetails = null; models = emptyList() },
                        label = { Text("Base URL") },
                        supportingText = {
                            if (provider.fixedBaseUrl) Text("Fixed by ${provider.title}")
                        },
                        readOnly = provider.fixedBaseUrl,
                        enabled = !provider.fixedBaseUrl,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (agentKind == AgentKind.DEEPSEEK_HARNESS && provider in DSH_PROTOCOL_PROVIDERS && !provider.fixedProtocol) {
                        DshApiProtocolPicker(selected = dshApi, onSelected = { onDshApi(it); status = null })
                    }
                    OutlinedTextField(
                        apiKey,
                        { onApiKey(it); status = null; statusDetails = null },
                        label = { Text("API key") },
                        placeholder = { Text(if (hasStoredSecret) "Saved securely — leave blank to keep it" else "Enter your API key") },
                        supportingText = {
                            if (hasStoredSecret && apiKey.isBlank()) Text("A saved key is ready to use")
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        model,
                        { onModel(it); status = null; statusDetails = null },
                        label = { Text("Model name") },
                        supportingText = { Text("Select an available model or enter an exact model ID.") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        item {
            NeoButton(
                onClick = {
                    if (models.isEmpty()) discoverModels() else showModels = true
                },
                enabled = baseUrl.isNotBlank() && hasKey && !isDiscovering && !isValidating,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                buttonColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                if (isDiscovering) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = NeoLime)
                    Spacer(Modifier.width(8.dp))
                }
                Icon(if (models.isEmpty()) Icons.Default.Search else Icons.Default.KeyboardArrowDown, null, Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (models.isEmpty()) "Find available models" else "Available models (${models.size})", fontWeight = FontWeight.Bold)
            }
        }
        if (status != null) {
            item {
                Text(
                    status.orEmpty(),
                    color = if (statusOk) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                )
                statusDetails?.takeIf(String::isNotBlank)?.let { details ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        details,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                    )
                }
            }
        }
        item {
            NeoButton(
                onClick = {
                    scope.launch {
                        isValidating = true
                        status = "Checking API key, model, and Claude Code settings…"
                        statusDetails = null
                        statusOk = true
                        when (val result = onValidate(models)) {
                            is ConnectionValidation.Success -> {
                                status = result.message
                                statusOk = true
                                onSave()
                            }
                            is ConnectionValidation.Failure -> {
                                status = result.message
                                statusDetails = result.providerMessage
                                statusOk = false
                            }
                        }
                        isValidating = false
                    }
                },
                enabled = baseUrl.isNotBlank() && model.isNotBlank() && hasKey && !isDiscovering && !isValidating,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                containerColor = NeoLime,
                contentColor = NeoBlack,
            ) {
                if (isValidating) {
                    CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp, color = NeoBlack)
                    Spacer(Modifier.width(7.dp))
                }
                Text(if (isValidating) "Checking" else "Continue", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
        item {
            TextButton(
                onClick = onChangeAgent,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Use another coding agent", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ClaudeSubscriptionCredentialsStep(
    token: String,
    hasStoredToken: Boolean,
    onToken: (String) -> Unit,
    onSave: () -> Unit,
    onChangeAgent: () -> Unit,
) {
    var tokenVisible by rememberSaveable { mutableStateOf(false) }
    val hasToken = token.isNotBlank() || hasStoredToken

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("STEP 3 OF 3", color = if (isSystemInDarkTheme()) NeoLime else NeoBlack, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Spacer(Modifier.weight(1f))
                NeoBadge(text = "Encrypted locally", containerColor = NeoLime, contentColor = NeoBlack)
            }
            Spacer(Modifier.height(16.dp))
            Text("Claude subscription", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Connect a Claude Pro, Max, Team, or Enterprise subscription to Claude Code.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            NeoCard(
                cornerRadius = 16.dp,
                shadowOffset = 3.5.dp,
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("1. On a computer where Claude Code is installed, run:", fontSize = 13.sp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F141A), RoundedCornerShape(10.dp))
                            .border(1.5.dp, NeoLime, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                    ) {
                        Text(
                            "claude setup-token",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = NeoLime,
                        )
                    }
                    Text("2. Sign in to Claude and paste the generated token here.", fontSize = 13.sp)
                    OutlinedTextField(
                        value = token,
                        onValueChange = onToken,
                        label = { Text("Claude setup token") },
                        placeholder = { Text(if (hasStoredToken) "Saved securely — leave blank to keep it" else "Paste token") },
                        supportingText = if (hasStoredToken && token.isBlank()) ({ Text("A saved subscription token is ready to use") }) else null,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { tokenVisible = !tokenVisible }) {
                                Icon(if (tokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Toggle token visibility")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        item {
            NeoButton(
                onClick = onSave,
                enabled = hasToken,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                containerColor = NeoLime,
                contentColor = NeoBlack,
            ) {
                Text("Save and continue", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
        item {
            TextButton(onClick = onChangeAgent, modifier = Modifier.fillMaxWidth()) {
                Text("Use another coding agent", fontSize = 12.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectsScreen(
    state: AppUiState,
    listState: LazyListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() },
    onOpen: (Project) -> Unit,
    onCreate: (String) -> Unit,
    onCreateQuickProject: () -> Unit,
    onImportZip: (Uri) -> Unit,
    onCloneGit: (String) -> Unit,
    onStartGitHubLogin: () -> Unit,
    onGenerateNewGitHubCode: () -> Unit,
    onRefreshGitHub: () -> Unit,
    onDisconnectGitHub: () -> Unit,
    onCloneGitHub: (GitHubRepository) -> Unit,
    onRenameProject: (String, String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onSettings: () -> Unit,
    onPing: () -> Unit,
    onToggleTheme: () -> Unit,
    onInstallUpdate: () -> Unit,
) {
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var showUpdateDialog by rememberSaveable { mutableStateOf(false) }
    var showGitDialog by rememberSaveable { mutableStateOf(false) }
    var showGitHubDialog by rememberSaveable { mutableStateOf(false) }
    var importExpanded by rememberSaveable { mutableStateOf(false) }
    var gitUrl by rememberSaveable { mutableStateOf("") }
    var repositorySearch by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    val projects = state.projects
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        onInstallUpdate()
    }
    val importZipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onImportZip(uri)
    }
    LaunchedEffect(state.appUpdate?.versionCode) {
        if (state.appUpdate != null) showUpdateDialog = true
    }
    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = 8.dp),
                title = { Row(verticalAlignment = Alignment.CenterVertically) { BrandMark(compact = true); Spacer(Modifier.width(9.dp)); Text("Mobile Harness", fontWeight = FontWeight.Bold) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Build from your phone", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Chat, review changes, and preview your project.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NeoButton(
                        onClick = onCreateQuickProject,
                        modifier = Modifier.weight(1f).height(48.dp),
                        containerColor = NeoLime,
                        contentColor = NeoBlack,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                            tint = NeoBlack,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Quick project",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            softWrap = false,
                            color = NeoBlack,
                        )
                    }
                    NeoButton(
                        onClick = { showCreate = true },
                        modifier = Modifier.weight(1f).height(48.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "New project",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                val isImportExpanded = importExpanded || state.projectImporting || state.gitCloneRunning
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { importExpanded = !importExpanded }
                                .padding(horizontal = 14.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Bring an existing project", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    if (isImportExpanded) "Import files or clone complete Git history" else "ZIP file, Git repository, or GitHub",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(
                                imageVector = if (isImportExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isImportExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        AnimatedVisibility(visible = isImportExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                    ImportSourceButton(
                                        icon = Icons.Default.Download,
                                        title = if (state.projectImporting) "Importing…" else "ZIP file",
                                        enabled = !state.projectImporting && !state.gitCloneRunning,
                                        modifier = Modifier.weight(1f),
                                        onClick = { importZipLauncher.launch("*/*") },
                                        loading = state.projectImporting,
                                    )
                                    ImportSourceButton(
                                        icon = Icons.Default.Code,
                                        title = if (state.gitCloneRunning) "Cloning…" else "Git URL",
                                        enabled = !state.projectImporting && !state.gitCloneRunning,
                                        modifier = Modifier.weight(1f),
                                        onClick = { showGitDialog = true },
                                        loading = state.gitCloneRunning,
                                    )
                                }
                                Surface(
                                    modifier = Modifier.fillMaxWidth().clickable(enabled = !state.gitCloneRunning) {
                                        showGitHubDialog = true
                                        if (state.githubAuthStatus == GitHubAuthStatus.CONNECTED && state.githubRepositories.isEmpty()) onRefreshGitHub()
                                    },
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(13.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                ) {
                                    Row(Modifier.padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Code, null, tint = NeoLime, modifier = Modifier.size(19.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                state.githubLogin?.let { "GitHub · @$it" } ?: "Connect GitHub",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                            Text(
                                                if (state.githubLogin != null) "Browse public and private repositories" else "Sign in to access your repositories",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                (state.projectImportMessage ?: state.gitCloneMessage)?.let { message ->
                                    Text(message, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
            state.appUpdate?.let { update ->
                item {
                    val isDark = isSystemInDarkTheme()
                    val borderColor = if (isDark) NeoDarkBorder else NeoBlack
                    NeoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neoTactile(shadowOffset = 3.dp) { showUpdateDialog = true },
                        cornerRadius = 16.dp,
                        shadowOffset = 3.dp,
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(NeoLime, RoundedCornerShape(12.dp))
                                    .border(1.5.dp, borderColor, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.Download, null, tint = NeoBlack)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Mobile Harness ${update.versionName}", fontWeight = FontWeight.Bold)
                                Text("A new update is ready", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            NeoBadge(
                                text = "UPDATE",
                                color = NeoLime,
                                textColor = NeoBlack,
                            )
                        }
                    }
                }
            }
            item { Text("Your projects", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            if (projects.isEmpty()) {
                item {
                    val isDark = isSystemInDarkTheme()
                    val borderColor = if (isDark) NeoDarkBorder else NeoBlack
                    NeoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        cornerRadius = 18.dp,
                        shadowOffset = 3.5.dp,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(NeoLime, RoundedCornerShape(16.dp))
                                    .border(2.dp, borderColor, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = NeoBlack,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "No projects yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                "Create a named project or start instantly with a Quick Project.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp,
                            )
                        }
                    }
                }
            } else {
                items(projects, key = { it.id }) { project ->
                    ProjectCard(
                        project = project,
                        taskRunning = state.isRunning && state.activeProject?.id == project.id,
                        terminalRunning = state.projectTerminalRunning && state.activeProject?.id == project.id,
                        onOpen = { onOpen(project) },
                        onRename = { onRenameProject(project.id, it) },
                        onDelete = { onDeleteProject(project.id) },
                    )
                }
            }
        }
    }
    if (showCreate) AlertDialog(
        onDismissRequest = { showCreate = false },
        title = { Text("Create a starter project") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Project name") }, singleLine = true)
                if (name.isNotBlank()) {
                    Text(
                        "Terminal folder: /workspace/${projectSlug(name)}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { onCreate(name); showCreate = false; name = "" }, enabled = name.isNotBlank()) { Text("Create") } },
        dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } },
    )
    if (showGitDialog) AlertDialog(
        onDismissRequest = { if (!state.gitCloneRunning) showGitDialog = false },
        icon = { Icon(Icons.Default.Code, null, tint = NeoLime) },
        title = { Text("Clone Git repository") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Paste a public HTTPS repository URL. Its complete Git history and current branch will be kept.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.5.sp)
                OutlinedTextField(
                    value = gitUrl,
                    onValueChange = { gitUrl = it },
                    label = { Text("HTTPS Git URL") },
                    placeholder = { Text("https://github.com/owner/repository.git") },
                    singleLine = true,
                )
                state.gitCloneMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            NeoButton(
                enabled = gitUrl.isNotBlank() && !state.gitCloneRunning,
                buttonColor = NeoLime,
                contentColor = NeoBlack,
                onClick = { onCloneGit(gitUrl); showGitDialog = false; gitUrl = "" },
            ) { Text(if (state.gitCloneRunning) "Cloning…" else "Clone project", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = { showGitDialog = false }, enabled = !state.gitCloneRunning) { Text("Cancel") } },
    )
    if (showGitHubDialog) {
        val clipboard = LocalClipboardManager.current
        val filteredRepositories = state.githubRepositories.filter { repository ->
            repositorySearch.isBlank() || repository.fullName.contains(repositorySearch, ignoreCase = true)
        }
        AlertDialog(
            onDismissRequest = { if (!state.gitCloneRunning) showGitHubDialog = false },
            icon = { Icon(Icons.Default.Code, null, tint = NeoLime) },
            title = { Text(state.githubLogin?.let { "GitHub · @$it" } ?: "Connect GitHub") },
            text = {
                when (state.githubAuthStatus) {
                    GitHubAuthStatus.DISCONNECTED, GitHubAuthStatus.ERROR -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            state.githubMessage ?: "Sign in with GitHub's official CLI to browse public and private repositories.",
                            color = if (state.githubAuthStatus == GitHubAuthStatus.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                        )
                        NeoButton(onClick = onStartGitHubLogin, buttonColor = NeoLime, contentColor = NeoBlack, modifier = Modifier.fillMaxWidth()) { Text("Sign in with GitHub", fontWeight = FontWeight.Bold) }
                    }
                    GitHubAuthStatus.STARTING -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.5.dp, color = NeoLime)
                        Spacer(Modifier.height(12.dp))
                        Text(state.githubMessage ?: "Starting GitHub sign-in…")
                    }
                    GitHubAuthStatus.AWAITING_USER -> Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Enter this one-time code in the GitHub page opened in your browser.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { state.githubUserCode?.let { clipboard.setText(AnnotatedString(it)) } },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                state.githubUserCode.orEmpty(),
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                letterSpacing = 2.sp,
                            )
                        }
                        Text("Tap the code to copy it. PocketDev will connect automatically after approval.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        NeoButton(
                            onClick = onGenerateNewGitHubCode,
                            buttonColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Generate new code", fontWeight = FontWeight.Bold)
                        }
                    }
                    GitHubAuthStatus.CONNECTED -> Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(state.githubMessage ?: "Select a repository", modifier = Modifier.weight(1f), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            IconButton(onClick = onRefreshGitHub, enabled = !state.githubRepositoriesLoading) {
                                Icon(Icons.Default.Refresh, "Refresh repositories")
                            }
                        }
                        OutlinedTextField(
                            value = repositorySearch,
                            onValueChange = { repositorySearch = it },
                            placeholder = { Text("Search repositories") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (state.githubRepositoriesLoading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = NeoLime)
                        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 350.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(filteredRepositories, key = { it.fullName }) { repository ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth().clickable(enabled = !state.gitCloneRunning) {
                                        showGitHubDialog = false
                                        onCloneGitHub(repository)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                ) {
                                    Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(if (repository.private) Icons.Default.Key else Icons.Default.Code, null, modifier = Modifier.size(17.dp), tint = NeoLime)
                                        Spacer(Modifier.width(9.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(repository.fullName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${if (repository.private) "Private" else "Public"} · ${repository.defaultBranch}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (state.githubAuthStatus == GitHubAuthStatus.CONNECTED) {
                    TextButton(onClick = { showGitHubDialog = false }) { Text("Close") }
                }
            },
            dismissButton = {
                if (state.githubAuthStatus == GitHubAuthStatus.CONNECTED) {
                    TextButton(onClick = { onDisconnectGitHub(); showGitHubDialog = false }) { Text("Disconnect") }
                } else if (state.githubAuthStatus != GitHubAuthStatus.STARTING) {
                    TextButton(onClick = { showGitHubDialog = false }) { Text("Cancel") }
                }
            },
        )
    }
    val update = state.appUpdate
    if (showUpdateDialog && update != null) {
        val canInstall = Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()
        val downloading = state.appUpdateStatus == AppUpdateStatus.DOWNLOADING
        val installing = state.appUpdateStatus == AppUpdateStatus.INSTALLING
        val total = state.appUpdateTotalBytes
        val downloaded = state.appUpdateDownloadedBytes
        val progress = if (total > 0) (downloaded.toFloat() / total).coerceIn(0f, 1f) else 0f
        AlertDialog(
            onDismissRequest = { if (!installing) showUpdateDialog = false },
            icon = { Icon(Icons.Default.Download, null, tint = NeoLime, modifier = Modifier.size(34.dp)) },
            title = { Text("Update to ${update.versionName}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(update.notes.ifBlank { "Get the latest improvements and fixes for Mobile Harness." })
                    if (update.sizeBytes > 0) Text("Download size: ${formatMegabytes(update.sizeBytes)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    if (!canInstall) {
                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f)) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Allow ‘Install unknown apps’ for Mobile Harness. Without this permission, Android will not install the update.", fontSize = 13.sp)
                            }
                        }
                    }
                    if (downloading) {
                        if (total > 0) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = NeoLime)
                        else LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = NeoLime)
                        Text(
                            if (total > 0) "Downloading ${formatMegabytes(downloaded)} / ${formatMegabytes(total)} · ${(progress * 100).toInt()}%" else "Downloading ${formatMegabytes(downloaded)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (installing) Text("Download verified. Opening Android installer…", color = PocketGreen, fontSize = 13.sp)
                    state.appUpdateError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
                }
            },
            confirmButton = {
                NeoButton(
                    enabled = !downloading && !installing,
                    buttonColor = NeoLime,
                    contentColor = NeoBlack,
                    onClick = {
                        if (!canInstall && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            permissionLauncher.launch(
                                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")),
                            )
                        } else {
                            onInstallUpdate()
                        }
                    },
                ) {
                    Text(when { !canInstall -> "Grant permission"; downloading -> "Downloading…"; installing -> "Installing…"; else -> "Download and install" }, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { if (!installing) TextButton(onClick = { showUpdateDialog = false }) { Text("Later") } },
        )
    }
}

@Composable
private fun ImportSourceButton(
    icon: ImageVector,
    title: String,
    enabled: Boolean,
    loading: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) NeoDarkBorder else NeoBlack
    Box(
        modifier = modifier
            .neoShadow(offsetX = 2.dp, offsetY = 2.dp, cornerRadius = 12.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .neoTactile(enabled = enabled && !loading, cornerRadius = 12.dp, shadowOffset = 2.dp, onClick = onClick)
            .height(46.dp)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (loading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = NeoLime)
            else Icon(icon, null, Modifier.size(17.dp), tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(7.dp))
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun ApiStatusChip(state: AppUiState, onSettings: () -> Unit, onPing: () -> Unit) {
    val dotColor = when (state.apiPingStatus) {
        ApiPingStatus.OK -> PocketGreen
        ApiPingStatus.FAILED -> MaterialTheme.colorScheme.error
        ApiPingStatus.PINGING -> NeoLime
        ApiPingStatus.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }
    val providerLabel = when {
        state.agentKind == AgentKind.ANTIGRAVITY ->
            state.antigravityModel.ifBlank { state.agentKind.title }
        state.provider.model.isNotBlank() -> state.provider.model
        state.provider.baseUrl.isNotBlank() -> {
            runCatching { java.net.URI(state.provider.baseUrl).host ?: state.provider.kind.title }
                .getOrDefault(state.provider.kind.title)
        }
        else -> state.provider.kind.title
    }

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    Box(
        modifier = Modifier
            .neoTactile(
                shadowOffset = 2.5.dp,
                pressedOffset = 0.8.dp,
                cornerRadius = 50.dp,
                onClick = onSettings,
            )
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(50))
            .border(1.5.dp, if (isDark) NeoDarkBorder else NeoBlack, RoundedCornerShape(50)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 5.dp, bottom = 5.dp),
        ) {
            // Status dot with live pulse when active
            if (state.apiPingStatus == ApiPingStatus.OK || state.apiPingStatus == ApiPingStatus.PINGING) {
                PulsingDot(color = dotColor, size = 7.dp)
            } else {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(dotColor, CircleShape),
                )
            }
            Spacer(Modifier.width(8.dp))
            // Model / provider name
            Text(
                text = providerLabel,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Spacer(Modifier.width(4.dp))
            // Ping button
            IconButton(
                onClick = onPing,
                modifier = Modifier.size(28.dp),
            ) {
                if (state.apiPingStatus == ApiPingStatus.PINGING) {
                    CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = NeoLime)
                } else {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Ping API",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}



@Composable
private fun ProjectCard(
    project: Project,
    taskRunning: Boolean,
    terminalRunning: Boolean,
    onOpen: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by rememberSaveable(project.id) { mutableStateOf(false) }
    var showRename by rememberSaveable(project.id) { mutableStateOf(false) }
    var showDelete by rememberSaveable(project.id) { mutableStateOf(false) }
    var renameText by rememberSaveable(project.id) { mutableStateOf(project.name) }
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp)
            .neoTactile(
                shadowOffset = 3.5.dp,
                pressedOffset = 1.dp,
                cornerRadius = 14.dp,
                onClick = onOpen,
            )
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .border(2.dp, if (isDark) NeoDarkBorder else NeoBlack, RoundedCornerShape(14.dp)),
    ) {
        Row(Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(NeoLime.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                    .border(1.5.dp, NeoLime, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Folder, null, Modifier.size(24.dp), tint = NeoLime)
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        project.name,
                        modifier = Modifier.weight(1f, fill = false),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (taskRunning || terminalRunning) {
                        Spacer(Modifier.width(8.dp))
                        PulsingDot(color = NeoLime, size = 6.dp)
                        Spacer(Modifier.width(5.dp))
                        Text(
                            if (taskRunning) "Task running" else "Terminal running",
                            color = NeoLime,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Text(
                    if (project.kind == ProjectKind.QUICK_PROJECT) "Quick project" else project.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
                Text("/workspace/${project.slug}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text("${project.language} · ${project.formattedUpdatedAt}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
            Box {
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, "Project options") }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename project") },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        onClick = { menuOpen = false; renameText = project.name; showRename = true },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete project") },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { menuOpen = false; showDelete = true },
                    )
                }
            }
        }
    }
    if (showRename) {
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("Rename project") },
            text = { OutlinedTextField(renameText, { renameText = it }, label = { Text("Project name") }, singleLine = true) },
            confirmButton = { TextButton(onClick = { onRename(renameText); showRename = false }, enabled = renameText.isNotBlank()) { Text("Save") } },
            dismissButton = { TextButton(onClick = { showRename = false }) { Text("Cancel") } },
        )
    }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete this project?") },
            text = { Text("Its chats, files, attachments, changes, and terminal history will be permanently removed.") },
            confirmButton = { TextButton(onClick = { onDelete(); showDelete = false }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadOnlyProjectScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onSwitchChat: (String) -> Unit,
    onContinueHere: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val project = state.readOnlyProject ?: return
    val activeChat = state.readOnlyProjectChats.firstOrNull { it.id == state.readOnlyChatId }
    val listState = rememberLazyListState()
    var showChats by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.readOnlyChatId) {
        if (state.readOnlyMessages.isNotEmpty()) listState.scrollToItem(state.readOnlyMessages.lastIndex)
    }

    if (showChats) {
        ChatSwitcherDialog(
            chats = state.readOnlyProjectChats,
            activeChatId = state.readOnlyChatId,
            switchingEnabled = true,
            allowCreate = false,
            onDismiss = { showChats = false },
            onCreate = {},
            onSwitch = { chatId ->
                onSwitchChat(chatId)
                showChats = false
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(project.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${activeChat?.title ?: "Chat"} · History",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Projects") }
                },
                actions = {
                    IconButton(onClick = { showChats = true }) { Icon(Icons.Default.History, "Project chats") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            ChatTab(
                messages = state.readOnlyMessages,
                approval = null,
                liveProcess = emptyList(),
                isRunning = false,
                onSend = {},
                onStop = {},
                onApproval = {},
                listState = listState,
                taskStartedAtMillis = null,
                taskFinishedAtMillis = null,
                thinkingActive = false,
                agentKind = state.agentKind,
                pendingAttachments = emptyList(),
                onAttach = {},
                onRemoveAttachment = {},
                onOpenAttachment = {},
                onRunInTerminal = {},
                readOnly = true,
                readOnlyBlocked = state.isRunning || state.projectTerminalRunning,
                onContinueHere = onContinueHere,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun WorkspaceScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onApproval: (Boolean) -> Unit,
    onRefreshFiles: () -> Unit,
    onOpenFile: (WorkspaceEntry) -> Unit,
    onCloseFile: () -> Unit,
    onUndoChanges: () -> Unit,
    onKeepChanges: () -> Unit,
    onUndoFileChange: (String) -> Unit,
    onKeepFileChange: (String) -> Unit,
    onCreateChat: () -> Unit,
    onSwitchChat: (String) -> Unit,
    onTerminalRun: (String) -> Unit,
    onTerminalInput: (String) -> Unit,
    onTerminalInterrupt: () -> Unit,
    onTerminalPrepare: (String) -> Unit,
    onTerminalDraftConsumed: () -> Unit,
    onTerminalOpened: () -> Unit,
    onTerminalStop: () -> Unit,
    onTerminalClear: () -> Unit,
    onTerminalConfirm: () -> Unit,
    onTerminalCancel: () -> Unit,
    onUseSuggestedProjectRoot: () -> Unit,
    onExportProject: (Uri) -> Unit,
    onAddAttachments: (List<Uri>) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onOpenAttachment: (ChatAttachment) -> Unit,
    onBuildAndRunAndroid: () -> Unit,
    onRefreshGit: () -> Unit = {},
    onStageFile: (String) -> Unit = {},
    onUnstageFile: (String) -> Unit = {},
    onStageAll: () -> Unit = {},
    onUnstageAll: () -> Unit = {},
    onDiscardFile: (String) -> Unit = {},
    onCommit: (String, Boolean) -> Unit = { _, _ -> },
    onCreateBranch: (String, Boolean) -> Unit = { _, _ -> },
    onCheckoutBranch: (String) -> Unit = {},
    onDeleteBranch: (String) -> Unit = {},
    onMergeBranch: (String) -> Unit = {},
    onCreateWorktree: (String) -> Unit = {},
    onRemoveWorktree: (String) -> Unit = {},
    onPushGit: () -> Unit = {},
    onPullGit: () -> Unit = {},
    onSelectCommit: (com.jarves.mh.model.GitCommit?) -> Unit = {},
    onStartGitHubLogin: () -> Unit = {},
    onDecomposeGoal: (String) -> Unit = {},
    onCreateTask: (String, String, com.jarves.mh.model.TaskPriority, String?, List<String>) -> Unit = { _, _, _, _, _ -> },
    onMoveTaskStatus: (String, com.jarves.mh.model.TaskStatus) -> Unit = { _, _ -> },
    onAssignTask: (String, String?) -> Unit = { _, _ -> },
    onResetCircuitBreaker: () -> Unit = {},
    onSendDirectMessage: (String, String, String) -> Unit = { _, _, _ -> },
    onAddBlackboardEntry: (String, String, String) -> Unit = { _, _, _ -> },
    onSendContextToAgent: (String) -> Unit = {},
    onConsumeDesignModeDraft: () -> Unit = {},
    onSetEffort: (String) -> Unit = {},
    onCreateSchedule: (String, String, Int, Boolean, String) -> Unit = { _, _, _, _, _ -> },
    onToggleSchedule: (String) -> Unit = {},
    onMergeWorktree: (String) -> Unit = {},
    onToggleSkill: (String) -> Unit = {},
    onCreateSkill: (String, String, com.jarves.mh.skills.SkillCategory, String) -> Unit = { _, _, _, _ -> },
    onToggleAnnotationResolved: (String) -> Unit = {},
    onRunAutomatedReview: (String) -> Unit = {},
    onAddSshProfile: (String, String, Int, String, com.jarves.mh.ssh.SshAuthType, String) -> Unit = { _, _, _, _, _, _ -> },
    onSetActiveSshProfile: (String?) -> Unit = {},
    onDeleteSshProfile: (String) -> Unit = {},
    onSaveLinearApiKey: (String) -> Unit = {},
    onRefreshLinearIssues: () -> Unit = {},
    onImportLinearIssue: (com.jarves.mh.network.LinearIssue) -> Unit = {},
    onRefreshGitHubIssues: (String?) -> Unit = {},
    onImportGitHubIssue: (com.jarves.mh.network.GitHubIssue) -> Unit = {},
    onSelectAgent: (AgentKind) -> Unit = {},
    onToggleAutoApprove: () -> Unit = {},
    onSelectModel: (String) -> Unit = {},
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val isAndroidProject = state.androidProjectDetected
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    var showAgentPicker by rememberSaveable { mutableStateOf(false) }

    if (showAgentPicker) {
        AgentSwitchSheet(
            selected = state.agentKind,
            onSelect = { agent ->
                showAgentPicker = false
                onSelectAgent(agent)
            },
            onDismiss = { showAgentPicker = false },
        )
    }

    val exportProjectLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri -> if (uri != null) onExportProject(uri) },
    )
    val attachmentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = onAddAttachments,
    )
    val unknownAppsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()) {
                onBuildAndRunAndroid()
            } else {
                Toast.makeText(context, "Allow app installs to run Android projects", Toast.LENGTH_LONG).show()
            }
        },
    )
    val chatListState = rememberLazyListState()
    var userScrolledUp by rememberSaveable { mutableStateOf(false) }

    val chatItemCount = state.messages.size +
        (if (state.liveProcess.isNotEmpty() || state.liveThinking) 1 else 0) +
        (if (state.pendingApproval != null) 1 else 0)

    LaunchedEffect(state.activeChatId) {
        userScrolledUp = false
        if (chatItemCount > 0) chatListState.scrollToItem(chatItemCount - 1)
    }

    // When the user actively scrolls/touches the screen, detect if they scrolled up to read thinking/messages.
    LaunchedEffect(chatListState.isScrollInProgress) {
        if (chatListState.isScrollInProgress) {
            if (chatListState.canScrollForward) {
                userScrolledUp = true
            }
        } else {
            // If user scrolled back down to the very bottom, re-enable follow mode
            if (!chatListState.canScrollForward) {
                userScrolledUp = false
            }
        }
    }

    // Follow new tokens/updates only when user is at the bottom and has not scrolled up to read.
    LaunchedEffect(
        state.messages.size,
        state.messages.lastOrNull()?.text?.length,
        state.liveProcess.size,
        state.liveProcess.lastOrNull()?.detail,
        state.pendingApproval,
    ) {
        if (!state.isRunning || chatItemCount <= 0 || userScrolledUp || chatListState.isScrollInProgress) return@LaunchedEffect
        if (!chatListState.canScrollForward) {
            chatListState.scrollToItem(chatItemCount - 1)
        }
    }

    var selectedTab by rememberSaveable { mutableStateOf(WorkspaceTab.CHAT) }
    var showChats by rememberSaveable { mutableStateOf(false) }
    var showSshDialog by rememberSaveable { mutableStateOf(false) }
    val activeChat = state.projectChats.firstOrNull { it.id == state.activeChatId }

    // If a file is open, show the FileViewerScreen on top
    if (state.openedFilePath != null) {
        BackHandler(onBack = {
            onCloseFile()
            selectedTab = WorkspaceTab.FILES
        })
        FileViewerScreen(
            filePath = state.openedFilePath,
            content = state.openedFileContent,
            loading = state.fileContentLoading,
            onClose = {
                onCloseFile()
                selectedTab = WorkspaceTab.FILES
            },
        )
        return
    }

    if (showChats) {
        ChatSwitcherDialog(
            chats = state.projectChats,
            activeChatId = state.activeChatId,
            switchingEnabled = !state.isRunning,
            onDismiss = { showChats = false },
            onCreate = {
                onCreateChat()
                showChats = false
                selectedTab = WorkspaceTab.CHAT
            },
            onSwitch = { chatId ->
                onSwitchChat(chatId)
                showChats = false
                selectedTab = WorkspaceTab.CHAT
            },
        )
    }

    if (showSshDialog) {
        SshRuntimeDialog(
            activeProfile = state.activeSshProfile,
            profiles = state.sshProfiles,
            onDismiss = { showSshDialog = false },
            onSelectProfile = { profileId ->
                onSetActiveSshProfile(profileId)
                showSshDialog = false
            },
            onAddProfile = { name, host, port, user, authType, remotePath ->
                onAddSshProfile(name, host, port, user, authType, remotePath)
            },
            onDeleteProfile = onDeleteSshProfile,
        )
    }
    state.pendingTerminalCommand?.let { command ->
        AlertDialog(
            onDismissRequest = onTerminalCancel,
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Run potentially destructive command?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("This command can delete files, rewrite Git history, or change the project significantly.")
                    Surface(color = Color(0xFF14171E), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            command,
                            Modifier.fillMaxWidth().padding(10.dp),
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFE2E8F0),
                        )
                    }
                }
            },
            confirmButton = { Button(onClick = onTerminalConfirm) { Text("Run anyway") } },
            dismissButton = { TextButton(onClick = onTerminalCancel) { Text("Cancel") } },
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            state.activeProject?.name.orEmpty(),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    Toast.makeText(context, state.activeProject?.name.orEmpty(), Toast.LENGTH_LONG).show()
                                },
                            ),
                        )
                        Text(
                            "${activeChat?.title ?: "Chat"} · ${if (state.agentKind == AgentKind.ANTIGRAVITY) state.agentKind.title else state.provider.kind.title}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Projects") } },
                actions = {
                    if (isAndroidProject) {
                        IconButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                                    !context.packageManager.canRequestPackageInstalls()) {
                                    unknownAppsLauncher.launch(
                                        Intent(
                                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                            Uri.parse("package:${context.packageName}"),
                                        ),
                                    )
                                } else {
                                    onBuildAndRunAndroid()
                                }
                            },
                            enabled = !state.androidBuildRunning && !state.isRunning && !state.projectTerminalRunning,
                        ) {
                            if (state.androidBuildRunning) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Default.PlayArrow, "Build and run Android app")
                        }
                    }
                    val isRemoteSsh = state.activeSshProfile != null
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isRemoteSsh) NeoLime.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, if (isRemoteSsh) NeoLime else if (androidx.compose.foundation.isSystemInDarkTheme()) NeoDarkBorder else NeoBlack, RoundedCornerShape(6.dp))
                            .clickable { showSshDialog = true }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (isRemoteSsh) Color(0xFF00E676) else NeoLime, CircleShape),
                            )
                            Text(
                                if (isRemoteSsh) "SSH" else "PROOT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = if (isRemoteSsh) NeoLime else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    IconButton(onClick = { showChats = true }) { Icon(Icons.Default.History, "Project chats") }
                    if (state.isRunning) {
                        Box(Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                            PulsingDot(color = NeoLime, size = 9.dp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            if (!keyboardVisible) {
                val isDark = androidx.compose.foundation.isSystemInDarkTheme()
                val navBorder = if (isDark) NeoDarkBorder else NeoBlack
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.border(BorderStroke(2.dp, navBorder)),
                ) {
                    WorkspaceTab.entries.forEach { tab ->
                        val isSelected = selectedTab == tab
                        val tabScale by androidx.compose.animation.core.animateFloatAsState(
                            targetValue = if (isSelected) 1.08f else 1.0f,
                            animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
                            ),
                            label = "wsTabScale",
                        )
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                selectedTab = tab
                                if (tab == WorkspaceTab.FILES) onRefreshFiles()
                                if (tab == WorkspaceTab.TERMINAL) onTerminalOpened()
                                if (tab == WorkspaceTab.CHANGES) onRefreshGit()
                            },
                            icon = {
                                Icon(
                                    tab.icon,
                                    tab.label,
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = tabScale
                                        scaleY = tabScale
                                    },
                                )
                            },
                            label = {
                                Text(
                                    tab.label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeoBlack,
                                selectedTextColor = if (isDark) NeoLime else NeoBlack,
                                indicatorColor = NeoLime,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                WorkspaceTab.CHAT -> ChatTab(
                    state.messages,
                    state.pendingApproval,
                    state.liveProcess,
                    state.isRunning,
                    onSend,
                    onStop,
                    onApproval,
                    listState = chatListState,
                    taskStartedAtMillis = state.workSegmentStartedAtMillis ?: state.taskStartedAtMillis,
                    taskFinishedAtMillis = state.taskFinishedAtMillis,
                    thinkingActive = state.liveThinking,
                    agentKind = state.agentKind,
                    pendingAttachments = state.pendingAttachments,
                    onAttach = {
                        attachmentLauncher.launch(arrayOf("image/*", "text/*", "application/json", "application/xml"))
                    },
                    onRemoveAttachment = onRemoveAttachment,
                    onOpenAttachment = onOpenAttachment,
                    onRunInTerminal = { command ->
                        selectedTab = WorkspaceTab.TERMINAL
                        onTerminalOpened()
                        onTerminalPrepare(command)
                    },
                    initialPrompt = state.designModePromptDraft,
                    onConsumeInitialPrompt = onConsumeDesignModeDraft,
                    activeModel = if (state.agentKind == AgentKind.ANTIGRAVITY) state.antigravityModel else state.provider.model,
                    reasoningEffort = state.antigravityEffort,
                    onSetEffort = onSetEffort,
                    autoApproveTools = state.autoApproveTools,
                    onToggleAutoApprove = onToggleAutoApprove,
                    onSelectModel = onSelectModel,
                    onSelectAgent = onSelectAgent,
                    onDelegateToSwarm = { taskText ->
                        onCreateTask("Task from Chat", taskText, com.jarves.mh.model.TaskPriority.MEDIUM, null, emptyList())
                    },
                    onOpenAgentSwitch = { showAgentPicker = true },
                )
                WorkspaceTab.TASKS -> TaskBoardScreen(
                    tasks = state.orchestratorTasks,
                    agents = state.orchestratorAgents,
                    blackboardEntries = state.blackboardEntries,
                    auditLogs = state.swarmAuditLogs,
                    circuitBreaker = state.circuitBreakerState,
                    inboxMessages = state.swarmInboxMessages,
                    scheduledTasks = state.scheduledTasks,
                    onDecomposeGoal = onDecomposeGoal,
                    onCreateTask = onCreateTask,
                    onMoveTaskStatus = onMoveTaskStatus,
                    onAssignTask = onAssignTask,
                    onResetCircuitBreaker = onResetCircuitBreaker,
                    onSendDirectMessage = onSendDirectMessage,
                    onAddBlackboardEntry = onAddBlackboardEntry,
                    onCreateSchedule = onCreateSchedule,
                    onToggleSchedule = onToggleSchedule,
                    onDeleteSchedule = onDeleteSchedule,
                    onMergeWorktree = onMergeWorktree,
                    onRemoveWorktree = onRemoveWorktree,
                    skills = state.skills,
                    diffReviews = state.diffReviews,
                    onToggleSkill = onToggleSkill,
                    onCreateSkill = onCreateSkill,
                    onToggleAnnotationResolved = onToggleAnnotationResolved,
                    onRunAutomatedReview = onRunAutomatedReview,
                    linearIssues = state.linearIssues,
                    linearApiKey = state.linearApiKey,
                    linearLoading = state.linearLoading,
                    onSaveLinearApiKey = onSaveLinearApiKey,
                    onRefreshLinearIssues = onRefreshLinearIssues,
                    onImportLinearIssue = onImportLinearIssue,
                    githubIssues = state.githubIssues,
                    githubIssuesLoading = state.githubIssuesLoading,
                    githubRepoName = state.githubRepoOverride ?: state.activeProject?.description?.takeIf { it.startsWith("GitHub · ") }?.removePrefix("GitHub · ")?.trim(),
                    onRefreshGitHubIssues = onRefreshGitHubIssues,
                    onImportGitHubIssue = onImportGitHubIssue,
                )
                WorkspaceTab.FILES -> FilesTab(
                    files = state.workspaceFiles,
                    loading = state.filesLoading,
                    suggestedProjectRoot = state.suggestedProjectRoot,
                    onRefresh = onRefreshFiles,
                    onOpenFile = onOpenFile,
                    onUseSuggestedProjectRoot = onUseSuggestedProjectRoot,
                    onExport = {
                        exportProjectLauncher.launch("${state.activeProject?.slug ?: "project"}.zip")
                    },
                )
                WorkspaceTab.TERMINAL -> TerminalScreen(
                    lines = state.projectTerminalLines,
                    isRunning = state.projectTerminalRunning,
                    onRun = onTerminalRun,
                    onInput = onTerminalInput,
                    onInterrupt = onTerminalInterrupt,
                    onClear = onTerminalClear,
                    onToggleTheme = {},
                    themeMode = state.themeMode,
                    title = "Project Terminal",
                    subtitle = "${state.projectTerminalCwd} · Ubuntu PRoot",
                    liveOutput = state.projectTerminalLiveOutput,
                    currentCommand = state.projectTerminalCommand,
                    commandDraft = state.projectTerminalDraft,
                    onCommandDraftConsumed = onTerminalDraftConsumed,
                    promptPath = state.projectTerminalCwd,
                    onStop = onTerminalStop,
                    showThemeAction = false,
                    showQuickCommands = false,
                    compactHeader = true,
                )
                WorkspaceTab.CHANGES -> GitScreen(
                    status = state.gitStatus,
                    branches = state.gitBranches,
                    worktrees = state.gitWorktrees,
                    commits = state.gitCommits,
                    stagedDiffs = state.gitStagedDiffs,
                    unstagedDiffs = state.gitUnstagedDiffs,
                    pullRequests = state.gitPullRequests,
                    selectedCommit = state.selectedGitCommit,
                    selectedCommitDiffs = state.selectedGitCommitDiffs,
                    isLoading = state.gitOperationRunning,
                    operationMessage = state.gitOperationMessage,
                    isGitHubConnected = state.githubAuthStatus == GitHubAuthStatus.CONNECTED,
                    onRefresh = onRefreshGit,
                    onStageFile = onStageFile,
                    onUnstageFile = onUnstageFile,
                    onStageAll = onStageAll,
                    onUnstageAll = onUnstageAll,
                    onDiscardFile = onDiscardFile,
                    onCommit = onCommit,
                    onCreateBranch = onCreateBranch,
                    onCheckoutBranch = onCheckoutBranch,
                    onDeleteBranch = onDeleteBranch,
                    onMergeBranch = onMergeBranch,
                    onCreateWorktree = onCreateWorktree,
                    onRemoveWorktree = onRemoveWorktree,
                    onPush = onPushGit,
                    onPull = onPullGit,
                    onSelectCommit = onSelectCommit,
                    onCreatePullRequest = onCreatePullRequest,
                    onOpenGitHubSettings = onStartGitHubLogin,
                )
                WorkspaceTab.PREVIEW -> com.jarves.mh.browser.BrowserInspectionScreen(
                    ready = state.previewReady,
                    url = state.previewUrl,
                    onSendContextToAgent = { elementContext ->
                        onSendContextToAgent(elementContext)
                        selectedTab = WorkspaceTab.CHAT
                    },
                )
            }
        }
    }
}

@Composable
private fun ChatSwitcherDialog(
    chats: List<ProjectChat>,
    activeChatId: String?,
    switchingEnabled: Boolean,
    onDismiss: () -> Unit,
    onCreate: () -> Unit,
    onSwitch: (String) -> Unit,
    allowCreate: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Project chats") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (allowCreate) {
                    Button(onClick = onCreate, enabled = switchingEnabled, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("New chat")
                    }
                }
                if (!switchingEnabled) {
                    Text("Finish the running task before switching chats.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 380.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(chats, key = { it.id }) { chat ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable(enabled = switchingEnabled) { onSwitch(chat.id) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (chat.id == activeChatId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(chat.title, fontWeight = if (chat.id == activeChatId) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
                                    Text(
                                        if (chat.id == activeChatId) "Current chat" else "Saved conversation",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (chat.id == activeChatId) Icon(Icons.Default.Check, "Current", tint = PocketGreen)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun SshRuntimeDialog(
    activeProfile: com.jarves.mh.ssh.SshServerProfile?,
    profiles: List<com.jarves.mh.ssh.SshServerProfile>,
    onDismiss: () -> Unit,
    onSelectProfile: (String?) -> Unit,
    onAddProfile: (String, String, Int, String, com.jarves.mh.ssh.SshAuthType, String) -> Unit,
    onDeleteProfile: (String) -> Unit,
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val borderColor = if (isDark) NeoDarkBorder else NeoBlack
    var showAddForm by remember { mutableStateOf(false) }

    var serverName by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("22") }
    var user by remember { mutableStateOf("root") }
    var remotePath by remember { mutableStateOf("~/workspace") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🖥️", fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text("Execution Runtime Backend", fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "PocketDev runs a local Linux PRoot environment on Android by default. You can also connect to a remote SSH server for high-performance builds and agent execution.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Option 1: Local PRoot Linux
                val isLocalActive = activeProfile == null
                Surface(
                    color = if (isLocalActive) NeoLime.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(if (isLocalActive) 2.dp else 1.dp, if (isLocalActive) NeoLime else borderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectProfile(null) },
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).background(if (isLocalActive) NeoLime else MaterialTheme.colorScheme.outline, CircleShape))
                                Spacer(Modifier.width(8.dp))
                                Text("Local Linux (PRoot on Android)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Text("Default · Fully offline · Ubuntu userspace", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp))
                        }
                        if (isLocalActive) {
                            Text("ACTIVE", fontSize = 9.sp, fontWeight = FontWeight.Black, color = if (isDark) NeoLime else NeoBlack)
                        }
                    }
                }

                // Configured Remote SSH Profiles
                Text("REMOTE SSH SERVERS (${profiles.size})", fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeoLime)

                profiles.forEach { p ->
                    val isActive = p.id == activeProfile?.id
                    Surface(
                        color = if (isActive) NeoLime.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(if (isActive) 2.dp else 1.dp, if (isActive) NeoLime else borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectProfile(p.id) },
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(p.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("${p.user}@${p.host}:${p.port}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Workspace: ${p.remoteWorkspacePath}", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isActive) {
                                    Text("ACTIVE", fontSize = 9.sp, fontWeight = FontWeight.Black, color = if (isDark) NeoLime else NeoBlack)
                                    Spacer(Modifier.width(8.dp))
                                }
                                IconButton(onClick = { onDeleteProfile(p.id) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                if (showAddForm) {
                    HorizontalDivider(color = borderColor)
                    Text("ADD REMOTE SERVER", fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeoLime)
                    OutlinedTextField(
                        value = serverName,
                        onValueChange = { serverName = it },
                        label = { Text("Server Name *") },
                        placeholder = { Text("E.g. Home Lab / AWS EC2") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = host,
                            onValueChange = { host = it },
                            label = { Text("Host / IP *") },
                            singleLine = true,
                            modifier = Modifier.weight(2f),
                        )
                        OutlinedTextField(
                            value = port,
                            onValueChange = { port = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Port") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    OutlinedTextField(
                        value = user,
                        onValueChange = { user = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = remotePath,
                        onValueChange = { remotePath = it },
                        label = { Text("Remote Workspace Path") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            if (serverName.isNotBlank() && host.isNotBlank()) {
                                onAddProfile(
                                    serverName.trim(),
                                    host.trim(),
                                    port.toIntOrNull() ?: 22,
                                    user.trim(),
                                    com.jarves.mh.ssh.SshAuthType.PASSWORD,
                                    remotePath.trim(),
                                )
                                showAddForm = false
                                serverName = ""
                                host = ""
                            }
                        },
                        enabled = serverName.isNotBlank() && host.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeoLime, contentColor = NeoBlack),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("SAVE SERVER", fontWeight = FontWeight.Black)
                    }
                } else {
                    OutlinedButton(
                        onClick = { showAddForm = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Add, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("ADD SSH SERVER")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileViewerScreen(
    filePath: String,
    content: String?,
    loading: Boolean,
    onClose: () -> Unit,
) {
    val fileName = filePath.substringAfterLast('/')
    val ext = fileName.substringAfterLast('.', "")
    val isMarkdown = ext == "md"
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(fileName, fontWeight = FontWeight.SemiBold)
                        Text(filePath, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close file") }
                },
                actions = {
                    if (!content.isNullOrEmpty()) {
                        IconButton(onClick = {
                            clipboard.setText(AnnotatedString(content))
                            copied = true
                            scope.launch { delay(2000); copied = false }
                        }) {
                            Icon(
                                if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                                "Copy file contents",
                                tint = if (copied) NeoLime else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NeoLime)
                    }
                }
                content == null -> {
                    EmptyState(Icons.Default.Description, "No content", "The file could not be read.")
                }
                isMarkdown -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item { MarkdownText(markdown = content, color = MaterialTheme.colorScheme.onSurface) }
                    }
                }
                else -> {
                    // Code / plain-text viewer
                    LazyColumn(
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0D1117)),
                    ) {
                        val lines = content.lines()
                        items(lines.size) { idx ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    modifier = Modifier
                                        .width(42.dp)
                                        .padding(start = 8.dp, end = 6.dp),
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = Color(0xFF4A5568),
                                    textAlign = TextAlign.End,
                                )
                                Text(
                                    text = lines[idx],
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 12.dp),
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                    color = Color(0xFFE2E8F0),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilesTab(
    files: List<WorkspaceEntry>,
    loading: Boolean,
    suggestedProjectRoot: String?,
    onRefresh: () -> Unit,
    onOpenFile: (WorkspaceEntry) -> Unit,
    onUseSuggestedProjectRoot: () -> Unit,
    onExport: () -> Unit,
) {
    var expandedDirectories by rememberSaveable { mutableStateOf(emptyList<String>()) }
    LaunchedEffect(files.map { it.path }) {
        val directories = files.asSequence().filter { it.isDirectory }.map { it.path }.toSet()
        expandedDirectories = expandedDirectories.filter { it in directories }
    }
    val expandedSet = expandedDirectories.toSet()
    val visibleFiles = files.filter { entry ->
        val segments = entry.path.split('/')
        segments.size == 1 || (1 until segments.size).all { depth ->
            segments.take(depth).joinToString("/") in expandedSet
        }
    }
    val directChildCounts = files.filter { candidate ->
        candidate.path.contains('/')
    }.groupingBy { candidate -> candidate.path.substringBeforeLast('/') }.eachCount()

    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            NeoCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 14.dp,
                shadowOffset = 3.dp,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Files",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (expandedDirectories.isNotEmpty()) {
                        TextButton(onClick = { expandedDirectories = emptyList() }) {
                            Icon(Icons.Default.KeyboardArrowUp, null, Modifier.size(17.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Collapse all", fontSize = 11.sp, color = NeoLime, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (!loading && files.any { !it.isDirectory }) {
                        IconButton(onClick = onExport, modifier = Modifier.neoBounce()) { Icon(Icons.Default.Download, "Export project as ZIP") }
                    }
                    if (loading) {
                        CircularProgressIndicator(Modifier.padding(12.dp).size(20.dp), strokeWidth = 2.dp, color = NeoLime)
                    } else {
                        IconButton(onClick = onRefresh, modifier = Modifier.neoBounce()) { Icon(Icons.Default.Refresh, "Refresh files") }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        if (suggestedProjectRoot != null) {
            item(key = "suggested-project-root") {
                NeoCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = NeoLime.copy(alpha = 0.15f),
                    borderColor = NeoLime,
                    cornerRadius = 14.dp,
                    shadowOffset = 3.5.dp,
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Project folder detected", fontWeight = FontWeight.Bold)
                        Text(
                            "Use $suggestedProjectRoot as the project root so Chat, Terminal, Changes, and Preview all run from the same folder.",
                            fontSize = 13.sp,
                        )
                        NeoButton(
                            onClick = onUseSuggestedProjectRoot,
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = NeoLime,
                            contentColor = NeoBlack,
                        ) {
                            Text("Use $suggestedProjectRoot as project root", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        if (!loading && files.isEmpty()) {
            item { EmptyState(Icons.Default.Folder, "No files yet", "Ask your coding agent to create something in this project.") }
        }
        items(visibleFiles, key = { it.path }) { entry ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (entry.isDirectory) {
                            expandedDirectories = if (entry.path in expandedSet) {
                                expandedDirectories.filterNot { it == entry.path || it.startsWith("${entry.path}/") }
                            } else {
                                expandedDirectories + entry.path
                            }
                        } else {
                            onOpenFile(entry)
                        }
                    }
                    .padding(start = (entry.depth * 20).dp)
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (entry.isDirectory) {
                    Icon(
                        if (entry.path in expandedSet) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        if (entry.path in expandedSet) "Collapse folder" else "Expand folder",
                        Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(5.dp))
                }
                Icon(
                    if (entry.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                    null,
                    tint = if (entry.isDirectory) NeoLime else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(11.dp))
                Text(
                    if (entry.isDirectory) "${entry.name} (${directChildCounts[entry.path] ?: 0})" else entry.name,
                    Modifier.weight(1f),
                    fontWeight = if (entry.isDirectory) FontWeight.Bold else FontWeight.Medium,
                    color = if (!entry.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
                if (!entry.isDirectory) {
                    Spacer(Modifier.width(8.dp))
                    Text(formatFileSize(entry.sizeBytes), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!entry.isDirectory) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(start = (entry.depth * 20 + 42).dp))
            }
        }
    }
}

@Composable
private fun ChatTab(
    messages: List<ChatMessage>,
    approval: ToolRequest?,
    liveProcess: List<ActivityItem>,
    isRunning: Boolean,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onApproval: (Boolean) -> Unit,
    listState: LazyListState,
    taskStartedAtMillis: Long?,
    taskFinishedAtMillis: Long?,
    thinkingActive: Boolean,
    agentKind: AgentKind,
    pendingAttachments: List<ChatAttachment>,
    onAttach: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onOpenAttachment: (ChatAttachment) -> Unit,
    onRunInTerminal: (String) -> Unit,
    readOnly: Boolean = false,
    readOnlyBlocked: Boolean = false,
    onContinueHere: () -> Unit = {},
    initialPrompt: String? = null,
    onConsumeInitialPrompt: () -> Unit = {},
    activeModel: String = "",
    reasoningEffort: String = "medium",
    onSetEffort: (String) -> Unit = {},
    onDelegateToSwarm: (String) -> Unit = {},
    onOpenAgentSwitch: () -> Unit = {},
    autoApproveTools: Boolean = false,
    onToggleAutoApprove: () -> Unit = {},
    onSelectModel: (String) -> Unit = {},
    onSelectAgent: (AgentKind) -> Unit = {},
) {
    val context = LocalContext.current
    val view = LocalView.current
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
                if (!spoken.isNullOrBlank()) {
                    prompt = if (prompt.isBlank()) spoken else "$prompt $spoken"
                }
            }
        },
    )
    // Keep the screen on while the selected agent is working in this chat. Released automatically
    // when the task finishes or the user leaves the chat tab.
    DisposableEffect(isRunning) {
        view.keepScreenOn = isRunning
        onDispose { view.keepScreenOn = false }
    }
    var prompt by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(initialPrompt) {
        if (!initialPrompt.isNullOrBlank()) {
            prompt = if (prompt.isBlank()) initialPrompt else "$prompt\n\n$initialPrompt"
            onConsumeInitialPrompt()
        }
    }
    val chatScope = rememberCoroutineScope()
    // True while the newest item (message, live panel, or approval card) is on screen.
    val readerAtBottom by remember {
        derivedStateOf {
            !listState.canScrollForward
        }
    }
    Column(Modifier.fillMaxSize().imePadding()) {
        ContextWindowHeader(
            messages = messages,
            agentKind = agentKind,
            activeModel = activeModel,
            reasoningEffort = reasoningEffort,
            autoApproveTools = autoApproveTools,
            onToggleAutoApprove = onToggleAutoApprove,
            onCycleEffort = {
                val nextEffort = when (reasoningEffort.lowercase()) {
                    "low" -> "medium"
                    "medium" -> "high"
                    else -> "low"
                }
                onSetEffort(nextEffort)
            },
        )
        Box(Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(messages, key = { it.id }) { message ->
                    if (message.workItems.isNotEmpty()) {
                        WorkBlockCard(message)
                    } else {
                        MessageBubble(message, onRunInTerminal, onOpenAttachment)
                    }
                }
                if (liveProcess.isNotEmpty() || thinkingActive) {
                    item(key = "live-claude-process") {
                        LiveClaudeProcess(
                            processItems = liveProcess,
                            isRunning = isRunning,
                            startedAtMillis = taskStartedAtMillis,
                            finishedAtMillis = taskFinishedAtMillis,
                            thinkingActive = thinkingActive,
                        )
                    }
                }
                approval?.let { request -> item { ApprovalCard(request, onApproval) } }
            }
            if (!readerAtBottom) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                        .clickable {
                            chatScope.launch {
                                listState.animateScrollToItem(
                                    (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0),
                                )
                            }
                        },
                    shape = CircleShape,
                    shadowElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Row(
                        Modifier.padding(start = 13.dp, end = 15.dp, top = 7.dp, bottom = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Latest",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
        if (readOnly) {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(Modifier.weight(1f)) {
                        Text("Read-only history", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (readOnlyBlocked) {
                                "Another project has a running task. You can read this chat, but cannot send a message."
                            } else {
                                "The other task finished. Open this project to continue chatting."
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!readOnlyBlocked) {
                        TextButton(onClick = onContinueHere) { Text("Open") }
                    }
                }
            }
        } else Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                if (pendingAttachments.isNotEmpty()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        pendingAttachments.forEach { attachment ->
                            AttachmentChip(
                                attachment = attachment,
                                onOpen = null,
                                onRemove = { onRemoveAttachment(attachment.id) },
                            )
                        }
                    }
                }

                val canSend = prompt.isNotBlank() || pendingAttachments.isNotEmpty()
                val isDark = androidx.compose.foundation.isSystemInDarkTheme()
                val inputBorder = if (canSend) NeoLime else (if (isDark) NeoDarkBorder else NeoBlack)

                // Quick steering and delegation action row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, if (isDark) NeoDarkBorder else NeoBlack, RoundedCornerShape(8.dp))
                            .clickable {
                                onSend("Continue executing the next step from where you left off.")
                            },
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(Icons.Default.FastForward, null, modifier = Modifier.size(12.dp), tint = NeoLime)
                            Text("Continue", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, if (isDark) NeoDarkBorder else NeoBlack, RoundedCornerShape(8.dp))
                            .clickable {
                                prompt = "Steer direction: Focus on "
                            },
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(Icons.Default.DirectionsRun, null, modifier = Modifier.size(12.dp), tint = NeoLime)
                            Text("Steer", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, if (isDark) NeoDarkBorder else NeoBlack, RoundedCornerShape(8.dp))
                            .clickable {
                                if (prompt.isNotBlank()) {
                                    onDelegateToSwarm(prompt)
                                    prompt = ""
                                }
                            },
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(Icons.Default.Hub, null, modifier = Modifier.size(12.dp), tint = NeoLime)
                            Text("Delegate Swarm", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, if (isDark) NeoDarkBorder else NeoBlack, RoundedCornerShape(8.dp))
                            .clickable(onClick = onOpenAgentSwitch),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(Icons.Default.SmartToy, null, modifier = Modifier.size(12.dp), tint = NeoLime)
                            Text("Switch Agent", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, if (autoApproveTools) NeoLime else (if (isDark) NeoDarkBorder else NeoBlack), RoundedCornerShape(8.dp))
                            .clickable { onToggleAutoApprove() },
                        color = if (autoApproveTools) NeoLime.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(Icons.Default.Security, null, modifier = Modifier.size(12.dp), tint = if (autoApproveTools) NeoLime else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (autoApproveTools) "Bypass: ON" else "Bypass: OFF", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, if (isDark) NeoDarkBorder else NeoBlack, RoundedCornerShape(8.dp))
                            .clickable { prompt = if (prompt.startsWith("/")) "" else "/" },
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(Icons.Default.Terminal, null, modifier = Modifier.size(12.dp), tint = NeoLime)
                            Text("/ Commands", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Slash command quick autocomplete popup when prompt starts with "/"
                if (prompt.startsWith("/")) {
                    val slashChips = listOf(
                        Triple("/model ", "⚡ /model", "Switch AI model"),
                        Triple("/effort ", "🧠 /effort", "Switch reasoning effort"),
                        Triple("/bypass", "🛡️ /bypass", "Toggle auto-approval"),
                        Triple("/agent ", "🤖 /agent", "Switch coding agent"),
                        Triple("/jcode", "⚡ /jcode", "Switch to JCode agent"),
                        Triple("/piagent", "🥧 /piagent", "Switch to Pi agent"),
                        Triple("/commandcode", "⌨️ /cmdcode", "Switch to Command Code"),
                        Triple("/cline", "🔧 /cline", "Switch to Cline agent"),
                        Triple("/custom", "🛠️ /custom", "Switch to Custom Runner"),
                        Triple("/help", "❓ /help", "List all slash commands"),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        slashChips.forEach { (cmd, label, _) ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, NeoLime, RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (cmd.endsWith(" ")) {
                                            prompt = cmd
                                        } else {
                                            onSend(cmd)
                                            prompt = ""
                                        }
                                    },
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            ) {
                                Text(
                                    label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) NeoLime else NeoBlack,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neoShadow(offsetX = 3.dp, offsetY = 3.dp, cornerRadius = 18.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                        .border(2.dp, inputBorder, RoundedCornerShape(18.dp)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        IconButton(
                            onClick = onAttach,
                            enabled = !isRunning && pendingAttachments.size < 5,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Attach files",
                                modifier = Modifier.size(20.dp),
                                tint = if (pendingAttachments.isNotEmpty()) NeoLime else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        IconButton(
                            onClick = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak prompt for ${agentKind.title}…")
                                }
                                runCatching {
                                    speechLauncher.launch(intent)
                                }.onFailure {
                                    Toast.makeText(context, "Voice dictation not available on this device", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isRunning,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Dictate prompt",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        BasicTextField(
                            value = prompt,
                            onValueChange = { prompt = it },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp, vertical = 10.dp)
                                .heightIn(min = 20.dp, max = 130.dp),
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                lineHeight = 20.sp,
                            ),
                            cursorBrush = SolidColor(NeoLime),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (prompt.isEmpty()) {
                                        Text(
                                            text = "Message ${agentKind.title}…",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 15.sp,
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                        )

                        Spacer(Modifier.width(4.dp))

                        if (isRunning) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .neoBounce(onClick = onStop)
                                    .background(
                                        color = MaterialTheme.colorScheme.error,
                                        shape = CircleShape,
                                    )
                                    .border(1.5.dp, if (isDark) NeoDarkBorder else NeoBlack, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop AI task",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .then(
                                        if (canSend) {
                                            Modifier
                                                .neoBounce(
                                                    pressedScale = 0.88f,
                                                    onClick = {
                                                        onSend(prompt)
                                                        prompt = ""
                                                    },
                                                )
                                                .background(NeoLime, CircleShape)
                                                .border(1.5.dp, if (isDark) NeoDarkBorder else NeoBlack, CircleShape)
                                        } else {
                                            Modifier
                                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                        }
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Send",
                                    tint = if (canSend) NeoBlack else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContextWindowHeader(
    messages: List<ChatMessage>,
    agentKind: AgentKind,
    activeModel: String,
    reasoningEffort: String,
    autoApproveTools: Boolean = false,
    onToggleAutoApprove: () -> Unit = {},
    onCycleEffort: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) NeoDarkBorder else NeoBlack

    val totalChars = remember(messages) {
        messages.sumOf { it.text.length + it.workItems.sumOf { w -> w.title.length + w.body.length } }
    }
    val estimatedTokens = (totalChars / 4).coerceAtLeast(120)
    val maxTokens = 200000
    val usageRatio = (estimatedTokens.toFloat() / maxTokens).coerceIn(0.01f, 1f)
    val cacheWarmthPercent = if (messages.size > 1) 85 + (messages.size % 12) else 30

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, borderColor)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Left: Cache warmth & context usage
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Cache warmth badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeoLime.copy(alpha = 0.2f))
                            .border(1.dp, NeoLime, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(NeoLime, CircleShape),
                        )
                        Text(
                            "CACHE $cacheWarmthPercent%",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (isDark) NeoLime else NeoBlack,
                        )
                    }

                    // Context tokens text
                    Text(
                        "${estimatedTokens / 1000}k / ${maxTokens / 1000}k tokens (${(usageRatio * 100).toInt()}%)",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Right: Model & Reasoning Effort
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val displayModel = if (activeModel.isNotBlank()) {
                        activeModel.substringAfterLast("/")
                    } else {
                        agentKind.displayName
                    }

                    Text(
                        displayModel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    // Effort toggle pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                            .clickable { onCycleEffort() }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            "EFFORT: ${reasoningEffort.uppercase()}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = NeoLime,
                        )
                    }

                    // Auto-Bypass tool approval toggle pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (autoApproveTools) NeoLime else MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, if (autoApproveTools) NeoBlack else borderColor, RoundedCornerShape(4.dp))
                            .clickable { onToggleAutoApprove() }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            if (autoApproveTools) "BYPASS: ON" else "BYPASS: OFF",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (autoApproveTools) NeoBlack else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Visual linear progress indicator
            LinearProgressIndicator(
                progress = { usageRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp)),
                color = NeoLime,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
private fun LiveClaudeProcess(
    processItems: List<ActivityItem>,
    isRunning: Boolean,
    startedAtMillis: Long?,
    finishedAtMillis: Long?,
    thinkingActive: Boolean,
) {
    val elapsedSeconds = startedAtMillis?.let { rememberLiveElapsedSeconds(it).toLong() } ?: 0L
    ClaudeActivityDisclosure(
        items = processItems,
        headline = activityHeadline(processItems, elapsedSeconds, thinkingActive),
        isRunning = isRunning,
    )
}

@Composable
private fun WorkBlockCard(message: ChatMessage) {
    val seconds = (message.workedMillis / 1_000L).coerceAtLeast(1L)
    Column {
        ClaudeActivityDisclosure(
            items = message.workItems,
            headline = activityHeadline(message.workItems, seconds, message.workItems.isEmpty()),
        )
        if (message.workItems.lastOrNull()?.title?.startsWith("Task stopped") == true) {
            Text(
                text = "Worked for ${formatDuration(seconds)}",
                modifier = Modifier.padding(start = 29.dp, bottom = 6.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun ClaudeActivityDisclosure(
    items: List<ActivityItem>,
    headline: String,
    isRunning: Boolean = false,
) {
    var expandedItems by rememberSaveable { mutableStateOf(emptyList<Int>()) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp)) {
        if (items.isEmpty()) {
            ActivitySummaryRow(
                item = null,
                text = headline,
                expanded = 0 in expandedItems,
                showProgress = isRunning,
                onToggle = {
                    expandedItems = if (0 in expandedItems) expandedItems - 0 else expandedItems + 0
                },
            )
            if (0 in expandedItems) ActivityExpandedDetail(null, "Reviewing the request and planning the next action.")
        } else {
            items.forEachIndexed { index, item ->
                ActivitySummaryRow(
                    item = item,
                    text = compactActivityText(item),
                    expanded = index in expandedItems,
                    showProgress = isRunning && !item.isComplete,
                    onToggle = {
                        expandedItems = if (index in expandedItems) expandedItems - index else expandedItems + index
                    },
                )
                if (index in expandedItems) ActivityExpandedDetail(item, activityDetail(item))
            }
        }
    }
}

@Composable
private fun AnimatedThinkingDots(
    modifier: Modifier = Modifier,
    dotColor: Color = NeoLime,
) {
    val transition = rememberInfiniteTransition(label = "thinking_dots")
    val dot1Offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = -3.5f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1100
                0f at 0
                -3.5f at 220
                0f at 440
                0f at 1100
            },
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(0),
        ),
        label = "dot1",
    )
    val dot2Offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = -3.5f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1100
                0f at 0
                -3.5f at 220
                0f at 440
                0f at 1100
            },
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(180),
        ),
        label = "dot2",
    )
    val dot3Offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = -3.5f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1100
                0f at 0
                -3.5f at 220
                0f at 440
                0f at 1100
            },
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(360),
        ),
        label = "dot3",
    )

    val dot1Alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1100
                0.35f at 0
                1f at 220
                0.35f at 440
                0.35f at 1100
            },
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(0),
        ),
        label = "dot1_alpha",
    )
    val dot2Alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1100
                0.35f at 0
                1f at 220
                0.35f at 440
                0.35f at 1100
            },
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(180),
        ),
        label = "dot2_alpha",
    )
    val dot3Alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1100
                0.35f at 0
                1f at 220
                0.35f at 440
                0.35f at 1100
            },
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(360),
        ),
        label = "dot3_alpha",
    )

    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(3.5.dp)
                .graphicsLayer { translationY = dot1Offset * density }
                .background(dotColor.copy(alpha = dot1Alpha), CircleShape),
        )
        Box(
            Modifier
                .size(3.5.dp)
                .graphicsLayer { translationY = dot2Offset * density }
                .background(dotColor.copy(alpha = dot2Alpha), CircleShape),
        )
        Box(
            Modifier
                .size(3.5.dp)
                .graphicsLayer { translationY = dot3Offset * density }
                .background(dotColor.copy(alpha = dot3Alpha), CircleShape),
        )
    }
}

@Composable
private fun ActivitySummaryRow(
    item: ActivityItem?,
    text: String,
    expanded: Boolean,
    showProgress: Boolean,
    onToggle: () -> Unit,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            activityIcon(item),
            null,
            Modifier.size(16.dp),
            tint = muted,
        )
        Spacer(Modifier.width(9.dp))
        Text(text, Modifier.weight(1f), fontSize = 13.sp, color = muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (showProgress) {
            AnimatedThinkingDots(dotColor = muted)
            Spacer(Modifier.width(6.dp))
        }
        Icon(
            if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            if (expanded) "Collapse activity" else "Expand activity",
            Modifier.size(18.dp),
            tint = muted,
        )
    }
}

private fun activityIcon(item: ActivityItem?): ImageVector {
    if (item == null) return Icons.Default.AutoAwesome
    val task = item.title
        .removePrefix("Running ")
        .removeSuffix(" completed")
        .trim()
    return when {
        item.isCommand || task.equals("Bash", ignoreCase = true) -> Icons.Default.Terminal
        task.equals("Write", ignoreCase = true) ||
            task.equals("Edit", ignoreCase = true) ||
            task.equals("NotebookEdit", ignoreCase = true) -> Icons.Default.Edit
        task.equals("Read", ignoreCase = true) -> Icons.Default.Description
        task.equals("Glob", ignoreCase = true) ||
            task.equals("Grep", ignoreCase = true) -> Icons.Default.Search
        task.contains("file", ignoreCase = true) -> Icons.Default.Description
        else -> Icons.Default.AutoAwesome
    }
}

@Composable
private fun ActivityExpandedDetail(item: ActivityItem?, detail: String) {
    if (item?.isCommand == true) {
        Text(
            detail,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 25.dp, end = 8.dp, bottom = 8.dp),
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
        )
    } else {
        MarkdownText(
            markdown = detail,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 25.dp, end = 8.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun compactActivityText(item: ActivityItem): String = "${activityName(item)} · ${activityDetail(item).replace(Regex("\\s+"), " ").take(105)}"

private fun activityDetail(item: ActivityItem): String {
    if (item.title == "Think" && item.detail.contains("reasoning tokens processed", true)) {
        return "Reviewed the request and planned the next action"
    }
    return item.detail.ifBlank { item.title }
}

private fun activityHeadline(items: List<ActivityItem>, seconds: Long, thinking: Boolean): String {
    val latest = items.lastOrNull()
    if (latest == null) return "Think · Analyzing the request · ${formatDuration(seconds)}"
    if (thinking && latest.title == "Think") return "Think · ${latest.detail} · ${formatDuration(seconds)}"
    val detail = latest.detail.replace(Regex("\\s+"), " ").trim().ifBlank { latest.title }
    return "${activityName(latest)} · ${detail.take(100)} · ${formatDuration(seconds)}"
}

private fun activityName(item: ActivityItem): String = item.title
    .removePrefix("Running ")
    .removeSuffix(" completed")
    .replaceFirstChar { it.uppercase() }

private fun completedProcessSummary(
    processItems: List<ActivityItem>,
    startedAtMillis: Long?,
    finishedAtMillis: Long?,
): String {
    val stopped = processItems.lastOrNull()?.title?.startsWith("Task stopped") == true
    val outcome = if (stopped) "Task stopped" else "Task completed"
    val steps = "${processItems.size} step${if (processItems.size == 1) "" else "s"}"
    val duration = startedAtMillis?.let { start ->
        val end = finishedAtMillis ?: System.currentTimeMillis()
        formatDuration(((end - start) / 1000L).coerceAtLeast(0))
    }
    return if (duration != null) "$outcome · $duration · $steps" else "$outcome · $steps"
}

@Composable
private fun rememberLiveElapsedSeconds(startedAtMillis: Long): Int {
    var seconds by remember(startedAtMillis) {
        mutableIntStateOf(((System.currentTimeMillis() - startedAtMillis) / 1000L).toInt().coerceAtLeast(0))
    }
    LaunchedEffect(startedAtMillis) {
        while (true) {
            delay(1_000)
            seconds = ((System.currentTimeMillis() - startedAtMillis) / 1000L).toInt().coerceAtLeast(0)
        }
    }
    return seconds
}

private fun formatDuration(totalSeconds: Long): String = when {
    totalSeconds >= 3_600 -> "${totalSeconds / 3_600}h ${(totalSeconds % 3_600) / 60}m"
    totalSeconds >= 60 -> "${totalSeconds / 60}m ${totalSeconds % 60}s"
    else -> "${totalSeconds}s"
}

@Composable
private fun MessageBubble(message: ChatMessage, onRunInTerminal: (String) -> Unit, onOpenAttachment: (ChatAttachment) -> Unit) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val bubbleBorder = if (isDark) NeoDarkBorder else NeoBlack
    val bubbleBg = if (message.fromUser) NeoLime else MaterialTheme.colorScheme.surface
    val bubbleCornerRadius = 14.dp

    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start) {
        Box(
            modifier = Modifier
                .fillMaxWidth(if (message.fromUser) .84f else .94f)
                .padding(bottom = 6.dp)
                .neoShadow(
                    offsetX = if (message.fromUser) 3.5.dp else 3.dp,
                    offsetY = if (message.fromUser) 3.5.dp else 3.dp,
                    cornerRadius = bubbleCornerRadius,
                )
                .background(bubbleBg, RoundedCornerShape(bubbleCornerRadius))
                .border(2.dp, bubbleBorder, RoundedCornerShape(bubbleCornerRadius)),
        ) {
            Column(Modifier.padding(top = 12.dp)) {
                SelectionContainer {
                    if (message.fromUser) {
                        Text(
                            text = message.text,
                            modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 8.dp),
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp, fontWeight = FontWeight.Medium),
                            color = NeoBlack,
                        )
                    } else {
                        MarkdownText(
                            markdown = message.text,
                            modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            onRunCode = onRunInTerminal,
                        )
                    }
                }
                if (!message.fromUser && message.workedMillis > 0L) {
                    Text(
                        text = "Worked for ${formatDuration((message.workedMillis / 1_000L).coerceAtLeast(1L))}",
                        modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 10.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                }
                if (message.attachments.isNotEmpty()) {
                    Column(
                        Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        message.attachments.forEach { attachment ->
                            AttachmentChip(attachment = attachment, onOpen = { onOpenAttachment(attachment) }, onRemove = null)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun AttachmentChip(
    attachment: ChatAttachment,
    onOpen: (() -> Unit)?,
    onRemove: (() -> Unit)?,
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val icon = when {
        attachment.mimeType.startsWith("image/") -> Icons.Default.Image
        else -> Icons.Default.Description
    }
    Box(
        modifier = Modifier
            .then(if (onOpen != null) Modifier.clickable(onClick = onOpen) else Modifier)
            .neoShadow(offsetX = 2.dp, offsetY = 2.dp, cornerRadius = 10.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
            .border(1.5.dp, if (isDark) NeoDarkBorder else NeoBlack, RoundedCornerShape(10.dp)),
    ) {
        Row(Modifier.padding(start = 9.dp, end = if (onRemove == null) 10.dp else 3.dp, top = 7.dp, bottom = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(17.dp), tint = NeoLime)
            Spacer(Modifier.width(7.dp))
            Column(Modifier.widthIn(max = 180.dp)) {
                Text(attachment.displayName, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatFileSize(attachment.sizeBytes), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onRemove != null) {
                IconButton(onClick = onRemove, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Close, "Remove attachment", Modifier.size(15.dp))
                }
            }
        }
    }
}

@Composable
private fun ApprovalCard(request: ToolRequest, onApproval: (Boolean) -> Unit) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .neoShadow(offsetX = 3.5.dp, offsetY = 3.5.dp, cornerRadius = 14.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .border(2.dp, if (isDark) NeoDarkBorder else NeoBlack, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(30.dp)
                        .background(NeoLime.copy(alpha = 0.2f), CircleShape)
                        .border(1.5.dp, NeoLime, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Warning, null, tint = NeoLime, modifier = Modifier.size(17.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text("Review this action", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Text(request.explanation, fontSize = 13.5.sp)
            request.affectedPaths.forEach { Text("• $it", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NeoButton(
                    onClick = { onApproval(false) },
                    modifier = Modifier.weight(1f).height(44.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Text("Reject", fontWeight = FontWeight.Bold)
                }
                NeoButton(
                    onClick = { onApproval(true) },
                    modifier = Modifier.weight(1f).height(44.dp),
                    containerColor = NeoLime,
                    contentColor = NeoBlack,
                ) {
                    Text("Allow once", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FilesTab(files: List<WorkspaceEntry>, loading: Boolean, onRefresh: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Project files", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (loading) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Refresh files") }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        if (!loading && files.isEmpty()) {
            item { EmptyState(Icons.Default.Folder, "No files yet", "Ask your coding agent to create something in this project.") }
        }
        items(files, key = { it.path }) { entry ->
            Row(
                Modifier.fillMaxWidth().padding(start = (entry.depth * 20).dp).padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (entry.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                    null,
                    tint = if (entry.isDirectory) NeoLime else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(11.dp))
                Text(entry.name, Modifier.weight(1f))
                if (!entry.isDirectory) {
                    Text(formatFileSize(entry.sizeBytes), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1_024 -> "$bytes B"
    bytes < 1_048_576 -> "%.1f KB".format(bytes / 1_024.0)
    else -> "%.1f MB".format(bytes / 1_048_576.0)
}

@Composable
private fun ChangesTab(
    changes: List<ChangeItem>,
    onUndo: () -> Unit,
    onKeep: () -> Unit,
    onUndoFile: (String) -> Unit,
    onKeepFile: (String) -> Unit,
) {
    var expandedPath by rememberSaveable { mutableStateOf<String?>(null) }
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            NeoCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 14.dp,
                shadowOffset = 3.dp,
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text("Changes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Review everything the AI changed.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (changes.isEmpty()) item { EmptyState(Icons.Default.Code, "No changes yet", "Ask Mobile Harness to update your project.") }
        items(changes, key = { it.path }) { change ->
            val expanded = expandedPath == change.path
            NeoCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 14.dp,
                shadowOffset = 3.5.dp,
            ) {
                Column {
                    Row(
                        Modifier.fillMaxWidth().clickable { expandedPath = if (expanded) null else change.path }.padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Description, null, tint = NeoLime)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(change.path, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(
                                if (expanded) "Hide line-by-line diff" else "Tap to review diff",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (change.additions > 0) {
                            NeoBadge(text = "+${change.additions}", containerColor = NeoLime, contentColor = NeoBlack)
                            Spacer(Modifier.width(6.dp))
                        }
                        if (change.deletions > 0) {
                            NeoBadge(text = "-${change.deletions}", containerColor = Color(0xFFFF5252), contentColor = Color.White)
                        }
                    }
                    if (expanded) {
                        HorizontalDivider(thickness = 1.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        Column(
                            Modifier.fillMaxWidth().background(Color(0xFF090C10)).horizontalScroll(rememberScrollState()),
                        ) {
                            change.diffLines.forEach { line -> DiffLineRow(line) }
                        }
                        HorizontalDivider(thickness = 1.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            NeoButton(
                                onClick = {
                                    expandedPath = null
                                    onUndoFile(change.path)
                                },
                                modifier = Modifier.weight(1f),
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.error,
                                borderColor = MaterialTheme.colorScheme.error,
                            ) { Text("Undo file", fontWeight = FontWeight.Bold) }
                            NeoButton(
                                onClick = {
                                    expandedPath = null
                                    onKeepFile(change.path)
                                },
                                modifier = Modifier.weight(1f),
                                containerColor = NeoLime,
                                contentColor = NeoBlack,
                            ) { Text("Keep file", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
        if (changes.isNotEmpty()) item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NeoButton(
                    onClick = onUndo,
                    modifier = Modifier.weight(1f).height(50.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.error,
                    borderColor = MaterialTheme.colorScheme.error,
                ) { Text("Undo task", fontWeight = FontWeight.Bold) }
                NeoButton(
                    onClick = onKeep,
                    modifier = Modifier.weight(1f).height(50.dp),
                    containerColor = NeoLime,
                    contentColor = NeoBlack,
                ) { Text("Keep changes", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun DiffLineRow(line: DiffLine) {
    val marker = when (line.type) {
        DiffLineType.ADDITION -> "+"
        DiffLineType.DELETION -> "-"
        DiffLineType.CONTEXT -> " "
        DiffLineType.INFO -> "·"
    }
    val background = when (line.type) {
        DiffLineType.ADDITION -> Color(0xFF142410)
        DiffLineType.DELETION -> Color(0xFF38151A)
        else -> Color.Transparent
    }
    val foreground = when (line.type) {
        DiffLineType.ADDITION -> NeoLime
        DiffLineType.DELETION -> Color(0xFFFFA4A4)
        DiffLineType.INFO -> Color(0xFF8993A4)
        DiffLineType.CONTEXT -> Color(0xFFD5DAE3)
    }
    val oldNumber = line.oldLine?.toString().orEmpty().padStart(4)
    val newNumber = line.newLine?.toString().orEmpty().padStart(4)
    Text(
        text = "$oldNumber $newNumber  $marker ${line.text}",
        modifier = Modifier.fillMaxWidth().background(background).padding(horizontal = 8.dp, vertical = 2.dp),
        color = foreground,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        softWrap = false,
    )
}

@Composable
private fun PreviewTab(ready: Boolean, url: String?) {
    var address by rememberSaveable(url) { mutableStateOf(if (ready) url.orEmpty() else "") }
    var activeUrl by rememberSaveable(url) { mutableStateOf(if (ready) url else null) }
    var addressError by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    val navigate = {
        val normalized = normalizePreviewUrl(address)
        if (normalized == null) {
            addressError = "Use a local URL such as localhost:3000"
        } else {
            addressError = null
            address = normalized
            activeUrl = normalized
        }
    }

    LaunchedEffect(ready, url) {
        if (ready && !url.isNullOrBlank() && activeUrl == null) {
            normalizePreviewUrl(url)?.let {
                address = it
                activeUrl = it
            }
        }
    }

    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) NeoDarkBorder else NeoBlack

    Column(Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 2.dp, color = borderColor),
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                // Retro window controls row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(10.dp).background(Color(0xFFFF5252), CircleShape).border(1.dp, NeoBlack, CircleShape))
                        Box(Modifier.size(10.dp).background(Color(0xFFFFD600), CircleShape).border(1.dp, NeoBlack, CircleShape))
                        Box(Modifier.size(10.dp).background(NeoLime, CircleShape).border(1.dp, NeoBlack, CircleShape))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "WEB PREVIEW",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.weight(1f))
                    if (activeUrl != null) {
                        NeoBadge(text = "LIVE", containerColor = NeoLime, contentColor = NeoBlack, isPulsing = true)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = address,
                        onValueChange = {
                            address = it
                            addressError = null
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Preview URL") },
                        placeholder = { Text("localhost:3000") },
                        shape = RoundedCornerShape(10.dp),
                        leadingIcon = {
                            if (activeUrl != null) {
                                PulsingDot(color = NeoLime, size = 7.dp)
                            } else {
                                Box(
                                    Modifier.size(8.dp).background(MaterialTheme.colorScheme.outline, CircleShape),
                                )
                            }
                        },
                        trailingIcon = {
                            IconButton(onClick = navigate, modifier = Modifier.neoBounce()) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open URL", tint = NeoLime)
                            }
                        },
                        isError = addressError != null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go,
                        ),
                        keyboardActions = KeyboardActions(onGo = { navigate() }),
                    )
                    Spacer(Modifier.width(6.dp))
                    IconButton(
                        onClick = { webView?.reload() ?: navigate() },
                        enabled = address.isNotBlank(),
                        modifier = Modifier.neoBounce(),
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh preview", tint = if (address.isNotBlank()) NeoLime else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (addressError != null) {
                    Text(
                        addressError.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 3.dp),
                    )
                } else if (loading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp), color = NeoLime)
                }
            }
        }
        val targetUrl = activeUrl
        if (targetUrl == null) {
            EmptyState(Icons.Default.PlayArrow, "Preview not running", "Enter a localhost URL above, or start a local web server in the project Terminal.")
        } else {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webView = this
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loading = newProgress < 100
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val target = request?.url ?: return true
                                if (!target.isLoopbackPreviewUrl()) {
                                    addressError = "External navigation is blocked in project preview"
                                    return true
                                }
                                address = target.toString()
                                return false
                            }

                            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                                val target = request?.url ?: return blockedPreviewResponse()
                                return if (target.isLoopbackPreviewUrl()) null else blockedPreviewResponse()
                            }
                        }
                        loadUrl(targetUrl)
                    }
                },
                update = { current ->
                    webView = current
                    if (current.url != targetUrl) current.loadUrl(targetUrl)
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun normalizePreviewUrl(input: String): String? {
    val raw = input.trim()
    if (raw.isBlank()) return null
    val withScheme = if ("://" in raw) raw else "http://$raw"
    val parsed = runCatching { Uri.parse(withScheme) }.getOrNull() ?: return null
    if (!parsed.isLoopbackPreviewUrl() || parsed.host.isNullOrBlank()) return null
    return if (parsed.host == "0.0.0.0") {
        parsed.buildUpon().encodedAuthority(
            buildString {
                append("127.0.0.1")
                if (parsed.port >= 0) append(":${parsed.port}")
            },
        ).build().toString()
    } else {
        parsed.toString()
    }
}

private fun Uri.isLoopbackPreviewUrl(): Boolean =
    scheme in setOf("data", "blob", "about") ||
        (scheme in setOf("http", "https", "ws", "wss") && host in setOf("127.0.0.1", "localhost", "0.0.0.0"))

private fun blockedPreviewResponse(): WebResourceResponse =
    WebResourceResponse("text/plain", "UTF-8", 403, "Blocked", emptyMap(), ByteArrayInputStream(ByteArray(0)))

@Composable
private fun EmptyState(icon: ImageVector, title: String, body: String) {
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun BrandMark(modifier: Modifier = Modifier, compact: Boolean = false) {
    val size = if (compact) 34.dp else 50.dp
    val iconSize = if (compact) 18.dp else 26.dp
    val cornerRadius = if (compact) 8.dp else 12.dp

    val infiniteTransition = rememberInfiniteTransition(label = "brandBreath")
    val rot by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "brandRot",
    )

    Box(
        modifier = modifier
            .graphicsLayer { rotationZ = rot }
            .neoShadow(
                offsetX = if (compact) 2.5.dp else 4.dp,
                offsetY = if (compact) 2.5.dp else 4.dp,
                cornerRadius = cornerRadius,
            )
            .size(size)
            .background(
                color = NeoLime,
                shape = RoundedCornerShape(cornerRadius),
            )
            .border(
                width = 2.dp,
                color = if (androidx.compose.foundation.isSystemInDarkTheme()) NeoDarkBorder else NeoBlack,
                shape = RoundedCornerShape(cornerRadius),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Terminal,
            contentDescription = "Mobile Harness",
            modifier = Modifier.size(iconSize),
            tint = NeoBlack,
        )
    }
}
