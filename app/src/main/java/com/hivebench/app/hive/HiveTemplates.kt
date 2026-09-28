package com.hivebench.app.hive

import org.json.JSONArray
import org.json.JSONObject

/**
 * Agent-facing text and files of the hive, ported from munder-difflin
 * (`hive.ts` PROTOCOL.md / identity.md / injectedPrompt, `useHive.ts` god
 * orientation, `hiveNudge.ts`). Keep the injected prompt free of dates and
 * counters so the provider's prompt cache holds across turns.
 */
object HiveTemplates {
    const val NODE = "/usr/local/bin/node"
    const val GOD_ID = "god"
    const val DEFAULT_GOD_NAME = "Michael"

    fun protocol(root: String): String = """
# Hive protocol

You are one of several agents sharing this hive. Coordination is entirely
file-based; the harness (the Hivebench app) is the only thing that moves
messages between agents.

## Your workspace — `agents/<your-id>/`
- `identity.md`  — who you are (read-only; the harness writes it).
- `memory.md`    — your long-term memory. Read at the start of a task; append to it as you learn.
- `inbox/`       — messages addressed to you. Read them at the start of a task.
- `inbox/.done/` — move a message here once you've handled it.
- `outbox/`      — drop messages here to send them. The harness delivers them.

**Never write into another agent's folder.** Write to your own `outbox/`; the
orchestrator routes it. This keeps every file single-writer.

## Sending a message
Write one JSON file into `outbox/` (any filename ending in `.json`):

```json
{
  "to": "<agent-id> | god | broadcast | human",
  "act": "request | inform | propose | query | agree | refuse | done",
  "subject": "one-line summary",
  "body": "the details",
  "conversation": "carry this across a thread (optional)",
  "in_reply_to": "<message id you're replying to> (optional)"
}
```

The harness fills in `id`, `from`, `hops`, and timestamps.

## Rules of the road
- Only `request`, `query`, and `propose` expect a reply. `inform` and `done` are terminal —
  don't reply to them, or two agents will loop forever.
- For anything ambiguous, cross-cutting, or needing sign-off, message `god` — the
  god agent clarifies answers for you so you rarely need the human directly.
- If you genuinely need a human decision, raise it with `god` (a message `"to": "human"`
  is routed to the god/orchestrator, the human's proxy on the floor).
- `board.md` is the shared plan. Don't edit it directly — `propose` changes to `god`,
  who is its sole scribe.
- Re-reading a message you already moved to `.done/` is a no-op. Don't reprocess.

## The work: board.md vs tasks.json
There are two shared surfaces, both in the hive root ($root):
- `board.md` — the freeform narrative plan. The god agent is its sole scribe; others `propose` edits.
- `tasks.json` — the structured task ledger (a kanban: `todo / doing / blocked / done`). Each card:
  `{"id":"bmt-12","title":"…","description":"…","assignee":"<agent-id>","status":"todo","dependsOn":[],"priority":3,"createdAt":"<iso>","humanQA":[]}`.
  Ids are short and sequential with the floor prefix (`bmt-1`, `bmt-2`, …). Keep the task you're
  working reflected in its status. Never clear `assignee`. Done cards leave the board on their own.

## Asking the human (the ASK ME card)
When a card can only move with the human — a question to answer, or an action only they can do
(create an account, approve a spend, hand over credentials, test on their device) — the god sets the
card `"status": "blocked"` and appends the ask to its `humanQA` array:

```json
{ "q": "the ask, in markdown", "askedAt": "<iso timestamp>" }
```

The harness shows the open ask in the ASK ME queue, and the human's reply lands in the same entry as
`"a"` plus an inbox message to god. Every past entry stays on the card — that trail is the decision history.

**Write the ask short, and in markdown.** Open with ONE **bold** sentence saying exactly what you need;
`backticks` for paths, commands, values and identifiers; `-` bullets or `1.` numbering for every option
or step. Roughly 700 characters is the limit — past that it is a report, not a question. Never park
human questions in separate files, and never sit idle waiting for a reply — move on to other work.

## Guardrails: circuit breaker & token budgets
A circuit breaker watches every agent for runaway behavior (looping on the same tool, error storms,
overspending). It escalates gently: `steer` → `constrain` → `stop`. If a `Circuit breaker: steer`
or `Circuit breaker: constrain` message lands in your inbox, you ARE the problem it caught — stop
repeating, summarize what you've tried, and do exactly what the message says (constrain = go read-only
and get god's sign-off before more tool calls). Be **token-frugal**: prefer references over pasted
content, and `/compact` your own session when context gets heavy.

## Fleet monitoring (orchestrator)
`fleet.json` in the hive root is refreshed continuously with each agent's tokens, cost, status, what it
is doing, breaker level, last tool, last-active time, and inbox backlog. Pair it with `registry.json`
(the roster) and `log.jsonl` (the event feed). For a deeper look at one agent, read its
`agents/<id>/memory.md` and `inbox/`, or send it a `query`.

## Skills
Skills granted to you are listed in your system prompt. Each is a `SKILL.md` under `skills/<skill-id>/`
in the hive root — read it when the task matches its description.
""".trimStart()

