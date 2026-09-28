package com.hivebench.app.ui

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
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material.icons.filled.Warning
import com.hivebench.app.ui.neo.AlertDialog
import com.hivebench.app.ui.neo.Badge
import com.hivebench.app.ui.neo.Button
import com.hivebench.app.ui.neo.ButtonDefaults
import com.hivebench.app.ui.neo.CircularProgressIndicator
import com.hivebench.app.ui.neo.DropdownMenu
import com.hivebench.app.ui.neo.DropdownMenuItem
import com.hivebench.app.ui.neo.LinearProgressIndicator
import com.hivebench.app.ui.neo.OutlinedButton
import com.hivebench.app.ui.neo.OutlinedTextField
import com.hivebench.app.ui.neo.OutlinedTextFieldDefaults
import com.hivebench.app.ui.neo.ScrollableTabRow
import com.hivebench.app.ui.neo.Surface
import com.hivebench.app.ui.neo.Switch
import com.hivebench.app.ui.neo.SwitchDefaults
import com.hivebench.app.ui.neo.Tab
import com.hivebench.app.ui.neo.TabRowDefaults
import com.hivebench.app.ui.neo.tabIndicatorOffset
import com.hivebench.app.ui.neo.Text
import com.hivebench.app.ui.neo.TextButton
import com.hivebench.app.ui.neo.MaterialTheme
import com.hivebench.app.ui.neo.Icon
import com.hivebench.app.ui.neo.IconButton
import com.hivebench.app.ui.neo.HorizontalDivider
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
import com.hivebench.app.network.LinearIssue
import com.hivebench.app.network.GitHubIssue
import com.hivebench.app.ui.theme.NeoBlack
import com.hivebench.app.ui.theme.NeoDarkBorder
import com.hivebench.app.ui.theme.NeoLime
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Pulls Linear / GitHub issues into the Office's To-do column as cards. The
 * externalId format ("github:<url>", "linear:<identifier>") lets re-imports dedupe.
 */
@Composable
fun IssueImportSheet(state: AppUiState, viewModel: MainViewModel, onDismiss: () -> Unit) {
    var source by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("github") }
    com.hivebench.app.ui.neo.NeoBottomDrawer(
        visible = true,
        onDismiss = onDismiss,
        initialDetent = com.hivebench.app.ui.neo.DrawerDetent.Full,
        allowedDetents = listOf(com.hivebench.app.ui.neo.DrawerDetent.Full),
        title = "Import issues",
        subtitle = "Each becomes a card in To do; assign it or let ${state.hive.god?.name ?: "the orchestrator"} pick an owner",
    ) { _, _ ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.hivebench.app.ui.neo.NeoChip(text = "GitHub", selected = source == "github", onClick = { source = "github" })
            com.hivebench.app.ui.neo.NeoChip(text = "Linear", selected = source == "linear", onClick = { source = "linear" })
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().weight(1f)) {
            if (source == "github") {
                GitHubIssuesContent(
                    issues = state.githubIssues,
                    loading = state.githubIssuesLoading,
                    repoName = state.githubRepoOverride
                        ?: state.activeProject?.description?.takeIf { it.startsWith("GitHub · ") }?.removePrefix("GitHub · ")?.trim(),
                    onRefresh = viewModel::refreshGitHubIssues,
                    onImportIssue = { issue ->
                        viewModel.hive.importTask(
                            title = "#${issue.number} ${issue.title}",
                            description = "${issue.body}\n\nGitHub: ${issue.htmlUrl}".trim(),
                            source = "github",
                            externalId = "github:${issue.htmlUrl.ifBlank { issue.number.toString() }}",
                        )
                    },
                )
            } else {
                LinearIssuesContent(
                    issues = state.linearIssues,
                    apiKey = state.linearApiKey,
                    loading = state.linearLoading,
                    onSaveApiKey = viewModel::setLinearApiKey,
                    onRefresh = viewModel::refreshLinearIssues,
                    onImportIssue = { issue ->
                        viewModel.hive.importTask(
                            title = "${issue.identifier} ${issue.title}",
                            description = "${issue.description}\n\nLinear: ${issue.url}".trim(),
                            source = "linear",
                            externalId = "linear:${issue.identifier}",
                        )
                    },
                )
            }
        }
    }
}

