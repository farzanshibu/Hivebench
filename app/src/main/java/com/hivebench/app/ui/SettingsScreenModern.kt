package com.hivebench.app.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.hivebench.app.ui.neo.AlertDialog
import com.hivebench.app.ui.neo.Button
import com.hivebench.app.ui.neo.ButtonDefaults
import com.hivebench.app.ui.neo.CircularProgressIndicator
import com.hivebench.app.ui.neo.LinearProgressIndicator
import com.hivebench.app.ui.neo.OutlinedButton
import com.hivebench.app.ui.neo.OutlinedTextField
import com.hivebench.app.ui.neo.Scaffold
import com.hivebench.app.ui.neo.Surface
import com.hivebench.app.ui.neo.Text
import com.hivebench.app.ui.neo.TextButton
import com.hivebench.app.ui.neo.MaterialTheme
import com.hivebench.app.ui.neo.Icon
import com.hivebench.app.ui.neo.IconButton
import com.hivebench.app.ui.neo.HorizontalDivider
import com.hivebench.app.ui.neo.TopAppBar
import com.hivebench.app.ui.neo.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hivebench.app.BuildConfig
import com.hivebench.app.data.ApiKeyInfo
import com.hivebench.app.model.AgentKind
import com.hivebench.app.model.DEEPSEEK_HARNESS_PROVIDERS
import com.hivebench.app.model.DSH_PROTOCOL_PROVIDERS
import com.hivebench.app.model.DevStack
import com.hivebench.app.model.ProviderKind
import com.hivebench.app.model.ProviderProfile
import com.hivebench.app.model.providersForAgent
import com.hivebench.app.network.ConnectionValidation
import com.hivebench.app.network.DiscoveredModel
import com.hivebench.app.network.ModelDiscoveryResult
import com.hivebench.app.ui.theme.AppThemeMode
import com.hivebench.app.ui.theme.PocketOrange
import com.hivebench.app.ui.theme.NeoLime
import com.hivebench.app.ui.theme.NeoBlack
import com.hivebench.app.ui.theme.NeoDarkBorder
import com.hivebench.app.ui.theme.neoShadow
import com.hivebench.app.ui.theme.neoTactile
import com.hivebench.app.ui.theme.neoBounce
import com.hivebench.app.ui.theme.neoBorder
import com.hivebench.app.ui.theme.PulsingDot
import com.hivebench.app.ui.theme.NeoButton
import com.hivebench.app.ui.theme.NeoCard
import kotlinx.coroutines.launch

private enum class SettingsSection { APPEARANCE, TOOLS, RUNTIME }

