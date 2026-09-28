package com.hivebench.app.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hivebench.app.runtime.RuntimeInstaller
import com.hivebench.app.ui.neo.CircularProgressIndicator
import com.hivebench.app.ui.neo.MaterialTheme
import com.hivebench.app.ui.neo.Text
import com.hivebench.app.ui.theme.NeoBlack
import com.hivebench.app.ui.theme.NeoLime
import kotlinx.coroutines.launch

private val ShellShortcuts = listOf(
    TerminalQuickAction("git status", "git status\r"),
    TerminalQuickAction("ls -la", "ls -la\r"),
    TerminalQuickAction("npm i", "npm install\r"),
    TerminalQuickAction("npm run dev", "npm run dev\r"),
    TerminalQuickAction("clear", "clear\r"),
)

/**
 * A real login shell in a PTY (bash inside the Linux runtime). `cd`, `export`,
 * history, full-screen programs and long-running dev servers behave as in any
 * terminal, and the session keeps running while other tabs are open.
 */
@Composable
fun ShellTerminal(
    sessionKey: String,
    title: String,
    subtitle: String,
    launch: suspend () -> RuntimeInstaller.GuestLaunch,
    modifier: Modifier = Modifier,
    attachments: TerminalAttachmentTarget? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val info by TerminalSessions.info.collectAsState()
    var error by remember(sessionKey) { mutableStateOf<String?>(null) }

    fun start(restart: Boolean) {
        scope.launch {
            error = runCatching {
                if (!restart && TerminalSessions.get(sessionKey) != null) return@runCatching
                TerminalSessions.restart(context, sessionKey, title, launch())
            }.exceptionOrNull()?.message
        }
    }

    LaunchedEffect(sessionKey) { start(restart = false) }

    Column(modifier.fillMaxSize().imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Black, fontSize = 13.sp)
                Text(subtitle, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            ShellButton("Restart") { start(restart = true) }
        }
        Box(Modifier.fillMaxWidth().weight(1f)) {
            val session = info[sessionKey]
            when {
                error != null -> Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                    Text("Could not start the shell", fontWeight = FontWeight.Black)
                    Text(error.orEmpty(), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                session == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                else -> {
                    AgentTerminalView(sessionKey = sessionKey, modifier = Modifier.fillMaxSize(), quickActions = ShellShortcuts, attachments = attachments)
                    if (!session.running) {
                        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp)) {
                            ShellButton("Shell exited (${session.exitStatus}) · Restart", primary = true) { start(restart = true) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShellButton(label: String, primary: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier.clip(shape)
            .background(if (primary) NeoLime else MaterialTheme.colorScheme.surface)
            .border(1.5.dp, NeoBlack, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (primary) NeoBlack else MaterialTheme.colorScheme.onSurface)
    }
}
