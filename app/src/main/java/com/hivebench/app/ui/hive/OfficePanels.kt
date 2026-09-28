package com.hivebench.app.ui.hive

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hivebench.app.hive.AutomationKind
import com.hivebench.app.hive.HiveAgent
import com.hivebench.app.hive.HiveAutomation
import com.hivebench.app.hive.HiveCapabilities
import com.hivebench.app.hive.HiveConnections
import com.hivebench.app.hive.HiveMcpCatalog
import com.hivebench.app.hive.HiveMemory
import com.hivebench.app.hive.HiveSkills
import com.hivebench.app.hive.HiveSnapshot
import com.hivebench.app.hive.MemoryHit
import com.hivebench.app.hive.RoleBundles
import com.hivebench.app.model.AgentKind
import com.hivebench.app.runtime.AgentCatalog
import com.hivebench.app.terminal.AgentTerminalView
import com.hivebench.app.terminal.TerminalSessions
import com.hivebench.app.ui.AppUiState
import com.hivebench.app.ui.MainViewModel
import com.hivebench.app.ui.neo.Checkbox
import com.hivebench.app.ui.neo.DrawerDetent
import com.hivebench.app.ui.neo.Icon
import com.hivebench.app.ui.neo.IconButton
import com.hivebench.app.ui.neo.MaterialTheme
import com.hivebench.app.ui.neo.NeoBottomDrawer
import com.hivebench.app.ui.neo.OutlinedTextField
import com.hivebench.app.ui.neo.Text
import com.hivebench.app.ui.theme.NeoBadge
import com.hivebench.app.ui.theme.NeoBlack
import com.hivebench.app.ui.theme.NeoButton
import com.hivebench.app.ui.theme.NeoCard
import com.hivebench.app.ui.theme.NeoLime
import kotlinx.coroutines.launch

// ---- Agent room ---------------------------------------------------------------------------

private enum class RoomPane { CHAT, TERMINAL }
private enum class SendMode(val label: String, val hint: String) {
    QUEUE("Queue", "Typed into its terminal when it is idle"),
    STEER("Steer", "Injected into its context at the next step, no interruption"),
    MAIL("Mail", "Delivered to its hive inbox as a request"),
}

/**
 * One screen per agent: the conversation on one side, its live terminal on the
 * other (stacked with a toggle on narrow phones), and the queue of what you
 * sent, which you can reorder, send now or drop.
 */
@Composable
internal fun AgentRoomScreen(hive: HiveSnapshot, agent: HiveAgent, viewModel: MainViewModel, onBack: () -> Unit) {
    val info by TerminalSessions.info.collectAsState()
    val key = viewModel.hive.terminalKey(agent.id)
    val live = info[key]?.running == true
    var pane by rememberSaveable { mutableStateOf(RoomPane.CHAT) }
    var controls by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to the office") }
            Avatar(agent, live)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(agent.name, fontWeight = FontWeight.Black, fontSize = 15.sp, maxLines = 1)
                Text(
                    hive.status[agent.id]?.doing?.ifBlank { null } ?: if (live) "Idle" else "Offline",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            LevelBadge(hive.breaker.levels[agent.id])
            SmallBtn("⋯") { controls = true }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val wide = maxWidth >= 720.dp
            if (wide) {
                Row(Modifier.fillMaxSize()) {
                    RoomConversation(hive, agent, viewModel, Modifier.weight(1f).fillMaxHeight())
                    RoomTerminal(agent, key, live, viewModel, Modifier.weight(1.2f).fillMaxHeight())
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    ChipRow(Modifier.padding(horizontal = 12.dp)) {
                        Chip("Conversation", pane == RoomPane.CHAT) { pane = RoomPane.CHAT }
                        Chip(if (live) "● Terminal" else "Terminal", pane == RoomPane.TERMINAL) { pane = RoomPane.TERMINAL }
                        val queued = hive.queue.count { it.agentId == agent.id }
                        if (queued > 0) NeoBadge("$queued queued", containerColor = Sky)
                    }
                    when (pane) {
                        RoomPane.CHAT -> RoomConversation(hive, agent, viewModel, Modifier.fillMaxWidth().weight(1f))
                        RoomPane.TERMINAL -> RoomTerminal(agent, key, live, viewModel, Modifier.fillMaxWidth().weight(1f))
                    }
                }
            }
        }
    }
    if (controls) RoomControls(hive, agent, live, viewModel, onBack) { controls = false }
}