@Composable
fun SettingsScreen(
    state: AppUiState,
    onSetThemeMode: (AppThemeMode) -> Unit,
    onInstallDevStack: (DevStack) -> Unit = {},
    onRemoveDevStack: (DevStack) -> Unit = {},
    onCheckAppUpdate: () -> Unit = {},
    initialDebugUpdateManifestUrl: String = "",
    onSetDebugUpdateManifestUrl: (String) -> Unit = {},
    onClearDebugUpdateManifestUrl: () -> Unit = {},
) {
    val context = LocalContext.current
    var expanded by rememberSaveable { mutableStateOf<SettingsSection?>(null) }
    var updateRequested by remember { mutableStateOf(false) }
    var showReliabilityHelp by rememberSaveable { mutableStateOf(false) }
    var stackPendingRemoval by remember { mutableStateOf<DevStack?>(null) }

    stackPendingRemoval?.let { stack ->
        AlertDialog(
            onDismissRequest = { stackPendingRemoval = null },
            title = { Text("Remove ${stack.label}?") },
            text = {
                Text("This removes the toolchain and its runtime caches to free storage. Your projects and source files will not be deleted.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        stackPendingRemoval = null
                        onRemoveDevStack(stack)
                    },
                ) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { stackPendingRemoval = null }) { Text("Cancel") } },
        )
    }

    fun toggle(section: SettingsSection) {
        expanded = if (expanded == section) null else section
    }

    val isDark = isSystemInDarkTheme()

    Scaffold(
        applySystemInsets = false,
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = 8.dp),
                title = { com.hivebench.app.ui.theme.TitlePill("Settings", com.hivebench.app.ui.theme.NeoPurple, fontSize = 20.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).imePadding(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            item {
                SettingsAccordion(
                    title = "Appearance",
                    accent = com.hivebench.app.ui.theme.NeoPink,
                    subtitle = when (state.themeMode) { AppThemeMode.DARK -> "Dark theme"; AppThemeMode.LIGHT -> "Light theme"; AppThemeMode.SYSTEM -> "Follow system" },
                    icon = Icons.Default.Tune,
                    expanded = expanded == SettingsSection.APPEARANCE,
                    onClick = { toggle(SettingsSection.APPEARANCE) },
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModernThemeChoice("Dark", Icons.Default.DarkMode, state.themeMode == AppThemeMode.DARK, { onSetThemeMode(AppThemeMode.DARK) }, Modifier.weight(1f))
                        ModernThemeChoice("Light", Icons.Default.LightMode, state.themeMode == AppThemeMode.LIGHT, { onSetThemeMode(AppThemeMode.LIGHT) }, Modifier.weight(1f))
                        ModernThemeChoice("System", Icons.Default.PhoneAndroid, state.themeMode == AppThemeMode.SYSTEM, { onSetThemeMode(AppThemeMode.SYSTEM) }, Modifier.weight(1f))
                    }
                }
            }

            item {
                val installedCount = state.installedDevStacks.size
                SettingsAccordion(
                    title = "Developer tools",
                    accent = com.hivebench.app.ui.theme.NeoCyan,
                    subtitle = "Core tools + $installedCount optional toolchain${if (installedCount == 1) "" else "s"}",
                    icon = Icons.Default.Code,
                    expanded = expanded == SettingsSection.TOOLS,
                    onClick = { toggle(SettingsSection.TOOLS) },
                ) {
                    Text("Node.js, npm, Git, and Claude Code are included.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    DevStack.entries.forEachIndexed { index, stack ->
                        val installed = stack in state.installedDevStacks
                        val installing = state.devStackInstalling == stack
                        val removing = installing && state.devStackRemoving
                        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(stack.label, fontWeight = FontWeight.SemiBold)
                                Text(stack.installsSummary, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            when {
                                removing -> Text("Removing…", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                installing -> Text("${(state.devStackProgress * 100).toInt()}%", color = NeoLime, fontWeight = FontWeight.Bold)
                                installed && stack == DevStack.WEB -> Text("Included", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                installed -> TextButton(
                                    onClick = { stackPendingRemoval = stack },
                                    enabled = state.devStackInstalling == null,
                                ) { Text("Remove", color = MaterialTheme.colorScheme.error) }
                                else -> NeoButton(
                                    onClick = { onInstallDevStack(stack) },
                                    enabled = state.devStackInstalling == null,
                                    buttonColor = NeoLime,
                                    contentColor = NeoBlack,
                                    cornerRadius = 8.dp,
                                ) { Text("Add", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NeoBlack) }
                            }
                        }
                        if (installing) {
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { state.devStackProgress.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(7.dp),
                                color = NeoLime,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                            Spacer(Modifier.height(9.dp))
                            state.devStackBytes?.let { (downloaded, total) ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                ) {
                                    Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(
                                                "${formatTransferMb(downloaded)} of ${formatTransferMb(total)}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = FontFamily.Monospace,
                                            )
                                            state.devStackBytesPerSecond?.takeIf { it > 0L }?.let { speed ->
                                                Text(
                                                    "${formatTransferSpeed(speed)} · ${formatTransferEta(downloaded, total, speed)} left",
                                                    fontSize = 11.sp,
                                                    color = NeoLime,
                                                    fontFamily = FontFamily.Monospace,
                                                )
                                            }
                                        }
                                        Text(
                                            state.devStackMessage ?: "Downloading…",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            } ?: Text(
                                state.devStackMessage ?: "Processing…",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (index != DevStack.entries.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }

            item {
                SettingsAccordion(
                    title = "Linux runtime",
                    accent = com.hivebench.app.ui.theme.NeoMint,
                    subtitle = "Ubuntu 24.04 PRoot · ARM64",
                    icon = Icons.Default.Terminal,
                    expanded = expanded == SettingsSection.RUNTIME,
                    onClick = { toggle(SettingsSection.RUNTIME) },
                ) {
                    RuntimeInfoRow("Architecture", "ARM64 (aarch64)")
                    RuntimeInfoRow("Environment", "Ubuntu 24.04 PRoot")
                    RuntimeInfoRow(
                        "Active agent",
                        state.agentKind.title + if (state.installedAgentVersions.containsKey(state.agentKind)) "" else " · Not installed",
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Text(
                        "Installed agents",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.installedAgentVersions.isEmpty()) {
                        RuntimeInfoRow("Status", "No verified agent installation")
                    } else {
                        AgentKind.entries.forEach { agent ->
                            state.installedAgentVersions[agent]?.let { version ->
                                RuntimeInfoRow(agent.title, "v$version")
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    NeoButton(
                        onClick = { onCheckAppUpdate(); updateRequested = true },
                        modifier = Modifier.fillMaxWidth(),
                        buttonColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(
                            when {
                                state.appUpdate != null -> "Update ${state.appUpdate.versionName} available on the Projects screen"
                                updateRequested -> "Checked for app updates"
                                else -> "Check for app update"
                            },
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    NeoButton(
                        onClick = { showReliabilityHelp = !showReliabilityHelp },
                        modifier = Modifier.fillMaxWidth(),
                        buttonColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ) {
                        Text("Advanced runtime reliability", fontWeight = FontWeight.Bold)
                    }
                    AnimatedVisibility(showReliabilityHelp) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "If large builds stop unexpectedly, Android Developer options may provide a child-process restriction toggle.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            NeoButton(
                                onClick = {
                                    runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }
                                        .onFailure { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                buttonColor = NeoLime,
                                contentColor = NeoBlack,
                            ) { Text("Open Developer options", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }

            if (BuildConfig.DEBUG) {
                item {
                    DebugUpdateChannelSection(
                        initialUrl = initialDebugUpdateManifestUrl,
                        onSave = onSetDebugUpdateManifestUrl,
                        onClear = onClearDebugUpdateManifestUrl,
                    )
                }
            }

            item {
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, null, Modifier.size(20.dp), tint = NeoLime)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Hivebench", fontWeight = FontWeight.SemiBold)
                            Text("Local AI coding workspace", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("v${BuildConfig.VERSION_NAME}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PRIVACY_POLICY_URL)),
                                )
                            }
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.PrivacyTip,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Privacy policy", fontWeight = FontWeight.Medium)
                        Text(
                            "How local data and AI provider requests are handled",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open privacy policy",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

private fun formatTransferMb(bytes: Long): String = "%.1f MB".format(bytes.coerceAtLeast(0L) / 1_048_576.0)

private fun formatTransferSpeed(bytesPerSecond: Long): String = when {
    bytesPerSecond >= 1_048_576L -> "%.1f MB/s".format(bytesPerSecond / 1_048_576.0)
    else -> "%.0f KB/s".format(bytesPerSecond / 1_024.0)
}

private fun formatTransferEta(downloaded: Long, total: Long, bytesPerSecond: Long): String {
    val seconds = ((total - downloaded).coerceAtLeast(0L) / bytesPerSecond.coerceAtLeast(1L)).coerceAtLeast(1L)
    return if (seconds >= 60L) "${seconds / 60}m ${seconds % 60}s" else "${seconds}s"
}

@Composable
private fun SettingsAccordion(
    title: String,
    subtitle: String,
    icon: ImageVector,
    expanded: Boolean,
    onClick: () -> Unit,
    accent: Color = NeoLime,
    content: @Composable () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) NeoDarkBorder else NeoBlack

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .neoShadow(offsetX = 3.dp, offsetY = 3.dp, cornerRadius = 16.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(2.dp, borderColor, RoundedCornerShape(16.dp)),
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(accent, RoundedCornerShape(10.dp))
                        .border(1.5.dp, NeoBlack, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, null, Modifier.size(20.dp), tint = NeoBlack)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            AnimatedVisibility(expanded) {
                Column {
                    HorizontalDivider(thickness = 1.5.dp, color = if (isDark) NeoDarkBorder else NeoBlack.copy(alpha = 0.15f))
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
                }
            }
        }
    }
}

@Composable
private fun SelectionDot(selected: Boolean) {
    val isDark = isSystemInDarkTheme()
    Box(
        Modifier
            .size(22.dp)
            .border(2.dp, if (selected) NeoLime else (if (isDark) NeoDarkBorder else NeoBlack), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Box(Modifier.size(10.dp).background(NeoLime, CircleShape))
    }
}

@Composable
private fun ModernThemeChoice(title: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val isDark = isSystemInDarkTheme()
    val borderColor = if (selected) NeoBlack else if (isDark) NeoDarkBorder else NeoBlack
    // Solid fills only: a translucent fill lets the hard shadow underneath bleed through.
    val bgColor = if (selected) NeoLime else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) NeoBlack else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .neoTactile(
                shadowOffset = if (selected) 3.dp else 2.dp,
                pressedOffset = 0.5.dp,
                cornerRadius = 12.dp,
                onClick = onClick,
            )
            .background(bgColor, RoundedCornerShape(12.dp))
            .border(if (selected) 2.dp else 1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, title, Modifier.size(22.dp), tint = if (selected) NeoBlack else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(5.dp))
            Text(title, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Black else FontWeight.Medium, color = fg)
        }
    }
}

@Composable
private fun RuntimeInfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    }
}

@Composable
private fun DebugUpdateChannelSection(
    initialUrl: String,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var url by rememberSaveable(initialUrl) { mutableStateOf(initialUrl) }
    val isOverridden = initialUrl.isNotBlank()
    SettingsAccordion(
        title = "Update channel",
        subtitle = if (isOverridden) "Overridden · debug only" else "Default GitHub release",
        icon = Icons.Default.Tune,
        expanded = expanded,
        onClick = { expanded = !expanded },
    ) {
        Text(
            "Debug builds only. Paste the temporary manifest URL from Cloudflare Tunnel, ngrok, or any HTTPS server hosting hivebench-update.json and a newer APK.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Manifest URL") },
            placeholder = { Text("https://your-tunnel.example/hivebench-update.json") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NeoButton(
                onClick = { onSave(url) },
                enabled = url.startsWith("https://"),
                modifier = Modifier.weight(1f),
                buttonColor = NeoLime,
                contentColor = NeoBlack,
            ) {
                Text(if (isOverridden) "Replace" else "Use & check", fontWeight = FontWeight.Bold)
            }
            NeoButton(
                onClick = onClear,
                enabled = isOverridden,
                modifier = Modifier.weight(1f),
                buttonColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Text("Reset", fontWeight = FontWeight.Bold)
            }
        }
        if (isOverridden) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Current: $initialUrl",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
