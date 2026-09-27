package com.jarves.mh.skills

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Registry managing built-in and user-defined agent skills.
 * Persists customized skills to .pocketdev/skills.json.
 */
class SkillRegistry(private val projectDir: File) {

    private val skillsFile: File
        get() {
            val pocketDevDir = File(projectDir, ".pocketdev").apply { if (!exists()) mkdirs() }
            return File(pocketDevDir, "skills.json")
        }

    private val builtinSkills = listOf(
        SkillDefinition(
            id = "skill-security-audit",
            name = "Security & Vulnerability Audit",
            description = "Inspect code for hardcoded secrets, injection vectors, unsafe reflection, and permission bypasses.",
            category = SkillCategory.SECURITY,
            instructions = "Perform rigorous OWASP mobile security analysis. Flag any credentials, plaintext tokens, unvalidated intents, or unsafe external storage operations. Recommend secure alternatives.",
            requiredTools = listOf("filesystem", "terminal", "git"),
            enabled = true,
            version = "1.2.0",
        ),
        SkillDefinition(
            id = "skill-android-arch",
            name = "Modern Android & Jetpack Compose Review",
            description = "Enforces unidirectional data flow (UDF), state hoisting, coroutine lifecycle scoping, and Neobrutal styling conventions.",
            category = SkillCategory.ARCHITECTURE,
            instructions = "Audit Compose hierarchy for unnecessary recompositions, remember/derivedStateOf correctness, and Material3 compliance. Ensure state is hoisted to ViewModel.",
            requiredTools = listOf("filesystem"),
            enabled = true,
            version = "1.0.0",
        ),
        SkillDefinition(
            id = "skill-test-generator",
            name = "JUnit & UI Test Generation",
            description = "Generates comprehensive unit tests, edge-case mocks, and assertion suites for Kotlin components.",
            category = SkillCategory.TESTING,
            instructions = "Generate JUnit4/JUnit5 unit tests with MockK or Fake implementations. Cover boundary conditions, nullability, coroutine cancellation, and error branches.",
            requiredTools = listOf("filesystem", "terminal"),
            enabled = true,
            version = "1.1.0",
        ),
        SkillDefinition(
            id = "skill-docker-compose",
            name = "Docker & Containerization",
            description = "Creates Dockerfiles, docker-compose.yml setups, and multi-stage container build recipes for backend and dev services.",
            category = SkillCategory.DEVOPS,
            instructions = "Generate lean, production-ready multi-stage Dockerfiles. Configure volume mounts, environment variables, healthchecks, and non-root users.",
            requiredTools = listOf("filesystem", "terminal", "docker"),
            enabled = true,
            version = "1.0.0",
        ),
        SkillDefinition(
            id = "skill-api-investigator",
            name = "REST & GraphQL API Investigator",
            description = "Diagnoses network endpoints, request headers, payload serialization, and mock server responses.",
            category = SkillCategory.API_INTEGRATION,
            instructions = "Examine network request payloads, response status codes, JSON schema models, and authentication headers. Provide curl reproductions for failures.",
            requiredTools = listOf("terminal", "browser"),
            enabled = true,
            version = "1.0.0",
        ),
        SkillDefinition(
            id = "skill-db-optimizer",
            name = "Database & Query Optimizer",
            description = "Analyzes Room/SQLite schemas, foreign keys, indexing, and transactional integrity.",
            category = SkillCategory.DATABASE,
            instructions = "Inspect SQL queries, indexing on foreign keys, transaction boundaries, and schema migrations. Prevent main-thread database I/O.",
            requiredTools = listOf("filesystem"),
            enabled = true,
            version = "1.0.0",
        ),
        SkillDefinition(
            id = "skill-browser-automation",
            name = "Web & Mobile Browser Automation",
            description = "Uses WebView DevTools, DOM evaluation, and CSS selector inspection to test web interfaces.",
            category = SkillCategory.DEBUGGING,
            instructions = "Execute client-side DOM queries, capture console warnings/errors, monitor network latency, and extract layout bounding boxes.",
            requiredTools = listOf("browser", "terminal"),
            enabled = true,
            version = "1.1.0",
        ),
    )

    private val customSkills = mutableListOf<SkillDefinition>()
    private val disabledBuiltinIds = mutableSetOf<String>()