@Composable
private fun RoomTerminal(agent: HiveAgent, key: String, live: Boolean, viewModel: MainViewModel, modifier: Modifier) {
    val info by TerminalSessions.info.collectAsState()
    Box(modifier) {
        if (info[key] == null) {
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${agent.name} is offline", fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                SmallBtn("Start ${agent.cli.title}", primary = true) { viewModel.hive.start(agent.id) }
            }
        } else {
            androidx.compose.runtime.key(key) {
                AgentTerminalView(sessionKey = key, modifier = Modifier.fillMaxSize(), quickActions = AgentCatalog.spec(agent.cli).quickActions, attachments = viewModel.terminalAttachmentTarget())
            }
            if (!live) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp)) {
                SmallBtn("Session ended · Start again", primary = true) { viewModel.hive.start(agent.id, restart = true) }
            }
        }
    }
}

@Composable
private fun RoomConversation(hive: HiveSnapshot, agent: HiveAgent, viewModel: MainViewModel, modifier: Modifier) {
    val thread = hive.messages.filter { it.from == agent.id || it.to == agent.id || (it.to == "broadcast" && it.from != agent.id) }
    val queue = hive.queue.filter { it.agentId == agent.id }
    var text by rememberSaveable(agent.id) { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf(SendMode.QUEUE) }
    val listState = rememberLazyListState()
    LaunchedEffect(thread.size) { if (thread.isNotEmpty()) listState.scrollToItem(thread.lastIndex) }

    Column(modifier) {
        LazyColumn(Modifier.fillMaxWidth().weight(1f), state = listState, contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (thread.isEmpty()) item { Muted("No hive messages with ${agent.name} yet. What you queue is typed into its terminal; mail lands in its inbox.") }
            items(thread, key = { "${it.id}-${it.to}" }) { m ->
                val mine = m.from == agent.id
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.Start else Arrangement.End) {
                    Column(
                        Modifier.widthIn(max = 320.dp).clip(RoundedCornerShape(12.dp))
                            .background(if (mine) MaterialTheme.colorScheme.surfaceVariant else if (m.from == "human") NeoLime.copy(alpha = 0.35f) else Sky.copy(alpha = 0.18f))
                            .padding(10.dp),
                    ) {
                        val who = hive.agents.firstOrNull { it.id == m.from }?.name ?: m.from
                        val to = hive.agents.firstOrNull { it.id == m.to }?.name ?: m.to
                        Text("$who → $to · ${m.act.wire}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(m.subject, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        if (m.body.isNotBlank()) Text(m.body, fontSize = 12.sp, maxLines = 12, overflow = TextOverflow.Ellipsis)
                        Mono(timeOf(m.deliveredAt))
                    }
                }
            }
        }
        if (queue.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Queue · ${queue.size}", fontWeight = FontWeight.Black, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text("Clear", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { viewModel.hive.clearQueue(agent.id) }.padding(4.dp))
                }
                queue.forEachIndexed { index, q ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${index + 1}.", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(6.dp))
                        Column(Modifier.weight(1f)) {
                            Text(q.text.lineSequence().first(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Mono(q.origin + if (q.manual) " · send now" else "")
                        }
                        QueueKey("↑", index > 0) { viewModel.hive.moveQueued(q.id, -1) }
                        QueueKey("↓", index < queue.lastIndex) { viewModel.hive.moveQueued(q.id, +1) }
                        QueueKey("▶", !q.manual) { viewModel.hive.sendNow(q.id) }
                        QueueKey("✕", true) { viewModel.hive.removeQueued(q.id) }
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                SendMode.entries.forEach { m -> Chip(m.label, mode == m) { mode = m } }
            }
            Muted(mode.hint)
            Row(verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(text, { text = it }, Modifier.weight(1f), placeholder = { Text("Message ${agent.name}…") }, maxLines = 5)
                Spacer(Modifier.width(8.dp))
                SmallBtn("Send", primary = true, enabled = text.isNotBlank()) {
                    when (mode) {
                        SendMode.QUEUE -> viewModel.hive.enqueue(agent.id, text)
                        SendMode.STEER -> viewModel.hive.steer(agent.id, text)
                        SendMode.MAIL -> viewModel.hive.mail(agent.id, "", text)
                    }
                    text = ""
                }
            }
        }
    }
}

@Composable
private fun QueueKey(label: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        label,
        fontSize = 13.sp,
        fontWeight = FontWeight.Black,
        color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 7.dp, vertical = 4.dp),
    )
}

