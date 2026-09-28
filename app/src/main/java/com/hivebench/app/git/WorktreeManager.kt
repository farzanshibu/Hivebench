package com.hivebench.app.git

import com.hivebench.app.model.GitWorktree
import java.io.File

class WorktreeManager(
    private val runner: GitCommandRunner,
) {
    suspend fun listWorktrees(workspace: File, guestPath: String): Result<List<GitWorktree>> {
        val result = runner.execute(workspace, guestPath, listOf("worktree", "list", "--porcelain"))
        if (!result.isSuccess) {
            return Result.failure(Exception(result.stderr.ifBlank { "git worktree list failed" }))
        }

        val worktrees = mutableListOf<GitWorktree>()
        var currentPath = ""
        var currentHead = ""
        var currentBranch: String? = null
        var isBare = false
        var isDetached = false
        var isLocked = false
        var lockReason: String? = null

        fun flush() {
            if (currentPath.isNotBlank()) {
                val relPath = if (currentPath.startsWith(guestPath)) {
                    currentPath.removePrefix(guestPath).trimStart('/')
                } else if (currentPath.startsWith(workspace.absolutePath)) {
                    currentPath.removePrefix(workspace.absolutePath).trimStart('/')
                } else {
                    currentPath.substringAfterLast('/')
                }
                worktrees.add(
                    GitWorktree(
                        path = if (relPath.isBlank()) "." else relPath,
                        headCommit = currentHead.take(8),
                        branch = currentBranch?.removePrefix("refs/heads/"),
                        isBare = isBare,
                        isDetached = isDetached,
                        isLocked = isLocked,
                        lockReason = lockReason,
                    ),
                )
                currentPath = ""
                currentHead = ""
                currentBranch = null
                isBare = false
                isDetached = false
                isLocked = false
                lockReason = null
            }
        }

        for (line in result.stdout.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                flush()
                continue
            }
            when {
                trimmed.startsWith("worktree ") -> {
                    flush()
                    currentPath = trimmed.removePrefix("worktree ").trim()
                }
                trimmed.startsWith("HEAD ") -> currentHead = trimmed.removePrefix("HEAD ").trim()
                trimmed.startsWith("branch ") -> currentBranch = trimmed.removePrefix("branch ").trim()
                trimmed == "bare" -> isBare = true
                trimmed == "detached" -> isDetached = true
                trimmed.startsWith("locked") -> {
                    isLocked = true
                    lockReason = trimmed.removePrefix("locked").trim().takeIf(String::isNotBlank)
                }
            }
        }
        flush()

        return Result.success(worktrees)
    }

    suspend fun addWorktree(
        workspace: File,
        guestPath: String,
        branchName: String,
        createBranch: Boolean = true,
        customRelativePath: String? = null,
    ): Result<GitWorktree> {
        val sanitizedBranch = branchName.trim().replace(Regex("""[^a-zA-Z0-9._-]"""), "-")
        require(sanitizedBranch.isNotBlank()) { "Invalid branch name" }

        val relPath = customRelativePath ?: ".worktrees/$sanitizedBranch"
        val guestTargetPath = "$guestPath/$relPath"

        val targetDir = File(workspace, relPath)
        targetDir.parentFile?.mkdirs()

        val args = buildList {
            add("worktree")
            add("add")
            if (createBranch) {
                add("-b")
                add(sanitizedBranch)
            }
            add(guestTargetPath)
            if (!createBranch) {
                add(sanitizedBranch)
            }
        }

        val result = runner.execute(workspace, guestPath, args)
        if (!result.isSuccess) {
            return Result.failure(Exception(result.stderr.ifBlank { "Failed to add worktree" }))
        }

        val list = listWorktrees(workspace, guestPath).getOrNull().orEmpty()
        val created = list.firstOrNull { it.branch == sanitizedBranch || it.path == relPath }
            ?: GitWorktree(
                path = relPath,
                headCommit = "HEAD",
                branch = sanitizedBranch,
            )

        return Result.success(created)
    }

    suspend fun removeWorktree(
        workspace: File,
        guestPath: String,
        worktreeRelPath: String,
        force: Boolean = false,
    ): Result<Unit> {
        val cleanPath = worktreeRelPath.trim().trimStart('/')
        require(cleanPath.isNotBlank() && cleanPath != ".") { "Cannot remove main worktree" }
        val guestTargetPath = "$guestPath/$cleanPath"

        val args = buildList {
            add("worktree")
            add("remove")
            if (force) add("--force")
            add(guestTargetPath)
        }
        val result = runner.execute(workspace, guestPath, args)
        if (!result.isSuccess) {
            // If git worktree remove failed, try pruning and deleting folder
            pruneWorktrees(workspace, guestPath)
            val folder = File(workspace, cleanPath)
            if (folder.exists()) folder.deleteRecursively()
        }
        pruneWorktrees(workspace, guestPath)
        return Result.success(Unit)
    }

    suspend fun pruneWorktrees(workspace: File, guestPath: String): Result<Unit> {
        val result = runner.execute(workspace, guestPath, listOf("worktree", "prune"))
        return if (result.isSuccess) Result.success(Unit) else Result.failure(Exception(result.stderr))
    }
}
