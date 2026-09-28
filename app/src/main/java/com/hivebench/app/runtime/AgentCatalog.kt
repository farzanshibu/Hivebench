package com.hivebench.app.runtime

import com.hivebench.app.model.AgentKind
import com.hivebench.app.terminal.TerminalQuickAction

/**
 * What the app can tell about an agent's sign-in without running it.
 * [account] is null when the agent keeps credentials somewhere we cannot detect.
 */
data class AgentAuthState(val account: Boolean?, val apiKeyEnvs: Set<String>) {
    val ready: Boolean get() = account == true || apiKeyEnvs.isNotEmpty()
}

enum class AgentLaunchPurpose { SESSION, LOGIN, API_KEY_LOGIN }

/** Where an agent's executable comes from. */
sealed interface AgentPackage {
    /** Installed by an existing RuntimeInstaller overlay (Claude Code, DeepSeek Harness, Antigravity). */
    data object Bundled : AgentPackage

    /** Global npm package; npm links [bin] into /usr/local/bin. */
    data class Npm(val name: String, val bin: String) : AgentPackage

    /** GitHub release tarball containing [bin], verified against the release's SHA256SUMS. */
    data class GitHubRelease(val repo: String, val asset: String, val bin: String) : AgentPackage

    /** Runs a user-supplied shell command; nothing to install. */
    data object UserCommand : AgentPackage
}

/** An API key the agent reads from an environment variable. */
data class AgentApiKey(
    val envVar: String,
    val label: String,
    /** Optional companion variable for a custom endpoint (e.g. ANTHROPIC_BASE_URL). */
    val baseUrlEnv: String? = null,
)

/**
 * Everything the app needs to install, launch and sign in to one agent. The
 * agent's own TUI owns slash commands, modes, model choice and usage display.
 */
data class AgentSpec(
    val kind: AgentKind,
    val pkg: AgentPackage,
    /** Guest argv that starts the interactive TUI. */
    val launch: List<String>,
    /** Guest argv for account sign-in, or null when sign-in happens inside the TUI. */
    val login: List<String>?,
    /** Shown on the sign-in screen, explaining what happens. */
    val loginHint: String,
    /** Guest paths whose presence means an account is signed in; empty = cannot tell. */
    val credentialFiles: List<String>,
    val apiKeys: List<AgentApiKey>,
    /** Guest command (run with the key in its environment) that stores an API key in the agent's own config. */
    val apiKeyLogin: String? = null,
    /** Slash command that shows plan usage / rate limits in the TUI. */
    val usageCommand: String? = null,
    val quickActions: List<TerminalQuickAction> = emptyList(),
    val environment: Map<String, String> = emptyMap(),
    /**
     * PRoot's hard-link emulation (`--link2symlink`) turns an atomic
     * write-temp-then-rename save into a dangling `.l2s` link, losing the file.
     * Agents that save that way must run without it.
     */
    val emulateHardLinks: Boolean = true,
    /** Extra argv that resumes the agent's most recent conversation on restart. */
    val resumeArgs: List<String> = emptyList(),
) {
    val supportsAccountLogin: Boolean get() = login != null || loginHint.isNotBlank()

    /** Where the agent is installed from, shown before it is installed. */
    val sourceLabel: String
        get() = when (pkg) {
            AgentPackage.Bundled -> "Official build"
            is AgentPackage.Npm -> "npm · ${pkg.name}"
            is AgentPackage.GitHubRelease -> "GitHub · ${pkg.repo}"
            AgentPackage.UserCommand -> "Runs your own command"
        }
}

object AgentCatalog {
    private fun cmd(text: String) = TerminalQuickAction(text, "$text\r")

    private val anthropicKey = AgentApiKey("ANTHROPIC_API_KEY", "Anthropic API key", baseUrlEnv = "ANTHROPIC_BASE_URL")
    private val openAiKey = AgentApiKey("OPENAI_API_KEY", "OpenAI API key", baseUrlEnv = "OPENAI_BASE_URL")
    private val openRouterKey = AgentApiKey("OPENROUTER_API_KEY", "OpenRouter API key")
    private val geminiKey = AgentApiKey("GEMINI_API_KEY", "Gemini API key")
    private val deepSeekKey = AgentApiKey("DEEPSEEK_API_KEY", "DeepSeek API key")

