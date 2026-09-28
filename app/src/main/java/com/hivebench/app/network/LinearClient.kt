package com.hivebench.app.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Client for Linear GraphQL API (https://api.linear.app/graphql).
 */
class LinearClient {

    companion object {
        private const val GRAPHQL_ENDPOINT = "https://api.linear.app/graphql"
    }

    suspend fun testConnection(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val query = "{ viewer { id name email organization { name } } }"
            val response = executeGraphQL(apiKey, query)
            val data = response.optJSONObject("data") ?: throw IllegalStateException("Invalid response from Linear")
            val viewer = data.optJSONObject("viewer") ?: throw IllegalStateException("Could not resolve Linear user")
            val org = viewer.optJSONObject("organization")?.optString("name") ?: "Linear Workspace"
            "${viewer.optString("name")} ($org)"
        }
    }

    suspend fun fetchAssignedIssues(apiKey: String): Result<List<LinearIssue>> = withContext(Dispatchers.IO) {
        runCatching {
            val query = """
                {
                    viewer {
                    assignedIssues(first: 50, orderBy: updatedAt, filter: { state: { type: { nin: ["completed", "canceled"] } } }) {
                        nodes {
                            id
                            identifier
                            title
                            description
                            priority
                            url
                            branchName
                            state { name type }
                            assignee { name }
                            createdAt
                        }
                    }
                    }
                }
            """.trimIndent()

            val response = executeGraphQL(apiKey, query)
            // Only issues assigned to the key's owner, not every open issue in the workspace.
            val nodes = response.optJSONObject("data")
                ?.optJSONObject("viewer")
                ?.optJSONObject("assignedIssues")
                ?.optJSONArray("nodes") ?: JSONArray()

            val issues = mutableListOf<LinearIssue>()
            for (i in 0 until nodes.length()) {
                val node = nodes.getJSONObject(i)
                val stateObj = node.optJSONObject("state")
                val assigneeObj = node.optJSONObject("assignee")

                issues.add(
                    LinearIssue(
                        id = node.getString("id"),
                        identifier = node.getString("identifier"),
                        title = node.getString("title"),
                        description = if (node.isNull("description")) "" else node.optString("description", ""),
                        priority = node.optInt("priority", 0),
                        stateName = stateObj?.optString("name") ?: "Todo",
                        stateType = stateObj?.optString("type") ?: "unstarted",
                        assigneeName = assigneeObj?.optString("name"),
                        branchName = node.optString("branchName").takeIf { it.isNotBlank() },
                        url = node.optString("url", ""),
                        createdAt = node.optString("createdAt", ""),
                    )
                )
            }
            issues
        }
    }

    suspend fun postIssueComment(apiKey: String, issueId: String, body: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val escapedBody = JSONObject.quote(body)
            val mutation = """
                mutation {
                    commentCreate(input: { issueId: "$issueId", body: $escapedBody }) {
                        success
                    }
                }
            """.trimIndent()

            val response = executeGraphQL(apiKey, mutation)
            val success = response.optJSONObject("data")
                ?.optJSONObject("commentCreate")
                ?.optBoolean("success") ?: false
            success
        }
    }

    private fun executeGraphQL(apiKey: String, query: String): JSONObject {
        val url = URL(GRAPHQL_ENDPOINT)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", apiKey.trim())
            connectTimeout = 15000
            readTimeout = 20000
            doOutput = true
        }

        val requestBody = JSONObject().apply {
            put("query", query)
        }

        OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
            writer.write(requestBody.toString())
            writer.flush()
        }

        val responseCode = conn.responseCode
        val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
        val responseText = stream?.bufferedReader()?.use { it.readText() } ?: "{}"

        if (responseCode !in 200..299) {
            throw IllegalStateException("Linear API error ($responseCode): $responseText")
        }

        return JSONObject(responseText)
    }
}
