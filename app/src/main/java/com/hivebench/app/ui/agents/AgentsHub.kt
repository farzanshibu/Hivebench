package com.hivebench.app.ui.agents

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hivebench.app.model.AgentKind
import com.hivebench.app.runtime.AgentAuthState
import com.hivebench.app.runtime.AgentCatalog
import com.hivebench.app.runtime.AgentLaunchPurpose
import com.hivebench.app.runtime.AgentSpec
import com.hivebench.app.terminal.AgentTerminalView
import com.hivebench.app.terminal.TerminalSessions
import com.hivebench.app.ui.AppUiState
import com.hivebench.app.ui.MainViewModel
import com.hivebench.app.ui.neo.AlertDialog
import com.hivebench.app.ui.neo.CircularProgressIndicator
import com.hivebench.app.ui.neo.DrawerDetent
import com.hivebench.app.ui.neo.Icon
import com.hivebench.app.ui.neo.LinearProgressIndicator
import com.hivebench.app.ui.neo.MaterialTheme
import com.hivebench.app.ui.neo.NeoBottomDrawer
import com.hivebench.app.ui.neo.NeoDrawerRow
import com.hivebench.app.ui.neo.OutlinedTextField
import com.hivebench.app.ui.neo.Text
import com.hivebench.app.ui.neo.TextButton
import com.hivebench.app.ui.theme.NeoBadge
import com.hivebench.app.ui.theme.NeoBlack
import com.hivebench.app.ui.theme.NeoButton
import com.hivebench.app.ui.theme.NeoCard
import com.hivebench.app.ui.theme.NeoDarkBorder
import com.hivebench.app.ui.theme.NeoLime
import kotlinx.coroutines.launch

private val OkGreen = Color(0xFF58C9A3)

private fun AppUiState.isInstalled(agent: AgentKind) =
    agent == AgentKind.CUSTOM_RUNNER || installedAgentVersions.containsKey(agent)

/** One-line sign-in summary for an agent. */
private fun authSummary(spec: AgentSpec, auth: AgentAuthState?): Pair<String, Boolean> = when {
    spec.pkg is com.hivebench.app.runtime.AgentPackage.UserCommand -> "Runs your own command" to true
    auth?.account == true -> "Signed in" to true
    auth?.apiKeyEnvs?.isNotEmpty() == true -> "API key · ${auth.apiKeyEnvs.first()}" to true
    spec.credentialFiles.isEmpty() && spec.apiKeys.isEmpty() -> "Sign in inside the session" to false
    spec.credentialFiles.isEmpty() -> "Sign-in managed by the agent" to false
    else -> "Not signed in" to false
}

/**
 * Agents screen: install, sign in, API keys and updates for every agent. The
 * agent's own TUI (Session tab) owns models, modes, slash commands and usage.
 */