@Composable
internal fun LinearIssuesContent(
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

@Composable
internal fun GitHubIssuesContent(
    issues: List<GitHubIssue>,
    loading: Boolean,
    repoName: String?,
    onRefresh: (String?) -> Unit,
    onImportIssue: (GitHubIssue) -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) NeoDarkBorder else NeoBlack
    var showRepoDialog by remember { mutableStateOf(false) }

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
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (!repoName.isNullOrBlank() || issues.isNotEmpty()) NeoLime else Color(0xFFFF9100)),
)
                        Text(
                            text = repoName?.ifBlank { "GITHUB REPO" } ?: "GITHUB REPO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
)
                    }
                    Text(
                        text = "${issues.size} issues available",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onRefresh(null) }, enabled = !loading) {
                        if (loading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = NeoLime)
                        } else {
                            Icon(Icons.Default.Refresh, "Refresh GitHub issues", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Button(
                        onClick = { showRepoDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface,
),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.5.dp, NeoBlack),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp),
) {
                        Text("CHANGE REPO", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        if (issues.isEmpty() && !loading) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Code, null, modifier = Modifier.size(48.dp), tint = NeoLime)
                    Text("No GitHub Issues Loaded", fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text(
"Connect your repository or specify owner/repo to sync open issues into Devon Coder worktrees.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onRefresh(null) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeoLime, contentColor = NeoBlack),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.5.dp, NeoBlack),
) {
                            Text("FETCH CURRENT REPO", fontWeight = FontWeight.Black)
                        }
                        Button(
                            onClick = { showRepoDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.5.dp, NeoBlack),
) {
                            Text("SPECIFY REPO", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
) {
                items(issues, key = { "${it.id}-${it.number}" }) { issue ->
                    val isClosed = issue.state.equals("closed", true)
                    val statusColor = if (isClosed) Color.Gray else NeoLime
                    val isUrgent = issue.labels.any { it.contains("urgent", true) || it.contains("critical", true) || it.contains("p0", true) }

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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f, fill = false),
) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(NeoLime)
                                            .border(1.dp, NeoBlack, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
) {
                                        Text("#${issue.number}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeoBlack)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(statusColor)
                                            .border(1.dp, NeoBlack, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp),
) {
                                        Text(issue.state.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Black, color = if (isClosed) Color.White else NeoBlack)
                                    }
                                    if (isUrgent) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFFF5252))
                                                .border(1.dp, NeoBlack, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 5.dp, vertical = 2.dp),
) {
                                            Text("CRITICAL", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White)
                                        }
                                    }
                                }

                                if (issue.author.isNotBlank()) {
                                    Text("@${issue.author}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Text(
                                text = issue.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
)

                            if (issue.labels.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
) {
                                    issue.labels.forEach { label ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    when {
                                                        label.contains("bug", true) -> Color(0xFFFF8A80)
                                                        label.contains("enhancement", true) || label.contains("feature", true) -> Color(0xFFB388FF)
                                                        label.contains("doc", true) -> Color(0xFF80D8FF)
                                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                                    }
)
                                                .border(1.dp, borderColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 5.dp, vertical = 2.dp),
) {
                                            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeoBlack)
                                        }
                                    }
                                }
                            }

                            if (issue.body.isNotBlank()) {
                                Text(
                                    text = issue.body,
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
                                    text = "Branch: issue/${issue.number}",
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

    if (showRepoDialog) {
        var tempRepo by remember { mutableStateOf(repoName.orEmpty()) }
        AlertDialog(
            onDismissRequest = { showRepoDialog = false },
            title = { Text("GitHub Repository", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
"Enter the GitHub repository in 'owner/repository' format (e.g. facebook/react or your user repo):",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
)
                    OutlinedTextField(
                        value = tempRepo,
                        onValueChange = { tempRepo = it },
                        label = { Text("Repository (owner/repo)") },
                        placeholder = { Text("owner/repo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempRepo.isNotBlank()) {
                            onRefresh(tempRepo.trim())
                            showRepoDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeoLime, contentColor = NeoBlack),
) {
                    Text("FETCH ISSUES", fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRepoDialog = false }) {
                    Text("Cancel")
                }
            },
)
    }
}
