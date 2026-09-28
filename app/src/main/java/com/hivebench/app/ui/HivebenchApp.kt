package com.hivebench.app.ui

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
import com.hivebench.app.BuildConfig
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
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
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
import com.hivebench.app.ui.neo.AlertDialog
import com.hivebench.app.ui.neo.AssistChip
import com.hivebench.app.ui.neo.Button
import com.hivebench.app.ui.neo.ButtonDefaults
import com.hivebench.app.ui.neo.CircularProgressIndicator
import com.hivebench.app.ui.neo.DropdownMenu
import com.hivebench.app.ui.neo.DropdownMenuItem
import com.hivebench.app.ui.neo.Icon
import com.hivebench.app.ui.neo.IconButton
import com.hivebench.app.ui.neo.LinearProgressIndicator
import com.hivebench.app.ui.neo.HorizontalDivider
import com.hivebench.app.ui.neo.MaterialTheme
import com.hivebench.app.ui.neo.NavigationBar
import com.hivebench.app.ui.neo.NavigationBarItem
import com.hivebench.app.ui.neo.NavigationBarItemDefaults
import com.hivebench.app.ui.neo.OutlinedButton
import com.hivebench.app.ui.neo.OutlinedTextField
import com.hivebench.app.ui.neo.Scaffold
import com.hivebench.app.ui.neo.Surface
import com.hivebench.app.ui.neo.Text
import com.hivebench.app.ui.neo.TextButton
import com.hivebench.app.ui.neo.TopAppBar
import com.hivebench.app.ui.neo.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hivebench.app.ui.neo.DrawerDetent
import com.hivebench.app.ui.neo.NeoBottomDrawer
import com.hivebench.app.ui.neo.NeoDrawerRow
import com.hivebench.app.ui.neo.NeoHistoryDrawer
import com.hivebench.app.ui.neo.NeoHistoryOption
import com.hivebench.app.ui.neo.NeoModelDrawer
import com.hivebench.app.ui.neo.NeoModelOption
import com.hivebench.app.ui.neo.NeoRepoDrawer
import com.hivebench.app.ui.neo.NeoRepoOption
import com.hivebench.app.ui.neo.NeoStagger
import com.hivebench.app.model.ActivityItem
import com.hivebench.app.model.AgentKind
import com.hivebench.app.model.ChangeItem
import com.hivebench.app.model.ChatMessage
import com.hivebench.app.model.ChatAttachment
import com.hivebench.app.model.DevStack
import com.hivebench.app.model.DEEPSEEK_HARNESS_PROVIDERS
import com.hivebench.app.model.DSH_PROTOCOL_PROVIDERS
import com.hivebench.app.model.DiffLine
import com.hivebench.app.model.DiffLineType
import com.hivebench.app.model.Project
import com.hivebench.app.model.ProjectKind
import com.hivebench.app.model.ProjectChat
import com.hivebench.app.model.ProviderKind
import com.hivebench.app.model.ProviderProfile
import com.hivebench.app.model.inferredDshApiForUrl
import com.hivebench.app.model.providersForAgent
import com.hivebench.app.model.ToolRequest
import com.hivebench.app.model.WorkspaceEntry
import com.hivebench.app.model.projectSlug
import com.hivebench.app.runtime.RuntimeExecutionService
import com.hivebench.app.runtime.RuntimeSetupService
import com.hivebench.app.runtime.supportsArm64Runtime
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.hivebench.app.network.ConnectionValidation
import com.hivebench.app.network.DiscoveredModel
import com.hivebench.app.network.ModelDiscoveryResult
import com.hivebench.app.network.GitHubRepository
import com.hivebench.app.ui.theme.PocketBlue
import com.hivebench.app.ui.theme.AppThemeMode
import com.hivebench.app.ui.theme.PocketGreen
import com.hivebench.app.ui.theme.PocketOrange
import com.hivebench.app.ui.theme.NeoLime
import com.hivebench.app.ui.theme.NeoBlack
import com.hivebench.app.ui.theme.NeoDarkBorder
import com.hivebench.app.ui.theme.neoShadow
import com.hivebench.app.ui.theme.neoTactile
import com.hivebench.app.ui.theme.neoBounce
import com.hivebench.app.ui.theme.neoBorder
import com.hivebench.app.ui.theme.PulsingDot
import com.hivebench.app.ui.theme.NeoBadge
import com.hivebench.app.ui.theme.NeoCard
import com.hivebench.app.ui.theme.NeoButton
import java.io.ByteArrayInputStream
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.Terminal

