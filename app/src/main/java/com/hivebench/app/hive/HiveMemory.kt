package com.hivebench.app.hive

import kotlin.math.ln

/**
 * "What the floor knows": ranked search over tickets (task cards, archived
 * ones included), agents (roster, roles, what they are doing) and notes
 * (every agent's memory.md and the board, split into sections). Answers come
 * back in those three shapes.
 *
 * Runs fully on-device with BM25 over stemmed terms plus a small synonym map,
 * so it matches related wording, not only exact keywords. It is not an
 * embedding model.
 */
object HiveMemory {
    data class Doc(val kind: String, val ref: String, val title: String, val text: String)

    data class Results(val tickets: List<MemoryHit>, val agents: List<MemoryHit>, val notes: List<MemoryHit>) {
        val isEmpty: Boolean get() = tickets.isEmpty() && agents.isEmpty() && notes.isEmpty()
    }

    private val synonyms: Map<String, List<String>> = listOf(
        listOf("bug", "defect", "error", "crash", "failure", "issue", "broken"),
        listOf("test", "spec", "qa", "verify", "check", "assert"),
        listOf("ui", "screen", "view", "layout", "design", "frontend", "component"),
        listOf("api", "endpoint", "backend", "server", "route", "request"),
        listOf("auth", "login", "signin", "credential", "token", "session", "password"),
        listOf("db", "database", "sql", "schema", "migration", "table"),
        listOf("deploy", "release", "ship", "publish", "version"),
        listOf("doc", "readme", "documentation", "guide"),
        listOf("slow", "performance", "latency", "speed", "fast"),
        listOf("security", "secret", "vulnerability", "leak", "audit"),
        listOf("plan", "decision", "architecture", "approach"),
        listOf("cost", "spend", "budget", "token", "price"),
    ).flatMap { group -> group.map { stem(it) to group.map(::stem) } }
        .groupBy({ it.first }, { it.second }).mapValues { (_, v) -> v.flatten().distinct() }

    private val stop = setOf("the", "a", "an", "and", "or", "of", "to", "in", "on", "for", "is", "it", "with", "that", "this", "be", "as", "at", "by", "we", "i", "you", "what", "who", "how")

    private fun stem(word: String): String {
        var w = word.lowercase()
        for (suffix in listOf("ations", "ation", "ings", "ing", "edly", "ed", "ies", "es", "s", "ly")) {
            if (w.length > suffix.length + 2 && w.endsWith(suffix)) {
                w = w.dropLast(suffix.length) + if (suffix == "ies") "y" else ""
                break
            }
        }
        return w
    }

    private fun terms(text: String): List<String> =
        Regex("[A-Za-z0-9_]+").findAll(text).map { it.value.lowercase() }.filter { it !in stop && it.length > 1 }.map(::stem).toList()

    fun documents(snapshot: HiveSnapshot, memories: Map<String, String>, archived: List<org.json.JSONObject>): List<Doc> {
        val docs = mutableListOf<Doc>()
        snapshot.tasks.forEach { t ->
            docs += Doc("ticket", t.id, "${t.id} · ${t.title}", listOf(t.title, t.description, t.assignee.orEmpty(), t.column.title, t.humanQA.joinToString(" ") { it.q + " " + (it.a ?: "") }, t.result.orEmpty()).joinToString("\n"))
        }
        archived.forEach { o ->
            docs += Doc("ticket", o.optString("id"), "${o.optString("id")} · ${o.optString("title")} (archived)", o.optString("title") + "\n" + o.optString("description") + "\n" + o.optString("assignee"))
        }
        snapshot.agents.forEach { a ->
            val status = snapshot.status[a.id]
            docs += Doc("agent", a.id, "${a.name} · ${a.role}", listOf(a.name, a.id, a.role, a.cli.title, a.model, a.goal, status?.doing.orEmpty(), a.capabilities.skills.joinToString(" ")).joinToString("\n"))
        }
        memories.forEach { (agentId, text) ->
            sections(text).forEach { (heading, body) ->
                if (body.isNotBlank()) docs += Doc("note", agentId, "${snapshot.agents.firstOrNull { it.id == agentId }?.name ?: agentId} · $heading", body)
            }
        }
        sections(snapshot.board).forEach { (heading, body) -> if (body.isNotBlank()) docs += Doc("note", "board", "Board · $heading", body) }
        return docs
    }

    private fun sections(markdown: String): List<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>()
        var heading = "Notes"
        val buf = StringBuilder()
        fun flush() {
            // Long sections are split into paragraph-sized chunks so a hit points somewhere precise.
            buf.toString().split(Regex("\n\\s*\n")).map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("_Append") && !it.startsWith("_Shared") }
                .forEach { out += heading to it }
            buf.clear()
        }
        markdown.lines().forEach { line ->
            if (line.startsWith("#")) {
                flush()
                heading = line.trimStart('#').trim().ifBlank { heading }
            } else buf.appendLine(line)
        }
        flush()
        return out
    }

    fun search(query: String, docs: List<Doc>, perShape: Int = 6): Results {
        val q = terms(query).flatMap { t -> listOf(t) + (synonyms[t] ?: emptyList()) }.distinct()
        if (q.isEmpty() || docs.isEmpty()) return Results(emptyList(), emptyList(), emptyList())
        val direct = terms(query).toSet()
        val tokenized = docs.map { terms(it.title + "\n" + it.text) }
        val avgLen = tokenized.sumOf { it.size }.toDouble() / tokenized.size.coerceAtLeast(1)
        val df = HashMap<String, Int>()
        tokenized.forEach { tokens -> tokens.toSet().forEach { df[it] = (df[it] ?: 0) + 1 } }
        val n = docs.size.toDouble()
        val scored = docs.mapIndexed { i, doc ->
            val tokens = tokenized[i]
            val tf = tokens.groupingBy { it }.eachCount()
            var score = 0.0
            q.forEach { term ->
                val f = tf[term] ?: return@forEach
                val idf = ln(1 + (n - (df[term] ?: 0) + 0.5) / ((df[term] ?: 0) + 0.5))
                val weight = if (term in direct) 1.0 else 0.6 // synonyms count, a little less
                score += weight * idf * (f * 2.2) / (f + 1.2 * (0.25 + 0.75 * tokens.size / avgLen))
            }
            MemoryHit(doc.kind, doc.ref, doc.title, snippet(doc.text, q), score)
        }.filter { it.score > 0 }.sortedByDescending { it.score }
        return Results(
            tickets = scored.filter { it.kind == "ticket" }.distinctBy { it.ref }.take(perShape),
            agents = scored.filter { it.kind == "agent" }.take(perShape),
            notes = scored.filter { it.kind == "note" }.take(perShape),
        )
    }

    private fun snippet(text: String, q: List<String>): String {
        val lines = text.lines().filter { it.isNotBlank() }
        val best = lines.maxByOrNull { line -> terms(line).count { it in q } } ?: return text.take(160)
        return best.trim().take(200)
    }
}