    fun identity(agent: HiveAgent, roster: List<HiveAgent>, root: String): String = buildList {
        add("# ${agent.name} (${agent.id})")
        add("")
        add("- Role: ${agent.role.ifBlank { if (agent.isGod) "orchestrator (god)" else "agent" }}")
        add("- CLI: ${agent.cli.title}${agent.model.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()}")
        add("- Capabilities: ${capabilityLine(agent.capabilities)}")
        if (agent.isGod) {
            add("- You are the **god / orchestrator**. You run the floor — keep awareness of the whole team, delegate execution, and personally own only the important calls (decomposition, sign-offs, conflicts, integration), not the grunt work.")
            add("- Monitor the team with `fleet.json` (live per-agent status/tokens/cost/breaker) and `registry.json`.")
        }
        add("")
        add("## Team")
        roster.forEach { add("- `${it.id}` — ${it.name}: ${it.role}${if (it.isGod) " (orchestrator)" else ""}") }
        add("")
    }.joinToString("\n")

    private fun capabilityLine(c: HiveCapabilities): String =
        (listOfNotNull(c.bundle?.let { "bundle:$it" }) + c.skills + c.connections.map { "conn:$it" } + c.mcpServers.map { "mcp:$it" })
            .joinToString(", ").ifBlank { "—" }

    fun initialMemory(agent: HiveAgent): String =
        "# Memory — ${agent.name} (${agent.id})\n\n_Append durable facts, decisions, and context below._\n"

    fun initialBoard(): String = "# Hive board\n\n_Shared plans live here. The god agent is the scribe._\n"

    /** System prompt appended to every hive agent (munder `injectedPrompt`). */
    fun injectedPrompt(agent: HiveAgent, root: String, godName: String): String {
        val dir = "$root/agents/${agent.id}"
        val lines = mutableListOf(
            "You are \"${agent.name}\" (${agent.id}), an autonomous agent in a collaborating hive of coding agents.",
            "Your private workspace is $dir. The shared hive is $root. Full protocol: $root/PROTOCOL.md.",
            "",
            "HIVE PROTOCOL — follow it every task:",
            "1. At the START of a task, read $dir/memory.md and EVERY file in $dir/inbox (messages other agents sent you). After handling an inbox message, move its file into $dir/inbox/.done.",
            "2. Record durable facts, decisions, and context by appending to $dir/memory.md.",
            "3. To ask another agent for something or share information, write ONE message JSON into $dir/outbox (schema in PROTOCOL.md). NEVER write into another agent's folder — the orchestrator delivers your outbox.",
            "4. At the END of a task, append what you learned to memory.md so future-you remembers.",
            GUARDRAILS,
        )
        if (agent.isGod) lines += godLine(root) else lines += WORKER_LINE
        if (agent.role.isNotBlank() && !agent.isGod) lines += "Your role on this floor: ${agent.role}."
        val bundle = agent.capabilities.bundle?.let { id -> RoleBundles.all.firstOrNull { it.id == id } }
        bundle?.let { lines += "Role brief (${it.title}): ${it.role}." }
        if (agent.capabilities.skills.isNotEmpty()) {
            lines += "Skills granted to you (read the SKILL.md when a task matches): " +
                agent.capabilities.skills.joinToString("; ") { id ->
                    val skill = HiveSkills.byId(id)
                    "$root/skills/$id/SKILL.md${skill?.let { " — ${it.description}" }.orEmpty()}"
                }
        }
        if (agent.capabilities.connections.isNotEmpty()) {
            lines += "Connections available in your environment: " + agent.capabilities.connections.joinToString(", ") { id ->
                HiveConnections.byId(id)?.let { "${it.title} (${it.envVars.joinToString("/")})" } ?: id
            } + "."
        }
        lines += CTX_LINE
        lines += "Env vars available to you: AGENT_ID, AGENT_NAME, HIVE_ROOT, AGENT_DIR."
        return lines.filter { it.isNotEmpty() || it == "" }.joinToString("\n")
    }

