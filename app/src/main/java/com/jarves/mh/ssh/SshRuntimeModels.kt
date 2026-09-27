package com.jarves.mh.ssh

import java.util.UUID

enum class SshAuthType(val label: String) {
    PASSWORD("Password"),
    PRIVATE_KEY("SSH Key"),
}

enum class SshConnectionStatus(val label: String) {
    DISCONNECTED("Disconnected"),
    CONNECTING("Connecting..."),
    CONNECTED("Connected (Remote SSH)"),
    ERROR("Connection Failed"),
}

/**
 * Server profile for remote SSH agent execution backend.
 */
data class SshServerProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val host: String,
    val port: Int = 22,
    val user: String = "root",
    val authType: SshAuthType = SshAuthType.PASSWORD,
    val remoteWorkspacePath: String = "~/workspace",
    val status: SshConnectionStatus = SshConnectionStatus.DISCONNECTED,
    val lastConnectedMillis: Long = 0L,
    val errorMessage: String? = null,
)

data class SshCommandResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val executionTimeMs: Long = 0L,
)