private enum class RootScreen(val label: String, val icon: ImageVector) {
    PROJECTS("Projects", Icons.Default.Folder),
    AGENT("Agent", Icons.Default.SmartToy),
    TERMINAL("Terminal", Icons.Default.Terminal),
    SETTINGS("Settings", Icons.Default.Settings),
}
private enum class WorkspaceTab(val label: String, val icon: ImageVector) {
    CHAT("Session", Icons.Default.AutoAwesome),
    OFFICE("Office", Icons.Default.Groups),
    AGENT("Agents", Icons.Default.SmartToy),
    FILES("Files", Icons.Default.Folder),
    CHANGES("Git", Icons.Default.Commit),
    TERMINAL("Terminal", Icons.Default.Terminal),
    PREVIEW("Browser", Icons.Default.Language),
}

@Composable
fun HivebenchApp(viewModel: MainViewModel = viewModel()) {
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
            agentConfigurationContent = { com.hivebench.app.ui.agents.AgentsHubScreen(state, viewModel) },
            agentSessionContent = { openAgents -> com.hivebench.app.ui.agents.AgentSessionTab(state, viewModel, openAgents) },
            sessionsDrawer = { onDismiss, onSwitched, onOpenAgents -> com.hivebench.app.ui.agents.AgentSessionsDrawer(state, viewModel, onDismiss, onSwitched, onOpenAgents) },
            officeContent = { com.hivebench.app.ui.hive.OfficeScreen(state, viewModel) },
            terminalContent = {
                state.activeProject?.let { project ->
                    com.hivebench.app.terminal.ShellTerminal(
                        sessionKey = "shell:${project.id}",
                        title = "Project terminal",
                        subtitle = "/workspace/${project.slug} · bash",
                        launch = { viewModel.shellLaunch(project) },
                        attachments = viewModel.terminalAttachmentTarget(),
                    )
                }
            },
            onBack = viewModel::closeProject,
            onRefreshFiles = viewModel::refreshProjectFiles,
            onOpenFile = viewModel::openFile,
            onCloseFile = viewModel::closeFile,
            onUseSuggestedProjectRoot = viewModel::useSuggestedProjectRoot,
            onExportProject = viewModel::exportActiveProject,
            onBuildAndRunAndroid = viewModel::buildAndRunAndroidApp,
            onRefreshGit = viewModel::refreshGitState,
            onInitGitRepository = viewModel::initGitRepository,
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
            onGenerateNewGitHubCode = viewModel::generateNewGitHubCode,
            browserContent = { openSession ->
                com.hivebench.app.browser.BrowserInspectionScreen(
                    projectKey = state.activeProject?.id ?: "none",
                    ready = state.previewReady,
                    url = state.previewUrl,
                    agentTargets = viewModel.browserAgentTargets(),
                    onPushToAgent = { targetId, text -> viewModel.pushToAgent(targetId, text, openSession) },
                    onSaveCapture = viewModel::saveBrowserCapture,
                )
            },
        )
        else -> RootScreenHost(state, viewModel, projectsListState)
    }
}

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
        0 -> "See live progress and receive an alert when an agent finishes or needs your attention."
        1 -> "Allow Hivebench to continue a task when you lock the phone or switch to another app."
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
                        com.hivebench.app.ui.theme.HivebenchWordmark()
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
                "Setup time depends on the toolchains you choose next. You may leave Hivebench in the background while it works.",
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
    DevStack.DOCKER -> DevStackVisuals(
        icon = Icons.Default.Inventory,
        accentColor = Color(0xFF2496ED),
        tag = "engine + CLI + compose",
    )
}

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
                        com.hivebench.app.ui.theme.HivebenchWordmark()
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
                    text = "Your phone meets the requirements. Choose your coding tools next and Hivebench will handle the setup.",
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
                    Column(modifier = Modifier.clip(RoundedCornerShape(16.dp))) {
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
                    "Only the selected optional agent is downloaded. You can install or switch agents later from the Agents tab.",
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
                    Column(modifier = Modifier.clip(RoundedCornerShape(16.dp))) {
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
                        text = if (compatible) "Install Hivebench" else "Device not supported",
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

private const val CORE_RUNTIME_DOWNLOAD_MB = 70
private const val CLAUDE_RUNTIME_DOWNLOAD_MB = 72
private const val DSH_RUNTIME_DOWNLOAD_MB = 27
private const val AGY_RUNTIME_DOWNLOAD_MB = 40
private const val PYTHON_RUNTIME_DOWNLOAD_MB = 55
private const val ANDROID_RUNTIME_DOWNLOAD_MB = 570
// Engine/CLI (~65 MB) plus the Compose binary (~60 MB).
private const val DOCKER_RUNTIME_DOWNLOAD_MB = 125

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
    if (DevStack.DOCKER in selected) {
        minimumMinutes += 3
        maximumMinutes += 5
    }
    return "$minimumMinutes–$maximumMinutes minutes"
}

private fun stackDownloadLabel(stack: DevStack): String = when {
    stack == DevStack.WEB -> " · included"
    BuildConfig.OFFLINE_RUNTIME_BUNDLES && stack in setOf(DevStack.PYTHON, DevStack.ANDROID) -> " · included"
    !BuildConfig.OFFLINE_RUNTIME_BUNDLES && stack == DevStack.PYTHON -> " · 55 MB"
    !BuildConfig.OFFLINE_RUNTIME_BUNDLES && stack == DevStack.ANDROID -> " · 570 MB"
    !BuildConfig.OFFLINE_RUNTIME_BUNDLES && stack == DevStack.DOCKER -> " · 125 MB"
    else -> ""
}

private fun toolchainDownloadSummary(selected: Set<DevStack>, agent: AgentKind): String {
    if (BuildConfig.OFFLINE_RUNTIME_BUNDLES) return "All selected bundles are included in this offline app"
    val total = CORE_RUNTIME_DOWNLOAD_MB +
        when (agent) {
            AgentKind.CLAUDE_CODE -> CLAUDE_RUNTIME_DOWNLOAD_MB
            AgentKind.DEEPSEEK_HARNESS -> DSH_RUNTIME_DOWNLOAD_MB
            AgentKind.ANTIGRAVITY -> AGY_RUNTIME_DOWNLOAD_MB
            AgentKind.JCODE -> 18
            AgentKind.PI_AGENT -> 15
            AgentKind.COMMAND_CODE -> 21
            AgentKind.CLINE -> 32
            AgentKind.CUSTOM_RUNNER -> 5
            AgentKind.OPENCODE -> 25
            AgentKind.CODEX -> 29
        } +
        (if (DevStack.PYTHON in selected) PYTHON_RUNTIME_DOWNLOAD_MB else 0) +
        (if (DevStack.ANDROID in selected) ANDROID_RUNTIME_DOWNLOAD_MB else 0) +
        (if (DevStack.DOCKER in selected) DOCKER_RUNTIME_DOWNLOAD_MB else 0)
    val laterPackages = selected.intersect(setOf(DevStack.CPP, DevStack.PHP, DevStack.DOCKER))
    return buildString {
        append("Download: ")
        append(total)
        append(" MB")
        if (laterPackages.isNotEmpty()) append(" · Extra packages download later")
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
        DevStack.DOCKER -> "Containers, images, services and compose apps"
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
        AgentKind.OPENCODE -> Color(0xFF2DD4BF)
        AgentKind.CODEX -> Color(0xFF10A37F)
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
        AgentKind.OPENCODE -> "OC"
        AgentKind.CODEX -> "CX"
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
            Text(com.hivebench.app.runtime.AgentCatalog.spec(agent).sourceLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                        Text("Set up Hivebench", fontWeight = FontWeight.Bold)
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
                "You can leave Hivebench in the background and follow setup from the notification.",
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
                        com.hivebench.app.ui.theme.HivebenchWordmark()
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
                    .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(16.dp))
                    .border(2.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Warning, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(20.dp))
            Text(
                if (isOffline) "You're offline" else "Hivebench couldn't finish starting",
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

@Composable
private fun RootScreenHost(
    state: AppUiState,
    viewModel: MainViewModel,
    projectsListState: LazyListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() },
) {
    var screen by rememberSaveable { mutableStateOf(RootScreen.PROJECTS) }
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!keyboardVisible) {
                val isDark = androidx.compose.foundation.isSystemInDarkTheme()
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    RootScreen.entries.forEach { tab ->
                        val isSelected = screen == tab
                        val tabScale by androidx.compose.animation.core.animateFloatAsState(
                            targetValue = if (isSelected) 1.05f else 1.0f,
                            animationSpec = androidx.compose.animation.core.tween(durationMillis = 180),
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
                    onInstallUpdate = viewModel::installAppUpdate,
                )
                RootScreen.AGENT -> com.hivebench.app.ui.agents.AgentsHubScreen(state, viewModel)
                RootScreen.TERMINAL -> com.hivebench.app.terminal.ShellTerminal(
                    sessionKey = "shell:root",
                    title = "Terminal",
                    subtitle = "bash · Ubuntu 24.04 (PRoot)",
                    launch = { viewModel.shellLaunch(null) },
                )
                RootScreen.SETTINGS -> SettingsScreen(
                    state = state,
                    onSetThemeMode = viewModel::setThemeMode,
                    onInstallDevStack = viewModel::installDevStack,
                    onRemoveDevStack = viewModel::removeDevStack,
                    onCheckAppUpdate = { viewModel.checkForAppUpdate(force = true) },
                    initialDebugUpdateManifestUrl = viewModel.debugUpdateManifestUrl(),
                    onSetDebugUpdateManifestUrl = viewModel::setDebugUpdateManifestUrl,
                    onClearDebugUpdateManifestUrl = viewModel::clearDebugUpdateManifestUrl,
                )
            }
        }
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
    onInstallUpdate: () -> Unit,
) {
    val liveSessions by com.hivebench.app.terminal.TerminalSessions.info.collectAsState()
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
        applySystemInsets = false,
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = 8.dp),
                title = { Row(verticalAlignment = Alignment.CenterVertically) { BrandMark(compact = true); Spacer(Modifier.width(9.dp)); com.hivebench.app.ui.theme.HivebenchWordmark() } },
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
                Text("Run coding agents, review changes, and preview your project.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                val importBorderColor = if (isSystemInDarkTheme()) NeoDarkBorder else NeoBlack
                val importShape = RoundedCornerShape(14.dp)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neoShadow(offsetX = 3.dp, offsetY = 3.dp, cornerRadius = 14.dp)
                        .border(2.dp, importBorderColor, importShape),
                    shape = importShape,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { importExpanded = !importExpanded }
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(NeoLime, RoundedCornerShape(9.dp))
                                    .border(2.dp, importBorderColor, RoundedCornerShape(9.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = NeoBlack, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Bring an existing project", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                                Text(
                                    if (isImportExpanded) "Import files or clone complete Git history" else "ZIP file, Git repository, or GitHub",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(NeoLime, RoundedCornerShape(8.dp))
                                    .border(1.5.dp, importBorderColor, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (isImportExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isImportExpanded) "Collapse" else "Expand",
                                    tint = NeoBlack,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
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
                                Text("Hivebench ${update.versionName}", fontWeight = FontWeight.Bold)
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
                        taskRunning = liveSessions.values.any { info ->
                            info.running && (info.key.startsWith("agent:${project.id}:") || info.key.startsWith("hive:${project.id}:"))
                        },
                        terminalRunning = liveSessions["shell:${project.id}"]?.running == true,
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
    if (showGitHubDialog && state.githubAuthStatus == GitHubAuthStatus.CONNECTED) {
        NeoRepoDrawer(
            visible = true,
            onDismiss = { if (!state.gitCloneRunning) showGitHubDialog = false },
            repos = state.githubRepositories
                .filter { repositorySearch.isBlank() || it.fullName.contains(repositorySearch, ignoreCase = true) }
                .map { NeoRepoOption(fullName = it.fullName, description = "${if (it.private) "Private" else "Public"} · ${it.defaultBranch}") },
            search = repositorySearch,
            onSearchChange = { repositorySearch = it },
            onSelect = { option ->
                state.githubRepositories.firstOrNull { it.fullName == option.fullName }?.let {
                    showGitHubDialog = false
                    onCloneGitHub(it)
                }
            },
            isLoading = state.githubRepositoriesLoading,
            onRefresh = onRefreshGitHub,
            login = state.githubLogin,
        )
    }
    if (showGitHubDialog && state.githubAuthStatus != GitHubAuthStatus.CONNECTED) {
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
                        Text("Tap the code to copy it. Hivebench will connect automatically after approval.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text(update.notes.ifBlank { "Get the latest improvements and fixes for Hivebench." })
                    if (update.sizeBytes > 0) Text("Download size: ${formatMegabytes(update.sizeBytes)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    if (!canInstall) {
                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f)) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Allow ‘Install unknown apps’ for Hivebench. Without this permission, Android will not install the update.", fontSize = 13.sp)
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
            ChatHistoryView(
                messages = state.readOnlyMessages,
                listState = listState,
                onContinueHere = onContinueHere,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WorkspaceScreen(
    state: AppUiState,
    agentConfigurationContent: @Composable () -> Unit = {},
    agentSessionContent: @Composable (onOpenAgents: () -> Unit) -> Unit = {},
    sessionsDrawer: @Composable (onDismiss: () -> Unit, onSwitched: () -> Unit, onOpenAgents: () -> Unit) -> Unit = { _, _, _ -> },
    officeContent: @Composable () -> Unit = {},
    /** Project shell (real PTY), kept alive across tab switches. */
    terminalContent: @Composable () -> Unit = {},
    onBack: () -> Unit,
    onRefreshFiles: () -> Unit,
    onOpenFile: (WorkspaceEntry) -> Unit,
    onCloseFile: () -> Unit,
    onUseSuggestedProjectRoot: () -> Unit,
    onExportProject: (Uri) -> Unit,
    onBuildAndRunAndroid: () -> Unit,
    onRefreshGit: () -> Unit = {},
    onInitGitRepository: () -> Unit = {},
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
    onSelectCommit: (com.hivebench.app.model.GitCommit?) -> Unit = {},
    onCreatePullRequest: (String, String, String) -> Unit = { _, _, _ -> },
    onStartGitHubLogin: () -> Unit = {},
    onGenerateNewGitHubCode: () -> Unit = {},
    /** Browser inspector; its argument switches to the Session tab after pushing to an agent. */
    browserContent: @Composable (openSession: () -> Unit) -> Unit = {},
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val isAndroidProject = state.androidProjectDetected
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    val exportProjectLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri -> if (uri != null) onExportProject(uri) },
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

    var selectedTab by rememberSaveable { mutableStateOf(WorkspaceTab.CHAT) }
    var showChats by rememberSaveable { mutableStateOf(false) }

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
        sessionsDrawer(
            { showChats = false },
            {
                showChats = false
                selectedTab = WorkspaceTab.CHAT
            },
            {
                showChats = false
                selectedTab = WorkspaceTab.AGENT
            },
        )
    }

    // Sign-in started from the Git tab: the one-time code must be visible here, not only on the Projects screen.
    var githubDialogDismissed by remember(state.githubUserCode) { mutableStateOf(false) }
    if (!githubDialogDismissed &&
        (state.githubAuthStatus == GitHubAuthStatus.STARTING || state.githubAuthStatus == GitHubAuthStatus.AWAITING_USER)
    ) {
        val clipboard = LocalClipboardManager.current
        AlertDialog(
            onDismissRequest = { githubDialogDismissed = true },
            icon = { Icon(Icons.Default.Code, null, tint = NeoLime) },
            title = { Text("Connect GitHub") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (state.githubAuthStatus == GitHubAuthStatus.STARTING || state.githubUserCode == null) {
                        CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.5.dp, color = NeoLime)
                        Text(state.githubMessage ?: "Starting GitHub sign-in…")
                    } else {
                        Text("Enter this one-time code on the GitHub page opened in your browser.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { clipboard.setText(AnnotatedString(state.githubUserCode)) },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                state.githubUserCode,
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                letterSpacing = 2.sp,
                            )
                        }
                        Text("Tap the code to copy it. The Git tab connects automatically after approval.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { githubDialogDismissed = true }) { Text("Hide") } },
            dismissButton = {
                if (state.githubAuthStatus == GitHubAuthStatus.AWAITING_USER) TextButton(onClick = onGenerateNewGitHubCode) { Text("New code") }
            },
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (selectedTab == WorkspaceTab.AGENT) {
                        Text("Agents", fontWeight = FontWeight.SemiBold, maxLines = 1)
                    } else Column(Modifier.fillMaxWidth()) {
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
                            state.agentSessions.firstOrNull { it.id == state.activeAgentSessionId }
                                ?.let { "${it.title} · ${it.kind.title}" }
                                ?: state.agentKind.title,
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
                            enabled = !state.androidBuildRunning,
                        ) {
                            if (state.androidBuildRunning) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Default.PlayArrow, "Build and run Android app")
                        }
                    }
                    val terminalInfo by com.hivebench.app.terminal.TerminalSessions.info.collectAsState()
                    val projectId = state.activeProject?.id
                    val liveCount = terminalInfo.values.count { info ->
                        info.running && projectId != null &&
                            (info.key.startsWith("agent:$projectId:") || info.key.startsWith("hive:$projectId:"))
                    }
                    Box(contentAlignment = Alignment.TopEnd) {
                        IconButton(onClick = { showChats = true }) { Icon(Icons.Default.History, "Sessions") }
                        if (liveCount > 0) {
                            Row(
                                Modifier.padding(top = 4.dp, end = 2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NeoLime)
                                    .padding(horizontal = 4.dp, vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                PulsingDot(color = NeoBlack, size = 4.dp)
                                Text("$liveCount", fontSize = 9.sp, fontWeight = FontWeight.Black, color = NeoBlack)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            if (!keyboardVisible) {
                val isDark = androidx.compose.foundation.isSystemInDarkTheme()
                // Show 5 tabs across the viewport; the rest scroll horizontally.
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                val visibleTabs = 5
                val tabGap = 4.dp
                // 24dp outer margin (matches home bar) + 12dp inner bar padding.
                val tabWidth = (maxWidth - 24.dp - 12.dp - tabGap * (visibleTabs - 1)) / visibleTabs
                val tabScroll = rememberScrollState()
                val density = LocalDensity.current
                LaunchedEffect(selectedTab, tabWidth) {
                    val index = WorkspaceTab.entries.indexOf(selectedTab)
                    val stepPx = with(density) { (tabWidth + tabGap).toPx() }
                    // Keep the selected tab centered where possible.
                    tabScroll.animateScrollTo(((index - visibleTabs / 2) * stepPx).toInt().coerceAtLeast(0))
                }
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Row(
                        Modifier.horizontalScroll(tabScroll),
                        horizontalArrangement = Arrangement.spacedBy(tabGap),
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
                        Row(Modifier.width(tabWidth)) {
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                selectedTab = tab
                                if (tab == WorkspaceTab.FILES) onRefreshFiles()
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
                }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                WorkspaceTab.AGENT -> agentConfigurationContent()
                WorkspaceTab.CHAT -> agentSessionContent { selectedTab = WorkspaceTab.AGENT }
                WorkspaceTab.OFFICE -> officeContent()
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
                WorkspaceTab.TERMINAL -> terminalContent()
                WorkspaceTab.CHANGES -> GitScreen(
                    repositoryMissing = state.gitRepositoryMissing,
                    onInitRepository = onInitGitRepository,
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
                WorkspaceTab.PREVIEW -> browserContent { selectedTab = WorkspaceTab.CHAT }
            }
        }
    }
}

/** Read-only transcript of a chat from the old chat mode, with a way to open its project. */
@Composable
private fun ChatHistoryView(
    messages: List<ChatMessage>,
    listState: LazyListState,
    onContinueHere: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(messages, key = { it.id }) { message ->
                if (message.workItems.isNotEmpty()) WorkBlockCard(message) else MessageBubble(message, {}, {})
            }
        }
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.weight(1f)) {
                    Text("Read-only history", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Chats from the old chat mode. Open the project to keep working with an agent session.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onContinueHere) { Text("Open") }
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
    // Proper neubrutalist bottom drawer (morph open/close + staggered rows).
    var search by rememberSaveable { mutableStateOf("") }
    NeoHistoryDrawer(
        visible = true,
        onDismiss = onDismiss,
        sessions = chats.map { chat ->
            NeoHistoryOption(
                id = chat.id,
                title = chat.title,
                subtitle = if (chat.id == activeChatId) "Current chat" else "Saved conversation",
                active = chat.id == activeChatId,
            )
        },
        search = search,
        onSearchChange = { search = it },
        onSelect = { option -> if (switchingEnabled) onSwitch(option.id) },
        onNewChat = if (allowCreate) { { if (switchingEnabled) onCreate() } } else null,
        title = "Project chats",
    )
}

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

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1_024 -> "$bytes B"
    bytes < 1_048_576 -> "%.1f KB".format(bytes / 1_024.0)
    else -> "%.1f MB".format(bytes / 1_048_576.0)
}

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
    // The Hivebench mark (lime hive cell, black outline, hard shadow, >_ prompt) carries its
    // own neo-brutalist frame, so it is drawn as-is with a gentle idle sway.
    val size = if (compact) 34.dp else 52.dp
    val infiniteTransition = rememberInfiniteTransition(label = "brandBreath")
    val rot by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "brandRot",
    )
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(com.hivebench.app.R.drawable.ic_brand_mark),
        contentDescription = "Hivebench",
        modifier = modifier.size(size).graphicsLayer { rotationZ = rot },
    )
}
