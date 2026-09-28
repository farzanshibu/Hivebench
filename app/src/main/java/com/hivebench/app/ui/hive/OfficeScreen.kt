package com.hivebench.app.ui.hive

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hivebench.app.hive.AgentSpend
import com.hivebench.app.hive.BreakerLevel
import com.hivebench.app.hive.ExternalDecision
import com.hivebench.app.hive.HiveAgent
import com.hivebench.app.hive.HiveMessage
import com.hivebench.app.hive.HiveSnapshot
import com.hivebench.app.hive.HiveTask
import com.hivebench.app.hive.Pricing
import com.hivebench.app.hive.TaskColumn
import com.hivebench.app.terminal.TerminalSessions
import com.hivebench.app.ui.AppUiState
import com.hivebench.app.ui.MainViewModel
import com.hivebench.app.ui.neo.DrawerDetent
import com.hivebench.app.ui.neo.LinearProgressIndicator
import com.hivebench.app.ui.neo.MaterialTheme
import com.hivebench.app.ui.neo.NeoBottomDrawer
import com.hivebench.app.ui.neo.OutlinedTextField
import com.hivebench.app.ui.neo.Switch
import com.hivebench.app.ui.neo.Text
import com.hivebench.app.ui.theme.NeoBadge
import com.hivebench.app.ui.theme.NeoBlack
import com.hivebench.app.ui.theme.NeoButton
import com.hivebench.app.ui.theme.NeoCard
import com.hivebench.app.ui.theme.NeoDarkBorder
import com.hivebench.app.ui.theme.NeoLime
import com.hivebench.app.ui.theme.PulsingDot

internal val LiveGreen = Color(0xFF58C9A3)
internal val Amber = Color(0xFFF4B740)
internal val Coral = Color(0xFFFF6B6B)
internal val Sky = Color(0xFF6BB8FF)

private enum class OfficeTab(val label: String) {
    ORCHESTRATOR("Orchestrator"),
    AGENTS("Agents"),
    TASKS("Tasks"),
    INBOX("Inbox"),
    AUTOMATIONS("Automations"),
    MEMORY("Memory"),
    CAPABILITIES("Capabilities"),
}

/**
 * The office (munder-difflin's command center, on a phone): the orchestrator's
 * own screen, a card per agent, per-agent rooms, the task board, the three
 * inbox queues, automations, memory search and capability grants.
 */
@Composable
fun OfficeScreen(state: AppUiState, viewModel: MainViewModel) {
    val hive = state.hive
    var tab by rememberSaveable { mutableStateOf(OfficeTab.ORCHESTRATOR) }
    var room by rememberSaveable { mutableStateOf<String?>(null) }
    val info by TerminalSessions.info.collectAsState()

    room?.let { id ->
        val agent = hive.agents.firstOrNull { it.id == id }
        if (agent != null) {
            BackHandler { room = null }
            AgentRoomScreen(hive, agent, viewModel, onBack = { room = null })
            return
        }
    }

    if (!hive.ready) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Opening the office…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        FloorHeader(hive, info, viewModel)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OfficeTab.entries.forEach { t ->
                val badge = when (t) {
                    OfficeTab.INBOX -> hive.askMe.size + hive.external.count { it.decision == ExternalDecision.PENDING }
                    OfficeTab.TASKS -> hive.tasks.count { it.column != TaskColumn.DONE }
                    OfficeTab.AGENTS -> hive.agents.size
                    else -> 0
                }
                Chip(if (badge > 0) "${t.label} $badge" else t.label, selected = tab == t) { tab = t }
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f)) {
            when (tab) {
                OfficeTab.ORCHESTRATOR -> OrchestratorTab(hive, info, viewModel) { room = it }
                OfficeTab.AGENTS -> AgentsTab(state, hive, info, viewModel) { room = it }
                OfficeTab.TASKS -> TasksTab(state, hive, viewModel)
                OfficeTab.INBOX -> InboxTab(hive, viewModel) { room = it }
                OfficeTab.AUTOMATIONS -> AutomationsTab(hive, viewModel)
                OfficeTab.MEMORY -> MemoryTab(hive, viewModel) { room = it }
                OfficeTab.CAPABILITIES -> CapabilitiesTab(hive, viewModel)
            }
        }
    }
}