    private const val GUARDRAILS = "Guardrails: a circuit breaker watches the floor — a \"Circuit breaker: steer/constrain\" message means you are looping or overspending, so STOP repeating, summarize what you tried, and follow it. Be token-frugal (a floor-wide or per-agent token budget can pause you). The shared plan has two parts: board.md (freeform; god is the sole scribe) and tasks.json (structured kanban — todo/doing/blocked/done)."

    private const val WORKER_LINE = "For anything ambiguous, cross-cutting, or needing sign-off, address a message to \"god\"."

    private const val CTX_LINE = "LIVE CONTEXT: each agent row in the LIVE ROSTER carries a `ctx NN%` tag — its live context-window occupancy. Treat it as the real headroom signal when routing: prefer an agent with a LOW `ctx` for a big task; treat a HIGH `ctx` (near 100%) as busy rather than idle."

    private fun godLine(root: String) = "You are the GOD / ORCHESTRATOR of this hive — your job is to ORCHESTRATE, not to implement: maintain live situational awareness and delegate the work. (1) AWARENESS — always know what is going on: keep an accurate picture of every agent, the task board, and all in-flight work; drain your inbox continually and triage every other agent's requests, answering clarifications so the team runs autonomously. (2) DELEGATE — decompose work and fan it out to the hive agents via their inboxes (route messages and assign owners; do not do their jobs); do NOT take on grunt implementation yourself. BEFORE you ask the human to hire anyone, CHECK THE LIVE ROSTER (registry.json + fleet.json) and prefer routing to an EXISTING agent that fits — above all when the request names one. Agents marked ON HOLD are 1:1 with the human: do not dispatch to them. (3) OWN ONLY THE IMPORTANT, high-leverage things — task decomposition, dispatch decisions, sign-offs, conflict resolution, branch integration, and final QA — and remain the sole scribe of board.md. You are otherwise fully autonomous. For the genuinely critical (destructive actions, spending real money, scope changes, unresolvable conflicts), ask the human through an ASK ME card and let the tool-permission prompt gate the action. When you DISPATCH a task, write it as a 4-part contract so the agent can run autonomously: (1) OBJECTIVE — the concrete goal; (2) OUTPUT — the expected deliverable/format; (3) TOOLS — what to use or avoid, and any references to read instead of re-deriving; (4) BOUNDARIES — scope limits + the definition of done. Pass references (file paths, message ids, board sections), not pasted content — keep dispatches short. MONITOR the floor by reading $root/fleet.json (live per-agent tokens, cost, status, what it is doing, last tool, breaker level, inbox backlog) and $root/registry.json. You periodically receive scheduler standup requests — on each, review every agent via fleet.json, re-engage anyone stalled, over-budget, or breaker-armed, and keep board.md and tasks.json accurate. In tasks.json, give every new card the next sequential id (`bmt-<n>`), ALWAYS set its \"assignee\" to the worker's agent id the moment you dispatch it, and NEVER clear it on status changes. HUMAN FEEDBACK is first-class in the ledger: when a task can only proceed with the human's input — a QUESTION to answer OR an ACTION only the human can perform — set its status to \"blocked\" and append the concrete ask to the card's \"humanQA\" array (push {\"q\":\"...\",\"askedAt\":\"<iso>\"}; keep every past entry). WRITE THE ASK SHORT AND IN MARKDOWN: open with ONE **bold** sentence saying exactly what you need, put paths/commands/values in `backticks`, give each option its own \"-\" bullet. When the ask originates in another agent's report, REWRITE it into that shape. The human's answer lands in the same entry (\"a\") AND arrives as an inbox message to you — read it, act on it, and unblock the card. Never sit waiting on the human in your own session. Steward the token budget: your own spend has a cap and the breaker stops you when you cross it."