@Composable
private fun RoomControls(hive: HiveSnapshot, agent: HiveAgent, live: Boolean, viewModel: MainViewModel, onFired: () -> Unit, onDismiss: () -> Unit) {
    var goal by remember(agent.id) { mutableStateOf(agent.goal) }
    var cap by remember(agent.id) { mutableStateOf(if (agent.tokenCap > 0) (agent.tokenCap / 1000).toString() else "") }
    var confirmFire by remember { mutableStateOf(false) }
    NeoBottomDrawer(visible = true, onDismiss = onDismiss, initialDetent = DrawerDetent.Full, allowedDetents = listOf(DrawerDetent.Half, DrawerDetent.Full), title = agent.name, subtitle = "${agent.role} · ${agent.cli.title}") { _, _ ->
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (live) {
                    SmallBtn("Restart") { viewModel.hive.start(agent.id, restart = true) }
                    SmallBtn("Halt") { viewModel.hive.halt(agent.id) }
                    SmallBtn("Stop") { viewModel.hive.stop(agent.id) }
                } else SmallBtn("Start", primary = true) { viewModel.hive.start(agent.id) }
            }
            ToggleRow("Paused", "Its tool calls are denied until you resume", agent.paused) { on -> viewModel.hive.update(agent.id) { it.copy(paused = on) } }
            if (!agent.isGod) ToggleRow("On hold", "1:1 with you — the orchestrator won't dispatch to it", agent.onHold) { on -> viewModel.hive.update(agent.id) { it.copy(onHold = on) } }
            ToggleRow("Pause auto-delivery", "Queued messages wait until you press send now", agent.autoDeliveryPaused) { on -> viewModel.hive.update(agent.id) { it.copy(autoDeliveryPaused = on) } }
            OutlinedTextField(goal, { goal = it }, Modifier.fillMaxWidth(), label = { Text("Standing goal") }, minLines = 2)
            OutlinedTextField(
                cap, { cap = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Token cap (thousands, empty = none)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            SmallBtn("Save goal & cap", primary = true) {
                viewModel.hive.update(agent.id) { it.copy(goal = goal, tokenCap = (cap.toLongOrNull() ?: 0L) * 1000) }
            }
            if ((hive.breaker.levels[agent.id] ?: com.hivebench.app.hive.BreakerLevel.HEALTHY) != com.hivebench.app.hive.BreakerLevel.HEALTHY) {
                Muted("Breaker: ${hive.breaker.reasons[agent.id].orEmpty()}")
                SmallBtn("Reset breaker") { viewModel.hive.resetBreaker(agent.id) }
            }
            Text("Memory", fontWeight = FontWeight.Black, fontSize = 14.sp)
            var memory by remember { mutableStateOf("") }
            LaunchedEffect(agent.id) { memory = viewModel.hive.memoryOf(agent.id) }
            Text(memory.ifBlank { "(empty)" }, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            if (!agent.isGod) {
                Spacer(Modifier.height(8.dp))
                if (confirmFire) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallBtn("Yes, let ${agent.name} go") { viewModel.hive.fire(agent.id); onDismiss(); onFired() }
                    SmallBtn("Keep") { confirmFire = false }
                } else SmallBtn("Let go…") { confirmFire = true }
            }
        }
    }
}

// ---- Hire -----------------------------------------------------------------------------------

