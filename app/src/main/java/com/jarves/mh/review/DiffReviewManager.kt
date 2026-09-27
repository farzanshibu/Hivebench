package com.jarves.mh.review

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Manages inline diff annotations and automated agent code reviews.
 * Persists reviews to .pocketdev/diff_reviews.json.
 */
class DiffReviewManager(private val projectDir: File) {

    private val storageFile: File
        get() {
            val pocketDevDir = File(projectDir, ".pocketdev").apply { if (!exists()) mkdirs() }
            return File(pocketDevDir, "diff_reviews.json")
        }

    private val sessions = mutableListOf<DiffReviewSession>()

    init {
        loadReviews()
    }

    @Synchronized
    fun getAllSessions(): List<DiffReviewSession> = sessions.toList()

    @Synchronized
    fun getSessionForWorktree(worktreeBranch: String): DiffReviewSession? {
        return sessions.firstOrNull { it.worktreeBranch == worktreeBranch }
    }

    @Synchronized
    fun getAnnotationsForFile(filePath: String): List<LineDiffAnnotation> {
        return sessions.flatMap { it.annotations }.filter { it.filePath == filePath }
    }

    @Synchronized
    fun addAnnotation(
        worktreeBranch: String,
        filePath: String,
        lineNumber: Int,
        author: String,
        authorAvatar: String,
        type: AnnotationType,
        comment: String,
        suggestedPatch: String? = null,
    ): LineDiffAnnotation {
        val annotation = LineDiffAnnotation(
            filePath = filePath,
            lineNumber = lineNumber,
            author = author,
            authorAvatar = authorAvatar,
            type = type,
            comment = comment,
            suggestedPatch = suggestedPatch,
        )

        val existingSession = sessions.firstOrNull { it.worktreeBranch == worktreeBranch }
        if (existingSession != null) {
            val updatedAnnotations = existingSession.annotations + annotation
            val newStatus = if (type == AnnotationType.REQUEST_CHANGES || type == AnnotationType.SECURITY_WARNING) {
                ReviewStatus.CHANGES_REQUESTED
            } else {
                existingSession.status
            }
            val updatedSession = existingSession.copy(
                annotations = updatedAnnotations,
                status = newStatus,
            )
            val idx = sessions.indexOf(existingSession)
            sessions[idx] = updatedSession
        } else {
            val newSession = DiffReviewSession(
                worktreeBranch = worktreeBranch,
                summary = "Code review for worktree $worktreeBranch",
                annotations = listOf(annotation),
                status = if (type == AnnotationType.REQUEST_CHANGES) ReviewStatus.CHANGES_REQUESTED else ReviewStatus.PENDING,
            )
            sessions.add(0, newSession)
        }

        saveReviews()
        return annotation
    }

    @Synchronized
    fun toggleAnnotationResolved(annotationId: String): Boolean {
        for (i in sessions.indices) {
            val session = sessions[i]
            val annIndex = session.annotations.indexOfFirst { it.id == annotationId }
            if (annIndex >= 0) {
                val current = session.annotations[annIndex]
                val updatedAnn = current.copy(resolved = !current.resolved)
                val newAnnotations = session.annotations.toMutableList()
                newAnnotations[annIndex] = updatedAnn

                val allResolved = newAnnotations.none { !it.resolved && (it.type == AnnotationType.REQUEST_CHANGES || it.type == AnnotationType.SECURITY_WARNING) }
                val newStatus = if (allResolved) ReviewStatus.APPROVED else ReviewStatus.CHANGES_REQUESTED

                sessions[i] = session.copy(annotations = newAnnotations, status = newStatus)
                saveReviews()
                return updatedAnn.resolved
            }
        }
        return false
    }

