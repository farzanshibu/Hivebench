package com.hivebench.app.git

import android.content.Context
import com.hivebench.app.runtime.NativeSpawnProcess
import com.hivebench.app.runtime.RuntimeInstaller
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class GitExecResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
) {
    val isSuccess: Boolean get() = exitCode == 0
}

class GitCommandRunner(
    private val context: Context,
    private val installer: RuntimeInstaller = RuntimeInstaller(context),
) {
    suspend fun execute(
        workspace: File,
        guestWorkspacePath: String,
        gitArgs: List<String>,
        extraEnv: Map<String, String> = emptyMap(),
        authToken: String? = null,
        timeoutMs: Long = 60_000L,
    ): GitExecResult = withContext(Dispatchers.IO) {
        if (!installer.isInstalled()) {
            return@withContext GitExecResult(-1, "", "Runtime is not installed yet")
        }

        val installed = installer.installedRuntime()
        val outputFile = File(context.cacheDir, "git-out-${System.nanoTime()}.log")

        val environment = mutableMapOf(
            "GIT_TERMINAL_PROMPT" to "0",
            "GIT_CONFIG_NOSYSTEM" to "1",
            "LC_ALL" to "C.UTF-8",
            "LANG" to "C.UTF-8",
            "PATH" to "/usr/local/bin:/usr/bin:/bin",
            "HOME" to "/root",
        )
        environment.putAll(extraEnv)

        val fullCommand = buildList {
            add("git")
            if (!authToken.isNullOrBlank()) {
                add("-c")
                add("http.extraheader=Authorization: Bearer $authToken")
            }
            addAll(gitArgs)
        }

        try {
            val process = installer.process(
                proot = installed.proot,
                rootfs = installed.rootfs,
                workspace = workspace,
                environment = environment,
                guestCommand = fullCommand,
                guestWorkspacePath = guestWorkspacePath,
                outputFile = outputFile,
            )

            val exitCode = process.waitFor()
            val output = if (outputFile.isFile) outputFile.readText() else ""
            GitExecResult(
                exitCode = exitCode,
                stdout = output,
                stderr = if (exitCode != 0) output else "",
            )
        } catch (e: Throwable) {
            GitExecResult(
                exitCode = -1,
                stdout = "",
                stderr = e.message ?: "Failed to execute git command",
            )
        } finally {
            outputFile.delete()
        }
    }
}
