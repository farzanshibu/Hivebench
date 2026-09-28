package com.hivebench.app.ui

import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.verticalScroll

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Sync
import com.hivebench.app.ui.neo.AlertDialog
import com.hivebench.app.ui.neo.Checkbox
import com.hivebench.app.ui.neo.CheckboxDefaults
import com.hivebench.app.ui.neo.CircularProgressIndicator
import com.hivebench.app.ui.neo.OutlinedTextField
import com.hivebench.app.ui.neo.OutlinedTextFieldDefaults
import com.hivebench.app.ui.neo.ScrollableTabRow
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hivebench.app.model.DiffLine
import com.hivebench.app.model.DiffLineType
import com.hivebench.app.model.GitBranch
import com.hivebench.app.model.GitCommit
import com.hivebench.app.model.GitFileDiff
import com.hivebench.app.model.GitPullRequest
import com.hivebench.app.model.GitStatus
import com.hivebench.app.model.GitStatusFile
import com.hivebench.app.model.GitWorktree
import com.hivebench.app.ui.theme.NeoBadge
import com.hivebench.app.ui.theme.NeoBlack
import com.hivebench.app.ui.theme.NeoButton
import com.hivebench.app.ui.theme.NeoCard
import com.hivebench.app.ui.theme.NeoDarkBorder
import com.hivebench.app.ui.theme.NeoLime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class GitTab(val title: String) {
    CHANGES("Changes"),
    BRANCHES("Branches & Worktrees"),
    HISTORY("History"),
    PULL_REQUESTS("GitHub PRs"),
}