    val specs: Map<AgentKind, AgentSpec> = listOf(
        AgentSpec(
            kind = AgentKind.CLAUDE_CODE,
            pkg = AgentPackage.Bundled,
            launch = listOf(RuntimeInstaller.CLAUDE_GUEST_PATH),
            login = listOf(RuntimeInstaller.CLAUDE_GUEST_PATH, "/login"),
            loginHint = "Opens Claude Code's own /login. Choose your Claude subscription or Console account, open the link, then paste the code back.",
            credentialFiles = listOf("/root/.claude/.credentials.json"),
            apiKeys = listOf(anthropicKey),
            usageCommand = "/usage",
            quickActions = listOf(cmd("/model"), cmd("/usage"), cmd("/compact"), cmd("/clear"), cmd("/resume"), cmd("/help")),
            environment = mapOf("DISABLE_AUTOUPDATER" to "1"),
        ),
        AgentSpec(
            kind = AgentKind.CODEX,
            pkg = AgentPackage.Npm("@openai/codex", "codex"),
            // Codex's app-server daemon records its pid start time from /proc, which
            // PRoot does not emulate; the in-process mode works the same otherwise.
            launch = listOf("/usr/local/bin/codex", "--no-daemon"),
            login = listOf("/usr/local/bin/codex", "login"),
            loginHint = "Runs `codex login`. Open the link to sign in with ChatGPT; the browser returns to Codex on this device.",
            credentialFiles = listOf("/root/.codex/auth.json"),
            apiKeys = listOf(openAiKey),
            apiKeyLogin = "printenv OPENAI_API_KEY | /usr/local/bin/codex login --with-api-key",
            usageCommand = "/status",
            quickActions = listOf(cmd("/model"), cmd("/status"), cmd("/approvals"), cmd("/new"), cmd("/compact"), cmd("/diff")),
            resumeArgs = listOf("resume", "--last"),
        ),
        AgentSpec(
            kind = AgentKind.OPENCODE,
            pkg = AgentPackage.Npm("opencode-ai", "opencode"),
            launch = listOf("/usr/local/bin/opencode"),
            login = listOf("/usr/local/bin/opencode", "auth", "login"),
            loginHint = "Runs `opencode auth login`. Pick a provider and follow its sign-in or paste its key.",
            credentialFiles = listOf("/root/.local/share/opencode/auth.json"),
            apiKeys = listOf(anthropicKey, openAiKey, openRouterKey, geminiKey, deepSeekKey),
            quickActions = listOf(cmd("/models"), cmd("/new"), cmd("/sessions"), cmd("/compact"), cmd("/help")),
            resumeArgs = listOf("--continue"),
        ),
        AgentSpec(
            kind = AgentKind.ANTIGRAVITY,
            pkg = AgentPackage.Bundled,
            launch = listOf(RuntimeInstaller.AGY_GUEST_PATH),
            login = listOf(RuntimeInstaller.AGY_GUEST_PATH),
            loginHint = "Starts Antigravity, which asks you to sign in with Google when needed.",
            credentialFiles = listOf("/root/.gemini/antigravity-cli/antigravity-oauth-token"),
            apiKeys = emptyList(),
            quickActions = listOf(cmd("/model"), cmd("/help")),
            // Selects agy's manual URL + one-time-code flow instead of a local browser.
            environment = mapOf("SSH_CONNECTION" to "127.0.0.1 1 127.0.0.1 1"),
            emulateHardLinks = false,
        ),
        AgentSpec(
            kind = AgentKind.DEEPSEEK_HARNESS,
            pkg = AgentPackage.Bundled,
            launch = listOf("/usr/local/bin/dsh"),
            login = null,
            loginHint = "",
            credentialFiles = emptyList(),
            // dsh natively reads only DEEPSEEK_API_KEY; other routes need a hand-declared provider.
            apiKeys = listOf(deepSeekKey),
            quickActions = listOf(cmd("/help")),
            environment = mapOf("DSH_HOME" to "/root/.dsh"),
            emulateHardLinks = false,
        ),
        AgentSpec(
            kind = AgentKind.CLINE,
            pkg = AgentPackage.Npm("cline", "cline"),
            launch = listOf("/usr/local/bin/cline"),
            login = listOf("/usr/local/bin/cline", "auth"),
            loginHint = "Runs `cline auth` to sign in to Cline or connect a provider.",
            credentialFiles = emptyList(),
            apiKeys = listOf(anthropicKey, openAiKey, openRouterKey, geminiKey),
            quickActions = listOf(cmd("/help")),
        ),
        AgentSpec(
            kind = AgentKind.PI_AGENT,
            pkg = AgentPackage.Npm("@mariozechner/pi-coding-agent", "pi"),
            launch = listOf("/usr/local/bin/pi"),
            login = null,
            loginHint = "Pi signs in from inside its session: run /login and pick Claude, ChatGPT, Copilot or Gemini.",
            credentialFiles = listOf("/root/.pi/agent/auth.json"),
            apiKeys = listOf(anthropicKey, openAiKey, openRouterKey, geminiKey),
            quickActions = listOf(cmd("/login"), cmd("/model"), cmd("/session"), cmd("/compact"), cmd("/hotkeys")),
            resumeArgs = listOf("--continue"),
        ),
        AgentSpec(
            kind = AgentKind.COMMAND_CODE,
            pkg = AgentPackage.Npm("command-code", "command-code"),
            launch = listOf("/usr/local/bin/command-code"),
            login = null,
            loginHint = "Command Code asks you to sign in when it first starts.",
            credentialFiles = emptyList(),
            apiKeys = emptyList(),
            quickActions = listOf(cmd("/help")),
        ),
        AgentSpec(
            kind = AgentKind.JCODE,
            pkg = AgentPackage.GitHubRelease("1jehuang/jcode", "jcode-linux-aarch64.tar.gz", "jcode"),
            launch = listOf("/usr/local/bin/jcode"),
            login = null,
            loginHint = "JCode walks you through provider sign-in when it first starts.",
            credentialFiles = emptyList(),
            apiKeys = listOf(anthropicKey, openAiKey, openRouterKey),
            quickActions = listOf(cmd("/help")),
        ),
        AgentSpec(
            kind = AgentKind.CUSTOM_RUNNER,
            pkg = AgentPackage.UserCommand,
            launch = emptyList(),
            login = null,
            loginHint = "",
            credentialFiles = emptyList(),
            apiKeys = listOf(anthropicKey, openAiKey, openRouterKey, geminiKey, deepSeekKey),
        ),
    ).associateBy { it.kind }

    fun spec(kind: AgentKind): AgentSpec = specs.getValue(kind)

    /** Vault id for an agent's API key stored under [envVar]. */
    fun keyId(kind: AgentKind, envVar: String) = "agent-env:${kind.stableId}:$envVar"
}
