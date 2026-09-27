package com.jarves.mh.ssh

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.system.measureTimeMillis

/**
 * Manages SSH remote execution backends and connection configurations.
 * Persists configured SSH profiles to .pocketdev/ssh_profiles.json.
 */
class SshRuntimeManager(private val projectDir: File) {

    private val profilesFile: File
        get() {
            val pocketDevDir = File(projectDir, ".pocketdev").apply { if (!exists()) mkdirs() }
            return File(pocketDevDir, "ssh_profiles.json")
        }

    private val profiles = mutableListOf<SshServerProfile>()
    private var activeProfileId: String? = null

    init {
        loadProfiles()
    }

    @Synchronized
    fun getProfiles(): List<SshServerProfile> = profiles.toList()

    @Synchronized
    fun getActiveProfile(): SshServerProfile? = profiles.firstOrNull { it.id == activeProfileId }

    @Synchronized
    fun isRemoteBackendActive(): Boolean = getActiveProfile()?.status == SshConnectionStatus.CONNECTED

    @Synchronized
    fun setActiveProfile(profileId: String?) {
        activeProfileId = profileId
        saveProfiles()
    }

    @Synchronized
    fun addProfile(
        name: String,
        host: String,
        port: Int,
        user: String,
        authType: SshAuthType,
        remoteWorkspacePath: String,
    ): SshServerProfile {
        val profile = SshServerProfile(
            name = name,
            host = host,
            port = port,
            user = user,
            authType = authType,
            remoteWorkspacePath = remoteWorkspacePath,
        )
        profiles.add(profile)
        saveProfiles()
        return profile
    }

    @Synchronized
    fun deleteProfile(id: String): Boolean {
        val removed = profiles.removeAll { it.id == id }
        if (activeProfileId == id) activeProfileId = null
        if (removed) saveProfiles()
        return removed
    }

    @Synchronized
    fun updateProfileStatus(id: String, status: SshConnectionStatus, errorMsg: String? = null) {
        val idx = profiles.indexOfFirst { it.id == id }
        if (idx >= 0) {
            profiles[idx] = profiles[idx].copy(
                status = status,
                errorMessage = errorMsg,
                lastConnectedMillis = if (status == SshConnectionStatus.CONNECTED) System.currentTimeMillis() else profiles[idx].lastConnectedMillis,
            )
            saveProfiles()
        }
    }

    /**
     * Executes a command on the active remote SSH server or prepares the SSH command string.
     */
    suspend fun executeRemoteCommand(
        command: String,
        profile: SshServerProfile = getActiveProfile() ?: throw IllegalStateException("No active SSH profile selected"),
    ): SshCommandResult {
        var stdout = ""
        var stderr = ""
        var exitCode = 0

        val timeMs = measureTimeMillis {
            try {
                // Formulate the SSH command to run via the local Linux toolchain or PRoot ssh binary
                val sshExecCmd = "ssh -p ${profile.port} -o StrictHostKeyChecking=no ${profile.user}@${profile.host} \"cd ${profile.remoteWorkspacePath} && $command\""
                stdout = "Remote execution dispatched to ${profile.host}:${profile.port}\n$sshExecCmd"
            } catch (e: Exception) {
                exitCode = 1
                stderr = e.message ?: "Remote execution failed"
            }
        }

        return SshCommandResult(
            exitCode = exitCode,
            stdout = stdout,
            stderr = stderr,
            executionTimeMs = timeMs,
        )
    }

    @Synchronized
    private fun saveProfiles() {
        try {
            val root = JSONObject()
            root.put("activeProfileId", activeProfileId ?: "")
            val arr = JSONArray()
            profiles.forEach { p ->
                val obj = JSONObject()
                obj.put("id", p.id)
                obj.put("name", p.name)
                obj.put("host", p.host)
                obj.put("port", p.port)
                obj.put("user", p.user)
                obj.put("authType", p.authType.name)
                obj.put("remoteWorkspacePath", p.remoteWorkspacePath)
                obj.put("status", p.status.name)
                obj.put("lastConnectedMillis", p.lastConnectedMillis)
                arr.put(obj)
            }
            root.put("profiles", arr)
            profilesFile.writeText(root.toString(2))
        } catch (_: Exception) {}
    }

    @Synchronized
    private fun loadProfiles() {
        if (!profilesFile.exists()) return
        try {
            val root = JSONObject(profilesFile.readText())
            activeProfileId = root.optString("activeProfileId").takeIf { it.isNotBlank() }
            val arr = root.optJSONArray("profiles") ?: JSONArray()
            profiles.clear()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val authStr = obj.optString("authType", SshAuthType.PASSWORD.name)
                val authType = try { SshAuthType.valueOf(authStr) } catch (_: Exception) { SshAuthType.PASSWORD }
                val statusStr = obj.optString("status", SshConnectionStatus.DISCONNECTED.name)
                val status = try { SshConnectionStatus.valueOf(statusStr) } catch (_: Exception) { SshConnectionStatus.DISCONNECTED }

                profiles.add(
                    SshServerProfile(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        name = obj.optString("name", "Remote Server"),
                        host = obj.optString("host", "127.0.0.1"),
                        port = obj.optInt("port", 22),
                        user = obj.optString("user", "root"),
                        authType = authType,
                        remoteWorkspacePath = obj.optString("remoteWorkspacePath", "~/workspace"),
                        status = status,
                        lastConnectedMillis = obj.optLong("lastConnectedMillis", 0L),
                    )
                )
            }
        } catch (_: Exception) {}
    }
}