    /** Typed once into a freshly spawned (non-resumed) god session. */
    fun godOrientation(godName: String) = """
You're online as $godName, the orchestrator of the hive. Get oriented, then start running the floor:
1. Read your memory.md and drain every message in your inbox.
2. Review board.md + tasks.json and the current roster of agents.
3. Check fleet health: read fleet.json in the hive root for every agent's live tokens, cost, status, breaker level, and inbox backlog. Flag anyone stalled, over-budget, or breaker-armed.
Then begin orchestrating: triage requests, delegate work to the team, and keep everyone unblocked. You are fully autonomous — handle tool-permission prompts in this session yourself.
""".trim()

    /** First turn typed into a worker whose CLI cannot take an appended system prompt. */
    fun seedPrompt(agent: HiveAgent, root: String, godName: String) =
        injectedPrompt(agent, root, godName) + "\n\nAcknowledge briefly, then read your memory.md and inbox and wait for work."

    const val NUDGE_PREFIX = "You have new hive inbox message(s)"

    fun inboxNudge(ids: List<String>) =
        "$NUDGE_PREFIX — at least: ${ids.take(4).joinToString(", ")}. Read your inbox, act on what is pending there, and move handled ones to inbox/.done/. Your inbox directory is authoritative: work everything still pending in it, and if a named id is already in inbox/.done/ you handled it on an earlier turn and can ignore that one. Act autonomously; only message god if you genuinely need a decision."

    fun workOrder(m: HiveMessage) = """
WORK ORDER FROM HIVE
Message: ${m.id}
From: ${m.from}
Subject: ${m.subject}
Act: ${m.act.wire}${if (m.requiresReply) " (reply expected)" else ""}
Issued: ${m.createdAt}

${m.body}

Notes:
- This arrived through your terminal because this CLI does not read the hive inbox itself.
- Work in your current cwd.
- When done, report changes, validation, blockers, and next step in this terminal.
""".trim()

    fun humanAnswer(task: HiveTask, q: String, a: String) = """
The human answered the open question on task ${task.id} ("${task.title}"):
Q: $q
A: $a
The answer is also recorded in the card's humanQA. Act on it, unblock the card, and continue the work.
""".trim()

    fun steerBody(reason: String) = "Automated guardrail: $reason. Re-check your approach — if you're looping or stuck, STOP repeating, summarize what you've tried, and ask god for direction."
    fun constrainBody(reason: String) = "Automated guardrail escalated: $reason. Stop active work now: switch to read-only/plan, write a short plan of your next step, and send it to god for sign-off BEFORE running more tools."

    const val STANDUP = "Hourly ops standup. Review every agent: who is doing what, and confirm each is still running (not stalled or idle-stale). Check the task board — are in-flight tasks on track, and is anything blocked or unowned? Flag stale agents and at-risk tasks, and keep the board accurate."
    const val COMPACT_RULE = "Keep the current task, recent decisions, open questions, and file paths in play. Drop resolved tangents."

    /** Claude Code `--settings` file wiring every lifecycle hook to the hive hook. */
    fun claudeSettings(agent: HiveAgent, root: String): String {
        fun hook(event: String, matcher: Boolean) = JSONArray().put(
            JSONObject().apply {
                if (matcher) put("matcher", "*")
                put("hooks", JSONArray().put(JSONObject().put("type", "command").put("command", "$NODE $root/bin/hive-hook.js $event").put("timeout", 5)))
            },
        )
        val hooks = JSONObject()
            .put("SessionStart", hook("SessionStart", false))
            .put("UserPromptSubmit", hook("UserPromptSubmit", false))
            .put("PreToolUse", hook("PreToolUse", true))
            .put("PostToolUse", hook("PostToolUse", true))
            .put("Notification", hook("Notification", false))
            .put("Stop", hook("Stop", false))
        return JSONObject().put("hooks", hooks).toString(2)
    }

