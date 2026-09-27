package com.jarves.mh.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarves.mh.model.AgentInboxMessage
import com.jarves.mh.model.AgentInstance
import com.jarves.mh.model.AgentMessageType
import com.jarves.mh.model.AgentRole
import com.jarves.mh.model.AgentStatus
import com.jarves.mh.model.AgentTask
import com.jarves.mh.model.BlackboardEntry
import com.jarves.mh.model.CircuitBreakerState
import com.jarves.mh.model.ScheduledTask
import com.jarves.mh.model.TaskPriority
import com.jarves.mh.model.TaskStatus
import com.jarves.mh.network.LinearIssue
import com.jarves.mh.orchestrator.SwarmAuditEvent
import com.jarves.mh.ui.theme.NeoBlack
import com.jarves.mh.ui.theme.NeoDarkBorder
import com.jarves.mh.ui.theme.NeoLime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TaskBoardTab(val label: String, val icon: ImageVector) {
    KANBAN("Board", Icons.Default.ViewKanban),
    AGENTS("Agents (Swarm)", Icons.Default.SmartToy),
    BLACKBOARD("Blackboard", Icons.Default.Hub),
    SCHEDULE("Scheduler", Icons.Default.Schedule),
    SKILLS("Skills", Icons.Default.Tune),
    REVIEWS("Diff Review", Icons.Default.Search),
    LINEAR("Linear", Icons.Default.Bookmark),
    AUDIT("Audit Log", Icons.Default.History),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskBoardScreen(
    tasks: List<AgentTask>,
    agents: List<AgentInstance>,
    blackboardEntries: List<BlackboardEntry>,
    auditLogs: List<SwarmAuditEvent>,
    circuitBreaker: CircuitBreakerState,
    inboxMessages: List<AgentInboxMessage>,
    scheduledTasks: List<com.jarves.mh.model.ScheduledTask> = emptyList(),
    skills: List<com.jarves.mh.skills.SkillDefinition> = emptyList(),
    diffReviews: List<com.jarves.mh.review.DiffReviewSession> = emptyList(),
    onDecomposeGoal: (String) -> Unit,
    onCreateTask: (String, String, TaskPriority, String?, List<String>) -> Unit,
    onMoveTaskStatus: (String, TaskStatus) -> Unit,
    onAssignTask: (String, String?) -> Unit,
    onResetCircuitBreaker: () -> Unit,
    onSendDirectMessage: (String, String, String) -> Unit,
    onAddBlackboardEntry: (String, String, String) -> Unit,
    onCreateSchedule: (String, String, Int, Boolean, String) -> Unit = { _, _, _, _, _ -> },
    onToggleSchedule: (String) -> Unit = {},
    onDeleteSchedule: (String) -> Unit = {},
    onMergeWorktree: (String) -> Unit = {},
    onRemoveWorktree: (String) -> Unit = {},
    onToggleSkill: (String) -> Unit = {},
    onCreateSkill: (String, String, com.jarves.mh.skills.SkillCategory, String) -> Unit = { _, _, _, _ -> },
    onToggleAnnotationResolved: (String) -> Unit = {},
    onRunAutomatedReview: (String) -> Unit = {},
    linearIssues: List<LinearIssue> = emptyList(),
    linearApiKey: String? = null,
    linearLoading: Boolean = false,
    onSaveLinearApiKey: (String) -> Unit = {},
    onRefreshLinearIssues: () -> Unit = {},
    onImportLinearIssue: (LinearIssue) -> Unit = {},
) {
    var currentTab by rememberSaveable { mutableStateOf(TaskBoardTab.KANBAN) }
    var goalInput by rememberSaveable { mutableStateOf("") }
    var showNewTaskDialog by rememberSaveable { mutableStateOf(false) }
    var showAddFactDialog by rememberSaveable { mutableStateOf(false) }
    var showNewScheduleDialog by rememberSaveable { mutableStateOf(false) }
    var showNewSkillDialog by rememberSaveable { mutableStateOf(false) }
    var selectedAgentForInbox by remember { mutableStateOf<AgentInstance?>(null) }
    var selectedTaskForDetail by remember { mutableStateOf<AgentTask?>(null) }
    var selectedTaskForWorktreeReview by remember { mutableStateOf<AgentTask?>(null) }

    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) NeoDarkBorder else NeoBlack

    Column(Modifier.fillMaxSize()) {
        // 1. GOD Orchestrator Goal Bar & Circuit Breaker
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(2.dp, borderColor)),
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                // Header with circuit breaker pill
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("👑", fontSize = 16.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "GOD ORCHESTRATOR",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    // Circuit Breaker Pill
                    if (circuitBreaker.isTripped) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFF5252))
                                .border(1.5.dp, NeoBlack, RoundedCornerShape(6.dp))
                                .clickable { onResetCircuitBreaker() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(Icons.Default.Warning, null, Modifier.size(12.dp), tint = Color.White)
                            Text("TRIPPED (RESET)", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Box(Modifier.size(7.dp).background(NeoLime, CircleShape).border(1.dp, NeoBlack, CircleShape))
                            Text("CIRCUIT BREAKER ARMED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Decompose Goal Input
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = goalInput,
                        onValueChange = { goalInput = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("E.g. Build SQLite caching layer & tests", fontSize = 12.sp) },
                        shape = RoundedCornerShape(8.dp),
                        leadingIcon = {
                            Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp), tint = NeoLime)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeoLime,
                            unfocusedBorderColor = borderColor,
                        ),
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (goalInput.isNotBlank()) {
                                onDecomposeGoal(goalInput)
                                goalInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeoLime, contentColor = NeoBlack),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(2.dp, NeoBlack),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(48.dp),
                    ) {
                        Text("DECOMPOSE", fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }
            }
        }

        // 2. Tab Navigation Row
        ScrollableTabRow(
            selectedTabIndex = currentTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            edgePadding = 8.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[currentTab.ordinal]),
                    height = 3.dp,
                    color = NeoLime,
                )
            },
            modifier = Modifier.border(BorderStroke(1.dp, borderColor)),
        ) {
            TaskBoardTab.entries.forEach { tab ->
                val selected = currentTab == tab
                Tab(
                    selected = selected,
                    onClick = { currentTab = tab },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(tab.icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = if (selected) NeoLime else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                tab.label,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (tab == TaskBoardTab.KANBAN) {
                                Text("(${tasks.size})", fontSize = 10.sp, color = NeoLime, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                )
            }
        }

        // 3. Tab Contents
        Box(Modifier.fillMaxSize()) {
            when (currentTab) {
                TaskBoardTab.KANBAN -> KanbanBoardContent(
                    tasks = tasks,
                    agents = agents,
                    onMoveStatus = onMoveTaskStatus,
                    onSelectTask = { selectedTaskForDetail = it },
                    onAddNewTask = { showNewTaskDialog = true },
                    onReviewWorktree = { selectedTaskForWorktreeReview = it },
                )
                TaskBoardTab.AGENTS -> AgentSwarmContent(
                    agents = agents,
                    tasks = tasks,
                    onOpenInbox = { selectedAgentForInbox = it },
                )
                TaskBoardTab.BLACKBOARD -> BlackboardContent(
                    entries = blackboardEntries,
                    onAddFact = { showAddFactDialog = true },
                )
                TaskBoardTab.SCHEDULE -> SchedulerContent(
                    scheduledTasks = scheduledTasks,
                    agents = agents,
                    onAddNewSchedule = { showNewScheduleDialog = true },
                    onToggleSchedule = onToggleSchedule,
                    onDeleteSchedule = onDeleteSchedule,
                )
                TaskBoardTab.SKILLS -> SkillsContent(
                    skills = skills,
                    onToggleSkill = onToggleSkill,
                    onAddNewSkill = { showNewSkillDialog = true },
                )
                TaskBoardTab.REVIEWS -> DiffReviewsContent(
                    sessions = diffReviews,
                    onToggleResolved = onToggleAnnotationResolved,
                    onRunAutomatedReview = { onRunAutomatedReview("main") },
                )
                TaskBoardTab.LINEAR -> LinearIssuesContent(
                    issues = linearIssues,
                    apiKey = linearApiKey,
                    loading = linearLoading,
                    onSaveApiKey = onSaveLinearApiKey,
                    onRefresh = onRefreshLinearIssues,
                    onImportIssue = onImportLinearIssue,
                )
                TaskBoardTab.AUDIT -> AuditLogContent(auditLogs = auditLogs)
            }
        }
    }

    // New Task Dialog
    if (showNewTaskDialog) {
        CreateTaskDialog(
            agents = agents,
            existingTasks = tasks,
            onDismiss = { showNewTaskDialog = false },
            onCreate = { title, desc, prio, agentId, deps ->
                onCreateTask(title, desc, prio, agentId, deps)
                showNewTaskDialog = false
            },
        )
    }

    // Add Blackboard Entry Dialog
    if (showAddFactDialog) {
        AddBlackboardDialog(
            onDismiss = { showAddFactDialog = false },
            onAdd = { key, cat, value ->
                onAddBlackboardEntry(key, cat, value)
                showAddFactDialog = false
            },
        )
    }

    // Agent Inbox Dialog
    selectedAgentForInbox?.let { agent ->
        AgentInboxDialog(
            agent = agent,
            messages = inboxMessages.filter { it.toAgentId == agent.id || it.fromAgentId == agent.id },
            allAgents = agents,
            onDismiss = { selectedAgentForInbox = null },
            onSendMessage = { toId, subj, body ->
                onSendDirectMessage(agent.id, toId, body)
            },
        )
    }

    // Task Detail / Activity Dialog
    selectedTaskForDetail?.let { task ->
        TaskDetailDialog(
            task = task,
            agents = agents,
            onDismiss = { selectedTaskForDetail = null },
            onMoveStatus = { newStatus ->
                onMoveTaskStatus(task.id, newStatus)
                selectedTaskForDetail = null
            },
            onAssign = { agentId ->
                onAssignTask(task.id, agentId)
                selectedTaskForDetail = null
            },
        )
    }

    // Worktree Review & Merge Dialog
    selectedTaskForWorktreeReview?.let { task ->
        WorktreeMergeDialog(
            task = task,
            onDismiss = { selectedTaskForWorktreeReview = null },
            onMerge = {
                task.worktreeBranch?.let { onMergeWorktree(it) }
                selectedTaskForWorktreeReview = null
            },
            onDiscard = {
                task.worktreeBranch?.let { onRemoveWorktree(it) }
                selectedTaskForWorktreeReview = null
            },
        )
    }

    // Create Schedule Dialog
    if (showNewScheduleDialog) {
        CreateScheduleDialog(
            agents = agents,
            onDismiss = { showNewScheduleDialog = false },
            onCreate = { title, desc, interval, isRec, agentId ->
                onCreateSchedule(title, desc, interval, isRec, agentId)
                showNewScheduleDialog = false
            },
        )
    }

    // Create Custom Skill Dialog
    if (showNewSkillDialog) {
        CreateSkillDialog(
            onDismiss = { showNewSkillDialog = false },
            onCreate = { name, desc, cat, instructions ->
                onCreateSkill(name, desc, cat, instructions)
                showNewSkillDialog = false
            },
        )
    }
}

@Composable
private fun KanbanBoardContent(
    tasks: List<AgentTask>,
    agents: List<AgentInstance>,
    onMoveStatus: (String, TaskStatus) -> Unit,
    onSelectTask: (AgentTask) -> Unit,
    onAddNewTask: () -> Unit,
    onReviewWorktree: (AgentTask) -> Unit = {},
) {
    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) NeoDarkBorder else NeoBlack

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("SWARM KANBAN BOARD", fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Button(
                onClick = onAddNewTask,
                colors = ButtonDefaults.buttonColors(containerColor = NeoLime, contentColor = NeoBlack),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.5.dp, NeoBlack),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp),
            ) {
                Icon(Icons.Default.Add, null, Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("NEW TASK", fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
        HorizontalDivider(color = borderColor)

        // Horizontal columns
        LazyRow(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(TaskStatus.entries) { status ->
                val columnTasks = tasks.filter { it.status == status }
                KanbanColumnView(
                    status = status,
                    tasks = columnTasks,
                    agents = agents,
                    onMoveStatus = onMoveStatus,
                    onSelectTask = onSelectTask,
                    onReviewWorktree = onReviewWorktree,
                )
            }
        }
    }
}

@Composable
private fun KanbanColumnView(
    status: TaskStatus,
    tasks: List<AgentTask>,
    agents: List<AgentInstance>,
    onMoveStatus: (String, TaskStatus) -> Unit,
    onSelectTask: (AgentTask) -> Unit,
    onReviewWorktree: (AgentTask) -> Unit = {},
) {
    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) NeoDarkBorder else NeoBlack

    val headerColor = when (status) {
        TaskStatus.BACKLOG -> MaterialTheme.colorScheme.surfaceVariant
        TaskStatus.TODO -> Color(0xFF64B5F6)
        TaskStatus.IN_PROGRESS -> NeoLime
        TaskStatus.IN_REVIEW -> Color(0xFFFFD54F)
        TaskStatus.DONE -> Color(0xFF81C784)
        TaskStatus.BLOCKED -> Color(0xFFE57373)
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(2.dp, borderColor),
        modifier = Modifier
            .width(260.dp)
            .fillMaxSize(),
    ) {
        Column(Modifier.fillMaxSize()) {
            // Column Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerColor)
                    .border(BorderStroke(1.dp, NeoBlack))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    status.label.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = NeoBlack,
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(NeoBlack)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text("${tasks.size}", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }

            // Task cards list
            if (tasks.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("No tasks", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(tasks, key = { it.id }) { task ->
                        val assignedAgent = agents.firstOrNull { it.id == task.assignedAgentId }
                        TaskCardView(
                            task = task,
                            assignedAgent = assignedAgent,
                            onMoveLeft = {
                                val prev = getPreviousStatus(task.status)
                                if (prev != null) onMoveStatus(task.id, prev)
                            },
                            onMoveRight = {
                                val next = getNextStatus(task.status)
                                if (next != null) onMoveStatus(task.id, next)
                            },
                            onClick = { onSelectTask(task) },
                            onReviewWorktree = { onReviewWorktree(task) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskCardView(
    task: AgentTask,
    assignedAgent: AgentInstance?,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onClick: () -> Unit,
    onReviewWorktree: () -> Unit = {},
) {
    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) NeoDarkBorder else NeoBlack

    val priorityColor = when (task.priority) {
        TaskPriority.CRITICAL -> Color(0xFFFF5252)
        TaskPriority.HIGH -> Color(0xFFFF9100)
        TaskPriority.MEDIUM -> Color(0xFFFFD600)
        TaskPriority.LOW -> Color(0xFF00E676)
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.5.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Priority & Agent row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Priority pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(priorityColor)
                        .border(1.dp, NeoBlack, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                ) {
                    Text(task.priority.label, fontSize = 8.sp, fontWeight = FontWeight.Black, color = NeoBlack)
                }

                // Assigned agent avatar
                if (assignedAgent != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(assignedAgent.avatarEmoji, fontSize = 11.sp)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            assignedAgent.name.split(" ").first(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            // Title
            Text(
                task.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            // Worktree tag if present with MERGE action
            if (task.worktreeBranch != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(NeoLime.copy(alpha = 0.15f))
                        .border(1.dp, NeoLime, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "🌿 ${task.worktreeBranch}",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) NeoLime else NeoBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeoLime)
                            .border(1.dp, NeoBlack, RoundedCornerShape(4.dp))
                            .clickable { onReviewWorktree() }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text("MERGE", fontSize = 8.sp, fontWeight = FontWeight.Black, color = NeoBlack)
                    }
                }
            }

            // Quick Status Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val hasPrev = getPreviousStatus(task.status) != null
                val hasNext = getNextStatus(task.status) != null

                if (hasPrev) {
                    IconButton(onClick = onMoveLeft, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Move back", Modifier.size(14.dp))
                    }
                } else {
                    Spacer(Modifier.size(24.dp))
                }

                Text(
                    "${task.activityLog.size} activities",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (hasNext) {
                    IconButton(onClick = onMoveRight, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, "Move forward", Modifier.size(14.dp), tint = NeoLime)
                    }
                } else {
                    Spacer(Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun AgentSwarmContent(
    agents: List<AgentInstance>,
    tasks: List<AgentTask>,
    onOpenInbox: (AgentInstance) -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) NeoDarkBorder else NeoBlack

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(agents, key = { it.id }) { agent ->
            val assignedCount = tasks.count { it.assignedAgentId == agent.id }
            val activeTask = tasks.firstOrNull { it.id == agent.currentTaskId }

            val statusColor = when (agent.status) {
                AgentStatus.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
                AgentStatus.THINKING -> Color(0xFF00E5FF)
                AgentStatus.WORKING -> NeoLime
                AgentStatus.WAITING_FOR_INPUT -> Color(0xFFFFD600)
                AgentStatus.BLOCKED -> Color(0xFFFF5252)
                AgentStatus.COMPLETED -> Color(0xFF00E676)
                AgentStatus.FAILED -> Color(0xFFFF1744)
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(2.dp, borderColor),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Agent header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.5.dp, borderColor, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(agent.avatarEmoji, fontSize = 20.sp)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(agent.name, fontSize = 14.sp, fontWeight = FontWeight.Black)
                                Text(agent.role.title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Operational Status Pill
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(statusColor.copy(alpha = 0.2f))
                                .border(1.dp, statusColor, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Box(Modifier.size(7.dp).background(statusColor, CircleShape))
                            Text(
                                agent.status.label.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDark) statusColor else NeoBlack,
                            )
                        }
                    }

                    // Active task info
                    if (activeTask != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(6.dp))
                                .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.PlayArrow, null, Modifier.size(14.dp), tint = NeoLime)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Active Task: ${activeTask.title}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    // Token quota bar
                    val quotaPercent = (agent.tokensUsed.toFloat() / agent.quotaLimit.coerceAtLeast(1L)).coerceIn(0f, 1f)
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Usage Quota", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${agent.tokensUsed} / ${agent.quotaLimit} tokens", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        LinearProgressIndicator(
                            progress = { quotaPercent },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = if (quotaPercent > 0.85f) Color(0xFFFF5252) else NeoLime,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }

                    // Actions row: Inbox button & role details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("$assignedCount assigned tasks", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(
                            onClick = { onOpenInbox(agent) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, borderColor),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp),
                        ) {
                            Icon(Icons.Default.Email, null, Modifier.size(13.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("AGENT INBOX", fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlackboardContent(
    entries: List<BlackboardEntry>,
    onAddFact: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) NeoDarkBorder else NeoBlack
    var selectedCategory by remember { mutableStateOf("ALL") }

    val categories = remember(entries) {
        listOf("ALL") + entries.map { it.category }.distinct()
    }
    val filtered = if (selectedCategory == "ALL") entries else entries.filter { it.category == selectedCategory }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("SHARED PROJECT BLACKBOARD", fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Button(
                onClick = onAddFact,
                colors = ButtonDefaults.buttonColors(containerColor = NeoLime, contentColor = NeoBlack),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.5.dp, NeoBlack),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp),
            ) {
                Icon(Icons.Default.Add, null, Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("ADD FACT", fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
        HorizontalDivider(color = borderColor)

        // Categories chip row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(categories) { cat ->
                val isSelected = cat == selectedCategory
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) NeoLime else MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, if (isSelected) NeoBlack else borderColor, RoundedCornerShape(6.dp))
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        cat,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isSelected) NeoBlack else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No blackboard facts posted yet", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filtered, key = { it.id }) { entry ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.5.dp, borderColor),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(NeoLime)
                                        .border(1.dp, NeoBlack, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp),
                                ) {
                                    Text(entry.category, fontSize = 9.sp, fontWeight = FontWeight.Black, color = NeoBlack)
                                }
                                Text("key: ${entry.key}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(entry.value, fontSize = 12.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Author: ${entry.authorRole.title}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Confidence: ${(entry.confidence * 100).toInt()}%", fontSize = 10.sp, color = NeoLime, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditLogContent(auditLogs: List<SwarmAuditEvent>) {
    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) NeoDarkBorder else NeoBlack
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("SWARM AUDIT TRAIL", fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
        HorizontalDivider(color = borderColor)

        if (auditLogs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No swarm audit events recorded yet", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(auditLogs, key = { it.id }) { event ->
                    val typeBadgeColor = when (event.type) {
                        "CIRCUIT_BREAKER" -> Color(0xFFFF5252)
                        "DECOMPOSITION" -> NeoLime
                        "HANDOFF" -> Color(0xFF00E5FF)
                        "DELEGATION" -> Color(0xFFFFD600)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(typeBadgeColor)
                                .border(1.dp, NeoBlack, RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp),
                        ) {
                            Text(event.type, fontSize = 8.sp, fontWeight = FontWeight.Black, color = NeoBlack)
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(event.agentName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(6.dp))
                                Text(event.summary, fontSize = 11.sp)
                            }
                            if (event.details.isNotBlank()) {
                                Text(
                                    event.details,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                        }
                        Text(
                            timeFormat.format(Date(event.timestamp)),
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
        }
    }
}

// Dialogs

@Composable
private fun CreateTaskDialog(
    agents: List<AgentInstance>,
    existingTasks: List<AgentTask>,
    onDismiss: () -> Unit,
    onCreate: (String, String, TaskPriority, String?, List<String>) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var selectedAgentId by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Swarm Task", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description & Instructions") },
                    modifier = Modifier.fillMaxWidth(),
                )
                // Priority selector
                Text("Priority", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TaskPriority.entries.forEach { prio ->
                        val isSelected = prio == priority
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) NeoLime else MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, NeoBlack, RoundedCornerShape(6.dp))
                                .clickable { priority = prio }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(prio.label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) NeoBlack else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                // Assign agent
                Text("Assign Agent", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(agents) { ag ->
                        val isSelected = ag.id == selectedAgentId
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) NeoLime else MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, NeoBlack, RoundedCornerShape(6.dp))
                                .clickable { selectedAgentId = if (isSelected) null else ag.id }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text("${ag.avatarEmoji} ${ag.name.split(" ").first()}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) NeoBlack else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onCreate(title, description, priority, selectedAgentId, emptyList())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeoLime, contentColor = NeoBlack),
            ) {
                Text("CREATE TASK", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun AddBlackboardDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String) -> Unit,
) {
    var key by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("ARCHITECTURE") }
    var value by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Shared Blackboard Fact", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("Fact Key (e.g. db_engine)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it.uppercase() },
                    label = { Text("Category (e.g. ARCHITECTURE, ENV, DECISION)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Fact Value / Details") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (key.isNotBlank() && value.isNotBlank()) {
                        onAdd(key, category, value)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeoLime, contentColor = NeoBlack),
            ) {
                Text("POST FACT", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun AgentInboxDialog(
    agent: AgentInstance,
    messages: List<AgentInboxMessage>,
    allAgents: List<AgentInstance>,
    onDismiss: () -> Unit,
    onSendMessage: (toAgentId: String, subject: String, body: String) -> Unit,
) {
    var composeSubject by remember { mutableStateOf("") }
    var composeBody by remember { mutableStateOf("") }
    var targetAgentId by remember { mutableStateOf(allAgents.firstOrNull { it.id != agent.id }?.id ?: "god-master") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(agent.avatarEmoji, fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text("${agent.name}'s Inbox", fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("MESSAGE STREAM (${messages.size})", fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeoLime)
                if (messages.isEmpty()) {
                    Text("No inbox messages yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    messages.forEach { msg ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(msg.type.label, fontSize = 9.sp, fontWeight = FontWeight.Black, color = NeoLime)
                                    Text(if (msg.fromAgentId == agent.id) "OUTGOING" else "INCOMING", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(msg.subject, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(msg.content, fontSize = 11.sp)
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text("SEND DIRECT MESSAGE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeoLime)
                OutlinedTextField(
                    value = composeSubject,
                    onValueChange = { composeSubject = it },
                    label = { Text("Subject") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = composeBody,
                    onValueChange = { composeBody = it },
                    label = { Text("Message Body") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        if (composeBody.isNotBlank()) {
                            onSendMessage(targetAgentId, composeSubject.ifBlank { "Direct message" }, composeBody)
                            composeBody = ""
                            composeSubject = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeoLime, contentColor = NeoBlack),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Send, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("DISPATCH MESSAGE", fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun TaskDetailDialog(
    task: AgentTask,
    agents: List<AgentInstance>,
    onDismiss: () -> Unit,
    onMoveStatus: (TaskStatus) -> Unit,
    onAssign: (String?) -> Unit,
) {
    val assignedAgent = agents.firstOrNull { it.id == task.assignedAgentId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(task.title, fontWeight = FontWeight.Black, fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (task.description.isNotBlank()) {
                    Text(task.description, fontSize = 13.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Priority: ${task.priority.label}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Status: ${task.status.label}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeoLime)
                }
                if (assignedAgent != null) {
                    Text("Assigned to: ${assignedAgent.name} (${assignedAgent.role.title})", fontSize = 11.sp)
                }
                if (task.worktreeBranch != null) {
                    Text("Worktree Branch: ${task.worktreeBranch}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                HorizontalDivider()
                Text("MOVE STATUS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeoLime)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(TaskStatus.entries) { st ->
                        val isCurrent = st == task.status
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isCurrent) NeoLime else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onMoveStatus(st) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(st.label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isCurrent) NeoBlack else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                HorizontalDivider()
                Text("ACTIVITY TRAIL", fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeoLime)
                task.activityLog.forEach { act ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                            .padding(6.dp),
                    ) {
                        Text("${act.author}: ${act.action}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        if (act.detail.isNotBlank()) {
                            Text(act.detail, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}

private fun getPreviousStatus(status: TaskStatus): TaskStatus? = when (status) {
    TaskStatus.BACKLOG -> null
    TaskStatus.TODO -> TaskStatus.BACKLOG
    TaskStatus.IN_PROGRESS -> TaskStatus.TODO
    TaskStatus.IN_REVIEW -> TaskStatus.IN_PROGRESS
    TaskStatus.DONE -> TaskStatus.IN_REVIEW
    TaskStatus.BLOCKED -> TaskStatus.IN_PROGRESS
}

private fun getNextStatus(status: TaskStatus): TaskStatus? = when (status) {
    TaskStatus.BACKLOG -> TaskStatus.TODO
    TaskStatus.TODO -> TaskStatus.IN_PROGRESS
    TaskStatus.IN_PROGRESS -> TaskStatus.IN_REVIEW
    TaskStatus.IN_REVIEW -> TaskStatus.DONE
    TaskStatus.DONE -> null
    TaskStatus.BLOCKED -> TaskStatus.IN_PROGRESS
}

@Composable
private fun WorktreeMergeDialog(
    task: AgentTask,
    onDismiss: () -> Unit,
    onMerge: () -> Unit,
    onDiscard: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) NeoDarkBorder else NeoBlack

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🌿", fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text("Worktree Comparison & Merge", fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, borderColor),
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(task.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            "Branch: ${task.worktreeBranch}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = NeoLime,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Directory: .worktrees/${task.worktreeBranch}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Text(
                    "This agent completed their isolated task in a dedicated Git worktree branch. " +
                    "Reviewing will compare changes against your primary branch and merge cleanly.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Surface(
                    color = NeoLime.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, NeoLime),
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Security, null, tint = NeoLime, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Isolated execution guarantees zero branch pollution until explicitly merged.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onMerge,
                colors = ButtonDefaults.buttonColors(containerColor = NeoLime, contentColor = NeoBlack),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.5.dp, NeoBlack),
            ) {
                Icon(Icons.Default.CheckCircle, null, Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("MERGE INTO MAIN", fontWeight = FontWeight.Black, fontSize = 11.sp)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onDiscard) {
                    Text("Discard Worktree", color = Color(0xFFFF5252), fontSize = 11.sp)
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", fontSize = 11.sp)
                }
            }
        },
    )
}

@Composable
private fun SchedulerContent(
    scheduledTasks: List<ScheduledTask>,
    agents: List<AgentInstance>,
    onAddNewSchedule: () -> Unit,
    onToggleSchedule: (String) -> Unit,
    onDeleteSchedule: (String) -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) NeoDarkBorder else NeoBlack
    val timeFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("AGENT SCHEDULER", fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Text(
                    "Automated one-time & recurring agent jobs",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onAddNewSchedule,
                colors = ButtonDefaults.buttonColors(containerColor = NeoLime, contentColor = NeoBlack),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.5.dp, NeoBlack),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp),
            ) {
                Icon(Icons.Default.Add, null, Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("NEW SCHEDULE", fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
        HorizontalDivider(color = borderColor)

        if (scheduledTasks.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("⏰", fontSize = 36.sp)
                    Text(
                        "No Scheduled Tasks Yet",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "Schedule recurring test runs, security scans, or one-off background maintenance tasks.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(scheduledTasks, key = { it.id }) { item ->
                    val targetAgent = agents.firstOrNull { it.id == item.targetAgentId }

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.5.dp, borderColor),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (item.isRecurring) NeoLime else Color(0xFF64B5F6))
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            if (item.isRecurring) "RECURRING (${item.intervalMinutes}m)" else "ONE-TIME",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = NeoBlack,
                                        )
                                    }
                                    if (targetAgent != null) {
                                        Text("${targetAgent.avatarEmoji} ${targetAgent.name}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onToggleSchedule(item.id) },
                                        modifier = Modifier.size(28.dp),
                                    ) {
                                        Icon(
                                            if (item.enabled) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (item.enabled) "Pause" else "Resume",
                                            tint = if (item.enabled) NeoLime else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteSchedule(item.id) },
                                        modifier = Modifier.size(28.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color(0xFFFF5252),
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }

                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                            if (item.description.isNotBlank()) {
                                Text(
                                    item.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val nextRunText = if (item.nextRunMillis <= System.currentTimeMillis()) {
                                    "Ready to execute"
                                } else {
                                    "Next: ${timeFormat.format(Date(item.nextRunMillis))}"
                                }
                                Text(
                                    nextRunText,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (item.enabled) NeoLime else MaterialTheme.colorScheme.onSurfaceVariant,
                                )

                                val statusLabel = if (item.enabled) "ACTIVE" else "PAUSED"
                                Text(
                                    statusLabel,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (item.enabled) NeoLime else MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun CreateScheduleDialog(
    agents: List<AgentInstance>,
    onDismiss: () -> Unit,
    onCreate: (title: String, desc: String, interval: Int, isRecurring: Boolean, agentId: String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var intervalMinutes by remember { mutableStateOf("30") }
    var isRecurring by remember { mutableStateOf(false) }
    var selectedAgentId by remember { mutableStateOf(agents.firstOrNull()?.id ?: "god-master") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Create Scheduled Task", fontWeight = FontWeight.Black, fontSize = 16.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *") },
                    placeholder = { Text("E.g. Run test suite every hour") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Instructions / Prompt") },
                    placeholder = { Text("Detailed prompt for the agent...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )

                Text("ASSIGN TO AGENT", fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeoLime)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(agents) { agent ->
                        val isSelected = agent.id == selectedAgentId
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) NeoLime else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedAgentId = agent.id }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                        ) {
                            Text(
                                "${agent.avatarEmoji} ${agent.name}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) NeoBlack else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Recurring Schedule", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Switch(
                        checked = isRecurring,
                        onCheckedChange = { isRecurring = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeoBlack,
                            checkedTrackColor = NeoLime,
                        ),
                    )
                }

                if (isRecurring) {
                    OutlinedTextField(
                        value = intervalMinutes,
                        onValueChange = { intervalMinutes = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Interval (minutes)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val interval = intervalMinutes.toIntOrNull() ?: 30
                        onCreate(title.trim(), description.trim(), interval, isRecurring, selectedAgentId)
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NeoLime, contentColor = NeoBlack),
            ) {
                Text("CREATE SCHEDULE", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun SkillsContent(
    skills: List<com.jarves.mh.skills.SkillDefinition>,
    onToggleSkill: (String) -> Unit,
    onAddNewSkill: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) NeoDarkBorder else NeoBlack

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text("SWARM SKILL REGISTRY", fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Text(
                    "Dynamic capabilities & MCP tools equipped by agents",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onAddNewSkill,
                colors = ButtonDefaults.buttonColors(containerColor = NeoLime, contentColor = NeoBlack),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.5.dp, NeoBlack),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp),
            ) {
                Icon(Icons.Default.Add, null, Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("NEW SKILL", fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
        HorizontalDivider(color = borderColor)

        if (skills.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No skills registered", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(skills, key = { it.id }) { skill ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.5.dp, borderColor),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(skill.category.iconEmoji, fontSize = 16.sp)
                                    Column {
                                        Text(skill.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(
                                            skill.category.label.uppercase(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = NeoLime,
                                        )
                                    }
                                }
                                Switch(
                                    checked = skill.enabled,
                                    onCheckedChange = { onToggleSkill(skill.id) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = NeoBlack,
                                        checkedTrackColor = NeoLime,
                                    ),
                                )
                            }

                            if (skill.description.isNotBlank()) {
                                Text(
                                    skill.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            // Instructions box
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    skill.instructions,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(8.dp),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            // Required tools pills & author
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    items(skill.requiredTools) { tool ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 5.dp, vertical = 2.dp),
                                        ) {
                                            Text(tool, fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }

                                Text(
                                    skill.author,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun DiffReviewsContent(
    sessions: List<com.jarves.mh.review.DiffReviewSession>,
    onToggleResolved: (String) -> Unit,
    onRunAutomatedReview: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) NeoDarkBorder else NeoBlack

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text("DIFF REVIEWS & ANNOTATIONS", fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Text(
                    "Automated analysis by Rhea Reviewer + human feedback",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onRunAutomatedReview,
                colors = ButtonDefaults.buttonColors(containerColor = NeoLime, contentColor = NeoBlack),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.5.dp, NeoBlack),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp),
            ) {
                Icon(Icons.Default.AutoAwesome, null, Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("RUN REVIEW", fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
        HorizontalDivider(color = borderColor)

        if (sessions.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🔍", fontSize = 36.sp)
                    Text("No Code Reviews Yet", fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Text(
                        "Click 'RUN REVIEW' to have Rhea Reviewer audit diffs for security risks, exceptions, and code quality.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(sessions, key = { it.id }) { sess ->
                    val statusColor = when (sess.status) {
                        com.jarves.mh.review.ReviewStatus.APPROVED -> Color(0xFF00E676)
                        com.jarves.mh.review.ReviewStatus.CHANGES_REQUESTED -> Color(0xFFFF5252)
                        com.jarves.mh.review.ReviewStatus.PENDING -> Color(0xFFFFD600)
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.5.dp, borderColor),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("🌿", fontSize = 14.sp)
                                    Text(
                                        "${sess.worktreeBranch} -> ${sess.targetBranch}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(statusColor.copy(alpha = 0.2f))
                                        .border(1.dp, statusColor, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Text(sess.status.label.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Black, color = if (isDark) statusColor else NeoBlack)
                                }
                            }

                            if (sess.summary.isNotBlank()) {
                                Text(sess.summary, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            HorizontalDivider(color = borderColor.copy(alpha = 0.5f))

                            Text("FINDINGS & INLINE ANNOTATIONS (${sess.annotations.size})", fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeoLime)

                            sess.annotations.forEach { ann ->
                                val badgeColor = Color(ann.type.badgeColor)
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, if (ann.resolved) borderColor.copy(alpha = 0.3f) else badgeColor),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(ann.authorAvatar, fontSize = 12.sp)
                                                Text(
                                                    "${ann.filePath}:${ann.lineNumber}",
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(badgeColor)
                                                        .padding(horizontal = 5.dp, vertical = 1.dp),
                                                ) {
                                                    Text(ann.type.label, fontSize = 8.sp, fontWeight = FontWeight.Black, color = NeoBlack)
                                                }
                                                Spacer(Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(if (ann.resolved) NeoLime else MaterialTheme.colorScheme.background)
                                                        .border(1.dp, NeoBlack, RoundedCornerShape(4.dp))
                                                        .clickable { onToggleResolved(ann.id) }
                                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                                ) {
                                                    Text(
                                                        if (ann.resolved) "RESOLVED ✓" else "RESOLVE",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = if (ann.resolved) NeoBlack else MaterialTheme.colorScheme.onSurface,
                                                    )
                                                }
                                            }
                                        }

                                        Text(ann.comment, fontSize = 11.sp)

                                        if (!ann.suggestedPatch.isNullOrBlank()) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.background,
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.fillMaxWidth(),
                                            ) {
                                                Text(
                                                    ann.suggestedPatch,
                                                    fontSize = 9.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = NeoLime,
                                                    modifier = Modifier.padding(6.dp),
                                                )
                                            }
                                        }
                                    }
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
private fun CreateSkillDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, desc: String, category: com.jarves.mh.skills.SkillCategory, instructions: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(com.jarves.mh.skills.SkillCategory.DEBUGGING) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Create Custom Skill", fontWeight = FontWeight.Black, fontSize = 16.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Skill Name *") },
                    placeholder = { Text("E.g. GraphQL Query Validator") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text("CATEGORY", fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeoLime)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(com.jarves.mh.skills.SkillCategory.entries) { cat ->
                        val isSelected = cat == selectedCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) NeoLime else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                "${cat.iconEmoji} ${cat.label}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) NeoBlack else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("Summary of capability...") },
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("System Instructions / Guidelines *") },
                    placeholder = { Text("Specific instructions for agents when applying this skill...") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && instructions.isNotBlank()) {
                        onCreate(name.trim(), description.trim(), selectedCategory, instructions.trim())
                    }
                },
                enabled = name.isNotBlank() && instructions.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NeoLime, contentColor = NeoBlack),
            ) {
                Text("ADD SKILL", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun LinearIssuesContent(
    issues: List<LinearIssue>,
    apiKey: String?,
    loading: Boolean,
    onSaveApiKey: (String) -> Unit,
    onRefresh: () -> Unit,
    onImportIssue: (LinearIssue) -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) NeoDarkBorder else NeoBlack
    var showApiKeyDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, borderColor)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (!apiKey.isNullOrBlank()) NeoLime else Color(0xFFFF5252)),
                        )
                        Text(
                            text = if (!apiKey.isNullOrBlank()) "LINEAR CONNECTED" else "LINEAR NOT CONNECTED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        text = if (!apiKey.isNullOrBlank()) "${issues.size} assigned issues available" else "Connect API key to import backlog",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onRefresh, enabled = !apiKey.isNullOrBlank() && !loading) {
                        if (loading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = NeoLime)
                        } else {
                            Icon(Icons.Default.Refresh, "Refresh Linear issues", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Button(
                        onClick = { showApiKeyDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (apiKey.isNullOrBlank()) NeoLime else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (apiKey.isNullOrBlank()) NeoBlack else MaterialTheme.colorScheme.onSurface,
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.5.dp, NeoBlack),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp),
                    ) {
                        Text(if (apiKey.isNullOrBlank()) "CONFIGURE KEY" else "KEY SET", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        if (apiKey.isNullOrBlank()) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Bookmark, null, modifier = Modifier.size(48.dp), tint = NeoLime)
                    Text("Connect to Linear", fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text(
                        "Enter your Linear personal API token (lin_api_...) to sync and assign engineering issues straight into Devon Coder worktrees.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Button(
                        onClick = { showApiKeyDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NeoLime, contentColor = NeoBlack),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.5.dp, NeoBlack),
                    ) {
                        Text("ENTER LINEAR API KEY", fontWeight = FontWeight.Black)
                    }
                }
            }
        } else if (issues.isEmpty() && !loading) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No Active Issues Found", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("No uncompleted issues found in your Linear workspace.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onRefresh, colors = ButtonDefaults.buttonColors(containerColor = NeoLime, contentColor = NeoBlack)) {
                        Text("RETRY FETCH", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(issues, key = { it.id }) { issue ->
                    val priorityColor = when (issue.priority) {
                        1 -> Color(0xFFFF5252)
                        2 -> Color(0xFFFF9100)
                        3 -> Color(0xFFFFD600)
                        else -> Color(0xFF40C4FF)
                    }
                    val priorityLabel = when (issue.priority) {
                        1 -> "URGENT"
                        2 -> "HIGH"
                        3 -> "NORMAL"
                        4 -> "LOW"
                        else -> "NONE"
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.5.dp, borderColor),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(NeoLime)
                                            .border(1.dp, NeoBlack, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp),
                                    ) {
                                        Text(issue.identifier, fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeoBlack)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(priorityColor)
                                            .border(1.dp, NeoBlack, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp),
                                    ) {
                                        Text(priorityLabel, fontSize = 9.sp, fontWeight = FontWeight.Black, color = if (issue.priority == 1) Color.White else NeoBlack)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(horizontal = 5.dp, vertical = 2.dp),
                                    ) {
                                        Text(issue.stateName, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                if (issue.assigneeName != null) {
                                    Text(issue.assigneeName, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Text(
                                text = issue.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                            )

                            if (issue.description.isNotBlank()) {
                                Text(
                                    text = issue.description,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Branch: linear/${issue.identifier.lowercase()}",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )

                                Button(
                                    onClick = { onImportIssue(issue) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeoLime, contentColor = NeoBlack),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, NeoBlack),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp),
                                ) {
                                    Icon(Icons.Default.Add, null, Modifier.size(12.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("IMPORT TO SWARM", fontSize = 10.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showApiKeyDialog) {
        var tempKey by remember { mutableStateOf(apiKey.orEmpty()) }
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text("Linear API Key", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Generate a personal API token in Linear (Settings → Security & Access → Personal API Keys) and paste it below:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = tempKey,
                        onValueChange = { tempKey = it },
                        label = { Text("API Key") },
                        placeholder = { Text("lin_api_...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempKey.isNotBlank()) {
                            onSaveApiKey(tempKey.trim())
                            showApiKeyDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeoLime, contentColor = NeoBlack),
                ) {
                    Text("SAVE KEY", fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

