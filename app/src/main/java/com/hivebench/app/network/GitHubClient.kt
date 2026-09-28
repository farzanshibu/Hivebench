package com.hivebench.app.network

import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

data class GitHubDeviceCode(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val expiresInSeconds: Long,
    val intervalSeconds: Long,
)

data class GitHubAccount(val login: String, val avatarUrl: String)

data class GitHubRepository(
    val fullName: String,
    val cloneUrl: String,
    val private: Boolean,
    val defaultBranch: String,
    val description: String,
    val updatedAt: String,
)

data class GitHubIssue(
    val id: Long = 0L,
    val number: Int,
    val title: String,
    val body: String = "",
    val state: String = "open",
    val htmlUrl: String = "",
    val author: String = "",
    val authorAvatarUrl: String = "",
    val labels: List<String> = emptyList(),
    val assignee: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
)

sealed interface GitHubTokenPoll {
    data class Success(val accessToken: String) : GitHubTokenPoll
    data class Pending(val slowDown: Boolean = false) : GitHubTokenPoll
    data class Failure(val message: String) : GitHubTokenPoll
}

class GitHubClient {
    fun requestDeviceCode(clientId: String): GitHubDeviceCode {
        val json = postForm(
            "https://github.com/login/device/code",
            mapOf("client_id" to clientId),
        )
        return GitHubDeviceCode(
            deviceCode = json.getString("device_code"),
            userCode = json.getString("user_code"),
            verificationUri = json.optString("verification_uri", "https://github.com/login/device"),
            expiresInSeconds = json.optLong("expires_in", 900L),
            intervalSeconds = json.optLong("interval", 5L).coerceAtLeast(5L),
        )
    }

    fun pollDeviceToken(clientId: String, deviceCode: String): GitHubTokenPoll {
        val json = postForm(
            "https://github.com/login/oauth/access_token",
            mapOf(
                "client_id" to clientId,
                "device_code" to deviceCode,
                "grant_type" to "urn:ietf:params:oauth:grant-type:device_code",
            ),
        )
        json.optString("access_token").takeIf(String::isNotBlank)?.let { return GitHubTokenPoll.Success(it) }
        return when (val error = json.optString("error")) {
            "authorization_pending" -> GitHubTokenPoll.Pending()
            "slow_down" -> GitHubTokenPoll.Pending(slowDown = true)
            "access_denied" -> GitHubTokenPoll.Failure("GitHub authorization was cancelled")
            "expired_token" -> GitHubTokenPoll.Failure("The GitHub sign-in code expired. Try again.")
            else -> GitHubTokenPoll.Failure(json.optString("error_description").ifBlank { error.ifBlank { "GitHub sign-in failed" } })
        }
    }

    fun account(token: String): GitHubAccount {
        val json = getJson("https://api.github.com/user", token) as JSONObject
        return GitHubAccount(json.getString("login"), json.optString("avatar_url"))
    }

