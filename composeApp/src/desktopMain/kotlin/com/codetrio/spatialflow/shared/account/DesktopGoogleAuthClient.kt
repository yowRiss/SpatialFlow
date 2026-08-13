package com.codetrio.spatialflow.shared.account

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.net.ServerSocket
import java.net.URI
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** System-browser OAuth with an ephemeral loopback redirect and PKCE. The
 * Google OAuth client must register http://127.0.0.1:<ephemeral-port>/oauth2/callback.
 */
class DesktopGoogleAuthClient : GoogleAuthClient {
    override suspend fun requestAuthorization(clientId: String, scopes: List<String>): GoogleAuthResult = withContext(Dispatchers.IO) {
        if (clientId.isBlank()) return@withContext GoogleAuthResult.Failure("A desktop Google OAuth client ID is required.")
        runCatching {
            ServerSocket(0, 1).use { server ->
                server.soTimeout = 180_000
                val redirectUri = "http://127.0.0.1:${server.localPort}/oauth2/callback"
                val verifier = secureToken(64)
                val state = secureToken(32)
                val challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()))
                val authorizationUrl = buildString {
                    append("https://accounts.google.com/o/oauth2/v2/auth?")
                    append("client_id=").append(encode(clientId))
                    append("&redirect_uri=").append(encode(redirectUri))
                    append("&response_type=code&access_type=offline&prompt=consent")
                    append("&scope=").append(encode(scopes.joinToString(" ")))
                    append("&state=").append(encode(state))
                    append("&code_challenge=").append(encode(challenge)).append("&code_challenge_method=S256")
                }
                if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    return@use GoogleAuthResult.Failure("A system browser is required for Google sign-in.")
                }
                Desktop.getDesktop().browse(URI(authorizationUrl))
                val socket = server.accept()
                socket.use { connection ->
                    val request = connection.getInputStream().bufferedReader().readLine().orEmpty()
                    val target = request.substringAfter(' ').substringBefore(' ')
                    val query = target.substringAfter('?', "").split('&').associate { item ->
                        val (key, value) = item.split('=', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
                        java.net.URLDecoder.decode(key, "UTF-8") to java.net.URLDecoder.decode(value, "UTF-8")
                    }
                    val result = when {
                        query["state"] != state -> GoogleAuthResult.Failure("Google sign-in state did not match.")
                        query["error"] != null -> GoogleAuthResult.Cancelled
                        query["code"] != null -> GoogleAuthResult.AuthorizationCode(query.getValue("code"), redirectUri, verifier)
                        else -> GoogleAuthResult.Failure("Google did not return an authorization code.")
                    }
                    connection.getOutputStream().bufferedWriter().use { output ->
                        output.write("HTTP/1.1 200 OK\\r\\nContent-Type: text/html; charset=utf-8\\r\\nConnection: close\\r\\n\\r\\n")
                        output.write("<html><body><h2>SpatialFlow sign-in complete</h2><p>You can return to SpatialFlow.</p></body></html>")
                    }
                    result
                }
            }
        }.getOrElse { GoogleAuthResult.Failure(it.message ?: "Could not complete Google sign-in.") }
    }

    private fun secureToken(bytes: Int): String = ByteArray(bytes).also(SecureRandom()::nextBytes).let {
        Base64.getUrlEncoder().withoutPadding().encodeToString(it)
    }
    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
}

actual fun createGoogleAuthClient(): GoogleAuthClient = DesktopGoogleAuthClient()