    /** MCP config for `--mcp-config` from the agent's granted servers. */
    fun mcpConfig(agent: HiveAgent, cwd: String): String? {
        val servers = agent.capabilities.mcpServers.mapNotNull { HiveMcpCatalog.byId(it) }
        if (servers.isEmpty()) return null
        val map = JSONObject()
        servers.forEach { s ->
            map.put(
                s.id,
                JSONObject().put("command", s.command).put("args", JSONArray(s.args.map { it.replace("{cwd}", cwd) })),
            )
        }
        return JSONObject().put("mcpServers", map).toString(2)
    }

    /**
     * The hook every Claude hive agent runs (node, no dependencies). It never
     * blocks Stop — inbox mail is delivered later by the idle-only queue — and:
     * - writes `status.json` (what the agent is doing, in words) and `events.jsonl` (breaker signals);
     * - denies tools while paused/gated and stops the agent when halted (`control.json`);
     * - injects one queued operator steer, the standing goal and, for god, the live roster.
     */
    fun hookScript(): String = """
#!/usr/bin/env node
// Hivebench hive hook. Generated; edits are overwritten.
'use strict';
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const event = process.argv[2] || '';
const dir = process.env.AGENT_DIR;
const root = process.env.HIVE_ROOT;
const id = process.env.AGENT_ID;
let input = '';
process.stdin.on('data', (c) => { input += c; if (input.length > 262144) input = input.slice(0, 262144); });
process.stdin.on('end', () => { try { main(); } catch (e) { out({}); } });
function out(o) { process.stdout.write(JSON.stringify(o)); process.exit(0); }
function readJson(f, d) { try { return JSON.parse(fs.readFileSync(f, 'utf8')); } catch (_) { return d; } }
function writeAtomic(f, text) { const t = f + '.' + process.pid + '.tmp'; fs.writeFileSync(t, text); fs.renameSync(t, f); }
function short(s, n) { s = String(s || '').replace(/\s+/g, ' ').trim(); return s.length > n ? s.slice(0, n - 1) + '…' : s; }
function base(p) { return p ? path.basename(String(p)) : ''; }
function describe(tool, i) {
  i = i || {};
  switch (tool) {
    case 'Read': return 'Reading ' + base(i.file_path);
    case 'Edit': case 'MultiEdit': return 'Editing ' + base(i.file_path);
    case 'Write': return 'Writing ' + base(i.file_path);
    case 'Bash': return 'Running `' + short(i.command, 60) + '`';
    case 'Grep': return 'Searching for "' + short(i.pattern, 40) + '"';
    case 'Glob': return 'Finding files ' + short(i.pattern, 40);
    case 'WebFetch': return 'Fetching ' + short(i.url, 50);
    case 'WebSearch': return 'Searching the web for "' + short(i.query, 40) + '"';
    case 'Task': case 'Agent': return 'Delegating: ' + short(i.description || i.prompt, 50);
    case 'TodoWrite': return 'Updating its todo list';
    default: return 'Using ' + tool;
  }
}
function status(state, doing, tool) {
  if (!dir) return;
  try { writeAtomic(path.join(dir, 'status.json'), JSON.stringify({ state, doing, tool: tool || null, at: Date.now() })); } catch (_) {}
}
function takeSteer() {
  try {
    const sd = path.join(dir, 'steer');
    const files = fs.readdirSync(sd).filter((f) => f.endsWith('.txt')).sort();
    if (!files.length) return '';
    const f = path.join(sd, files[0]);
    const text = fs.readFileSync(f, 'utf8');
    fs.unlinkSync(f);
    return '[OPERATOR STEER — from the human on the floor]\n' + text.trim();
  } catch (_) { return ''; }
}
function goalContext(force) {
  try {
    const goal = fs.readFileSync(path.join(dir, 'goal.md'), 'utf8').trim();
    const seenFile = path.join(dir, '.goal-seen');
    const hash = crypto.createHash('sha1').update(goal).digest('hex');
    let seen = ''; try { seen = fs.readFileSync(seenFile, 'utf8'); } catch (_) {}
    if (!force && seen === hash) return '';
    fs.writeFileSync(seenFile, hash);
    if (!goal) return seen ? '<goal>\n[Cleared by the operator. Stop following the previous standing goal.]\n</goal>' : '';
    return '<goal>\n' + goal + '\n</goal>';
  } catch (_) { return ''; }
}
function rosterContext() {
  const fleet = readJson(path.join(root, 'fleet.json'), null);
  if (!fleet || !Array.isArray(fleet.agents)) return '';
  const age = Math.max(0, Math.round((Date.now() - (fleet.at || 0)) / 1000));
  const rows = fleet.agents.slice(0, 24).map((a) => {
    const bits = [a.role, a.state, a.doing ? 'doing: ' + short(a.doing, 60) : '', a.tokens ? Math.round(a.tokens / 1000) + 'k tok' : '',
      a.cost ? '$' + a.cost.toFixed(2) : '', a.inbox ? 'inbox ' + a.inbox : '', a.breaker && a.breaker !== 'healthy' ? 'breaker ' + a.breaker : '',
      a.id === id ? 'you' : '', a.onHold ? 'ON HOLD — 1:1 with the human' : '', a.paused ? 'PAUSED' : '', a.ctx ? 'ctx ' + a.ctx + '%' : ''].filter(Boolean);
    return a.id + ' "' + a.name + '" (' + bits.join(', ') + ')';
  });
  const more = fleet.agents.length > 24 ? '; +' + (fleet.agents.length - 24) + ' more' : '';
  return '[LIVE ROSTER — auto-injected from ' + root + '/fleet.json, snapshot ' + age + 's old] ' + fleet.agents.length +
    ' agent(s): ' + rows.join('; ') + more + '. This is the CURRENT floor and it SUPERSEDES any roster earlier in this conversation. Route work to someone on this list; ask the human to hire only when nobody fits.';
}
function main() {
  let p = {}; try { p = JSON.parse(input || '{}'); } catch (_) {}
  if (!dir || !id) return out({});
  const control = readJson(path.join(dir, 'control.json'), {});
  const floor = readJson(path.join(root, 'control.json'), {});
  if (control.halted || floor.halted) {
    status('halted', 'Halted by the operator', null);
    return out({ continue: false, stopReason: 'Halted by the operator from the floor.' });
  }
  const tool = p.tool_name || '';
  if (event === 'PreToolUse') {
    if (control.paused) {
      status('paused', 'Paused by the operator', tool);
      return out({ hookSpecificOutput: { hookEventName: 'PreToolUse', permissionDecision: 'deny', permissionDecisionReason: 'Paused by operator — resume from the floor to continue.' } });
    }
    if (Array.isArray(control.gatedTools) && control.gatedTools.includes(tool)) {
      return out({ hookSpecificOutput: { hookEventName: 'PreToolUse', permissionDecision: 'deny', permissionDecisionReason: 'Tool ' + tool + ' is gated by the operator.' } });
    }
    status('working', describe(tool, p.tool_input), tool);
    return out({});
  }
  if (event === 'PostToolUse') {
    try {
      const resp = p.tool_response;
      const err = !!(resp && (resp.is_error || resp.error || (typeof resp === 'string' && /^error/i.test(resp))));
      const key = tool + ':' + crypto.createHash('sha256').update(JSON.stringify(p.tool_input || {}).slice(0, 4096)).digest('hex').slice(0, 16);
      fs.appendFileSync(path.join(dir, 'events.jsonl'), JSON.stringify({ at: Date.now(), tool, key, err }) + '\n');
    } catch (_) {}
    const steer = takeSteer();
    if (steer) return out({ hookSpecificOutput: { hookEventName: 'PostToolUse', additionalContext: steer } });
    return out({});
  }
  if (event === 'SessionStart' || event === 'UserPromptSubmit') {
    status('working', event === 'SessionStart' ? 'Starting up' : 'Thinking', null);
    const parts = [];
    if (id === 'god') { const r = rosterContext(); if (r) parts.push(r); }
    const g = goalContext(event === 'SessionStart'); if (g) parts.push(g);
    const s = takeSteer(); if (s) parts.push(s);
    if (!parts.length) return out({});
    return out({ hookSpecificOutput: { hookEventName: event, additionalContext: parts.join('\n\n') } });
  }
  if (event === 'Notification') {
    const msg = String(p.message || '');
    if (/permission|approve|confirm|needs your/i.test(msg) && !/waiting for your input/i.test(msg)) status('waiting', 'Waiting on you: ' + short(msg, 80), null);
    else status('idle', 'Idle — waiting for work', null);
    return out({});
  }
  if (event === 'Stop') {
    if (p.stop_hook_active) return out({});
    status('idle', 'Finished — idle', null);
    return out({});
  }
  return out({});
}
""".trimStart()
}