@Composable
fun AgentsHubScreen(state: AppUiState, viewModel: MainViewModel, modifier: Modifier = Modifier) {
    var detailAgent by rememberSaveable { mutableStateOf<AgentKind?>(null) }
    var terminalSheet by remember { mutableStateOf<Pair<AgentKind, AgentLaunchPurpose>?>(null) }

    LaunchedEffect(Unit) { viewModel.refreshAgentAuth() }

    // Ordered once per visit: re-sorting after an install would move cards under the user's finger.
    // Keyed only on "versions loaded yet" so it sorts once they arrive, then stays put.
    val ordered = remember(state.installedAgentVersions.isEmpty()) {
        AgentKind.entries.sortedWith(
            compareByDescending<AgentKind> { it == state.agentKind }.thenByDescending { state.isInstalled(it) },
        )
    }

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Coding agents", fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Text(
                        state.agentUpdateMessage ?: "Each agent runs its own terminal app with its native commands.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                NeoButton(
                    onClick = viewModel::checkAgentUpdates,
                    enabled = !state.agentUpdatesChecking,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    if (state.agentUpdatesChecking) {
                        CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("Updates", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
        items(ordered, key = { it.stableId }) { agent ->
            AgentCard(
                agent = agent,
                state = state,
                onOpen = { detailAgent = agent },
                onInstall = { viewModel.installAgent(agent) },
                onUse = { viewModel.selectAgent(agent) },
                onUpdate = { viewModel.updateAgent(agent) },
            )
        }
    }

    detailAgent?.let { agent ->
        AgentDetailSheet(
            agent = agent,
            state = state,
            viewModel = viewModel,
            onDismiss = { detailAgent = null },
            onOpenTerminal = { purpose -> terminalSheet = agent to purpose },
        )
    }

    terminalSheet?.let { (agent, purpose) ->
        AgentTerminalSheet(agent, purpose, viewModel) {
            terminalSheet = null
            viewModel.refreshAgentAuth()
        }
    }
}

@Composable
private fun AgentCard(
    agent: AgentKind,
    state: AppUiState,
    onOpen: () -> Unit,
    onInstall: () -> Unit,
    onUse: () -> Unit,
    onUpdate: () -> Unit,
) {
    val spec = AgentCatalog.spec(agent)
    val installed = state.isInstalled(agent)
    val active = state.agentKind == agent
    val version = state.installedAgentVersions[agent]
    val update = state.agentUpdates[agent]
    val (authText, authOk) = authSummary(spec, state.agentAuth[agent])
    val installing = state.agentInstalling == agent
    val updating = state.agentUpdating == agent

    NeoCard(onClick = onOpen, borderColor = if (active) NeoLime else null) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                        .background(if (installed) NeoLime else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        agent.title.take(1),
                        fontWeight = FontWeight.Black,
                        color = if (installed) NeoBlack else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(agent.title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        agent.subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (active) NeoBadge("ACTIVE")
            }

            if (installed) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(if (authOk) OkGreen else MaterialTheme.colorScheme.error, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        listOfNotNull(version?.takeIf { it != "custom" }?.let { "v$it" }, authText).joinToString(" · "),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Text("Not installed · ${spec.sourceLabel}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (installing || updating) {
                Text(
                    (if (installing) state.agentMessage else state.agentUpdateMessage).orEmpty(),
                    fontSize = 11.sp,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(
                    progress = { if (installing) state.agentProgress else state.agentUpdateProgress },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when {
                        !installed -> SmallButton("Install", primary = true, onClick = onInstall)
                        !active -> SmallButton("Use", primary = true, onClick = onUse)
                    }
                    if (installed) {
                        val label = when {
                            spec.pkg is com.hivebench.app.runtime.AgentPackage.UserCommand -> "Command"
                            authOk -> "Account"
                            else -> "Sign in"
                        }
                        SmallButton(label, primary = false, onClick = onOpen)
                    }
                    if (update != null) SmallButton("Update ${update.latestVersion}", primary = false, onClick = onUpdate)
                }
            }
        }
    }
}

@Composable
private fun SmallButton(label: String, primary: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    NeoButton(
        onClick = onClick,
        enabled = enabled,
        containerColor = if (primary) NeoLime else MaterialTheme.colorScheme.surface,
        contentColor = if (primary) NeoBlack else MaterialTheme.colorScheme.onSurface,
        cornerRadius = 10.dp,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun AgentDetailSheet(
    agent: AgentKind,
    state: AppUiState,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onOpenTerminal: (AgentLaunchPurpose) -> Unit,
) {
    val spec = AgentCatalog.spec(agent)
    val auth = state.agentAuth[agent]
    val installed = state.isInstalled(agent)
    NeoBottomDrawer(
        visible = true,
        onDismiss = onDismiss,
        initialDetent = DrawerDetent.Full,
        allowedDetents = listOf(DrawerDetent.Half, DrawerDetent.Full),
        title = agent.title,
        subtitle = state.installedAgentVersions[agent]?.takeIf { it != "custom" }?.let { "v$it" } ?: agent.subtitle,
    ) { _, _ ->
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!installed) {
                SectionLabel("Install")
                if (state.agentInstalling == agent) {
                    Text(state.agentMessage.orEmpty(), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LinearProgressIndicator(progress = { state.agentProgress }, modifier = Modifier.fillMaxWidth())
                } else {
                    Text("${agent.title} is not installed yet · ${spec.sourceLabel}", fontSize = 13.sp)
                    state.agentMessage?.takeIf { state.agentInstalling == null && it.isNotBlank() && !it.endsWith("is ready") }?.let {
                        Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                    SmallButton("Install ${agent.title}", primary = true, enabled = state.agentInstalling == null) {
                        viewModel.installAgent(agent)
                    }
                }
                return@Column
            }

            state.agentUpdates[agent]?.let { update ->
                SectionLabel("Update")
                Text("${update.installedVersion} → ${update.latestVersion}", fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                SmallButton("Update to ${update.latestVersion}", primary = true, enabled = state.agentUpdating == null) {
                    viewModel.updateAgent(agent)
                }
            }

            if (agent == AgentKind.CUSTOM_RUNNER) {
                CustomRunnerSection(state.customRunnerCommand, viewModel::setCustomRunnerCommand)
            }

            if (spec.supportsAccountLogin) {
                SectionLabel("Account")
                val status = when (auth?.account) {
                    true -> "Signed in"
                    false -> "Not signed in"
                    null -> "Status is kept by ${agent.title}"
                }
                Text(status, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(spec.loginHint, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (spec.login != null) {
                        SmallButton(if (auth?.account == true) "Sign in again" else "Sign in with account", primary = auth?.account != true) {
                            onOpenTerminal(AgentLaunchPurpose.LOGIN)
                        }
                    }
                    if (auth?.account == true) SmallButton("Sign out", primary = false) { viewModel.signOutAgent(agent) }
                }
            }

            if (spec.apiKeys.isNotEmpty()) {
                ApiKeySection(
                    agent = agent,
                    spec = spec,
                    saved = auth?.apiKeyEnvs.orEmpty(),
                    onSave = { env, key, baseUrl ->
                        viewModel.saveAgentApiKey(agent, env, key, baseUrl)
                        if (spec.apiKeyLogin != null) onOpenTerminal(AgentLaunchPurpose.API_KEY_LOGIN)
                    },
                    onRemove = { env -> viewModel.removeAgentApiKey(agent, env) },
                )
            }

            spec.usageCommand?.let { command ->
                SectionLabel("Usage & limits")
                Text(
                    "Open the Session tab and run $command (or tap it in the shortcut row) to see ${agent.title}'s live plan usage and rate limits.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val color = when (text) {
        "Install", "Update" -> com.hivebench.app.ui.theme.NeoMint
        "Account" -> com.hivebench.app.ui.theme.NeoLime
        "API keys" -> com.hivebench.app.ui.theme.NeoCyan
        "Usage & limits" -> com.hivebench.app.ui.theme.NeoOrange
        else -> com.hivebench.app.ui.theme.NeoPurple
    }
    com.hivebench.app.ui.theme.SectionHeader(text, color)
}

@Composable
private fun CustomRunnerSection(command: String, onSave: (String) -> Unit) {
    var draft by rememberSaveable(command) { mutableStateOf(command) }
    SectionLabel("Command")
    Text("Runs in the project folder inside the Linux runtime.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    OutlinedTextField(
        value = draft,
        onValueChange = { draft = it },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text("e.g. aider --model sonnet", fontFamily = FontFamily.Monospace) },
    )
    SmallButton("Save command", primary = true, enabled = draft.isNotBlank() && draft != command) { onSave(draft) }
}

@Composable
private fun ApiKeySection(
    agent: AgentKind,
    spec: AgentSpec,
    saved: Set<String>,
    onSave: (String, String, String?) -> Unit,
    onRemove: (String) -> Unit,
) {
    var selectedEnv by rememberSaveable(agent) { mutableStateOf(spec.apiKeys.first().envVar) }
    var key by rememberSaveable(agent, selectedEnv) { mutableStateOf("") }
    var baseUrl by rememberSaveable(agent, selectedEnv) { mutableStateOf("") }
    val option = spec.apiKeys.first { it.envVar == selectedEnv }

    SectionLabel("API keys")
    Text(
        "Stored encrypted on this device and passed to ${agent.title} as environment variables.",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    spec.apiKeys.filter { it.envVar in saved }.forEach { savedOption ->
        NeoDrawerRow(
            title = savedOption.label,
            subtitle = savedOption.envVar,
            badge = "SAVED",
            selected = false,
            onClick = {},
            trailing = {
                Text(
                    "Remove",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.clickable { onRemove(savedOption.envVar) }.padding(8.dp),
                )
            },
        )
    }
    if (spec.apiKeys.size > 1) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            spec.apiKeys.forEach { candidate ->
                val selected = candidate.envVar == selectedEnv
                Box(
                    Modifier.clip(RoundedCornerShape(50))
                        .background(if (selected) NeoLime else Color.Transparent)
                        .border(1.dp, if (selected) NeoLime else borderColor(), RoundedCornerShape(50))
                        .clickable { selectedEnv = candidate.envVar }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        candidate.envVar,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) NeoBlack else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
    OutlinedTextField(
        value = key,
        onValueChange = { key = it },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(option.label) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    )
    option.baseUrlEnv?.let { baseEnv ->
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("$baseEnv (optional)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        )
    }
    SmallButton(if (selectedEnv in saved) "Replace key" else "Save key", primary = true, enabled = key.isNotBlank()) {
        onSave(selectedEnv, key, baseUrl.ifBlank { null })
        key = ""
    }
}

@Composable
private fun borderColor() = if (isSystemInDarkTheme()) NeoDarkBorder else NeoBlack

/** Full-height drawer running an agent's sign-in flow in a real terminal. */
@Composable
private fun AgentTerminalSheet(
    agent: AgentKind,
    purpose: AgentLaunchPurpose,
    viewModel: MainViewModel,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val key = remember(agent, purpose) { viewModel.agentSessionKey(agent, purpose) }
    val info by TerminalSessions.info.collectAsState()
    val links by TerminalSessions.links.collectAsState()
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(key) {
        error = runCatching {
            val launch = viewModel.agentLaunch(agent, purpose)
            TerminalSessions.restart(context, key, "${agent.title} sign-in", launch)
        }.exceptionOrNull()?.message
    }

    NeoBottomDrawer(
        visible = true,
        onDismiss = {
            TerminalSessions.close(key)
            onClose()
        },
        initialDetent = DrawerDetent.Full,
        allowedDetents = listOf(DrawerDetent.Full),
        title = if (purpose == AgentLaunchPurpose.API_KEY_LOGIN) "Saving key to ${agent.title}" else "Sign in to ${agent.title}",
        subtitle = when (info[key]?.running) {
            true -> "Follow the prompts below"
            false -> "Finished (exit ${info[key]?.exitStatus}). Swipe down to close."
            null -> "Starting…"
        },
    ) { _, _ ->
        Column(Modifier.fillMaxSize().imePadding()) {
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(8.dp)) }
            links[key]?.let { url -> OpenLinkBar(url) { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }
            if (info[key] != null) {
                AgentTerminalView(
                    sessionKey = key,
                    modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(10.dp)),
                )
            }
        }
    }
}

@Composable
internal fun OpenLinkBar(url: String, onOpen: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(10.dp))
            .background(NeoLime).clickable(onClick = onOpen).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.OpenInNew, null, tint = NeoBlack, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text("Open link", fontWeight = FontWeight.Black, fontSize = 13.sp, color = NeoBlack)
            Text(url, fontSize = 10.sp, color = NeoBlack.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * Session tab: the visible agent session's own TUI in the project folder. Any
 * number of sessions run in parallel; the strip on top switches between them
 * without stopping the others, and shows a live dot on every running one.
 */
@Composable
fun AgentSessionTab(state: AppUiState, viewModel: MainViewModel, onOpenAgents: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val entry = state.agentSessions.firstOrNull { it.id == state.activeAgentSessionId }
    val agent = entry?.kind ?: state.agentKind
    val spec = AgentCatalog.spec(agent)
    val installed = state.isInstalled(agent)
    val key = entry?.terminalKey
    val info by TerminalSessions.info.collectAsState()
    val links by TerminalSessions.links.collectAsState()
    var error by remember(key) { mutableStateOf<String?>(null) }
    var showNewSession by remember { mutableStateOf(false) }
    var confirmClose by remember { mutableStateOf<com.hivebench.app.session.AgentSessionEntry?>(null) }

    fun requestClose(id: String) {
        val target = state.agentSessions.firstOrNull { it.id == id } ?: return
        if (info[target.terminalKey]?.running == true) confirmClose = target else viewModel.closeAgentSession(id)
    }

    fun start(restart: Boolean) {
        val target = entry ?: return
        scope.launch {
            error = runCatching {
                if (!restart && TerminalSessions.get(target.terminalKey) != null) return@runCatching
                val launch = viewModel.agentSessionLaunch(target)
                TerminalSessions.restart(context, target.terminalKey, target.title, launch)
                viewModel.markAgentSessionLaunched(target.id)
            }.exceptionOrNull()?.message
        }
    }

    LaunchedEffect(key, installed) {
        if (installed && key != null) start(restart = false)
    }
    LaunchedEffect(Unit) { viewModel.refreshAgentAuth() }

    Column(modifier.fillMaxSize().imePadding()) {
        SessionStrip(
            state = state,
            info = info,
            onSelect = viewModel::switchAgentSession,
            onNew = { showNewSession = true },
            onClose = ::requestClose,
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val running = key != null && info[key]?.running == true
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                if (running) com.hivebench.app.ui.theme.PulsingDot(color = OkGreen, size = 7.dp)
                else Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(entry?.title ?: agent.title, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (running) "${agent.title} · live" else "${agent.title} · stopped",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (installed && key != null) {
                spec.usageCommand?.let { command ->
                    SmallButton("Usage", primary = false, enabled = running) { TerminalSessions.write(key, "$command\r") }
                }
                SmallButton("Restart", primary = false) { start(restart = true) }
            }
            if (entry != null) SmallButton("Close", primary = false) { requestClose(entry.id) }
        }

        val auth = state.agentAuth[agent]
        if (installed && auth != null && auth.account == false && auth.apiKeyEnvs.isEmpty()) {
            Text(
                "Not signed in — the agent will ask you to sign in, or set it up in Agents.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 14.dp),
            )
        }
        key?.let { links[it] }?.let { url ->
            Box(Modifier.padding(horizontal = 12.dp)) {
                OpenLinkBar(url) { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            }
        }

        Box(Modifier.fillMaxWidth().weight(1f)) {
            when {
                key == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                !installed -> CenterMessage(
                    title = "${agent.title} is not installed",
                    body = "Install it (and sign in) from the Agents tab.",
                    action = "Open Agents",
                    onAction = onOpenAgents,
                )
                error != null -> CenterMessage("Could not start ${agent.title}", error.orEmpty(), "Try again") { start(restart = true) }
                info[key] == null -> CenterMessage(
                    "Session stopped",
                    if (entry != null && entry.launched && viewModel.agentSessionResumable(entry)) "Start it again to continue the same conversation."
                    else "Start it again for a fresh ${agent.title} session.",
                    "Start",
                ) { start(restart = true) }
                else -> {
                    // key() gives each session its own TerminalView so switching re-binds cleanly.
                    androidx.compose.runtime.key(key) {
                        AgentTerminalView(sessionKey = key, modifier = Modifier.fillMaxSize(), quickActions = spec.quickActions, attachments = viewModel.terminalAttachmentTarget())
                    }
                    if (info[key]?.running == false) {
                        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp)) {
                            SmallButton("Session ended (exit ${info[key]?.exitStatus}) · Restart", primary = true) { start(restart = true) }
                        }
                    }
                }
            }
        }
    }

    confirmClose?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmClose = null },
            title = { Text("Close ${target.title}?") },
            text = { Text("The running ${target.kind.title} process will be stopped and this session removed.", fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClose = null
                    viewModel.closeAgentSession(target.id)
                }) { Text("Close session", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmClose = null }) { Text("Cancel") } },
        )
    }

    if (showNewSession) {
        NewSessionDrawer(
            state = state,
            onDismiss = { showNewSession = false },
            onPick = { kind ->
                showNewSession = false
                viewModel.newAgentSession(kind)
            },
            onOpenAgents = {
                showNewSession = false
                onOpenAgents()
            },
        )
    }
}

/** Horizontal chips, one per parallel session, with a live dot on running ones. */
@Composable
private fun SessionStrip(
    state: AppUiState,
    info: Map<String, com.hivebench.app.terminal.TerminalSessionInfo>,
    onSelect: (String) -> Unit,
    onNew: () -> Unit,
    onClose: (String) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 12.dp, end = 12.dp, top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        state.agentSessions.forEach { session ->
            val selected = session.id == state.activeAgentSessionId
            val running = info[session.terminalKey]?.running == true
            Row(
                Modifier.clip(RoundedCornerShape(9.dp))
                    .background(if (selected) NeoLime else MaterialTheme.colorScheme.surface)
                    .border(1.5.dp, if (selected) NeoBlack else borderColor(), RoundedCornerShape(9.dp))
                    .clickable { onSelect(session.id) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (running) com.hivebench.app.ui.theme.PulsingDot(color = if (selected) NeoBlack else OkGreen, size = 6.dp)
                else Box(Modifier.size(6.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), CircleShape))
                Spacer(Modifier.width(6.dp))
                Text(
                    session.title,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
                    color = if (selected) NeoBlack else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close ${session.title}",
                    tint = if (selected) NeoBlack else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp).clip(CircleShape).clickable { onClose(session.id) }.padding(3.dp),
                )
            }
        }
        Box(
            Modifier.clip(RoundedCornerShape(9.dp))
                .border(1.5.dp, borderColor(), RoundedCornerShape(9.dp))
                .clickable(onClick = onNew)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text("+ New", fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
    }
}

/** Picks which installed agent a new parallel session runs. */
@Composable
internal fun NewSessionDrawer(
    state: AppUiState,
    onDismiss: () -> Unit,
    onPick: (AgentKind) -> Unit,
    onOpenAgents: () -> Unit,
) {
    NeoBottomDrawer(
        visible = true,
        onDismiss = onDismiss,
        initialDetent = DrawerDetent.Half,
        title = "New parallel session",
        subtitle = "Runs alongside the others in this project",
    ) { _, _ ->
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(AgentKind.entries.filter { state.isInstalled(it) }, key = { it.stableId }) { candidate ->
                NeoDrawerRow(
                    title = candidate.title,
                    subtitle = authSummary(AgentCatalog.spec(candidate), state.agentAuth[candidate]).first,
                    selected = candidate == state.agentKind,
                    onClick = { onPick(candidate) },
                )
            }
            item {
                NeoDrawerRow(title = "Install more agents…", selected = false, onClick = onOpenAgents)
            }
        }
    }
}

@Composable
private fun CenterMessage(title: String, body: String, action: String, onAction: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, fontWeight = FontWeight.Black, fontSize = 17.sp)
        Spacer(Modifier.height(6.dp))
        Text(body, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        SmallButton(action, primary = true, onClick = onAction)
    }
}

/**
 * History drawer: every agent session of the project, newest first, with a live
 * dot on running ones. Tapping switches (others keep running); Stop ends a
 * session's process but keeps it in the list (agents that support it resume
 * their conversation on the next start); Close forgets it.
 */
@Composable
fun AgentSessionsDrawer(
    state: AppUiState,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSwitched: () -> Unit,
    onOpenAgents: () -> Unit,
) {
    var renaming by remember { mutableStateOf<com.hivebench.app.session.AgentSessionEntry?>(null) }
    val info by TerminalSessions.info.collectAsState()
    var search by rememberSaveable { mutableStateOf("") }
    var pickKind by remember { mutableStateOf(false) }
    val runningCount = state.agentSessions.count { info[it.terminalKey]?.running == true }

    NeoBottomDrawer(
        visible = true,
        onDismiss = onDismiss,
        initialDetent = DrawerDetent.Half,
        title = "Sessions",
        subtitle = "${state.agentSessions.size} sessions · $runningCount live",
        searchValue = search,
        onSearchChange = { search = it },
        searchPlaceholder = "Search sessions",
        footer = {
            NeoButton(
                onClick = { pickKind = true },
                modifier = Modifier.fillMaxWidth(),
                containerColor = NeoLime,
                contentColor = NeoBlack,
            ) { Text("+  New parallel session", fontWeight = FontWeight.Black) }
        },
    ) { _, _ ->
        val q = search.trim()
        val sessions = state.agentSessions.filter { q.isBlank() || it.title.contains(q, true) || it.kind.title.contains(q, true) }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            items(sessions.asReversed(), key = { it.id }) { session ->
                val running = info[session.terminalKey]?.running == true
                val active = session.id == state.activeAgentSessionId
                NeoDrawerRow(
                    title = session.title,
                    subtitle = buildString {
                        append(session.kind.title)
                        append(if (running) " · live" else if (info[session.terminalKey] != null) " · ended" else " · stopped")
                        append(" · ")
                        append(android.text.format.DateUtils.getRelativeTimeSpanString(session.createdAt))
                    },
                    badge = if (active) "OPEN" else null,
                    selected = active,
                    onClick = {
                        viewModel.switchAgentSession(session.id)
                        onSwitched()
                    },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (running) {
                                com.hivebench.app.ui.theme.PulsingDot(color = OkGreen, size = 7.dp)
                                Text(
                                    "Stop",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { viewModel.stopAgentSession(session.id) }.padding(6.dp),
                                )
                            }
                            Text(
                                "✎",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { renaming = session }.padding(6.dp),
                            )
                            Text(
                                "✕",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { viewModel.closeAgentSession(session.id) }.padding(6.dp),
                            )
                        }
                    },
                )
            }
        }
    }

    renaming?.let { target ->
        var title by remember(target.id) { mutableStateOf(target.title) }
        NeoBottomDrawer(visible = true, onDismiss = { renaming = null }, initialDetent = DrawerDetent.Half, title = "Rename session") { _, _ ->
            OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(12.dp))
            NeoButton(
                onClick = {
                    viewModel.renameAgentSession(target.id, title)
                    renaming = null
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save", fontWeight = FontWeight.Black) }
        }
    }

    if (pickKind) {
        NewSessionDrawer(
            state = state,
            onDismiss = { pickKind = false },
            onPick = { kind ->
                pickKind = false
                viewModel.newAgentSession(kind)
                onSwitched()
            },
            onOpenAgents = {
                pickKind = false
                onOpenAgents()
            },
        )
    }
}
