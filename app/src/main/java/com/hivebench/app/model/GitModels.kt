package com.hivebench.app.model

data class GitBranch(
    val name: String,
    val isCurrent: Boolean,
    val isRemote: Boolean = false,
    val upstream: String? = null,
    val hash: String = "",
)

data class GitWorktree(
    val path: String,
    val headCommit: String,
    val branch: String?,
    val isBare: Boolean = false,
    val isDetached: Boolean = false,
    val isLocked: Boolean = false,
    val lockReason: String? = null,
)

data class GitCommit(
    val hash: String,
    val shortHash: String,
    val authorName: String,
    val authorEmail: String,
    val timestampMillis: Long,
    val message: String,
    val parents: List<String> = emptyList(),
)

enum class GitFileStatus {
    UNMODIFIED,
    MODIFIED,
    ADDED,
    DELETED,
    RENAMED,
    COPIED,
    UNTRACKED,
    IGNORED,
    CONFLICTED,
}

data class GitStatusFile(
    val path: String,
    val originalPath: String? = null,
    val indexStatus: GitFileStatus,
    val worktreeStatus: GitFileStatus,
) {
    val isStaged: Boolean
        get() = indexStatus != GitFileStatus.UNMODIFIED && indexStatus != GitFileStatus.UNTRACKED
    val hasUnstagedChanges: Boolean
        get() = worktreeStatus != GitFileStatus.UNMODIFIED || (indexStatus == GitFileStatus.UNTRACKED)
}

data class GitStatus(
    val currentBranch: String,
    val trackingBranch: String? = null,
    val ahead: Int = 0,
    val behind: Int = 0,
    val stagedFiles: List<GitStatusFile> = emptyList(),
    val unstagedFiles: List<GitStatusFile> = emptyList(),
    val untrackedFiles: List<GitStatusFile> = emptyList(),
) {
    val isClean: Boolean
        get() = stagedFiles.isEmpty() && unstagedFiles.isEmpty() && untrackedFiles.isEmpty()
    val totalChangedFiles: Int
        get() = (stagedFiles.map { it.path } + unstagedFiles.map { it.path } + untrackedFiles.map { it.path }).distinct().size
}

data class GitDiffHunk(
    val oldStart: Int,
    val oldLength: Int,
    val newStart: Int,
    val newLength: Int,
    val header: String,
    val lines: List<DiffLine>,
)

data class GitFileDiff(
    val oldPath: String,
    val newPath: String,
    val isNew: Boolean = false,
    val isDeleted: Boolean = false,
    val isBinary: Boolean = false,
    val isStaged: Boolean = false,
    val additions: Int = 0,
    val deletions: Int = 0,
    val hunks: List<GitDiffHunk> = emptyList(),
    val diffLines: List<DiffLine> = emptyList(),
)

data class GitPullRequest(
    val number: Int,
    val title: String,
    val body: String,
    val headBranch: String,
    val baseBranch: String,
    val htmlUrl: String,
    val state: String = "open",
    val author: String = "",
    val createdAt: String = "",
)
