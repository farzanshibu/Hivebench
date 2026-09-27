package com.jarves.mh.browser

import java.util.UUID

enum class ConsoleLogLevel(val label: String) {
    LOG("LOG"),
    INFO("INFO"),
    WARN("WARN"),
    ERROR("ERROR"),
}

data class ConsoleLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val level: ConsoleLogLevel = ConsoleLogLevel.LOG,
    val message: String,
    val source: String? = null,
    val lineNumber: Int? = null,
    val timestamp: Long = System.currentTimeMillis(),
)

data class NetworkRequestEntry(
    val id: String = UUID.randomUUID().toString(),
    val method: String = "GET",
    val url: String,
    val status: Int = 200,
    val type: String = "xhr",
    val durationMs: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
)

data class InspectedElement(
    val tagName: String,
    val id: String = "",
    val className: String = "",
    val textContent: String = "",
    val xpath: String = "",
    val computedStyles: Map<String, String> = emptyMap(),
    val boundingBox: String = "",
    val outerHtml: String = "",
) {
    val displaySelector: String
        get() {
            val tag = tagName.lowercase()
            val idPart = if (id.isNotBlank()) "#$id" else ""
            val classPart = if (className.isNotBlank()) "." + className.trim().split("\\s+".toRegex()).joinToString(".") else ""
            return "$tag$idPart$classPart"
        }

    fun toPromptContext(): String {
        val sb = StringBuilder()
        sb.append("### Selected DOM Element Context (Design Mode)\n")
        sb.append("- **Selector**: `$displaySelector`\n")
        sb.append("- **Tag**: `<$tagName>`\n")
        if (id.isNotBlank()) sb.append("- **ID**: `$id`\n")
        if (className.isNotBlank()) sb.append("- **Class**: `$className`\n")
        if (boundingBox.isNotBlank()) sb.append("- **Bounds**: $boundingBox\n")
        if (textContent.isNotBlank()) sb.append("- **Text**: \"${textContent.take(120)}\"\n")
        if (computedStyles.isNotEmpty()) {
            sb.append("- **Styles**:\n")
            computedStyles.forEach { (k, v) ->
                sb.append("  - `$k`: `$v`\n")
            }
        }
        if (outerHtml.isNotBlank()) {
            sb.append("\n**HTML Source Snippet**:\n```html\n${outerHtml.take(800)}\n```\n")
        }
        return sb.toString()
    }
}