    /**
     * Rhea Reviewer automated analysis pass over a set of diff hunks.
     */
    @Synchronized
    fun runAutomatedReview(
        worktreeBranch: String,
        fileDiffs: List<Pair<String, String>>, // (filePath, diffContent)
    ): DiffReviewSession {
        val detectedAnnotations = mutableListOf<LineDiffAnnotation>()

        fileDiffs.forEach { (filePath, diff) ->
            var currentLineNum = 1
            diff.lines().forEach { line ->
                if (line.startsWith("+") && !line.startsWith("+++")) {
                    val addedCode = line.removePrefix("+").trim()

                    // Security check: Potential secret
                    if (addedCode.contains("api_key", ignoreCase = true) ||
                        addedCode.contains("password", ignoreCase = true) ||
                        addedCode.contains("secret", ignoreCase = true) && addedCode.contains("=")) {
                        detectedAnnotations.add(
                            LineDiffAnnotation(
                                filePath = filePath,
                                lineNumber = currentLineNum,
                                author = "Rhea Reviewer",
                                authorAvatar = "🔍",
                                type = AnnotationType.SECURITY_WARNING,
                                comment = "Potential sensitive secret or credential in code. Use BuildConfig or secure storage instead.",
                            )
                        )
                    }

                    // Antipattern: println / Log in production
                    if (addedCode.startsWith("println(") || addedCode.startsWith("System.out.")) {
                        detectedAnnotations.add(
                            LineDiffAnnotation(
                                filePath = filePath,
                                lineNumber = currentLineNum,
                                author = "Rhea Reviewer",
                                authorAvatar = "🔍",
                                type = AnnotationType.SUGGESTION,
                                comment = "Raw stdout printing detected. Prefer structured logger.",
                                suggestedPatch = "// Log.d(TAG, message)",
                            )
                        )
                    }

                    // Antipattern: Empty catch block
                    if (addedCode.contains("catch (") && addedCode.contains("{}")) {
                        detectedAnnotations.add(
                            LineDiffAnnotation(
                                filePath = filePath,
                                lineNumber = currentLineNum,
                                author = "Rhea Reviewer",
                                authorAvatar = "🔍",
                                type = AnnotationType.REQUEST_CHANGES,
                                comment = "Silent exception suppression detected. Handle or log the error.",
                            )
                        )
                    }

                    currentLineNum++
                } else if (!line.startsWith("-")) {
                    currentLineNum++
                }
            }
        }

        val reviewStatus = if (detectedAnnotations.any { it.type == AnnotationType.SECURITY_WARNING || it.type == AnnotationType.REQUEST_CHANGES }) {
            ReviewStatus.CHANGES_REQUESTED
        } else {
            ReviewStatus.APPROVED
        }

        val session = DiffReviewSession(
            worktreeBranch = worktreeBranch,
            summary = if (detectedAnnotations.isEmpty()) "Automated review passed with 0 warnings." else "Found ${detectedAnnotations.size} review findings.",
            status = reviewStatus,
            annotations = detectedAnnotations,
        )

        sessions.removeAll { it.worktreeBranch == worktreeBranch }
        sessions.add(0, session)
        saveReviews()
        return session
    }

    @Synchronized
    private fun saveReviews() {
        try {
            val root = JSONArray()
            sessions.forEach { sess ->
                val obj = JSONObject()
                obj.put("id", sess.id)
                obj.put("targetBranch", sess.targetBranch)
                obj.put("worktreeBranch", sess.worktreeBranch)
                obj.put("summary", sess.summary)
                obj.put("status", sess.status.name)
                obj.put("reviewerAgentId", sess.reviewerAgentId)
                obj.put("createdTimestamp", sess.createdTimestamp)

                val annArr = JSONArray()
                sess.annotations.forEach { ann ->
                    val aObj = JSONObject()
                    aObj.put("id", ann.id)
                    aObj.put("filePath", ann.filePath)
                    aObj.put("lineNumber", ann.lineNumber)
                    aObj.put("author", ann.author)
                    aObj.put("authorAvatar", ann.authorAvatar)
                    aObj.put("type", ann.type.name)
                    aObj.put("comment", ann.comment)
                    aObj.put("suggestedPatch", ann.suggestedPatch ?: "")
                    aObj.put("resolved", ann.resolved)
                    aObj.put("timestamp", ann.timestamp)
                    annArr.put(aObj)
                }
                obj.put("annotations", annArr)
                root.put(obj)
            }
            storageFile.writeText(root.toString(2))
        } catch (_: Exception) {}
    }

    @Synchronized
    private fun loadReviews() {
        if (!storageFile.exists()) return
        try {
            val text = storageFile.readText()
            val root = JSONArray(text)
            sessions.clear()
            for (i in 0 until root.length()) {
                val obj = root.getJSONObject(i)
                val annArr = obj.optJSONArray("annotations") ?: JSONArray()
                val annotations = mutableListOf<LineDiffAnnotation>()

                for (j in 0 until annArr.length()) {
                    val aObj = annArr.getJSONObject(j)
                    val typeStr = aObj.optString("type", AnnotationType.COMMENT.name)
                    val type = try { AnnotationType.valueOf(typeStr) } catch (_: Exception) { AnnotationType.COMMENT }

                    annotations.add(
                        LineDiffAnnotation(
                            id = aObj.optString("id", java.util.UUID.randomUUID().toString()),
                            filePath = aObj.optString("filePath", ""),
                            lineNumber = aObj.optInt("lineNumber", 1),
                            author = aObj.optString("author", "Rhea Reviewer"),
                            authorAvatar = aObj.optString("authorAvatar", "🔍"),
                            type = type,
                            comment = aObj.optString("comment", ""),
                            suggestedPatch = aObj.optString("suggestedPatch").takeIf { it.isNotBlank() },
                            resolved = aObj.optBoolean("resolved", false),
                            timestamp = aObj.optLong("timestamp", System.currentTimeMillis()),
                        )
                    )
                }

                val statusStr = obj.optString("status", ReviewStatus.PENDING.name)
                val status = try { ReviewStatus.valueOf(statusStr) } catch (_: Exception) { ReviewStatus.PENDING }

                sessions.add(
                    DiffReviewSession(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        targetBranch = obj.optString("targetBranch", "main"),
                        worktreeBranch = obj.optString("worktreeBranch", "worktree"),
                        summary = obj.optString("summary", ""),
                        status = status,
                        reviewerAgentId = obj.optString("reviewerAgentId", "agent-reviewer"),
                        annotations = annotations,
                        createdTimestamp = obj.optLong("createdTimestamp", System.currentTimeMillis()),
                    )
                )
            }
        } catch (_: Exception) {}
    }
}