@Composable
private fun FloorHeader(hive: HiveSnapshot, info: Map<String, com.hivebench.app.terminal.TerminalSessionInfo>, viewModel: MainViewModel) {
    val live = hive.agents.count { isLive(info, hive, it) }
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("The office", fontWeight = FontWeight.Black, fontSize = 20.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (live > 0) PulsingDot(color = LiveGreen, size = 6.dp)
                Spacer(Modifier.width(4.dp))
                Text(
                    "$live/${hive.agents.size} live · ${usd(hive.floorSpendUsd)} spent" + if (hive.breaker.floorHalted) " · FLOOR HALTED" else "",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (hive.breaker.floorHalted) Coral else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (live < hive.agents.count { !it.paused }) SmallBtn("Start floor", primary = true) { viewModel.hive.startAll() }
    }
}

internal fun isLive(info: Map<String, com.hivebench.app.terminal.TerminalSessionInfo>, hive: HiveSnapshot, agent: HiveAgent) =
    info["hive:${hive.projectId}:${agent.id}"]?.running == true

internal fun usd(v: Double) = "$" + "%.2f".format(v)

internal fun tokens(v: Long) = when {
    v >= 1_000_000 -> "%.1fM".format(v / 1_000_000.0)
    v >= 1_000 -> "${v / 1000}k"
    else -> "$v"
}

internal fun ctxPct(spend: AgentSpend?): Int? =
    spend?.takeIf { it.contextTokens > 0 }?.let { (it.contextTokens * 100 / Pricing.contextLimit(it.model)).toInt() }

// ---- Orchestrator ------------------------------------------------------------------

@Composable
private fun OrchestratorTab(
    hive: HiveSnapshot,
    info: Map<String, com.hivebench.app.terminal.TerminalSessionInfo>,
    viewModel: MainViewModel,
    openRoom: (String) -> Unit,
) {
    val god = hive.god ?: return
    val spend = hive.spend[god.id] ?: AgentSpend()
    val status = hive.status[god.id]
    val live = isLive(info, hive, god)
    val breaker = hive.breaker
    var editCap by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            NeoCard(onClick = { openRoom(god.id) }, borderColor = if (live) NeoLime else null) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(god, live)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${god.name} · orchestrator", fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text(status?.doing?.ifBlank { null } ?: if (live) "Running the floor" else "Offline", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                        }
                        LevelBadge(breaker.levels[god.id])
                    }
                    // Spend against his cap.
                    val cap = breaker.godCapUsd
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Spend ${usd(spend.costUsd)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Text(if (cap > 0) "cap ${usd(cap)}" else "no cap", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { editCap = true }.padding(4.dp))
                    }
                    if (cap > 0) {
                        val frac = (spend.costUsd / cap).toFloat().coerceIn(0f, 1f)
                        LinearProgressIndicator(progress = { frac }, modifier = Modifier.fillMaxWidth(), color = if (frac > 0.85f) Coral else if (frac > 0.6f) Amber else LiveGreen)
                    }
                    Text(
                        "${tokens(spend.totalTokens)} tok" + (ctxPct(spend)?.let { " · ctx $it%" } ?: "") + " · inbox ${hive.unread[god.id] ?: 0}",
                        fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (live) {
                            SmallBtn("Room", primary = true) { openRoom(god.id) }
                            SmallBtn("Halt") { viewModel.hive.halt(god.id) }
                            SmallBtn("Stop") { viewModel.hive.stop(god.id) }
                        } else {
                            SmallBtn("Start ${god.name}", primary = true) { viewModel.hive.start(god.id) }
                        }
                        if ((breaker.levels[god.id] ?: BreakerLevel.HEALTHY) != BreakerLevel.HEALTHY) SmallBtn("Reset breaker") { viewModel.hive.resetBreaker(god.id) }
                    }
                }
            }
        }
        item {
            Section("Breaker") {
                ToggleRow("Breaker armed", "Steer → constrain agents that loop, error-storm or overspend", breaker.enabled) { on -> viewModel.hive.updateBreaker { it.copy(enabled = on) } }
                ToggleRow("Hard stop", "Escalate past constrain to killing the agent", breaker.hardStop) { on -> viewModel.hive.updateBreaker { it.copy(hardStop = on) } }
                ToggleRow("Halt the floor", "Every agent stops cleanly at its next step", breaker.floorHalted) { on -> viewModel.hive.setFloorHalt(on) }
                Text(
                    "Repeat limit ${breaker.repeatedToolLimit} · error storm ${breaker.errorStormLimit} · velocity ${tokens(breaker.tokenVelocityPerMin)}/min · floor cap ${if (breaker.floorCapUsd > 0) usd(breaker.floorCapUsd) else "off"}",
                    fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                hive.agents.filter { (breaker.levels[it.id] ?: BreakerLevel.HEALTHY) != BreakerLevel.HEALTHY }.forEach { a ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LevelBadge(breaker.levels[a.id])
                        Spacer(Modifier.width(8.dp))
                        Text("${a.name}: ${breaker.reasons[a.id].orEmpty()}", fontSize = 12.sp, modifier = Modifier.weight(1f))
                        SmallBtn("Reset") { viewModel.hive.resetBreaker(a.id) }
                    }
                }
            }
        }
        item { Text("Routing log", fontWeight = FontWeight.Black, fontSize = 15.sp) }
        if (hive.log.isEmpty()) item { Muted("Nothing routed yet.") }
        itemsIndexed(hive.log.take(150), key = { i, e -> "$i-${e.at}" }) { _, e ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Text(timeOf(e.at), fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(44.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        listOfNotNull(e.type.uppercase(), listOfNotNull(e.from, e.to).joinToString(" → ").ifBlank { null }).joinToString(" "),
                        fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace,
                        color = when (e.type) { "breaker" -> Coral; "drop" -> Amber; "message" -> Sky; else -> MaterialTheme.colorScheme.onSurfaceVariant },
                    )
                    Text(e.summary, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }

    if (editCap) {
        NumberDrawer(
            title = "${god.name}'s spend cap (USD)",
            initial = breaker.godCapUsd.toString(),
            hint = "The breaker stops him when his spend reaches this. 0 = no cap.",
            onDismiss = { editCap = false },
        ) { value ->
            viewModel.hive.updateBreaker { it.copy(godCapUsd = value.toDoubleOrNull()?.coerceAtLeast(0.0) ?: it.godCapUsd) }
            editCap = false
        }
    }
}

// ---- Agents --------------------------------------------------------------------------

@Composable
private fun AgentsTab(
    state: AppUiState,
    hive: HiveSnapshot,
    info: Map<String, com.hivebench.app.terminal.TerminalSessionInfo>,
    viewModel: MainViewModel,
    openRoom: (String) -> Unit,
) {
    var hiring by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Muted("Each agent is its own ${""}CLI session, running in parallel.", Modifier.weight(1f))
                SmallBtn("+ Hire", primary = true) { hiring = true }
            }
        }
        items(hive.agents, key = { it.id }) { agent ->
            AgentCard(hive, agent, isLive(info, hive, agent), onOpen = { openRoom(agent.id) }, viewModel = viewModel)
        }
    }
    if (hiring) HireDrawer(state, viewModel) { hiring = false }
}

