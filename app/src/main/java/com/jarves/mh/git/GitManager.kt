package com.jarves.mh.git

import com.jarves.mh.model.DiffLine
import com.jarves.mh.model.DiffLineType
import com.jarves.mh.model.GitBranch
import com.jarves.mh.model.GitCommit
import com.jarves.mh.model.GitFileDiff
import com.jarves.mh.model.GitFileStatus
import com.jarves.mh.model.GitStatus
import com.jarves.mh.model.GitStatusFile
import java.io.File

class GitManager(
    private val runner: GitCommandRunner,
) {
    suspend fun isGitRepository(workspace: File, guestPath: String): Boolean {
        val result = runner.execute(workspace, guestPath, listOf("rev-parse", "--is-inside-work-tree"))
        return result.isSuccess && result.stdout.trim() == "true"
    }

    suspend fun init(workspace: File, guestPath: String, defaultBranch: String = "main"): Result<Unit> {
        val initRes = runner.execute(workspace, guestPath, listOf("init", "-b", defaultBranch))
        if (!initRes.isSuccess) {
            val fallback = runner.execute(workspace, guestPath, listOf("init"))
            if (!fallback.isSuccess) return Result.failure(Exception(fallback.stderr.ifBlank { "git init failed" }))
            runner.execute(workspace, guestPath, listOf("checkout", "-b", defaultBranch))
        }

        // Configure default user if not yet set
        runner.execute(workspace, guestPath, listOf("config", "user.name", "Mobile Harness User"))
        runner.execute(workspace, guestPath, listOf("config", "user.email", "harness@mobile.internal"))
        return Result.success(Unit)
    }

    suspend fun status(workspace: File, guestPath: String): Result<GitStatus> {
        val result = runner.execute(workspace, guestPath, listOf("status", "--porcelain=v1", "-b"))
        if (!result.isSuccess) {
            return Result.failure(Exception(result.stderr.ifBlank { "git status failed" }))
        }

        val lines = result.stdout.lineSequence().filter(String::isNotBlank).toList()
        var currentBranch = "main"
        var trackingBranch: String? = null
        var ahead = 0
        var behind = 0

        val staged = mutableListOf<GitStatusFile>()
        val unstaged = mutableListOf<GitStatusFile>()
        val untracked = mutableListOf<GitStatusFile>()

        for (line in lines) {
            if (line.startsWith("##")) {
                // e.g. ## main...origin/main [ahead 1, behind 2] or ## Initial commit on main
                val header = line.removePrefix("##").trim()
                val branchPart = header.substringBefore(" [").trim()
                if (branchPart.contains("...")) {
                    currentBranch = branchPart.substringBefore("...")
                    trackingBranch = branchPart.substringAfter("...")
                } else if (branchPart.startsWith("No commits yet on ") || branchPart.startsWith("Initial commit on ")) {
                    currentBranch = branchPart.substringAfterLast(" ")
                } else {
                    currentBranch = branchPart
                }

                if (header.contains("[")) {
                    val trackingInfo = header.substringAfter("[").substringBefore("]")
                    val aheadMatch = Regex("""ahead (\d+)""").find(trackingInfo)
                    val behindMatch = Regex("""behind (\d+)""").find(trackingInfo)
                    ahead = aheadMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    behind = behindMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
                }
                continue
            }

            if (line.length < 3) continue
            val indexChar = line[0]
            val worktreeChar = line[1]
            val rawPath = line.substring(3).trim()
            val (origPath, targetPath) = if (rawPath.contains(" -> ")) {
                rawPath.substringBefore(" -> ") to rawPath.substringAfter(" -> ")
            } else {
                null to rawPath
            }

            val indexStatus = parseStatusCode(indexChar)
            val worktreeStatus = parseStatusCode(worktreeChar)
            val statusFile = GitStatusFile(
                path = targetPath,
                originalPath = origPath,
                indexStatus = indexStatus,
                worktreeStatus = worktreeStatus,
            )

            if (indexStatus == GitFileStatus.UNTRACKED) {
                untracked.add(statusFile)
            } else {
                if (statusFile.isStaged) staged.add(statusFile)
                if (statusFile.hasUnstagedChanges) unstaged.add(statusFile)
            }
        }

        return Result.success(
            GitStatus(
                currentBranch = currentBranch,
                trackingBranch = trackingBranch,
                ahead = ahead,
                behind = behind,
                stagedFiles = staged,
                unstagedFiles = unstaged,
                untrackedFiles = untracked,
            ),
        )
    }

    suspend fun diff(workspace: File, guestPath: String, staged: Boolean = false, path: String? = null): Result<List<GitFileDiff>> {
        val args = buildList {
            add("diff")
            if (staged) add("--cached")
            add("--")
            if (!path.isNullOrBlank()) add(path)
        }
        val result = runner.execute(workspace, guestPath, args)
        if (!result.isSuccess) {
            return Result.failure(Exception(result.stderr.ifBlank { "git diff failed" }))
        }
        val parsed = GitDiffParser.parse(result.stdout, isStaged = staged)

        // If path is specified and untracked (empty git diff), handle as new untracked file
        if (parsed.isEmpty() && !path.isNullOrBlank() && !staged) {
            val file = File(workspace, path)
            if (file.isFile) {
                val lines = file.readLines()
                val diffLines = lines.mapIndexed { index, lineText ->
                    DiffLine(DiffLineType.ADDITION, lineText, null, index + 1)
                }
                return Result.success(
                    listOf(
                        GitFileDiff(
                            oldPath = path,
                            newPath = path,
                            isNew = true,
                            isStaged = false,
                            additions = lines.size,
                            deletions = 0,
                            diffLines = diffLines,
                        ),
                    ),
                )
            }
        }

        return Result.success(parsed)
    }

    suspend fun stageFile(workspace: File, guestPath: String, path: String): Result<Unit> {
        val result = runner.execute(workspace, guestPath, listOf("add", "--", path))
        return if (result.isSuccess) Result.success(Unit) else Result.failure(Exception(result.stderr))
    }

    suspend fun stageAll(workspace: File, guestPath: String): Result<Unit> {
        val result = runner.execute(workspace, guestPath, listOf("add", "-A"))
        return if (result.isSuccess) Result.success(Unit) else Result.failure(Exception(result.stderr))
    }

    suspend fun unstageFile(workspace: File, guestPath: String, path: String): Result<Unit> {
        val restore = runner.execute(workspace, guestPath, listOf("restore", "--staged", "--", path))
        if (restore.isSuccess) return Result.success(Unit)
        val reset = runner.execute(workspace, guestPath, listOf("reset", "HEAD", "--", path))
        return if (reset.isSuccess) Result.success(Unit) else Result.failure(Exception(reset.stderr))
    }

    suspend fun unstageAll(workspace: File, guestPath: String): Result<Unit> {
        val restore = runner.execute(workspace, guestPath, listOf("restore", "--staged", "."))
        if (restore.isSuccess) return Result.success(Unit)
        val reset = runner.execute(workspace, guestPath, listOf("reset", "HEAD"))
        return if (reset.isSuccess) Result.success(Unit) else Result.failure(Exception(reset.stderr))
    }

    suspend fun discardChanges(workspace: File, guestPath: String, path: String): Result<Unit> {
        val file = File(workspace, path)
        val statusRes = status(workspace, guestPath)
        val isUntracked = statusRes.getOrNull()?.untrackedFiles?.any { it.path == path } == true

        if (isUntracked) {
            if (file.exists()) file.deleteRecursively()
            return Result.success(Unit)
        }

        val restore = runner.execute(workspace, guestPath, listOf("restore", "--", path))
        if (restore.isSuccess) return Result.success(Unit)
        val checkout = runner.execute(workspace, guestPath, listOf("checkout", "--", path))
        return if (checkout.isSuccess) Result.success(Unit) else Result.failure(Exception(checkout.stderr))
    }

    suspend fun commit(
        workspace: File,
        guestPath: String,
        message: String,
        authorName: String? = null,
        authorEmail: String? = null,
        amend: Boolean = false,
    ): Result<GitCommit> {
        require(message.isNotBlank()) { "Commit message cannot be blank" }
        val args = buildList {
            add("commit")
            if (amend) add("--amend")
            add("-m")
            add(message.trim())
            if (!authorName.isNullOrBlank() && !authorEmail.isNullOrBlank()) {
                add("--author=$authorName <$authorEmail>")
            }
        }
        val result = runner.execute(workspace, guestPath, args)
        if (!result.isSuccess) {
            return Result.failure(Exception(result.stderr.ifBlank { "git commit failed" }))
        }

        val log = log(workspace, guestPath, maxCount = 1).getOrNull()?.firstOrNull()
            ?: return Result.failure(Exception("Commit succeeded but could not read commit log"))
        return Result.success(log)
    }

    suspend fun log(workspace: File, guestPath: String, maxCount: Int = 50, branch: String? = null): Result<List<GitCommit>> {
        val args = buildList {
            add("log")
            add("-n")
            add(maxCount.toString())
            add("--pretty=format:%H%x00%h%x00%an%x00%ae%x00%at%x00%s")
            if (!branch.isNullOrBlank()) add(branch)
        }
        val result = runner.execute(workspace, guestPath, args)
        if (!result.isSuccess) {
            // New repository with no commits returns error
            if (result.stderr.contains("does not have any commits") || result.stderr.contains("fatal: your current branch")) {
                return Result.success(emptyList())
            }
            return Result.failure(Exception(result.stderr.ifBlank { "git log failed" }))
        }

        val commits = result.stdout.lineSequence().filter(String::isNotBlank).mapNotNull { line ->
            val parts = line.split('\u0000')
            if (parts.size >= 6) {
                GitCommit(
                    hash = parts[0],
                    shortHash = parts[1],
                    authorName = parts[2],
                    authorEmail = parts[3],
                    timestampMillis = (parts[4].toLongOrNull() ?: 0L) * 1000L,
                    message = parts[5],
                )
            } else null
        }.toList()

        return Result.success(commits)
    }

    suspend fun commitDiff(workspace: File, guestPath: String, commitHash: String): Result<List<GitFileDiff>> {
        val result = runner.execute(workspace, guestPath, listOf("show", "--format=", "--patch", commitHash))
        if (!result.isSuccess) {
            return Result.failure(Exception(result.stderr.ifBlank { "git show failed" }))
        }
        return Result.success(GitDiffParser.parse(result.stdout))
    }

    suspend fun branches(workspace: File, guestPath: String): Result<List<GitBranch>> {
        val result = runner.execute(workspace, guestPath, listOf("branch", "-a", "-vv"))
        if (!result.isSuccess) {
            return Result.failure(Exception(result.stderr.ifBlank { "git branch failed" }))
        }

        val branches = result.stdout.lineSequence().filter(String::isNotBlank).mapNotNull { line ->
            val trimmed = line.trim()
            val isCurrent = line.startsWith("*")
            val clean = trimmed.removePrefix("*").trim()
            val name = clean.substringBefore(" ").trim()
            val isRemote = name.startsWith("remotes/")
            if (name.contains("->")) return@mapNotNull null // skip HEAD pointer

            val tracking = if (clean.contains("[")) {
                clean.substringAfter("[").substringBefore("]").substringBefore(":")
            } else null

            val hash = clean.substringAfter(" ").trim().substringBefore(" ").take(8)

            GitBranch(
                name = name.removePrefix("remotes/"),
                isCurrent = isCurrent,
                isRemote = isRemote,
                upstream = tracking,
                hash = hash,
            )
        }.distinctBy { it.name }.toList()

        return Result.success(branches)
    }

    suspend fun createBranch(workspace: File, guestPath: String, branchName: String, checkout: Boolean = true): Result<Unit> {
        val cleanName = branchName.trim()
        val args = if (checkout) listOf("checkout", "-b", cleanName) else listOf("branch", cleanName)
        val result = runner.execute(workspace, guestPath, args)
        return if (result.isSuccess) Result.success(Unit) else Result.failure(Exception(result.stderr))
    }

    suspend fun checkoutBranch(workspace: File, guestPath: String, branchName: String): Result<Unit> {
        val result = runner.execute(workspace, guestPath, listOf("checkout", branchName.trim()))
        return if (result.isSuccess) Result.success(Unit) else Result.failure(Exception(result.stderr))
    }

    suspend fun deleteBranch(workspace: File, guestPath: String, branchName: String, force: Boolean = false): Result<Unit> {
        val flag = if (force) "-D" else "-d"
        val result = runner.execute(workspace, guestPath, listOf("branch", flag, branchName.trim()))
        return if (result.isSuccess) Result.success(Unit) else Result.failure(Exception(result.stderr))
    }

    suspend fun mergeBranch(workspace: File, guestPath: String, branchName: String): Result<Unit> {
        val result = runner.execute(workspace, guestPath, listOf("merge", branchName.trim()))
        return if (result.isSuccess) Result.success(Unit) else Result.failure(Exception(result.stderr))
    }

    suspend fun getRemotes(workspace: File, guestPath: String): Result<Map<String, String>> {
        val result = runner.execute(workspace, guestPath, listOf("remote", "-v"))
        if (!result.isSuccess) return Result.failure(Exception(result.stderr))
        val remotes = mutableMapOf<String, String>()
        result.stdout.lineSequence().filter(String::isNotBlank).forEach { line ->
            val parts = line.split(Regex("""\s+"""))
            if (parts.size >= 2) remotes[parts[0]] = parts[1]
        }
        return Result.success(remotes)
    }

    suspend fun setRemote(workspace: File, guestPath: String, name: String, url: String): Result<Unit> {
        val existing = getRemotes(workspace, guestPath).getOrNull().orEmpty()
        val args = if (name in existing) listOf("remote", "set-url", name, url) else listOf("remote", "add", name, url)
        val result = runner.execute(workspace, guestPath, args)
        return if (result.isSuccess) Result.success(Unit) else Result.failure(Exception(result.stderr))
    }

    suspend fun push(
        workspace: File,
        guestPath: String,
        remote: String = "origin",
        branch: String? = null,
        token: String? = null,
    ): Result<String> {
        val args = buildList {
            add("push")
            add("-u")
            add(remote)
            add(branch ?: "HEAD")
        }
        val result = runner.execute(workspace, guestPath, args, authToken = token, timeoutMs = 120_000L)
        return if (result.isSuccess) Result.success(result.stdout.ifBlank { "Pushed successfully" }) else Result.failure(Exception(result.stderr))
    }

    suspend fun pull(
        workspace: File,
        guestPath: String,
        remote: String = "origin",
        branch: String? = null,
        token: String? = null,
    ): Result<String> {
        val args = buildList {
            add("pull")
            add(remote)
            if (!branch.isNullOrBlank()) add(branch)
        }
        val result = runner.execute(workspace, guestPath, args, authToken = token, timeoutMs = 120_000L)
        return if (result.isSuccess) Result.success(result.stdout.ifBlank { "Pulled successfully" }) else Result.failure(Exception(result.stderr))
    }

    private fun parseStatusCode(char: Char): GitFileStatus = when (char) {
        'M' -> GitFileStatus.MODIFIED
        'A' -> GitFileStatus.ADDED
        'D' -> GitFileStatus.DELETED
        'R' -> GitFileStatus.RENAMED
        'C' -> GitFileStatus.COPIED
        '?' -> GitFileStatus.UNTRACKED
        '!' -> GitFileStatus.IGNORED
        'U' -> GitFileStatus.CONFLICTED
        ' ' -> GitFileStatus.UNMODIFIED
        else -> GitFileStatus.MODIFIED
    }
}
