package com.jarves.mh.orchestrator

import com.jarves.mh.model.AgentRole
import com.jarves.mh.model.BlackboardEntry
import com.jarves.mh.model.MemoryGraphNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Shared Project Memory & Blackboard inspired by Munder-Difflin.
 * Enables cross-agent communication, architectural decisions, and graph memory.
 */
class BlackboardMemoryStore {

    private val entries = mutableMapOf<String, BlackboardEntry>()
    private val graphNodes = mutableMapOf<String, MemoryGraphNode>()

    fun getAllEntries(): List<BlackboardEntry> = entries.values.sortedByDescending { it.timestamp }

    fun getEntriesByCategory(category: String): List<BlackboardEntry> =
        entries.values.filter { it.category.equals(category, ignoreCase = true) }.sortedByDescending { it.timestamp }

    fun getEntry(key: String): BlackboardEntry? = entries[key]

    fun putEntry(
        key: String,
        category: String,
        value: String,
        authorAgentId: String,
        authorRole: AgentRole,
        confidence: Float = 1.0f,
    ): BlackboardEntry {
        val entry = BlackboardEntry(
            key = key,
            category = category.uppercase(),
            value = value,
            authorAgentId = authorAgentId,
            authorRole = authorRole,
            timestamp = System.currentTimeMillis(),
            confidence = confidence,
        )
        entries[key] = entry
        return entry
    }

    fun removeEntry(key: String): Boolean {
        return entries.remove(key) != null
    }

    fun clear() {
        entries.clear()
        graphNodes.clear()
    }

    fun getAllGraphNodes(): List<MemoryGraphNode> = graphNodes.values.toList()

    fun addGraphNode(node: MemoryGraphNode) {
        graphNodes[node.id] = node
    }

    suspend fun saveToFile(projectDir: File) = withContext(Dispatchers.IO) {
        try {
            val pocketDevDir = File(projectDir, ".pocketdev")
            if (!pocketDevDir.exists()) pocketDevDir.mkdirs()
            val file = File(pocketDevDir, "blackboard.json")

            val rootObj = JSONObject()
            val entriesArray = JSONArray()
            entries.values.forEach { entry ->
                val obj = JSONObject().apply {
                    put("id", entry.id)
                    put("key", entry.key)
                    put("category", entry.category)
                    put("value", entry.value)
                    put("authorAgentId", entry.authorAgentId)
                    put("authorRole", entry.authorRole.name)
                    put("timestamp", entry.timestamp)
                    put("confidence", entry.confidence.toDouble())
                }
                entriesArray.put(obj)
            }
            rootObj.put("entries", entriesArray)

            val nodesArray = JSONArray()
            graphNodes.values.forEach { node ->
                val nodeObj = JSONObject().apply {
                    put("id", node.id)
                    put("label", node.label)
                    put("type", node.type)
                    val propsObj = JSONObject()
                    node.properties.forEach { (k, v) -> propsObj.put(k, v) }
                    put("properties", propsObj)
                    val connArray = JSONArray()
                    node.connectedNodeIds.forEach { connArray.put(it) }
                    put("connectedNodeIds", connArray)
                }
                nodesArray.put(nodeObj)
            }
            rootObj.put("nodes", nodesArray)

            file.writeText(rootObj.toString(2))
        } catch (_: Exception) {
            // Ignore persistence errors gracefully
        }
    }

    suspend fun loadFromFile(projectDir: File) = withContext(Dispatchers.IO) {
        try {
            val file = File(File(projectDir, ".pocketdev"), "blackboard.json")
            if (!file.exists()) return@withContext

            val text = file.readText()
            val rootObj = JSONObject(text)

            entries.clear()
            val entriesArray = rootObj.optJSONArray("entries") ?: JSONArray()
            for (i in 0 until entriesArray.length()) {
                val obj = entriesArray.getJSONObject(i)
                val roleName = obj.optString("authorRole", AgentRole.GOD_ORCHESTRATOR.name)
                val role = try { AgentRole.valueOf(roleName) } catch (_: Exception) { AgentRole.GOD_ORCHESTRATOR }
                val entry = BlackboardEntry(
                    id = obj.optString("id"),
                    key = obj.getString("key"),
                    category = obj.optString("category", "GENERAL"),
                    value = obj.getString("value"),
                    authorAgentId = obj.optString("authorAgentId", "system"),
                    authorRole = role,
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    confidence = obj.optDouble("confidence", 1.0).toFloat(),
                )
                entries[entry.key] = entry
            }

            graphNodes.clear()
            val nodesArray = rootObj.optJSONArray("nodes") ?: JSONArray()
            for (i in 0 until nodesArray.length()) {
                val obj = nodesArray.getJSONObject(i)
                val props = mutableMapOf<String, String>()
                val propsObj = obj.optJSONObject("properties")
                propsObj?.keys()?.forEach { k -> props[k] = propsObj.optString(k) }

                val conns = mutableListOf<String>()
                val connsArray = obj.optJSONArray("connectedNodeIds")
                if (connsArray != null) {
                    for (c in 0 until connsArray.length()) {
                        conns.add(connsArray.getString(c))
                    }
                }

                val node = MemoryGraphNode(
                    id = obj.optString("id"),
                    label = obj.optString("label"),
                    type = obj.optString("type"),
                    properties = props,
                    connectedNodeIds = conns,
                )
                graphNodes[node.id] = node
            }
        } catch (_: Exception) {
            // If corrupt, fallback to clean state
        }
    }
}
