package com.jarves.mh.orchestrator

import com.jarves.mh.model.AgentInboxMessage
import com.jarves.mh.model.AgentMessageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Agent-to-Agent Inbox & Messaging System.
 * Supports task delegation, handoffs, inquiries, and notifications.
 */
class AgentInboxManager {

    private val messages = mutableListOf<AgentInboxMessage>()

    fun getAllMessages(): List<AgentInboxMessage> = messages.toList().sortedByDescending { it.timestamp }

    fun getInboxForAgent(agentId: String): List<AgentInboxMessage> =
        messages.filter { it.toAgentId == agentId }.sortedByDescending { it.timestamp }

    fun getOutboxForAgent(agentId: String): List<AgentInboxMessage> =
        messages.filter { it.fromAgentId == agentId }.sortedByDescending { it.timestamp }

    fun getUnreadCount(agentId: String): Int =
        messages.count { it.toAgentId == agentId && !it.isRead }

    fun sendMessage(
        fromAgentId: String,
        toAgentId: String,
        type: AgentMessageType,
        subject: String,
        content: String,
        taskId: String? = null,
    ): AgentInboxMessage {
        val message = AgentInboxMessage(
            id = UUID.randomUUID().toString(),
            fromAgentId = fromAgentId,
            toAgentId = toAgentId,
            type = type,
            subject = subject,
            content = content,
            taskId = taskId,
            timestamp = System.currentTimeMillis(),
            isRead = false,
        )
        messages.add(0, message)
        return message
    }

    fun markAsRead(messageId: String) {
        val index = messages.indexOfFirst { it.id == messageId }
        if (index != -1) {
            messages[index] = messages[index].copy(isRead = true)
        }
    }

    fun markAllAsReadForAgent(agentId: String) {
        for (i in messages.indices) {
            if (messages[i].toAgentId == agentId) {
                messages[i] = messages[i].copy(isRead = true)
            }
        }
    }

    fun clear() {
        messages.clear()
    }

    suspend fun saveToFile(projectDir: File) = withContext(Dispatchers.IO) {
        try {
            val dir = File(projectDir, ".pocketdev")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "inbox.json")

            val array = JSONArray()
            messages.forEach { msg ->
                val obj = JSONObject().apply {
                    put("id", msg.id)
                    put("fromAgentId", msg.fromAgentId)
                    put("toAgentId", msg.toAgentId)
                    put("type", msg.type.name)
                    put("subject", msg.subject)
                    put("content", msg.content)
                    if (msg.taskId != null) put("taskId", msg.taskId)
                    put("timestamp", msg.timestamp)
                    put("isRead", msg.isRead)
                }
                array.put(obj)
            }
            file.writeText(array.toString(2))
        } catch (_: Exception) {
            // Ignore persistence errors gracefully
        }
    }

    suspend fun loadFromFile(projectDir: File) = withContext(Dispatchers.IO) {
        try {
            val file = File(File(projectDir, ".pocketdev"), "inbox.json")
            if (!file.exists()) return@withContext

            val text = file.readText()
            val array = JSONArray(text)
            messages.clear()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val typeName = obj.optString("type", AgentMessageType.DELEGATION.name)
                val type = try { AgentMessageType.valueOf(typeName) } catch (_: Exception) { AgentMessageType.DELEGATION }
                val msg = AgentInboxMessage(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    fromAgentId = obj.getString("fromAgentId"),
                    toAgentId = obj.getString("toAgentId"),
                    type = type,
                    subject = obj.optString("subject", "Message"),
                    content = obj.optString("content", ""),
                    taskId = if (obj.has("taskId")) obj.getString("taskId") else null,
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    isRead = obj.optBoolean("isRead", false),
                )
                messages.add(msg)
            }
        } catch (_: Exception) {
            // Fallback gracefully
        }
    }
}
