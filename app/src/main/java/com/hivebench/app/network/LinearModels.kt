package com.hivebench.app.network

import java.util.UUID

/**
 * Linear issue tracking domain models.
 */
data class LinearIssue(
    val id: String = UUID.randomUUID().toString(),
    val identifier: String, // e.g. "ENG-104"
    val title: String,
    val description: String = "",
    val priority: Int = 1, // 0 = None, 1 = Urgent, 2 = High, 3 = Normal, 4 = Low
    val stateName: String = "Todo",
    val stateType: String = "unstarted", // "backlog", "unstarted", "started", "completed", "canceled"
    val assigneeName: String? = null,
    val branchName: String? = null,
    val url: String = "",
    val createdAt: String = "",
)

data class LinearTeam(
    val id: String,
    val name: String,
    val key: String, // e.g. "ENG"
)

data class LinearProject(
    val id: String,
    val name: String,
    val description: String = "",
    val state: String = "planned",
)

data class LinearAuthState(
    val isConnected: Boolean = false,
    val apiKeyMasked: String? = null,
    val organizationName: String? = null,
)
