package com.hivebench.app.hive

import android.util.Log
import org.json.JSONObject
import java.io.BufferedInputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import kotlin.concurrent.thread

/**
 * Minimal inbound webhook endpoint for the office (munder's webhook trigger):
 * `POST http://<phone-lan-ip>:<port>/` with header `x-md-webhook-secret: <token>`
 * and a JSON body `{"message": "...", "title": "...", "from": "..."}`.
 * Runs only while at least one webhook automation is enabled. Requests without
 * a known secret get 401 and never reach the floor.
 */
class HiveWebhookServer(
    private val port: Int = DEFAULT_PORT,
    private val tokens: () -> Set<String>,
    private val onEvent: (token: String, title: String, body: String, from: String?) -> Unit,
) {
    @Volatile private var server: ServerSocket? = null

    val running: Boolean get() = server?.isClosed == false

    fun start() {
        if (running) return
        val socket = runCatching { ServerSocket(port) }.getOrElse {
            Log.w(TAG, "webhook port $port unavailable", it)
            return
        }
        server = socket
        thread(name = "hive-webhook", isDaemon = true) {
            while (!socket.isClosed) {
                val client = try { socket.accept() } catch (_: SocketException) { break } catch (_: Exception) { continue }
                thread(isDaemon = true) { runCatching { handle(client) } }
            }
        }
    }

    fun stop() {
        runCatching { server?.close() }
        server = null
    }

    private fun handle(client: Socket) = client.use { sock ->
        sock.soTimeout = 10_000
        val input = BufferedInputStream(sock.getInputStream())
        val requestLine = readLine(input) ?: return
        val headers = HashMap<String, String>()
        while (true) {
            val line = readLine(input) ?: break
            if (line.isEmpty()) break
            val idx = line.indexOf(':')
            if (idx > 0) headers[line.substring(0, idx).trim().lowercase()] = line.substring(idx + 1).trim()
        }
        val out = sock.getOutputStream()
        fun respond(code: Int, text: String) {
            val body = JSONObject().put("ok", code in 200..299).put("message", text).toString().toByteArray()
            out.write("HTTP/1.1 $code ${if (code < 300) "OK" else "Error"}\r\nContent-Type: application/json\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n".toByteArray())
            out.write(body)
            out.flush()
        }
        if (!requestLine.startsWith("POST ")) return respond(405, "POST only")
        val secret = headers["x-md-webhook-secret"].orEmpty()
        if (secret.isBlank() || secret !in tokens()) return respond(401, "unknown or missing x-md-webhook-secret")
        val length = headers["content-length"]?.toIntOrNull()?.coerceIn(0, 64 * 1024) ?: 0
        val bytes = ByteArray(length)
        var read = 0
        while (read < length) {
            val n = input.read(bytes, read, length - read)
            if (n < 0) break
            read += n
        }
        val raw = String(bytes, 0, read)
        val json = runCatching { JSONObject(raw) }.getOrNull()
        val message = json?.optString("message")?.ifBlank { null } ?: raw.trim()
        if (message.isBlank()) return respond(400, "body needs a \"message\"")
        onEvent(secret, json?.optString("title")?.ifBlank { null } ?: message.lineSequence().first().take(80), message.take(20_000), json?.optString("from")?.ifBlank { null })
        respond(202, "accepted")
    }

    private fun readLine(input: BufferedInputStream): String? {
        val sb = StringBuilder()
        while (true) {
            val c = input.read()
            if (c < 0) return if (sb.isEmpty()) null else sb.toString()
            if (c == '\n'.code) return sb.toString().trimEnd('\r')
            if (sb.length > 8192) return sb.toString()
            sb.append(c.toChar())
        }
    }

    companion object {
        const val DEFAULT_PORT = 8787
        private const val TAG = "HiveWebhook"

        /** The phone's LAN IPv4 address, for showing the webhook URL. */
        fun lanAddress(): String? = runCatching {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { it.isSiteLocalAddress }?.hostAddress
        }.getOrNull()
    }
}