/** Skills shipped with the hive, written to `.hive/skills/<id>/SKILL.md`. */
data class HiveSkill(val id: String, val title: String, val description: String, val body: String)

object HiveSkills {
    val all = listOf(
        HiveSkill(
            "hive-sync", "Hive sync", "keep memory, inbox and the task card in sync at every task boundary",
            "1. Start: read memory.md, drain inbox/ (move handled files to inbox/.done/).\n2. Set your card's status in tasks.json to `doing`.\n3. End: append what you learned to memory.md, set the card `done` (or `blocked` with a humanQA ask via god), and send god a `done` message with a 3-line summary and file references.",
        ),
        HiveSkill(
            "test-first", "Test first", "reproduce a bug with a failing test before fixing it",
            "1. Reproduce the defect in the smallest failing test.\n2. Run it and confirm it fails for the right reason.\n3. Fix the code, re-run the test and the surrounding suite.\n4. Report the test name and the command you ran.",
        ),
        HiveSkill(
            "code-review", "Code review", "review a diff for correctness, security and conventions",
            "Review with `git diff` against the base branch. For each finding give file:line, the concrete failure scenario and a fix. Rank by severity; skip style nits unless they hide a bug. Send the review to the author (act: inform) and god.",
        ),
        HiveSkill(
            "audit", "Audit", "security, license and secrets audit of the working tree",
            "Scan for committed secrets, risky dependencies, unsafe shell/SQL/HTML construction and license conflicts. Report each with evidence and a remediation. Never print a secret in full.",
        ),
        HiveSkill(
            "fetch-summarize", "Fetch & summarize", "read external sources and condense them into references",
            "Fetch only what the task needs. Summarize into memory.md as bullet facts with the source URL. Pass references to other agents, not pasted pages.",
        ),
        HiveSkill(
            "release-notes", "Release notes", "turn merged work into user-facing release notes",
            "Collect done cards and commits since the last tag. Group into Added / Changed / Fixed. One line each, user-facing language, no internal ids.",
        ),
        HiveSkill(
            "design-check", "Design check", "verify UI against the design system and accessibility",
            "Check spacing, colour tokens, typography, touch targets (48dp), contrast and dark mode. Report deviations with screenshots or file references.",
        ),
        HiveSkill(
            "plan-first", "Plan first", "write a short plan before touching code",
            "Before editing: list the files you will change, the approach, the risks and the definition of done. Send it to god as a `propose` when the change is cross-cutting.",
        ),
    )