@Composable
internal fun HireDrawer(state: AppUiState, viewModel: MainViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var bundle by remember { mutableStateOf<String?>("fullstack") }
    var role by remember { mutableStateOf("") }
    val installed = AgentKind.entries.filter { it == AgentKind.CUSTOM_RUNNER || state.installedAgentVersions.containsKey(it) }
    var cli by remember { mutableStateOf(installed.firstOrNull { it == AgentKind.CLAUDE_CODE } ?: installed.firstOrNull() ?: AgentKind.CLAUDE_CODE) }
    var model by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }
    var cap by remember { mutableStateOf("") }
    var startNow by remember { mutableStateOf(true) }
    NeoBottomDrawer(visible = true, onDismiss = onDismiss, initialDetent = DrawerDetent.Full, allowedDetents = listOf(DrawerDetent.Full), title = "Hire an agent", subtitle = "A new seat with its own CLI, memory and mailbox") { _, _ ->
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Name (e.g. Pam, Dwight)") }, singleLine = true)
            Text("Role bundle", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            ChipRow {
                Chip("None", bundle == null) { bundle = null }
                RoleBundles.all.filter { it.id != "orchestrator" }.forEach { b -> Chip(b.title, bundle == b.id) { bundle = b.id } }
            }
            RoleBundles.byId(bundle)?.let { b -> Muted("${b.role} · skills: ${b.skills.joinToString()} · MCP: ${b.mcpServers.joinToString().ifBlank { "—" }}") }
            OutlinedTextField(role, { role = it }, Modifier.fillMaxWidth(), label = { Text("Role (optional, overrides the bundle's)") }, singleLine = true)
            Text("CLI", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            if (installed.isEmpty()) Muted("No agent CLI installed yet — install one from the Agents tab.")
            ChipRow { installed.forEach { k -> Chip(k.title, cli == k) { cli = k } } }
            if (cli != AgentKind.CLAUDE_CODE) Muted("Claude Code gets the full hive wiring (hooks, status, spend, steer). Other CLIs get the protocol as a first prompt and inbox work orders.")
            OutlinedTextField(model, { model = it }, Modifier.fillMaxWidth(), label = { Text("Model (optional, e.g. sonnet, opus)") }, singleLine = true)
            OutlinedTextField(goal, { goal = it }, Modifier.fillMaxWidth(), label = { Text("Standing goal (optional)") }, minLines = 2)
            OutlinedTextField(
                cap, { cap = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Token cap in thousands (optional)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            ToggleRow("Start now", "Launch its session immediately", startNow) { startNow = it }
            NeoButton(
                onClick = {
                    viewModel.hive.hire(name, role, cli, model, bundle, goal, (cap.toLongOrNull() ?: 0L) * 1000, startNow)
                    onDismiss()
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Hire", fontWeight = FontWeight.Black) }
        }
    }
}

// ---- Automations ------------------------------------------------------------------------------

@Composable
internal fun AutomationsTab(hive: HiveSnapshot, viewModel: MainViewModel) {
    var editing by remember { mutableStateOf<HiveAutomation?>(null) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Muted("Missions on a schedule, context rules and webhooks — the floor runs while you're away.", Modifier.weight(1f))
                SmallBtn("+ New", primary = true) {
                    editing = HiveAutomation(java.util.UUID.randomUUID().toString(), AutomationKind.MISSION, "", "", target = "god")
                }
            }
        }
        items(hive.automations, key = { it.id }) { a ->
            NeoCard(onClick = { editing = a }) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NeoBadge(a.kind.title.uppercase(), containerColor = when (a.kind) { AutomationKind.MISSION -> Sky; AutomationKind.CONTEXT_RULE -> Amber; AutomationKind.WEBHOOK -> LiveGreen })
                        Spacer(Modifier.width(8.dp))
                        Text(a.title.ifBlank { "(untitled)" }, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1)
                        com.hivebench.app.ui.neo.Switch(checked = a.enabled, onCheckedChange = { on -> viewModel.hive.saveAutomation(a.copy(enabled = on)) })
                    }
                    Mono(
                        when (a.kind) {
                            AutomationKind.MISSION -> (if (a.dailyMinute >= 0) "daily at %02d:%02d".format(a.dailyMinute / 60, a.dailyMinute % 60) else "every ${a.intervalMinutes} min") + " → ${a.target}"
                            AutomationKind.CONTEXT_RULE -> "every ${a.intervalMinutes} min: /compact when ctx ≥ ${a.minContextPct}% → ${a.target}"
                            AutomationKind.WEBHOOK -> "POST :${viewModel.hive.webhookPort} secret ${a.token.take(6)}… → ${a.target}" + if (a.autoAllow) " · auto-allow" else " · you approve"
                        },
                    )
                    if (a.lastRunAt > 0) Mono("last ${timeOf(a.lastRunAt)}")
                    if (a.kind == AutomationKind.MISSION) Row { SmallBtn("Run now") { viewModel.hive.runAutomationNow(a.id) } }
                }
            }
        }
    }
    editing?.let { a -> AutomationDrawer(hive, a, viewModel) { editing = null } }
}