@Composable
internal fun AgentCard(hive: HiveSnapshot, agent: HiveAgent, live: Boolean, onOpen: () -> Unit, viewModel: MainViewModel) {
    val status = hive.status[agent.id]
    val spend = hive.spend[agent.id]
    val ticket = hive.tasks.firstOrNull { it.assignee == agent.id && it.column == TaskColumn.DOING }
        ?: hive.tasks.firstOrNull { it.assignee == agent.id && it.column != TaskColumn.DONE }
    NeoCard(onClick = onOpen, borderColor = if (live) NeoLime else null) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(agent, live)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(agent.name, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1)
                    Text("${agent.role} · ${agent.cli.title}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                (hive.unread[agent.id] ?: 0).takeIf { it > 0 }?.let { NeoBadge("✉ $it", containerColor = Sky) }
                Spacer(Modifier.width(4.dp))
                LevelBadge(hive.breaker.levels[agent.id])
            }
            // What it is doing right now, in words.
            Text(
                when {
                    !live -> if (agent.paused) "Paused" else "Offline"
                    agent.paused -> "Paused by you"
                    status?.state == "waiting" -> status.doing
                    status?.doing.isNullOrBlank() -> "Idle"
                    else -> status!!.doing
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (status?.state == "waiting") Amber else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Mono(ctxPct(spend)?.let { "ctx $it%" } ?: "ctx —")
                Mono(if (spend != null && spend.costUsd > 0) usd(spend.costUsd) else "$—")
                Mono(ticket?.let { "${it.id}" } ?: "no ticket")
                if (agent.onHold) NeoBadge("ON HOLD", containerColor = Amber)
            }
            ticket?.let { Text(it.title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            if (!live) Row { SmallBtn("Start", primary = true) { viewModel.hive.start(agent.id) } }
        }
    }
}

@Composable
internal fun Avatar(agent: HiveAgent, live: Boolean) {
    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                .background(if (agent.isGod) Amber else if (live) NeoLime else MaterialTheme.colorScheme.surfaceVariant)
                .border(1.5.dp, NeoBlack, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(agent.name.take(1).uppercase(), fontWeight = FontWeight.Black, color = NeoBlack)
        }
        if (live) Box(Modifier.size(11.dp).background(MaterialTheme.colorScheme.surface, CircleShape).padding(2.dp)) {
            PulsingDot(color = LiveGreen, size = 5.dp)
        }
    }
}