    init {
        loadSkills()
    }

    @Synchronized
    fun getAllSkills(): List<SkillDefinition> {
        val builtins = builtinSkills.map { skill ->
            skill.copy(enabled = !disabledBuiltinIds.contains(skill.id))
        }
        return builtins + customSkills
    }

    @Synchronized
    fun getSkill(id: String): SkillDefinition? {
        return getAllSkills().firstOrNull { it.id == id }
    }

    @Synchronized
    fun toggleSkill(id: String): Boolean {
        val builtin = builtinSkills.firstOrNull { it.id == id }
        if (builtin != null) {
            if (disabledBuiltinIds.contains(id)) {
                disabledBuiltinIds.remove(id)
            } else {
                disabledBuiltinIds.add(id)
            }
            saveSkills()
            return !disabledBuiltinIds.contains(id)
        }

        val idx = customSkills.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val updated = customSkills[idx].copy(enabled = !customSkills[idx].enabled)
            customSkills[idx] = updated
            saveSkills()
            return updated.enabled
        }
        return false
    }

    @Synchronized
    fun addCustomSkill(
        name: String,
        description: String,
        category: SkillCategory,
        instructions: String,
        requiredTools: List<String> = emptyList(),
    ): SkillDefinition {
        val skill = SkillDefinition(
            name = name,
            description = description,
            category = category,
            instructions = instructions,
            requiredTools = requiredTools,
            enabled = true,
            version = "1.0.0",
            author = "User Custom",
        )
        customSkills.add(skill)
        saveSkills()
        return skill
    }

    @Synchronized
    fun deleteCustomSkill(id: String): Boolean {
        val removed = customSkills.removeAll { it.id == id }
        if (removed) {
            saveSkills()
        }
        return removed
    }

    @Synchronized
    private fun saveSkills() {
        try {
            val root = JSONObject()
            val disabledArray = JSONArray()
            disabledBuiltinIds.forEach { disabledArray.put(it) }
            root.put("disabledBuiltins", disabledArray)

            val customArray = JSONArray()
            customSkills.forEach { skill ->
                val obj = JSONObject()
                obj.put("id", skill.id)
                obj.put("name", skill.name)
                obj.put("description", skill.description)
                obj.put("category", skill.category.name)
                obj.put("instructions", skill.instructions)
                val toolsArr = JSONArray()
                skill.requiredTools.forEach { toolsArr.put(it) }
                obj.put("requiredTools", toolsArr)
                obj.put("enabled", skill.enabled)
                obj.put("version", skill.version)
                obj.put("author", skill.author)
                customArray.put(obj)
            }
            root.put("customSkills", customArray)

            skillsFile.writeText(root.toString(2))
        } catch (_: Exception) {}
    }

    @Synchronized
    private fun loadSkills() {
        if (!skillsFile.exists()) return
        try {
            val text = skillsFile.readText()
            val root = JSONObject(text)

            disabledBuiltinIds.clear()
            val disabledArray = root.optJSONArray("disabledBuiltins") ?: JSONArray()
            for (i in 0 until disabledArray.length()) {
                disabledBuiltinIds.add(disabledArray.getString(i))
            }

            customSkills.clear()
            val customArray = root.optJSONArray("customSkills") ?: JSONArray()
            for (i in 0 until customArray.length()) {
                val obj = customArray.getJSONObject(i)
                val catStr = obj.optString("category", SkillCategory.DEBUGGING.name)
                val category = try { SkillCategory.valueOf(catStr) } catch (_: Exception) { SkillCategory.DEBUGGING }
                val toolsArr = obj.optJSONArray("requiredTools") ?: JSONArray()
                val tools = mutableListOf<String>()
                for (j in 0 until toolsArr.length()) {
                    tools.add(toolsArr.getString(j))
                }

                customSkills.add(
                    SkillDefinition(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        name = obj.optString("name", "Custom Skill"),
                        description = obj.optString("description", ""),
                        category = category,
                        instructions = obj.optString("instructions", ""),
                        requiredTools = tools,
                        enabled = obj.optBoolean("enabled", true),
                        version = obj.optString("version", "1.0.0"),
                        author = obj.optString("author", "User Custom"),
                    )
                )
            }
        } catch (_: Exception) {}
    }
}