@Composable
fun GitScreen(
    status: GitStatus?,
    branches: List<GitBranch>,
    worktrees: List<GitWorktree>,
    commits: List<GitCommit>,
    stagedDiffs: List<GitFileDiff>,
    unstagedDiffs: List<GitFileDiff>,
    pullRequests: List<GitPullRequest>,
    selectedCommit: GitCommit?,
    selectedCommitDiffs: List<GitFileDiff>,
    isLoading: Boolean,
    operationMessage: String?,
    isGitHubConnected: Boolean,
    onRefresh: () -> Unit,
    onStageFile: (String) -> Unit,
    onUnstageFile: (String) -> Unit,
    onStageAll: () -> Unit,
    onUnstageAll: () -> Unit,
    onDiscardFile: (String) -> Unit,
    onCommit: (message: String, amend: Boolean) -> Unit,
    onCreateBranch: (name: String, checkout: Boolean) -> Unit,
    onCheckoutBranch: (name: String) -> Unit,
    onDeleteBranch: (name: String) -> Unit,
    onMergeBranch: (name: String) -> Unit,
    onCreateWorktree: (branch: String) -> Unit,
    onRemoveWorktree: (path: String) -> Unit,
    onPush: () -> Unit,
    onPull: () -> Unit,
    onSelectCommit: (GitCommit?) -> Unit,
    onCreatePullRequest: (title: String, body: String, base: String) -> Unit,
    onOpenGitHubSettings: () -> Unit,
    repositoryMissing: Boolean = false,
    onInitRepository: () -> Unit = {},
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showNewBranchDialog by remember { mutableStateOf(false) }
    var showBranchSwitcher by remember { mutableStateOf(false) }

    if (repositoryMissing) {
        Column(
            Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Default.Source, null, tint = NeoLime, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(12.dp))
            Text("Not a Git repository yet", fontWeight = FontWeight.Black, fontSize = 17.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "Initialize Git to track changes, commit, branch and push this project.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            NeoButton(onClick = onInitRepository, enabled = !isLoading, buttonColor = NeoLime, contentColor = NeoBlack) {
                Text(if (isLoading) "Initializing…" else "Initialize Git repository", fontWeight = FontWeight.Bold)
            }
            operationMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            }
        }
        return
    }
    var showNewWorktreeDialog by remember { mutableStateOf(false) }
    var showNewPrDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        // Git Header: Current branch, Ahead/Behind, Sync actions
        GitHeaderBar(
            status = status,
            isLoading = isLoading,
            operationMessage = operationMessage,
            onRefresh = onRefresh,
            onPush = onPush,
            onPull = onPull,
            onOpenBranchDialog = { showBranchSwitcher = true },
        )

        // Sub Navigation Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = NeoLime,
            edgePadding = 12.dp,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = NeoLime,
                        height = 3.dp,
                    )
                }
            },
            divider = { HorizontalDivider(thickness = 1.dp, color = NeoDarkBorder) },
        ) {
            GitTab.entries.forEachIndexed { index, tab ->
                val badgeCount = when (tab) {
                    GitTab.CHANGES -> status?.totalChangedFiles ?: 0
                    GitTab.BRANCHES -> branches.size
                    GitTab.HISTORY -> commits.size
                    GitTab.PULL_REQUESTS -> pullRequests.size
                }
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                tab.title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (selectedTab == index) NeoLime else MaterialTheme.colorScheme.onSurface,
                            )
                            if (badgeCount > 0) {
                                Spacer(Modifier.width(6.dp))
                                NeoBadge(
                                    text = badgeCount.toString(),
                                    containerColor = if (selectedTab == index) NeoLime else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (selectedTab == index) NeoBlack else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                )
            }
        }

        // Tab Content
        Box(Modifier.weight(1f)) {
            when (GitTab.entries.getOrElse(selectedTab) { GitTab.CHANGES }) {
                GitTab.CHANGES -> GitChangesTab(
                    status = status,
                    stagedDiffs = stagedDiffs,
                    unstagedDiffs = unstagedDiffs,
                    isLoading = isLoading,
                    onStageFile = onStageFile,
                    onUnstageFile = onUnstageFile,
                    onStageAll = onStageAll,
                    onUnstageAll = onUnstageAll,
                    onDiscardFile = onDiscardFile,
                    onCommit = onCommit,
                )
                GitTab.BRANCHES -> GitBranchesWorktreesTab(
                    currentBranch = status?.currentBranch ?: "main",
                    branches = branches,
                    worktrees = worktrees,
                    onCreateBranch = { showNewBranchDialog = true },
                    onCheckoutBranch = onCheckoutBranch,
                    onDeleteBranch = onDeleteBranch,
                    onMergeBranch = onMergeBranch,
                    onCreateWorktree = { showNewWorktreeDialog = true },
                    onRemoveWorktree = onRemoveWorktree,
                )
                GitTab.HISTORY -> GitHistoryTab(
                    commits = commits,
                    selectedCommit = selectedCommit,
                    commitDiffs = selectedCommitDiffs,
                    onSelectCommit = onSelectCommit,
                )
                GitTab.PULL_REQUESTS -> GitPullRequestsTab(
                    pullRequests = pullRequests,
                    currentBranch = status?.currentBranch ?: "main",
                    isGitHubConnected = isGitHubConnected,
                    onCreatePr = { showNewPrDialog = true },
                    onConnectGitHub = onOpenGitHubSettings,
                )
            }
        }
    }

    // Dialogs
    if (showBranchSwitcher) {
        AlertDialog(
            onDismissRequest = { showBranchSwitcher = false },
            title = { Text("Switch branch") },
            text = {
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    branches.filter { !it.isRemote }.forEach { branch ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (branch.isCurrent) NeoLime.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable(enabled = !branch.isCurrent) {
                                    onCheckoutBranch(branch.name)
                                    showBranchSwitcher = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(branch.name, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            if (branch.isCurrent) Text("current", fontSize = 11.sp, color = NeoLime)
                        }
                    }
                    if (branches.none { !it.isRemote }) Text("No local branches yet — commit first.", fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showBranchSwitcher = false; showNewBranchDialog = true }) { Text("New branch…") }
            },
            dismissButton = { TextButton(onClick = { showBranchSwitcher = false }) { Text("Close") } },
        )
    }

    if (showNewBranchDialog) {
        CreateBranchDialog(
            onDismiss = { showNewBranchDialog = false },
            onCreate = { name, checkout ->
                onCreateBranch(name, checkout)
                showNewBranchDialog = false
            },
        )
    }

    if (showNewWorktreeDialog) {
        CreateWorktreeDialog(
            branches = branches,
            onDismiss = { showNewWorktreeDialog = false },
            onCreate = { branch ->
                onCreateWorktree(branch)
                showNewWorktreeDialog = false
            },
        )
    }

    if (showNewPrDialog) {
        CreatePullRequestDialog(
            currentBranch = status?.currentBranch ?: "main",
            onDismiss = { showNewPrDialog = false },
            onCreate = { title, body, base ->
                onCreatePullRequest(title, body, base)
                showNewPrDialog = false
            },
        )
    }
}