@Composable
internal fun LevelBadge(level: BreakerLevel?) {
    when (level) {
        BreakerLevel.STEERING -> NeoBadge("STEER", containerColor = Amber)
        BreakerLevel.CONSTRAINED -> NeoBadge("CONSTRAIN", containerColor = Coral)
        BreakerLevel.STOPPED -> NeoBadge("STOPPED", containerColor = Coral)
        else -> Unit
    }
}

// ---- Tasks ------------------------------------------------------------------------------

@Composable
private fun TasksTab(state: AppUiState, hive: HiveSnapshot, viewModel: MainViewModel) {
    var creating by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    var open by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Muted("Done cards leave the board on their own after 30 min.", Modifier.weight(1f))
            SmallBtn("Import") { importing = true }
            Spacer(Modifier.width(6.dp))
            SmallBtn("+ Card", primary = true) { creating = true }
        }
        Row(
            Modifier.fillMaxSize().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TaskColumn.entries.forEach { column ->
                val cards = hive.tasks.filter { it.column == column }.sortedBy { it.priority }
                Column(Modifier.width(250.dp).fillMaxSize()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                        Box(Modifier.size(10.dp).background(columnColor(column), CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text("${column.title} · ${cards.size}", fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                        items(cards, key = { it.id }) { task -> TaskCard(hive, task) { open = task.id } }
                    }
                }
            }
        }
    }
    if (creating) NewTaskDrawer(hive, viewModel) { creating = false }
    if (importing) com.hivebench.app.ui.IssueImportSheet(state, viewModel) { importing = false }
    open?.let { id -> hive.tasks.firstOrNull { it.id == id }?.let { TaskDrawer(hive, it, viewModel) { open = null } } ?: run { open = null } }
}

internal fun columnColor(c: TaskColumn) = when (c) {
    TaskColumn.TODO -> Sky
    TaskColumn.DOING -> Color(0xFFFFE066)
    TaskColumn.BLOCKED -> Coral
    TaskColumn.DONE -> LiveGreen
}