    fun byId(id: String) = all.firstOrNull { it.id == id }
}

/** A connection hands an agent credentials through environment variables. */
data class HiveConnection(val id: String, val title: String, val envVars: List<String>, val note: String)

object HiveConnections {
    val all = listOf(
        HiveConnection("github", "GitHub", listOf("GH_TOKEN", "GITHUB_PERSONAL_ACCESS_TOKEN"), "Personal access token for gh and the GitHub MCP server"),
        HiveConnection("linear", "Linear", listOf("LINEAR_API_KEY"), "Linear API key for issue sync"),
        HiveConnection("brave", "Brave Search", listOf("BRAVE_API_KEY"), "Web search for research agents"),
        HiveConnection("slack", "Slack", listOf("SLACK_BOT_TOKEN"), "Bot token for posting updates"),
        HiveConnection("sentry", "Sentry", listOf("SENTRY_AUTH_TOKEN"), "Read crash reports"),
    )

    fun byId(id: String) = all.firstOrNull { it.id == id }
}

/** MCP servers an agent can be granted (node-based, so they run in the phone's runtime). */
data class HiveMcpServer(
    val id: String,
    val title: String,
    val command: String,
    val args: List<String>,
    val safeReadOnly: Boolean,
    val needsConnection: String? = null,
)

