package com.hivebench.app.browser

import org.json.JSONObject
import java.util.UUID

enum class ConsoleLogLevel(val label: String) {
    LOG("LOG"),
    INFO("INFO"),
    WARN("WARN"),
    ERROR("ERROR"),
    DEBUG("DEBUG"),
}

data class ConsoleLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val level: ConsoleLogLevel = ConsoleLogLevel.LOG,
    val message: String,
    val source: String? = null,
    val lineNumber: Int? = null,
    val timestamp: Long = System.currentTimeMillis(),
) {
    fun format(): String = buildString {
        append("[${level.label}] $message")
        if (!source.isNullOrBlank()) append(" ($source${lineNumber?.let { ":$it" } ?: ""})")
    }
}

/** One request seen by the page: fetch/XHR (full detail) or a resource load (timing only). */
data class NetworkRequestEntry(
    val id: String = UUID.randomUUID().toString(),
    val method: String = "GET",
    val url: String,
    /** HTTP status; 0 = network error / blocked, -1 = unknown (resource timing without status). */
    val status: Int = -1,
    val statusText: String = "",
    /** fetch, xhr, script, img, css, link, document, … */
    val type: String = "xhr",
    val durationMs: Long = 0L,
    val sizeBytes: Long = -1L,
    val requestHeaders: Map<String, String> = emptyMap(),
    val responseHeaders: Map<String, String> = emptyMap(),
    val requestBody: String = "",
    val responseBody: String = "",
    val error: String = "",
    val timestamp: Long = System.currentTimeMillis(),
) {
    val failed: Boolean get() = status == 0 || status >= 400 || error.isNotBlank()

    fun summary(): String = buildString {
        append("${method} ${url} → ")
        append(
            when {
                error.isNotBlank() -> "ERROR $error"
                status > 0 -> "$status $statusText".trim()
                status == 0 -> "failed"
                else -> "done"
            },
        )
        append(" (${durationMs}ms")
        if (sizeBytes >= 0) append(", ${sizeBytes}B")
        append(", $type)")
    }

    /** Full request/response details for an agent or the clipboard. */
    fun toPromptContext(): String = buildString {
        appendLine("### Network request")
        appendLine("- ${summary()}")
        if (requestHeaders.isNotEmpty()) {
            appendLine("- Request headers:")
            requestHeaders.forEach { (k, v) -> appendLine("  - $k: ${redactHeader(k, v)}") }
        }
        if (requestBody.isNotBlank()) appendLine("- Request body:\n```\n${requestBody.take(2_000)}\n```")
        if (responseHeaders.isNotEmpty()) {
            appendLine("- Response headers:")
            responseHeaders.forEach { (k, v) -> appendLine("  - $k: ${redactHeader(k, v)}") }
        }
        if (responseBody.isNotBlank()) appendLine("- Response body (truncated):\n```\n${responseBody.take(3_000)}\n```")
    }

    companion object {
        private val secretHeaders = setOf("authorization", "cookie", "set-cookie", "x-api-key", "proxy-authorization")

        /** Credentials never leave the device through the inspector. */
        fun redactHeader(name: String, value: String): String =
            if (name.lowercase() in secretHeaders) "‹redacted›" else value

        fun fromJson(json: JSONObject): NetworkRequestEntry = NetworkRequestEntry(
            method = json.optString("method", "GET").uppercase(),
            url = json.optString("url"),
            status = json.optInt("status", -1),
            statusText = json.optString("statusText"),
            type = json.optString("type", "xhr"),
            durationMs = json.optLong("duration"),
            sizeBytes = json.optLong("size", -1L),
            requestHeaders = json.optJSONObject("requestHeaders").toStringMap(),
            responseHeaders = json.optJSONObject("responseHeaders").toStringMap(),
            requestBody = json.optString("requestBody"),
            responseBody = json.optString("responseBody"),
            error = json.optString("error"),
        )
    }
}

/** One ancestor in the element's path, used for the breadcrumb and parent selection. */
data class ElementAncestor(val label: String, val depth: Int)