@Composable
private fun TaskCard(hive: HiveSnapshot, task: HiveTask, onOpen: () -> Unit) {
    NeoCard(onClick = onOpen, shadowOffset = 2.dp) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Mono(task.id, Modifier.weight(1f))
                if (task.waitsOnHuman) NeoBadge("ASK ME", containerColor = Amber)
            }
            Text(task.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(
                task.assignee?.let { id -> "@" + (hive.agents.firstOrNull { it.id == id }?.name ?: id) } ?: "unassigned",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            hive.doneSince[task.id]?.let { since ->
                val left = ((com.hivebench.app.hive.DONE_CARD_TTL_MS - (System.currentTimeMillis() - since)) / 60_000).coerceAtLeast(0)
                Text("leaves the board in ${left}m", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun NewTaskDrawer(hive: HiveSnapshot, viewModel: MainViewModel, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var assignee by remember { mutableStateOf<String?>(null) }
    var dispatch by remember { mutableStateOf(true) }
    NeoBottomDrawer(visible = true, onDismiss = onDismiss, initialDetent = DrawerDetent.Full, allowedDetents = listOf(DrawerDetent.Full), title = "New card") { _, _ ->
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Title") }, singleLine = true)
            OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text("Details") }, minLines = 3)
            Text("Owner", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            ChipRow {
                Chip("Let ${hive.god?.name ?: "god"} decide", assignee == null) { assignee = null }
                hive.workers.forEach { a -> Chip(a.name, assignee == a.id) { assignee = a.id } }
            }
            ToggleRow("Dispatch now", "Mail the card to its owner (or the orchestrator)", dispatch) { dispatch = it }
            NeoButton(
                onClick = { viewModel.hive.createTask(title, description, assignee, dispatch); onDismiss() },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Create card", fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
private fun TaskDrawer(hive: HiveSnapshot, task: HiveTask, viewModel: MainViewModel, onDismiss: () -> Unit) {
    var answer by remember(task.id) { mutableStateOf("") }
    NeoBottomDrawer(visible = true, onDismiss = onDismiss, initialDetent = DrawerDetent.Full, allowedDetents = listOf(DrawerDetent.Half, DrawerDetent.Full), title = task.id, subtitle = task.title) { _, _ ->
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (task.description.isNotBlank()) com.hivebench.app.ui.MarkdownText(task.description)
            Text("Column", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            ChipRow { TaskColumn.entries.forEach { c -> Chip(c.title, task.column == c) { viewModel.hive.moveTask(task.id, c) } } }
            Text("Owner", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            ChipRow {
                Chip("Nobody", task.assignee == null) { viewModel.hive.assignTask(task.id, null) }
                hive.agents.forEach { a -> Chip(a.name, task.assignee == a.id) { viewModel.hive.assignTask(task.id, a.id) } }
            }
            if (task.humanQA.isNotEmpty()) {
                Text("Decision trail", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                task.humanQA.forEach { qa ->
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp)) {
                        com.hivebench.app.ui.MarkdownText(qa.q)
                        qa.a?.let { Text("↳ $it", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                        if (qa.dismissedAt != null) Muted("dismissed")
                    }
                }
            }
            if (task.openQuestion != null) {
                OutlinedTextField(answer, { answer = it }, Modifier.fillMaxWidth(), label = { Text("Your answer") }, minLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallBtn("Answer", primary = true, enabled = answer.isNotBlank()) { viewModel.hive.answer(task.id, answer); answer = "" }
                    SmallBtn("Dismiss ask") { viewModel.hive.dismissAsk(task.id) }
                }
            }
            Spacer(Modifier.height(8.dp))
            SmallBtn("Remove card") { viewModel.hive.deleteTask(task.id); onDismiss() }
        }
    }
}

// ---- Inbox: three queues ---------------------------------------------------------------------

private enum class InboxQueue(val label: String) { YOU("Waiting on you"), TEAM("Agents talking"), OUTSIDE("From outside") }

@Composable
private fun InboxTab(hive: HiveSnapshot, viewModel: MainViewModel, openRoom: (String) -> Unit) {
    var queue by rememberSaveable { mutableStateOf(InboxQueue.YOU) }
    val forYou = hive.messages.filter { it.needsHuman || it.to.equals("human", true) }.asReversed()
    val team = hive.messages.filter { it.from != "human" && !it.needsHuman && !it.to.equals("human", true) && it.from !in setOf("webhook") }.asReversed()
    Column(Modifier.fillMaxSize()) {
        ChipRow(Modifier.padding(horizontal = 16.dp)) {
            Chip("${InboxQueue.YOU.label} ${hive.askMe.size + forYou.size}", queue == InboxQueue.YOU) { queue = InboxQueue.YOU }
            Chip("${InboxQueue.TEAM.label} ${team.size}", queue == InboxQueue.TEAM) { queue = InboxQueue.TEAM }
            Chip("${InboxQueue.OUTSIDE.label} ${hive.external.size}", queue == InboxQueue.OUTSIDE) { queue = InboxQueue.OUTSIDE }
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (queue) {
                InboxQueue.YOU -> {
                    if (hive.askMe.isEmpty() && forYou.isEmpty()) item { Muted("Nothing is waiting on you.") }
                    items(hive.askMe, key = { "ask-${it.id}" }) { task -> AskMeCard(hive, task, viewModel) }
                    items(forYou, key = { "you-${it.id}" }) { m -> MessageCard(hive, m) { openRoom(m.from) } }
                }
                InboxQueue.TEAM -> {
                    if (team.isEmpty()) item { Muted("Your agents haven't messaged each other yet.") }
                    items(team.take(200), key = { "team-${it.id}-${it.to}" }) { m -> MessageCard(hive, m) { hive.agents.firstOrNull { it.id == m.from }?.let { openRoom(it.id) } } }
                }
                InboxQueue.OUTSIDE -> {
                    item {
                        Muted("Webhooks POST to http://${com.hivebench.app.hive.HiveWebhookServer.lanAddress() ?: "<phone-ip>"}:${viewModel.hive.webhookPort}/ with header x-md-webhook-secret. Add one in Automations.")
                    }
                    items(hive.external.asReversed(), key = { it.id }) { e ->
                        NeoCard {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Mono(e.source, Modifier.weight(1f))
                                    NeoBadge(e.decision.name.replace('_', ' '), containerColor = when (e.decision) {
                                        ExternalDecision.PENDING -> Amber
                                        ExternalDecision.REJECTED -> Coral
                                        else -> LiveGreen
                                    })
                                }
                                Text(e.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(e.body, fontSize = 12.sp, maxLines = 6, overflow = TextOverflow.Ellipsis)
                                if (e.decision == ExternalDecision.PENDING) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SmallBtn("Approve → ${e.routedTo ?: "god"}", primary = true) { viewModel.hive.decideExternal(e.id, true) }
                                    SmallBtn("Reject") { viewModel.hive.decideExternal(e.id, false) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AskMeCard(hive: HiveSnapshot, task: HiveTask, viewModel: MainViewModel) {
    var answer by remember(task.id) { mutableStateOf("") }
    val q = task.openQuestion ?: return
    val cascade = hive.tasks.filter { task.id in it.dependsOn && it.column != TaskColumn.DONE }
    NeoCard(borderColor = Amber) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NeoBadge("ASK ME", containerColor = Amber)
                Spacer(Modifier.width(8.dp))
                Mono("${task.id} · ${task.title}", Modifier.weight(1f))
            }
            com.hivebench.app.ui.MarkdownText(q.q)
            if (cascade.isNotEmpty()) Muted("Unblocks: " + cascade.joinToString { it.id })
            OutlinedTextField(answer, { answer = it }, Modifier.fillMaxWidth(), placeholder = { Text("Answer…") }, minLines = 1)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallBtn("Answer", primary = true, enabled = answer.isNotBlank()) { viewModel.hive.answer(task.id, answer); answer = "" }
                SmallBtn("Dismiss") { viewModel.hive.dismissAsk(task.id) }
            }
        }
    }
}

@Composable
internal fun MessageCard(hive: HiveSnapshot, m: HiveMessage, onClick: () -> Unit) {
    fun name(id: String) = hive.agents.firstOrNull { it.id == id }?.name ?: id
    NeoCard(onClick = onClick, shadowOffset = 2.dp) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${name(m.from)} → ${name(m.to)}", fontWeight = FontWeight.Black, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1)
                NeoBadge(m.act.wire.uppercase(), containerColor = if (m.act.obligatesReply) Sky else MaterialTheme.colorScheme.surfaceVariant)
            }
            Text(m.subject, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(m.body, fontSize = 12.sp, maxLines = 5, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Mono(timeOf(m.deliveredAt))
        }
    }
}

// ---- shared bits ------------------------------------------------------------------------------

internal fun timeOf(millis: Long): String =
    if (millis <= 0) "" else java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(millis))

@Composable
internal fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val border = if (isSystemInDarkTheme()) NeoDarkBorder else NeoBlack
    Box(
        Modifier.clip(RoundedCornerShape(9.dp))
            .background(if (selected) NeoLime else MaterialTheme.colorScheme.surface)
            .border(1.5.dp, if (selected) NeoBlack else border, RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold, color = if (selected) NeoBlack else MaterialTheme.colorScheme.onSurface, maxLines = 1)
    }
}

@Composable
internal fun ChipRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), content = content)
}

@Composable
internal fun SmallBtn(label: String, primary: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    NeoButton(
        onClick = onClick,
        enabled = enabled,
        containerColor = if (primary) NeoLime else MaterialTheme.colorScheme.surface,
        contentColor = if (primary) NeoBlack else MaterialTheme.colorScheme.onSurface,
        cornerRadius = 10.dp,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
    ) { Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1) }
}

@Composable
internal fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    NeoCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Black, fontSize = 15.sp)
            content()
        }
    }
}

@Composable
internal fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
internal fun Muted(text: String, modifier: Modifier = Modifier) =
    Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)

@Composable
internal fun Mono(text: String, modifier: Modifier = Modifier) =
    Text(text, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier, maxLines = 1, overflow = TextOverflow.Ellipsis)

@Composable
internal fun NumberDrawer(title: String, initial: String, hint: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember { mutableStateOf(initial) }
    NeoBottomDrawer(visible = true, onDismiss = onDismiss, initialDetent = DrawerDetent.Half, title = title, subtitle = hint) { _, _ ->
        OutlinedTextField(
            value, { value = it }, Modifier.fillMaxWidth(), singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
        )
        Spacer(Modifier.height(12.dp))
        NeoButton(onClick = { onSave(value) }, modifier = Modifier.fillMaxWidth()) { Text("Save", fontWeight = FontWeight.Black) }
    }
}