@Composable
private fun GitHeaderBar(
    status: GitStatus?,
    isLoading: Boolean,
    operationMessage: String?,
    onRefresh: () -> Unit,
    onPush: () -> Unit,
    onPull: () -> Unit,
    onOpenBranchDialog: () -> Unit,
) {
    NeoCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        cornerRadius = 14.dp,
        shadowOffset = 2.5.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Branch badge (clickable to switch/create)
                Row(
                    Modifier.clickable { onOpenBranchDialog() }
                        .background(NeoLime.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .border(1.5.dp, NeoLime, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Source, null, tint = NeoLime, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        status?.currentBranch ?: "main",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                // Sync controls
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (status != null && (status.ahead > 0 || status.behind > 0)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (status.ahead > 0) {
                                Icon(Icons.Default.ArrowUpward, null, tint = NeoLime, modifier = Modifier.size(14.dp))
                                Text("${status.ahead}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeoLime)
                                Spacer(Modifier.width(4.dp))
                            }
                            if (status.behind > 0) {
                                Icon(Icons.Default.ArrowDownward, null, tint = Color(0xFFFF5252), modifier = Modifier.size(14.dp))
                                Text("${status.behind}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
                            }
                        }
                    }

                    // Pull
                    IconButton(onClick = onPull, enabled = !isLoading, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ArrowDownward, "Pull", tint = MaterialTheme.colorScheme.onSurface)
                    }

                    // Push
                    IconButton(onClick = onPush, enabled = !isLoading, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ArrowUpward, "Push", tint = NeoLime)
                    }

                    // Refresh
                    IconButton(onClick = onRefresh, enabled = !isLoading, modifier = Modifier.size(36.dp)) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = NeoLime)
                        } else {
                            Icon(Icons.Default.Refresh, "Refresh Git", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            if (!operationMessage.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(operationMessage, fontSize = 11.sp, color = NeoLime, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun GitChangesTab(
    status: GitStatus?,
    stagedDiffs: List<GitFileDiff>,
    unstagedDiffs: List<GitFileDiff>,
    isLoading: Boolean,
    onStageFile: (String) -> Unit,
    onUnstageFile: (String) -> Unit,
    onStageAll: () -> Unit,
    onUnstageAll: () -> Unit,
    onDiscardFile: (String) -> Unit,
    onCommit: (message: String, amend: Boolean) -> Unit,
) {
    var commitMessage by rememberSaveable { mutableStateOf("") }
    var amendCommit by rememberSaveable { mutableStateOf(false) }
    var expandedStagedPath by rememberSaveable { mutableStateOf<String?>(null) }
    var expandedUnstagedPath by rememberSaveable { mutableStateOf<String?>(null) }

    val stagedFiles = status?.stagedFiles.orEmpty()
    val unstagedFiles = (status?.unstagedFiles.orEmpty() + status?.untrackedFiles.orEmpty()).distinctBy { it.path }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Commit Box at the top for quick developer access
        item {
            NeoCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp, shadowOffset = 3.dp) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text("Commit Changes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = commitMessage,
                        onValueChange = { commitMessage = it },
                        modifier = Modifier.fillMaxWidth().height(90.dp),
                        placeholder = { Text("feat: describe your change…", fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeoLime,
                            unfocusedBorderColor = NeoDarkBorder,
                        ),
                        shape = RoundedCornerShape(10.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = amendCommit,
                                onCheckedChange = { amendCommit = it },
                                colors = CheckboxDefaults.colors(checkedColor = NeoLime, checkmarkColor = NeoBlack),
                            )
                            Text("Amend previous", fontSize = 12.sp)
                        }

                        NeoButton(
                            onClick = {
                                if (commitMessage.isNotBlank()) {
                                    onCommit(commitMessage, amendCommit)
                                    commitMessage = ""
                                    amendCommit = false
                                }
                            },
                            enabled = commitMessage.isNotBlank() && !isLoading,
                            containerColor = NeoLime,
                            contentColor = NeoBlack,
                        ) {
                            Icon(Icons.Default.Commit, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Commit (${stagedFiles.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Section 1: Staged Changes
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Staged Changes", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.width(8.dp))
                    NeoBadge(text = "${stagedFiles.size}", containerColor = NeoLime, contentColor = NeoBlack)
                }
                if (stagedFiles.isNotEmpty()) {
                    TextButton(onClick = onUnstageAll) {
                        Text("Unstage All", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            }
        }

        if (stagedFiles.isEmpty()) {
            item {
                NeoCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 10.dp) {
                    Text(
                        "No staged changes. Stage modified files below to include them in the commit.",
                        modifier = Modifier.padding(14.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(stagedFiles, key = { "staged_${it.path}" }) { file ->
                val diff = stagedDiffs.firstOrNull { it.newPath == file.path || it.oldPath == file.path }
                val expanded = expandedStagedPath == file.path
                GitFileItemCard(
                    path = file.path,
                    isStaged = true,
                    diff = diff,
                    expanded = expanded,
                    onToggleExpand = { expandedStagedPath = if (expanded) null else file.path },
                    onAction = { onUnstageFile(file.path) },
                    actionIcon = Icons.Default.Clear,
                    actionDescription = "Unstage file",
                )
            }
        }

        // Section 2: Unstaged & Untracked Changes
        item {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Changes & Untracked", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.width(8.dp))
                    NeoBadge(text = "${unstagedFiles.size}", containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                }
                if (unstagedFiles.isNotEmpty()) {
                    TextButton(onClick = onStageAll) {
                        Text("Stage All", color = NeoLime, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        if (unstagedFiles.isEmpty()) {
            item {
                NeoCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 10.dp) {
                    Text(
                        "Working tree clean. No modified or untracked files.",
                        modifier = Modifier.padding(14.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(unstagedFiles, key = { "unstaged_${it.path}" }) { file ->
                val diff = unstagedDiffs.firstOrNull { it.newPath == file.path || it.oldPath == file.path }
                val expanded = expandedUnstagedPath == file.path
                GitFileItemCard(
                    path = file.path,
                    isStaged = false,
                    diff = diff,
                    expanded = expanded,
                    onToggleExpand = { expandedUnstagedPath = if (expanded) null else file.path },
                    onAction = { onStageFile(file.path) },
                    actionIcon = Icons.Default.Add,
                    actionDescription = "Stage file",
                    onDiscard = { onDiscardFile(file.path) },
                )
            }
        }
    }
}

@Composable
private fun GitFileItemCard(
    path: String,
    isStaged: Boolean,
    diff: GitFileDiff?,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onAction: () -> Unit,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector,
    actionDescription: String,
    onDiscard: (() -> Unit)? = null,
) {
    NeoCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp, shadowOffset = 2.dp) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable { onToggleExpand() }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Description, null, tint = if (isStaged) NeoLime else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(path, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (expanded) "Tap to collapse diff" else "Tap to view git diff",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Additions / Deletions count
                if (diff != null && (diff.additions > 0 || diff.deletions > 0)) {
                    if (diff.additions > 0) {
                        NeoBadge(text = "+${diff.additions}", containerColor = NeoLime, contentColor = NeoBlack)
                        Spacer(Modifier.width(4.dp))
                    }
                    if (diff.deletions > 0) {
                        NeoBadge(text = "-${diff.deletions}", containerColor = Color(0xFFFF5252), contentColor = Color.White)
                        Spacer(Modifier.width(6.dp))
                    }
                }

                // Discard Button (for unstaged changes)
                if (onDiscard != null) {
                    IconButton(onClick = onDiscard, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, "Discard changes", tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                    }
                }

                // Stage / Unstage Action
                IconButton(onClick = onAction, modifier = Modifier.size(32.dp)) {
                    Icon(actionIcon, actionDescription, tint = if (isStaged) Color(0xFFFF5252) else NeoLime, modifier = Modifier.size(20.dp))
                }
            }

            // Expanded Diff
            if (expanded && diff != null) {
                HorizontalDivider(thickness = 1.dp, color = NeoDarkBorder)
                Column(
                    Modifier.fillMaxWidth().background(Color(0xFF090C10)).horizontalScroll(rememberScrollState()),
                ) {
                    if (diff.diffLines.isEmpty()) {
                        Text("No text diff available (binary or empty file)", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(10.dp))
                    } else {
                        diff.diffLines.forEach { line ->
                            DiffLineRow(line)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GitBranchesWorktreesTab(
    currentBranch: String,
    branches: List<GitBranch>,
    worktrees: List<GitWorktree>,
    onCreateBranch: () -> Unit,
    onCheckoutBranch: (String) -> Unit,
    onDeleteBranch: (String) -> Unit,
    onMergeBranch: (String) -> Unit,
    onCreateWorktree: () -> Unit,
    onRemoveWorktree: (String) -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Section: Branches
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Git Branches", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                NeoButton(
                    onClick = onCreateBranch,
                    containerColor = NeoLime,
                    contentColor = NeoBlack,
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New Branch", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        items(branches, key = { it.name }) { branch ->
            val isCurrent = branch.name == currentBranch || branch.isCurrent
            NeoCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 12.dp,
                borderColor = if (isCurrent) NeoLime else NeoDarkBorder,
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Source,
                        null,
                        tint = if (isCurrent) NeoLime else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(branch.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (isCurrent) {
                                Spacer(Modifier.width(6.dp))
                                NeoBadge(text = "CURRENT", containerColor = NeoLime, contentColor = NeoBlack)
                            }
                            if (branch.isRemote) {
                                Spacer(Modifier.width(6.dp))
                                NeoBadge(text = "REMOTE", containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (!branch.upstream.isNullOrBlank()) {
                            Text("Tracking: ${branch.upstream}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (!isCurrent) {
                        IconButton(onClick = { onCheckoutBranch(branch.name) }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, "Checkout", tint = NeoLime)
                        }
                        IconButton(onClick = { onMergeBranch(branch.name) }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.MergeType, "Merge into current", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = { onDeleteBranch(branch.name) }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.Delete, "Delete branch", tint = Color(0xFFFF5252))
                        }
                    }
                }
            }
        }

        // Section: Worktrees
        item {
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Isolated Worktrees", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Parallel isolated workspaces for AI agents", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                NeoButton(
                    onClick = onCreateWorktree,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Worktree", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (worktrees.isEmpty()) {
            item {
                NeoCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 10.dp) {
                    Text("No worktrees found. Add a worktree to run isolated multi-agent tasks.", modifier = Modifier.padding(14.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(worktrees, key = { it.path }) { wt ->
                val isMain = wt.path == "." || wt.path == ""
                NeoCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, null, tint = if (isMain) NeoLime else MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (isMain) "Primary Workspace" else wt.path, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                if (isMain) {
                                    Spacer(Modifier.width(6.dp))
                                    NeoBadge(text = "MAIN", containerColor = NeoLime, contentColor = NeoBlack)
                                }
                            }
                            Text(
                                "Branch: ${wt.branch ?: "detached"} (${wt.headCommit})",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (!isMain) {
                            IconButton(onClick = { onRemoveWorktree(wt.path) }) {
                                Icon(Icons.Default.Delete, "Remove worktree", tint = Color(0xFFFF5252))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GitHistoryTab(
    commits: List<GitCommit>,
    selectedCommit: GitCommit?,
    commitDiffs: List<GitFileDiff>,
    onSelectCommit: (GitCommit?) -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault()) }

    if (selectedCommit != null) {
        // Detailed Commit View with Diff
        LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                NeoCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            NeoBadge(text = selectedCommit.shortHash, containerColor = NeoLime, contentColor = NeoBlack)
                            TextButton(onClick = { onSelectCommit(null) }) {
                                Text("Back to History", fontSize = 12.sp, color = NeoLime)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(selectedCommit.message, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("Author: ${selectedCommit.authorName} <${selectedCommit.authorEmail}>", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Date: ${dateFormat.format(Date(selectedCommit.timestampMillis))}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                Text("Files Changed in this Commit (${commitDiffs.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            items(commitDiffs, key = { it.newPath }) { diff ->
                NeoCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 10.dp) {
                    Column {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(diff.newPath, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            if (diff.additions > 0) NeoBadge(text = "+${diff.additions}", containerColor = NeoLime, contentColor = NeoBlack)
                            Spacer(Modifier.width(4.dp))
                            if (diff.deletions > 0) NeoBadge(text = "-${diff.deletions}", containerColor = Color(0xFFFF5252), contentColor = Color.White)
                        }
                        HorizontalDivider(thickness = 1.dp, color = NeoDarkBorder)
                        Column(Modifier.fillMaxWidth().background(Color(0xFF090C10)).horizontalScroll(rememberScrollState())) {
                            diff.diffLines.forEach { DiffLineRow(it) }
                        }
                    }
                }
            }
        }
    } else {
        // Commits List
        LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (commits.isEmpty()) {
                item {
                    NeoCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 10.dp) {
                        Text("No commits in repository history yet.", modifier = Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(commits, key = { it.hash }) { commit ->
                    NeoCard(
                        modifier = Modifier.fillMaxWidth().clickable { onSelectCommit(commit) },
                        cornerRadius = 12.dp,
                    ) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Commit, null, tint = NeoLime, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(commit.shortHash, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = NeoLime, fontSize = 13.sp)
                                }
                                Text(
                                    dateFormat.format(Date(commit.timestampMillis)),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(commit.message, fontWeight = FontWeight.Medium, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(4.dp))
                            Text("By ${commit.authorName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GitPullRequestsTab(
    pullRequests: List<GitPullRequest>,
    currentBranch: String,
    isGitHubConnected: Boolean,
    onCreatePr: () -> Unit,
    onConnectGitHub: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    Column(Modifier.fillMaxSize().padding(14.dp)) {
        if (!isGitHubConnected) {
            NeoCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("GitHub Not Connected", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Tap Connect to sign in to GitHub, then list and create pull requests from your phone.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    NeoButton(
                        onClick = onConnectGitHub,
                        containerColor = NeoLime,
                        contentColor = NeoBlack,
                    ) {
                        Text("Connect GitHub Account", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Open Pull Requests", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                NeoButton(
                    onClick = onCreatePr,
                    containerColor = NeoLime,
                    contentColor = NeoBlack,
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New PR", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            if (pullRequests.isEmpty()) {
                NeoCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 10.dp) {
                    Text("No open pull requests for this repository.", modifier = Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(pullRequests, key = { it.number }) { pr ->
                        NeoCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
                            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        NeoBadge(text = "#${pr.number}", containerColor = NeoLime, contentColor = NeoBlack)
                                        Spacer(Modifier.width(8.dp))
                                        Text(pr.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    IconButton(
                                        onClick = { if (pr.htmlUrl.isNotBlank()) uriHandler.openUri(pr.htmlUrl) },
                                        modifier = Modifier.size(30.dp),
                                    ) {
                                        Icon(Icons.Default.OpenInBrowser, "Open on GitHub", tint = NeoLime)
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    NeoBadge(text = pr.headBranch, containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(12.dp).padding(horizontal = 2.dp))
                                    NeoBadge(text = pr.baseBranch, containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                                    Spacer(Modifier.width(8.dp))
                                    if (pr.author.isNotBlank()) {
                                        Text("by ${pr.author}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun CreateBranchDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, checkout: Boolean) -> Unit,
) {
    var branchName by remember { mutableStateOf("") }
    var checkout by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Branch", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = branchName,
                    onValueChange = { branchName = it.replace(" ", "-") },
                    label = { Text("Branch name") },
                    placeholder = { Text("feature-payment-gateway") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = checkout,
                        onCheckedChange = { checkout = it },
                        colors = CheckboxDefaults.colors(checkedColor = NeoLime, checkmarkColor = NeoBlack),
                    )
                    Text("Checkout after creating", fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            NeoButton(
                onClick = { if (branchName.isNotBlank()) onCreate(branchName.trim(), checkout) },
                enabled = branchName.isNotBlank(),
                containerColor = NeoLime,
                contentColor = NeoBlack,
            ) { Text("Create", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun CreateWorktreeDialog(
    branches: List<GitBranch>,
    onDismiss: () -> Unit,
    onCreate: (branch: String) -> Unit,
) {
    var branchName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Isolated Worktree", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Creates an isolated workspace folder under .worktrees/<branch> on a separate Git branch. Ideal for parallel agent tasks.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = branchName,
                    onValueChange = { branchName = it.replace(" ", "-") },
                    label = { Text("Branch name for worktree") },
                    placeholder = { Text("agent-security-fix") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            NeoButton(
                onClick = { if (branchName.isNotBlank()) onCreate(branchName.trim()) },
                enabled = branchName.isNotBlank(),
                containerColor = NeoLime,
                contentColor = NeoBlack,
            ) { Text("Add Worktree", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun CreatePullRequestDialog(
    currentBranch: String,
    onDismiss: () -> Unit,
    onCreate: (title: String, body: String, base: String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var baseBranch by remember { mutableStateOf("main") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create GitHub Pull Request", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Branch: $currentBranch → $baseBranch", fontSize = 12.sp, color = NeoLime, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = baseBranch,
                    onValueChange = { baseBranch = it },
                    label = { Text("Base branch") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth().height(90.dp),
                )
            }
        },
        confirmButton = {
            NeoButton(
                onClick = { if (title.isNotBlank()) onCreate(title.trim(), body.trim(), baseBranch.trim()) },
                enabled = title.isNotBlank(),
                containerColor = NeoLime,
                contentColor = NeoBlack,
            ) { Text("Create PR", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
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
