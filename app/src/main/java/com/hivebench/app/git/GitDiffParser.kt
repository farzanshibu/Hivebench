package com.hivebench.app.git

import com.hivebench.app.model.DiffLine
import com.hivebench.app.model.DiffLineType
import com.hivebench.app.model.GitDiffHunk
import com.hivebench.app.model.GitFileDiff

object GitDiffParser {
    private val HUNK_HEADER_REGEX = Regex("""^@@\s+-(\d+)(?:,(\d+))?\s+\+(\d+)(?:,(\d+))?\s+@@(.*)""")
    private val DIFF_GIT_HEADER = Regex("""^diff --git (?:a/)?(\S+) (?:b/)?(\S+)""")

    fun parse(rawDiff: String, isStaged: Boolean = false): List<GitFileDiff> {
        if (rawDiff.isBlank()) return emptyList()

        val lines = rawDiff.lineSequence().toList()
        val fileDiffs = mutableListOf<GitFileDiff>()

        var currentOldPath = ""
        var currentNewPath = ""
        var isNewFile = false
        var isDeletedFile = false
        var isBinaryFile = false
        var currentHunks = mutableListOf<GitDiffHunk>()
        var currentHunkLines = mutableListOf<DiffLine>()
        var currentDiffLines = mutableListOf<DiffLine>()
        var currentOldStart = 0
        var currentOldLen = 0
        var currentNewStart = 0
        var currentNewLen = 0
        var currentHunkHeader = ""
        var oldLineCursor = 0
        var newLineCursor = 0

        fun flushHunk() {
            if (currentHunkLines.isNotEmpty() || currentHunkHeader.isNotEmpty()) {
                currentHunks.add(
                    GitDiffHunk(
                        oldStart = currentOldStart,
                        oldLength = currentOldLen,
                        newStart = currentNewStart,
                        newLength = currentNewLen,
                        header = currentHunkHeader,
                        lines = currentHunkLines.toList(),
                    ),
                )
                currentHunkLines.clear()
            }
        }

        fun flushFile() {
            flushHunk()
            if (currentOldPath.isNotBlank() || currentNewPath.isNotBlank()) {
                val additions = currentDiffLines.count { it.type == DiffLineType.ADDITION }
                val deletions = currentDiffLines.count { it.type == DiffLineType.DELETION }
                val targetPath = if (currentNewPath.isNotBlank() && currentNewPath != "/dev/null") {
                    currentNewPath
                } else {
                    currentOldPath
                }
                fileDiffs.add(
                    GitFileDiff(
                        oldPath = currentOldPath.removePrefix("a/"),
                        newPath = targetPath.removePrefix("b/"),
                        isNew = isNewFile,
                        isDeleted = isDeletedFile,
                        isBinary = isBinaryFile,
                        isStaged = isStaged,
                        additions = additions,
                        deletions = deletions,
                        hunks = currentHunks.toList(),
                        diffLines = currentDiffLines.toList(),
                    ),
                )
                currentOldPath = ""
                currentNewPath = ""
                isNewFile = false
                isDeletedFile = false
                isBinaryFile = false
                currentHunks.clear()
                currentDiffLines.clear()
            }
        }

        for (line in lines) {
            val diffMatch = DIFF_GIT_HEADER.matchEntire(line)
            if (diffMatch != null) {
                flushFile()
                currentOldPath = diffMatch.groupValues[1]
                currentNewPath = diffMatch.groupValues[2]
                continue
            }

            if (line.startsWith("new file mode")) {
                isNewFile = true
                continue
            }
            if (line.startsWith("deleted file mode")) {
                isDeletedFile = true
                continue
            }
            if (line.startsWith("Binary files ") || line.contains("GIT binary patch")) {
                isBinaryFile = true
                currentDiffLines.add(DiffLine(DiffLineType.INFO, "Binary file changed"))
                continue
            }
            if (line.startsWith("--- ")) {
                val p = line.removePrefix("--- ").trim()
                if (p == "/dev/null") isNewFile = true else if (p.startsWith("a/")) currentOldPath = p.removePrefix("a/")
                continue
            }
            if (line.startsWith("+++ ")) {
                val p = line.removePrefix("+++ ").trim()
                if (p == "/dev/null") isDeletedFile = true else if (p.startsWith("b/")) currentNewPath = p.removePrefix("b/")
                continue
            }

            val hunkMatch = HUNK_HEADER_REGEX.matchEntire(line)
            if (hunkMatch != null) {
                flushHunk()
                currentOldStart = hunkMatch.groupValues[1].toIntOrNull() ?: 1
                currentOldLen = hunkMatch.groupValues[2].takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 1
                currentNewStart = hunkMatch.groupValues[3].toIntOrNull() ?: 1
                currentNewLen = hunkMatch.groupValues[4].takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 1
                currentHunkHeader = hunkMatch.groupValues[5].trim()
                oldLineCursor = currentOldStart
                newLineCursor = currentNewStart

                val infoLine = DiffLine(DiffLineType.INFO, line)
                currentDiffLines.add(infoLine)
                continue
            }

            if (line.startsWith("+")) {
                val diffLine = DiffLine(DiffLineType.ADDITION, line.substring(1), null, newLineCursor++)
                currentHunkLines.add(diffLine)
                currentDiffLines.add(diffLine)
            } else if (line.startsWith("-")) {
                val diffLine = DiffLine(DiffLineType.DELETION, line.substring(1), oldLineCursor++, null)
                currentHunkLines.add(diffLine)
                currentDiffLines.add(diffLine)
            } else if (line.startsWith(" ")) {
                val diffLine = DiffLine(DiffLineType.CONTEXT, line.substring(1), oldLineCursor++, newLineCursor++)
                currentHunkLines.add(diffLine)
                currentDiffLines.add(diffLine)
            } else if (line.startsWith("\\ No newline at end of file")) {
                val diffLine = DiffLine(DiffLineType.INFO, line)
                currentHunkLines.add(diffLine)
                currentDiffLines.add(diffLine)
            }
        }

        flushFile()
        return fileDiffs
    }
}
