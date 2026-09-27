package com.jarves.mh.review

import java.util.UUID

enum class AnnotationType(val label: String, val badgeColor: Long) {
    COMMENT("Comment", 0xFF64B5F6),
    SUGGESTION("Suggestion", 0xFFB4F000),
    REQUEST_CHANGES("Changes Requested", 0xFFFF5252),
    SECURITY_WARNING("Security Warning", 0xFFFF9100),
    APPROVE("Approved", 0xFF00E676),
}

enum class ReviewStatus(val label: String) {
    PENDING("Under Review"),
    CHANGES_REQUESTED("Changes Requested"),
    APPROVED("Approved & Ready to Merge"),
}

/**
 * An inline line-level annotation on a Git diff.
 */
data class LineDiffAnnotation(
    val id: String = UUID.randomUUID().toString(),
    val filePath: String,
    val lineNumber: Int,
    val author: String = "Rhea Reviewer",
    val authorAvatar: String = "🔍",
    val type: AnnotationType = AnnotationType.COMMENT,
    val comment: String,
    val suggestedPatch: String? = null,
    val resolved: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * A full code review session comparing a worktree branch against the primary branch.
 */
data class DiffReviewSession(
    val id: String = UUID.randomUUID().toString(),
    val targetBranch: String = "main",
    val worktreeBranch: String,
    val taskId: String? = null,
    val summary: String = "",
    val status: ReviewStatus = ReviewStatus.PENDING,
    val reviewerAgentId: String = "agent-reviewer",
    val annotations: List<LineDiffAnnotation> = emptyList(),
    val createdTimestamp: Long = System.currentTimeMillis(),
)