    fun repositories(token: String): List<GitHubRepository> {
        val result = LinkedHashMap<String, GitHubRepository>()
        var page = 1
        while (page <= 10) {
            val array = getJson(
                "https://api.github.com/user/repos?visibility=all&affiliation=owner,collaborator,organization_member&sort=updated&per_page=100&page=$page",
                token,
            ) as JSONArray
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val repo = GitHubRepository(
                    fullName = item.getString("full_name"),
                    cloneUrl = item.getString("clone_url"),
                    private = item.optBoolean("private"),
                    defaultBranch = item.optString("default_branch", "main"),
                    description = item.optString("description"),
                    updatedAt = item.optString("updated_at"),
                )
                result[repo.fullName] = repo
            }
            if (array.length() < 100) break
            page++
        }
        return result.values.toList()
    }

    fun listBranches(token: String, repoFullName: String): List<String> {
        val array = getJson("https://api.github.com/repos/$repoFullName/branches?per_page=100", token) as JSONArray
        return (0 until array.length()).map { index ->
            array.getJSONObject(index).getString("name")
        }
    }

    fun listPullRequests(token: String, repoFullName: String, state: String = "open"): List<com.hivebench.app.model.GitPullRequest> {
        val array = getJson("https://api.github.com/repos/$repoFullName/pulls?state=$state&per_page=50", token) as JSONArray
        return (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            com.hivebench.app.model.GitPullRequest(
                number = item.getInt("number"),
                title = item.getString("title"),
                body = item.optString("body", ""),
                headBranch = item.getJSONObject("head").getString("ref"),
                baseBranch = item.getJSONObject("base").getString("ref"),
                htmlUrl = item.getString("html_url"),
                state = item.optString("state", "open"),
                author = item.optJSONObject("user")?.optString("login", "").orEmpty(),
                createdAt = item.optString("created_at", ""),
            )
        }
    }

    fun listIssues(token: String, repoFullName: String, state: String = "open"): List<GitHubIssue> {
        val array = getJson("https://api.github.com/repos/$repoFullName/issues?state=$state&per_page=50", token) as JSONArray
        val issues = mutableListOf<GitHubIssue>()
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            if (item.has("pull_request")) continue

            val labelsArray = item.optJSONArray("labels")
            val labelsList = mutableListOf<String>()
            if (labelsArray != null) {
                for (j in 0 until labelsArray.length()) {
                    val labelObj = labelsArray.optJSONObject(j)
                    val labelName = labelObj?.optString("name") ?: labelsArray.optString(j)
                    if (labelName.isNotBlank()) labelsList.add(labelName)
                }
            }

            issues.add(
                GitHubIssue(
                    id = item.optLong("id", 0L),
                    number = item.getInt("number"),
                    title = item.getString("title"),
                    // A JSON null body would otherwise become the literal text "null".
                    body = if (item.isNull("body")) "" else item.optString("body", ""),
                    state = item.optString("state", "open"),
                    htmlUrl = item.optString("html_url", ""),
                    author = item.optJSONObject("user")?.optString("login", "").orEmpty(),
                    authorAvatarUrl = item.optJSONObject("user")?.optString("avatar_url", "").orEmpty(),
                    labels = labelsList,
                    assignee = item.optJSONObject("assignee")?.optString("login"),
                    createdAt = item.optString("created_at", ""),
                    updatedAt = item.optString("updated_at", ""),
                )
            )
        }
        return issues
    }

    fun createPullRequest(
        token: String,
        repoFullName: String,
        title: String,
        body: String,
        head: String,
        base: String,
    ): com.hivebench.app.model.GitPullRequest {
        val payload = JSONObject().apply {
            put("title", title)
            put("body", body)
            put("head", head)
            put("base", base)
        }
        val json = postJson("https://api.github.com/repos/$repoFullName/pulls", payload, token) as JSONObject
        return com.hivebench.app.model.GitPullRequest(
            number = json.getInt("number"),
            title = json.getString("title"),
            body = json.optString("body", ""),
            headBranch = json.getJSONObject("head").getString("ref"),
            baseBranch = json.getJSONObject("base").getString("ref"),
            htmlUrl = json.getString("html_url"),
            state = json.optString("state", "open"),
            author = json.optJSONObject("user")?.optString("login", "").orEmpty(),
            createdAt = json.optString("created_at", ""),
        )
    }

    private fun postJson(endpoint: String, payload: JSONObject, token: String): Any {
        val body = payload.toString().toByteArray(Charsets.UTF_8)
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 25_000
            doOutput = true
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("X-GitHub-Api-Version", "2026-03-10")
            setRequestProperty("User-Agent", "Hivebench-Android")
        }
        connection.outputStream.use { it.write(body) }
        return readResponse(connection)
    }

    private fun postForm(endpoint: String, values: Map<String, String>): JSONObject {
        val body = values.entries.joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}" 
        }.toByteArray()
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            setRequestProperty("User-Agent", "Hivebench-Android")
        }
        connection.outputStream.use { it.write(body) }
        return readResponse(connection) as JSONObject
    }

    private fun getJson(endpoint: String, token: String): Any {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("X-GitHub-Api-Version", "2026-03-10")
            setRequestProperty("User-Agent", "Hivebench-Android")
        }
        return readResponse(connection)
    }

    private fun readResponse(connection: HttpURLConnection): Any {
        val status = connection.responseCode
        val text = (if (status in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (status !in 200..299) {
            val message = runCatching { JSONObject(text).optString("message") }.getOrNull()
            error(message?.takeIf(String::isNotBlank) ?: "GitHub returned HTTP $status")
        }
        return if (text.trimStart().startsWith("[")) JSONArray(text) else JSONObject(text)
    }
}