/** A DOM element picked in the page, with everything an agent needs to find and change it. */
data class InspectedElement(
    val pageUrl: String = "",
    val tagName: String,
    val id: String = "",
    val className: String = "",
    /** Unique CSS selector from the document root. */
    val selector: String = "",
    val xpath: String = "",
    val textContent: String = "",
    val attributes: Map<String, String> = emptyMap(),
    val computedStyles: Map<String, String> = emptyMap(),
    /** x, y, width, height in CSS pixels relative to the viewport. */
    val rect: List<Int> = emptyList(),
    val boxModel: Map<String, String> = emptyMap(),
    val outerHtml: String = "",
    val ancestors: List<ElementAncestor> = emptyList(),
    val childCount: Int = 0,
    /** React/Vue/Svelte component and source file hints (dev builds only). */
    val componentName: String = "",
    val sourceFile: String = "",
    val viewport: String = "",
) {
    val displaySelector: String
        get() {
            val tag = tagName.lowercase()
            val idPart = if (id.isNotBlank()) "#$id" else ""
            val classPart = className.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
                .take(3).joinToString("") { ".$it" }
            return "$tag$idPart$classPart"
        }

    val boundingBox: String
        get() = if (rect.size == 4) "${rect[2]}×${rect[3]} at (${rect[0]}, ${rect[1]})" else ""

    /** Context block for an agent, optionally with the user's instruction about this element. */
    fun toPromptContext(note: String = ""): String = buildString {
        if (note.isNotBlank()) {
            appendLine(note.trim())
            appendLine()
        }
        appendLine("### Element on ${pageUrl.ifBlank { "the page" }}")
        appendLine("- Selector: `${selector.ifBlank { displaySelector }}`")
        if (xpath.isNotBlank()) appendLine("- XPath: `$xpath`")
        if (componentName.isNotBlank()) appendLine("- Component: `$componentName`")
        if (sourceFile.isNotBlank()) appendLine("- Source: `$sourceFile`")
        if (boundingBox.isNotBlank()) appendLine("- Box: $boundingBox${if (viewport.isNotBlank()) " in viewport $viewport" else ""}")
        if (textContent.isNotBlank()) appendLine("- Text: \"${textContent.take(160)}\"")
        if (ancestors.isNotEmpty()) appendLine("- Path: ${ancestors.joinToString(" › ") { it.label }} › ${displaySelector}")
        val keyAttributes = attributes.filterKeys { it != "class" && it != "style" && it != "id" }
        if (keyAttributes.isNotEmpty()) {
            appendLine("- Attributes: " + keyAttributes.entries.take(12).joinToString(", ") { "${it.key}=\"${it.value.take(80)}\"" })
        }
        if (computedStyles.isNotEmpty()) {
            appendLine("- Computed styles: " + computedStyles.entries.joinToString("; ") { "${it.key}: ${it.value}" })
        }
        if (outerHtml.isNotBlank()) appendLine("\n```html\n${outerHtml.take(1_500)}\n```")
    }

    /** Clipboard format: the same context, self-contained for pasting anywhere. */
    fun toClipboardText(): String = toPromptContext()

    companion object {
        fun fromJson(json: JSONObject): InspectedElement = InspectedElement(
            pageUrl = json.optString("pageUrl"),
            tagName = json.optString("tagName", "div"),
            id = json.optString("id"),
            className = json.optString("className"),
            selector = json.optString("selector"),
            xpath = json.optString("xpath"),
            textContent = json.optString("text"),
            attributes = json.optJSONObject("attributes").toStringMap(),
            computedStyles = json.optJSONObject("styles").toStringMap(),
            rect = json.optJSONArray("rect")?.let { a -> (0 until a.length()).map { a.optInt(it) } }.orEmpty(),
            boxModel = json.optJSONObject("box").toStringMap(),
            outerHtml = json.optString("outerHtml"),
            ancestors = json.optJSONArray("ancestors")?.let { a ->
                (0 until a.length()).map { ElementAncestor(a.optString(it), a.length() - it) }
            }.orEmpty(),
            childCount = json.optInt("childCount"),
            componentName = json.optString("component"),
            sourceFile = json.optString("source"),
            viewport = json.optString("viewport"),
        )
    }
}

internal fun JSONObject?.toStringMap(): Map<String, String> {
    if (this == null) return emptyMap()
    val map = LinkedHashMap<String, String>()
    keys().forEach { key -> map[key] = optString(key) }
    return map
}
