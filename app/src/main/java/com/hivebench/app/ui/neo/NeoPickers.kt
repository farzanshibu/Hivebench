package com.hivebench.app.ui.neo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class NeoModelOption(
    val id: String,
    val displayName: String = id,
    val badge: String? = null,
)

data class NeoRepoOption(
    val fullName: String,
    val description: String? = null,
    val stars: Int? = null,
    val language: String? = null,
)

data class NeoHistoryOption(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val active: Boolean = false,
)

/**
 * Unified model picker drawer. Replaces all 3 ModalBottomSheet copies.
 * Morph open/close + staggered rows + tactile select.
 */
@Composable
fun NeoModelDrawer(
    visible: Boolean,
    onDismiss: () -> Unit,
    options: List<NeoModelOption>,
    selectedId: String,
    search: String,
    onSearchChange: (String) -> Unit,
    onSelect: (String) -> Unit,
    isLoading: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    title: String = "Select Model",
    allowCustomId: Boolean = true,
) {
    NeoBottomDrawer(
        visible = visible,
        onDismiss = onDismiss,
        initialDetent = DrawerDetent.Half,
        title = title,
        subtitle = "${options.size} available",
        searchValue = search,
        onSearchChange = onSearchChange,
        searchPlaceholder = "Search model or series",
        headerActions = {
            if (onRefresh != null) {
                NeoIconButton(onClick = onRefresh, enabled = !isLoading) {
                    NeoText(if (isLoading) "…" else "↻", style = NeoType.BodyBold)
                }
            }
        },
    ) { _, setDetent ->
        if (allowCustomId && search.isNotBlank() && options.none { it.id == search.trim() }) {
            NeoDrawerRow(
                title = "Use \"${search.trim()}\"",
                subtitle = "Custom model ID",
                badge = "CUSTOM",
                selected = selectedId == search.trim(),
                onClick = { onSelect(search.trim()) },
            )
            Spacer(Modifier.height(8.dp))
        }
        if (options.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                NeoCaption(if (isLoading) "Discovering models…" else "No matching models found")
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false).height(380.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(options, key = { _, o -> o.id }) { index, option ->
                    NeoStagger(index = index) {
                        NeoDrawerRow(
                            title = option.displayName,
                            subtitle = if (option.displayName != option.id) option.id else null,
                            badge = option.badge,
                            selected = selectedId == option.id,
                            onClick = { onSelect(option.id) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * GitHub repo picker drawer. Replaces centered AlertDialog.
 */
@Composable
fun NeoRepoDrawer(
    visible: Boolean,
    onDismiss: () -> Unit,
    repos: List<NeoRepoOption>,
    search: String,
    onSearchChange: (String) -> Unit,
    onSelect: (NeoRepoOption) -> Unit,
    isLoading: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    login: String? = null,
) {
    NeoBottomDrawer(
        visible = visible,
        onDismiss = onDismiss,
        initialDetent = DrawerDetent.Half,
        title = if (login != null) "GitHub · @$login" else "GitHub repositories",
        subtitle = "${repos.size} repositories",
        searchValue = search,
        onSearchChange = onSearchChange,
        searchPlaceholder = "Search repositories",
        headerActions = {
            if (onRefresh != null) {
                NeoIconButton(onClick = onRefresh, enabled = !isLoading) {
                    NeoText(if (isLoading) "…" else "↻", style = NeoType.BodyBold)
                }
            }
        },
    ) { _, _ ->
        if (repos.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                NeoCaption(if (isLoading) "Loading repositories…" else "No repositories found")
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false).height(380.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(repos, key = { _, r -> r.fullName }) { index, repo ->
                    NeoStagger(index = index) {
                        val meta = buildString {
                            if (repo.language != null) append(repo.language)
                            if (repo.stars != null) {
                                if (isNotEmpty()) append("  ·  ")
                                append("★ ${repo.stars}")
                            }
                            if (repo.description != null) {
                                if (isNotEmpty()) append("  ·  ")
                                append(repo.description)
                            }
                        }.takeIf { it.isNotEmpty() }
                        NeoDrawerRow(
                            title = repo.fullName,
                            subtitle = meta,
                            selected = false,
                            onClick = { onSelect(repo) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * History / chat-switcher drawer. Replaces ChatSwitcherDialog AlertDialog.
 */
@Composable
fun NeoHistoryDrawer(
    visible: Boolean,
    onDismiss: () -> Unit,
    sessions: List<NeoHistoryOption>,
    search: String,
    onSearchChange: (String) -> Unit,
    onSelect: (NeoHistoryOption) -> Unit,
    /** Null hides the "New chat" footer (e.g. read-only history). */
    onNewChat: (() -> Unit)?,
    title: String = "History",
) {
    NeoBottomDrawer(
        visible = visible,
        onDismiss = onDismiss,
        initialDetent = DrawerDetent.Half,
        title = title,
        subtitle = "${sessions.size} sessions",
        searchValue = search,
        onSearchChange = onSearchChange,
        searchPlaceholder = "Search sessions",
        footer = onNewChat?.let { newChat -> {
            NeoButton(onClick = newChat, modifier = Modifier.fillMaxWidth()) {
                NeoButtonLabel("+  New chat")
            }
        } },
    ) { _, _ ->
        val q = search.trim()
        val filtered = if (q.isBlank()) sessions else sessions.filter {
            it.title.contains(q, true) || (it.subtitle?.contains(q, true) == true)
        }
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                NeoCaption("No sessions yet")
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false).height(340.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(filtered, key = { _, s -> s.id }) { index, s ->
                    NeoStagger(index = index) {
                        NeoDrawerRow(
                            title = s.title,
                            subtitle = s.subtitle,
                            badge = if (s.active) "ACTIVE" else null,
                            selected = s.active,
                            onClick = { onSelect(s) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Empty-state helper for drawers (foundation-only).
 */
@Composable
fun NeoEmptyState(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(24.dp))
        NeoCaption(message)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(12.dp))
            NeoButton(onClick = onAction) { NeoButtonLabel(actionLabel) }
        }
        Spacer(Modifier.height(24.dp))
    }
}