object HiveMcpCatalog {
    val all = listOf(
        HiveMcpServer("sequential-thinking", "Sequential thinking", "npx", listOf("-y", "@modelcontextprotocol/server-sequential-thinking"), true),
        HiveMcpServer("context7", "Context7 docs", "npx", listOf("-y", "@upstash/context7-mcp"), true),
        HiveMcpServer("filesystem", "Filesystem (project)", "npx", listOf("-y", "@modelcontextprotocol/server-filesystem", "{cwd}"), true),
        HiveMcpServer("memory", "Knowledge-graph memory", "npx", listOf("-y", "@modelcontextprotocol/server-memory"), true),
        HiveMcpServer("github", "GitHub", "npx", listOf("-y", "@modelcontextprotocol/server-github"), false, needsConnection = "github"),
        HiveMcpServer("brave-search", "Brave search", "npx", listOf("-y", "@modelcontextprotocol/server-brave-search"), false, needsConnection = "brave"),
    )

    fun byId(id: String) = all.firstOrNull { it.id == id }
}

/** The eleven role bundles: one write sets an agent's role, skills, connections and MCP servers. */
object RoleBundles {
    val all = listOf(
        RoleBundle("orchestrator", "Orchestrator", "orchestrator (god) — runs the floor, triages requests, escalates only critical calls to you",
            listOf("hive-sync", "plan-first"), emptyList(), listOf("sequential-thinking")),
        RoleBundle("architect", "Architect", "system architect — specs, interfaces and the shared plan",
            listOf("hive-sync", "plan-first"), emptyList(), listOf("sequential-thinking", "context7")),
        RoleBundle("fullstack", "Full-stack developer", "full-stack developer — ships features end to end with tests",
            listOf("hive-sync", "test-first", "plan-first"), listOf("github"), listOf("context7")),
        RoleBundle("frontend", "Frontend / UI", "frontend developer — screens, components and the design system",
            listOf("hive-sync", "design-check", "test-first"), emptyList(), listOf("context7")),
        RoleBundle("backend", "Backend / API", "backend developer — APIs, data and integrations",
            listOf("hive-sync", "test-first", "plan-first"), emptyList(), listOf("context7")),
        RoleBundle("qa", "QA enforcer", "QA enforcer — reproduce, write a failing test, then fix",
            listOf("hive-sync", "test-first"), emptyList(), listOf("sequential-thinking")),
        RoleBundle("reviewer", "PR reviewer", "code reviewer — reviews every diff before it merges",
            listOf("hive-sync", "code-review"), listOf("github"), listOf("github")),
        RoleBundle("security", "Security auditor", "adversarial security auditor — secrets, dependencies, unsafe code",
            listOf("hive-sync", "audit", "code-review"), emptyList(), listOf("sequential-thinking")),
        RoleBundle("docs", "Docs writer", "docs writer — keeps the README and /docs true",
            listOf("hive-sync", "release-notes"), emptyList(), listOf("context7")),
        RoleBundle("release", "Release manager", "release manager — versions, changelogs and shipping",
            listOf("hive-sync", "release-notes", "audit"), listOf("github"), listOf("github")),
        RoleBundle("researcher", "Researcher", "research assistant — reads sources and condenses them into references",
            listOf("hive-sync", "fetch-summarize"), listOf("brave"), listOf("brave-search", "sequential-thinking")),
    )

    fun byId(id: String?) = all.firstOrNull { it.id == id }
}