@Composable
private fun AutomationDrawer(hive: HiveSnapshot, initial: HiveAutomation, viewModel: MainViewModel, onDismiss: () -> Unit) {
    var a by remember { mutableStateOf(initial) }
    val isNew = hive.automations.none { it.id == initial.id }
    NeoBottomDrawer(visible = true, onDismiss = onDismiss, initialDetent = DrawerDetent.Full, allowedDetents = listOf(DrawerDetent.Full), title = if (isNew) "New automation" else "Edit automation") { _, _ ->
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ChipRow {
                AutomationKind.entries.forEach { k ->
                    Chip(k.title, a.kind == k) {
                        a = a.copy(kind = k, token = if (k == AutomationKind.WEBHOOK && a.token.isBlank()) java.util.UUID.randomUUID().toString().replace("-", "") else a.token)
                    }
                }
            }
            OutlinedTextField(a.title, { a = a.copy(title = it) }, Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true)
            if (a.kind != AutomationKind.WEBHOOK) {
                OutlinedTextField(a.body, { a = a.copy(body = it) }, Modifier.fillMaxWidth(), label = { Text(if (a.kind == AutomationKind.MISSION) "Mission (sent as a request)" else "What to keep when compacting") }, minLines = 3)
            }
            Text("Target", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            ChipRow {
                if (a.kind == AutomationKind.CONTEXT_RULE) Chip("Every agent", a.target == "all") { a = a.copy(target = "all") }
                hive.agents.forEach { ag -> Chip(ag.name, a.target == ag.id) { a = a.copy(target = ag.id) } }
            }
            if (a.kind != AutomationKind.WEBHOOK) {
                OutlinedTextField(
                    a.intervalMinutes.toString(), { v -> a = a.copy(intervalMinutes = v.filter(Char::isDigit).toIntOrNull()?.coerceAtLeast(1) ?: 1) },
                    Modifier.fillMaxWidth(), label = { Text("Every N minutes") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
            if (a.kind == AutomationKind.MISSION) {
                var daily by remember { mutableStateOf(if (a.dailyMinute >= 0) "%02d:%02d".format(a.dailyMinute / 60, a.dailyMinute % 60) else "") }
                OutlinedTextField(daily, { v ->
                    daily = v
                    val m = Regex("^(\\d{1,2}):(\\d{2})$").matchEntire(v.trim())
                    a = a.copy(dailyMinute = m?.let { (it.groupValues[1].toInt().coerceIn(0, 23)) * 60 + it.groupValues[2].toInt().coerceIn(0, 59) } ?: -1)
                }, Modifier.fillMaxWidth(), label = { Text("Or daily at HH:MM (optional)") }, singleLine = true)
            }
            if (a.kind == AutomationKind.CONTEXT_RULE) {
                OutlinedTextField(
                    a.minContextPct.toString(), { v -> a = a.copy(minContextPct = v.filter(Char::isDigit).toIntOrNull()?.coerceIn(1, 100) ?: 60) },
                    Modifier.fillMaxWidth(), label = { Text("Compact when context ≥ %") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
            if (a.kind == AutomationKind.WEBHOOK) {
                val url = "http://${com.hivebench.app.hive.HiveWebhookServer.lanAddress() ?: "<phone-ip>"}:${viewModel.hive.webhookPort}/"
                Muted("POST $url\nHeader x-md-webhook-secret: ${a.token}\nBody {\"message\":\"…\",\"title\":\"…\"}\nReachable on the same network while the app is open.")
                ToggleRow("Auto-allow", "Deliver without asking you first (strict mode waits in Inbox › Outside)", a.autoAllow) { a = a.copy(autoAllow = it) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallBtn("Save", primary = true, enabled = a.title.isNotBlank()) { viewModel.hive.saveAutomation(a); onDismiss() }
                if (!isNew) SmallBtn("Delete") { viewModel.hive.deleteAutomation(a.id); onDismiss() }
            }
        }
    }
}

// ---- Memory -------------------------------------------------------------------------------------

@Composable
internal fun MemoryTab(hive: HiveSnapshot, viewModel: MainViewModel, openRoom: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<HiveMemory.Results?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(query) {
        kotlinx.coroutines.delay(250)
        results = if (query.isBlank()) null else viewModel.hive.searchMemory(query)
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text("Ask what the floor knows…") }, singleLine = true)
            Muted("Searches tickets, agents and every agent's notes on this phone; related words match too.")
        }
        val r = results
        if (r == null) {
            item { Text("Board", fontWeight = FontWeight.Black, fontSize = 15.sp) }
            item { NeoCard { Box(Modifier.padding(12.dp)) { com.hivebench.app.ui.MarkdownText(hive.board.ifBlank { "_Empty board._" }) } } }
            item { Text("Whose memory", fontWeight = FontWeight.Black, fontSize = 15.sp) }
            items(hive.agents, key = { "mem-${it.id}" }) { a ->
                NeoCard(onClick = { openRoom(a.id) }, shadowOffset = 2.dp) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(a, false)
                        Spacer(Modifier.width(8.dp))
                        Column { Text(a.name, fontWeight = FontWeight.Bold); Mono(".hive/agents/${a.id}/memory.md") }
                    }
                }
            }
        } else if (r.isEmpty) {
            item { Muted("Nothing on the floor matches \"$query\".") }
        } else {
            hitGroup("Tickets", r.tickets) { }
            hitGroup("Agents", r.agents) { openRoom(it.ref) }
            hitGroup("Notes", r.notes) { hit -> if (hive.agents.any { it.id == hit.ref }) openRoom(hit.ref) }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.hitGroup(title: String, hits: List<MemoryHit>, onOpen: (MemoryHit) -> Unit) {
    if (hits.isEmpty()) return
    item { Text("$title · ${hits.size}", fontWeight = FontWeight.Black, fontSize = 14.sp) }
    itemsIndexed(hits, key = { i, _ -> "$title-$i" }) { _, hit ->
        NeoCard(onClick = { onOpen(hit) }, shadowOffset = 2.dp) {
            Column(Modifier.padding(10.dp)) {
                Text(hit.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(hit.snippet, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ---- Capabilities -----------------------------------------------------------------------------------

@Composable
internal fun CapabilitiesTab(hive: HiveSnapshot, viewModel: MainViewModel) {
    var editing by remember { mutableStateOf<String?>(null) }
    var connection by remember { mutableStateOf<String?>(null) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Muted("Skills, connections and MCP servers per agent — or one of eleven role bundles in a single write. Changes apply on the agent's next start.") }
        items(hive.agents, key = { "cap-${it.id}" }) { a ->
            NeoCard(onClick = { editing = a.id }) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(a.name, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                        NeoBadge(RoleBundles.byId(a.capabilities.bundle)?.title ?: "custom", containerColor = NeoLime)
                    }
                    Mono("skills: ${a.capabilities.skills.joinToString().ifBlank { "—" }}")
                    Mono("connections: ${a.capabilities.connections.joinToString().ifBlank { "—" }}")
                    Mono("mcp: ${a.capabilities.mcpServers.joinToString().ifBlank { "—" }}")
                }
            }
        }
        item { Text("Connections", fontWeight = FontWeight.Black, fontSize = 15.sp) }
        items(HiveConnections.all, key = { "conn-${it.id}" }) { c ->
            NeoCard(onClick = { connection = c.id }, shadowOffset = 2.dp) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(c.title, fontWeight = FontWeight.Bold)
                        Muted(c.note)
                    }
                    NeoBadge(if (viewModel.hiveConnectionConfigured(c.id)) "SET" else "NOT SET", containerColor = if (viewModel.hiveConnectionConfigured(c.id)) LiveGreen else Amber)
                }
            }
        }
    }
    editing?.let { id -> hive.agents.firstOrNull { it.id == id }?.let { CapabilityDrawer(it, viewModel) { editing = null } } }
    connection?.let { id -> ConnectionDrawer(id, viewModel) { connection = null } }
}

@Composable
private fun CapabilityDrawer(agent: HiveAgent, viewModel: MainViewModel, onDismiss: () -> Unit) {
    var caps by remember(agent.id) { mutableStateOf(agent.capabilities) }
    NeoBottomDrawer(visible = true, onDismiss = onDismiss, initialDetent = DrawerDetent.Full, allowedDetents = listOf(DrawerDetent.Full), title = "${agent.name}'s capabilities") { _, _ ->
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Role bundles", fontWeight = FontWeight.Black, fontSize = 14.sp)
            RoleBundles.all.forEach { b ->
                val selected = caps.bundle == b.id
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (selected) NeoLime.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { viewModel.hive.grantBundle(agent.id, b.id); onDismiss() }.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(b.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(b.role, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                    }
                    Text(if (selected) "GRANTED" else "Grant", fontSize = 11.sp, fontWeight = FontWeight.Black, color = NeoBlack.takeIf { selected } ?: MaterialTheme.colorScheme.onSurface)
                }
            }
            Text("Skills", fontWeight = FontWeight.Black, fontSize = 14.sp)
            HiveSkills.all.forEach { s ->
                CheckRow(s.title, s.description, s.id in caps.skills) { on -> caps = caps.copy(bundle = null, skills = if (on) caps.skills + s.id else caps.skills - s.id) }
            }
            Text("Connections", fontWeight = FontWeight.Black, fontSize = 14.sp)
            HiveConnections.all.forEach { c ->
                CheckRow(c.title, c.envVars.joinToString(), c.id in caps.connections) { on -> caps = caps.copy(bundle = null, connections = if (on) caps.connections + c.id else caps.connections - c.id) }
            }
            Text("MCP servers", fontWeight = FontWeight.Black, fontSize = 14.sp)
            HiveMcpCatalog.all.forEach { m ->
                CheckRow(m.title, (if (m.safeReadOnly) "read-only" else "needs ${m.needsConnection}") + " · ${m.args.getOrNull(1) ?: m.command}", m.id in caps.mcpServers) { on ->
                    caps = caps.copy(bundle = null, mcpServers = if (on) caps.mcpServers + m.id else caps.mcpServers - m.id,
                        connections = if (on && m.needsConnection != null && m.needsConnection !in caps.connections) caps.connections + m.needsConnection else caps.connections)
                }
            }
            NeoButton(onClick = { viewModel.hive.update(agent.id) { it.copy(capabilities = caps) }; onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                Text("Save", fontWeight = FontWeight.Black)
            }
            if (agent.cli != AgentKind.CLAUDE_CODE) Muted("MCP servers and skills are wired for Claude Code; other CLIs get them listed in their first prompt.")
        }
    }
}

@Composable
private fun CheckRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }, verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
    }
}

@Composable
private fun ConnectionDrawer(id: String, viewModel: MainViewModel, onDismiss: () -> Unit) {
    val conn = HiveConnections.byId(id) ?: return
    var secret by remember { mutableStateOf("") }
    NeoBottomDrawer(visible = true, onDismiss = onDismiss, initialDetent = DrawerDetent.Half, title = conn.title, subtitle = "Exported to granted agents as ${conn.envVars.joinToString()}") { _, _ ->
        OutlinedTextField(secret, { secret = it }, Modifier.fillMaxWidth(), label = { Text("Token / key") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallBtn("Save", primary = true, enabled = secret.isNotBlank()) { viewModel.saveHiveConnection(id, secret); onDismiss() }
            if (viewModel.hiveConnectionConfigured(id)) SmallBtn("Remove") { viewModel.saveHiveConnection(id, ""); onDismiss() }
        }
    }
}
